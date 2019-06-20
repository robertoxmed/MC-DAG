/*******************************************************************************
 * Copyright (c) 2017, 2018 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package fr.tpt.s3.mcdag.bench;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.parser.MCParser;
import fr.tpt.s3.mcdag.scheduling.SchedulingException;
import fr.tpt.s3.mcdag.scheduling.federated.GenericFederatedMCSched;
import fr.tpt.s3.mcdag.scheduling.federated.impl.EarliestDeadlineFirstFedSched;
import fr.tpt.s3.mcdag.scheduling.galap.GlobalGenericMCScheduler;
import fr.tpt.s3.mcdag.scheduling.galap.impl.EartliestDeadlineFirstMCSched;
import fr.tpt.s3.mcdag.scheduling.galap.impl.HybridMCSched;
import fr.tpt.s3.mcdag.scheduling.galap.impl.LeastLaxityFirstMCSched;

public class BenchThreadDualCriticality implements Runnable {
	
	private Set<McDAG> dags;
	private MCParser mcp;
	private String inputFile;
	private String outputFile;
	private boolean debug;
	private int nbCores;
	
	// Global alap schedulers
	private GlobalGenericMCScheduler llf;
	private GlobalGenericMCScheduler edf;
	private GlobalGenericMCScheduler hybrid;
	
	// Federated schedulers
	private GenericFederatedMCSched fedEdf;

	private boolean schedFede;
	private boolean schedLax;
	private boolean schedEdf;
	private boolean schedHybrid;
	
	public BenchThreadDualCriticality (String input, String output, int cores, boolean debug) {
		setInputFile(input);
		dags = new HashSet<McDAG>();
		setOutputFile(output);
		setNbCores(cores);
		setDebug(debug);
		setSchedFede(true);
		setSchedLax(true);
		setSchedEdf(true);
		setSchedHybrid(true);
		mcp = new MCParser(inputFile, null, null, null, dags, false);
	}
	
	/**
	 * Writes the results of the thread in the text file
	 * @throws IOException 
	 */
	private synchronized void writeResults () throws IOException {
		Writer output;
		double uDAGs = 0.0;
		output = new BufferedWriter(new FileWriter(getOutputFile(), true));
		
		int outBFSched = 0;
		int outBLSched = 0;
		int outBEDFSched = 0;
		int outBHybridSched = 0;
		int outPreemptsFed = 0;
		int outPreemptsLax = 0;
		int outPreemptsEdf = 0;
		int outPreemptsHybrid = 0;
		int outActFed = 0;
		int outActLax = 0;
		int outActEdf = 0;
		int outActHybrid = 0;
		
		if (isSchedFede())
			outBFSched = 1;
		
		if (isSchedLax())
			outBLSched = 1;
		
		if (isSchedEdf())
			outBEDFSched = 1;
		
		if (isSchedHybrid())
			outBHybridSched = 1;
		
		if (isSchedEdf() && isSchedLax() && isSchedFede()) {
			Hashtable<VertexScheduling, Integer> pFed = fedEdf.getPreemptions();
			for (VertexScheduling task : pFed.keySet())
				outPreemptsFed += pFed.get(task);
			outActFed = fedEdf.getActivations();

			Hashtable<VertexScheduling, Integer> pLax = llf.getPreemptions();
			for (VertexScheduling task : pLax.keySet())
				outPreemptsLax += pLax.get(task);
			outActLax = llf.getActivations();
			
			Hashtable<VertexScheduling, Integer> pEdf = edf.getPreemptions();
			for (VertexScheduling task : pEdf.keySet())
				outPreemptsEdf += pEdf.get(task);
			outActEdf = edf.getActivations();
			
			Hashtable<VertexScheduling, Integer> pHybrid = hybrid.getPreemptions();
			for (VertexScheduling task : pHybrid.keySet())
				outPreemptsHybrid += pHybrid.get(task);
			outActHybrid = hybrid.getActivations();
		}
		
		for (McDAG d : dags)
			uDAGs += d.getUmax();
		
		output.write(Thread.currentThread().getName()+"; "+getInputFile()+"; "+outBFSched+"; "+outPreemptsFed+"; "+outActFed+"; "
		+outBLSched+"; "+outPreemptsLax+"; "+outActLax+"; "
		+outBEDFSched+"; "+outPreemptsEdf+"; "+outActEdf+"; "
		+outBHybridSched+"; "+outPreemptsHybrid+"; "+outActHybrid+"; "
		+uDAGs+"\n");
		output.close();
	}
	
	/**
	 * Internal function to reset visits on DAGs
	 * @param sd
	 */
	private void resetVisited (Set<McDAG> sd) {
		for (McDAG d : sd) {
			for (Vertex a : d.getVertices()) {
				((VertexScheduling) a).getVisitedL()[0] = false;
				((VertexScheduling) a).getVisitedL()[1] = false;
			}
		}
	}
	
	@Override
	public void run() {
		mcp.readXML();
		
		// Test federated approach
		// Make a copy of the system instance
		Set<McDAG> fedDAGs = new HashSet<McDAG>(dags);
		fedEdf = new EarliestDeadlineFirstFedSched(fedDAGs, nbCores, 2, debug);
		
		try {
			fedEdf.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedFede(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] FEDERATED non schedulable with "+nbCores+" cores.");
		}
		
		// Test edf
		// Make another copy of the system instance
		Set<McDAG> edfDAGs = new HashSet<McDAG>(dags);
		edf = new EartliestDeadlineFirstMCSched(edfDAGs, nbCores, 2, debug, true);
		
		try {
			resetVisited(edfDAGs);
			edf.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedEdf(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] EDF non schedulable with "+nbCores+" cores.");
		}
	
		// Test laxity
		llf = new LeastLaxityFirstMCSched(edfDAGs, nbCores, 2, debug, true);
		
		try {
			resetVisited(edfDAGs);
			llf.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedLax(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] LAXITY non schedulable with "+nbCores+" cores.");
		}
		
		// Test hybrid
		hybrid = new HybridMCSched(edfDAGs, nbCores, 2, debug, true);
		
		try {
			resetVisited(edfDAGs);
			hybrid.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedHybrid(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] HYBRID non schedulable with "+nbCores+" cores.");
		}
		
		// Write results
		try {
			writeResults();
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Writing results "+nbCores+" cores.");

		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}
	
	/*
	 * Getters & Setters
	 */
	public Set<McDAG> getDags() {
		return dags;
	}

	public void setDags(Set<McDAG> dags) {
		this.dags = dags;
	}

	public MCParser getMcp() {
		return mcp;
	}

	public void setMcp(MCParser mcp) {
		this.mcp = mcp;
	}

	public String getInputFile() {
		return inputFile;
	}

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isSchedFede() {
		return schedFede;
	}

	public void setSchedFede(boolean schedFede) {
		this.schedFede = schedFede;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public boolean isSchedLax() {
		return schedLax;
	}

	public void setSchedLax(boolean schedLax) {
		this.schedLax = schedLax;
	}

	public boolean isSchedEdf() {
		return schedEdf;
	}

	public void setSchedEdf(boolean schedEdf) {
		this.schedEdf = schedEdf;
	}

	public boolean isSchedHybrid() {
		return schedHybrid;
	}

	public void setSchedHybrid(boolean schedHybrid) {
		this.schedHybrid = schedHybrid;
	}
	
	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}
}
