/*******************************************************************************
 * Copyright (c) 2018 Roberto Medina
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

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.parser.MCParser;
import fr.tpt.s3.mcdag.scheduling.SchedulingException;
import fr.tpt.s3.mcdag.scheduling.federated.GenericFederatedMCSched;
import fr.tpt.s3.mcdag.scheduling.federated.impl.EarliestDeadlineFirstFedSched;
import fr.tpt.s3.mcdag.scheduling.federated.impl.EarliestDeadlineZeroLaxityFedSched;
import fr.tpt.s3.mcdag.scheduling.federated.impl.LeastLaxityFirstFedSched;
import fr.tpt.s3.mcdag.scheduling.galap.GlobalGenericMCScheduler;
import fr.tpt.s3.mcdag.scheduling.galap.impl.EarlistDeadlineZeroLaxityMCSched;
import fr.tpt.s3.mcdag.scheduling.galap.impl.EartliestDeadlineFirstMCSched;
import fr.tpt.s3.mcdag.scheduling.galap.impl.LeastLaxityFirstMCSched;

public class BenchThreadNLevels implements Runnable {

	private Set<McDAG> dags;
	private MCParser mcp;
	private String inputFile;
	private String outputFile;
	private boolean debug;
	private int nbCores;
	
	// Global alap schedulers
	private GlobalGenericMCScheduler llf;
	private GlobalGenericMCScheduler edf;
	private GlobalGenericMCScheduler ezl;
	private boolean schedLax;
	private boolean schedEdf;
	private boolean schedEzl;
	
	// Federated schedulers
	private GenericFederatedMCSched fedLlf;
	private GenericFederatedMCSched fedEdf;
	private GenericFederatedMCSched fedEzl;
	private boolean schedFedLax;
	private boolean schedFedEdf;
	private boolean schedFedEzl;
	
	public BenchThreadNLevels(String input, String output, int cores, boolean debug) {
		setInputFile(input);
		dags = new HashSet<McDAG>();
		setOutputFile(output);
		setNbCores(cores);
		setDebug(debug);
		setSchedLax(true);
		setSchedEdf(true);
		setSchedEzl(true);
		setSchedFedEdf(true);
		setSchedFedLax(true);
		setSchedFedEzl(true);
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
		
		int outBLSched = 0;
		int outBEDFSched = 0;
		int outBEZLSched = 0;
		int outPreemptsLax = 0;
		int outPreemptsEdf = 0;
		int outPreemptsEzl = 0;
		int outActLax = 0;
		int outActEdf = 0;
		int outActEzl = 0;
		
		// Federated variables
		int outSchedFedEdf = 0;
		int outSchedFedLax = 0;
		int outSchedFedEzl = 0;
		
		if (isSchedLax())
			outBLSched = 1;
		
		if (isSchedEdf())
			outBEDFSched = 1;
		
		if (isSchedEzl())
			outBEZLSched = 1;
		
		Hashtable<VertexScheduling, Integer> pLax = llf.getPreemptions();
		for (VertexScheduling task : pLax.keySet())
			outPreemptsLax += pLax.get(task);
		outActLax = llf.getActivations();
			
		Hashtable<VertexScheduling, Integer> pEdf = edf.getPreemptions();
		for (VertexScheduling task : pEdf.keySet())
			outPreemptsEdf += pEdf.get(task);
		outActEdf = edf.getActivations();
		
		Hashtable<VertexScheduling, Integer> pEzl = ezl.getPreemptions();
		for (VertexScheduling task : pEzl.keySet())
			outPreemptsEzl += pEzl.get(task);
		outActEzl = ezl.getActivations();
		
		for (McDAG d : dags)
			uDAGs += d.getUmax();
		
		if (isSchedFedEdf())
			outSchedFedEdf = 1;
		if (isSchedFedLax())
			outSchedFedLax = 1;
		if (isSchedFedEzl())
			outSchedFedEzl = 1;
		
		output.write(Thread.currentThread().getName()+"; "+getInputFile()+"; "
		+outBLSched+"; "+outPreemptsLax+"; "+outActLax+"; "
		+outBEDFSched+"; "+outPreemptsEdf+"; "+outActEdf+"; "
		+outBEZLSched+"; "+outPreemptsEzl+"; "+outActEzl+"; "
		+outSchedFedLax+"; "+outSchedFedEdf+"; "+outSchedFedEzl+"; "
		+uDAGs+"\n");
		output.close();
	}
	
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
		
		/*
		 * TEST GLOBAL ALAP IMPLEMENTATIONS
		 */
		
		// Test edf
		// Make another copy of the system instance
		edf = new EartliestDeadlineFirstMCSched(getDags(), nbCores, mcp.getNbLevels(), debug, true);
		try {
			resetVisited(getDags());
			edf.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedEdf(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] EDF non schedulable with "+nbCores+" cores.");
		}
	
		// Test laxity
		llf = new LeastLaxityFirstMCSched(getDags(), nbCores, mcp.getNbLevels(), debug, true);
		try {
			resetVisited(getDags());
			llf.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedLax(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] LAXITY non schedulable with "+nbCores+" cores.");
		}
		
		// Test ezl
		ezl = new EarlistDeadlineZeroLaxityMCSched(getDags(), nbCores, mcp.getNbLevels(), debug, true);
		try {
			resetVisited(getDags());
			ezl.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedEzl(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] HYBRID non schedulable with "+nbCores+" cores.");
		}
		
		/*
		 * TEST FEDERATED IMPLEMENTATIONS
		 */
		fedEdf = new EarliestDeadlineFirstFedSched(dags, nbCores, mcp.getNbLevels(), debug);
		try {
			resetVisited(getDags());
			fedEdf.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedFedEdf(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] FED-EDF non schedulable with "+nbCores+" cores.");

		}
		
		fedLlf = new LeastLaxityFirstFedSched(dags, nbCores, mcp.getNbLevels(), debug);
		try {
			resetVisited(getDags());
			fedLlf.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedFedLax(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] FED-LLF non schedulable with "+nbCores+" cores.");
		}
		
		fedEzl = new EarliestDeadlineZeroLaxityFedSched(dags, nbCores, mcp.getNbLevels(), debug);
		try {
			resetVisited(getDags());
			fedEzl.scheduleSystem();
		} catch (SchedulingException se) {
			setSchedFedEzl(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] FED-EZL non schedulable with "+nbCores+" cores.");
		}
		
		// Write results
		try {
			writeResults();
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Writing results "+nbCores+" cores.");

		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

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

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}

	public GlobalGenericMCScheduler getLlf() {
		return llf;
	}

	public void setLlf(GlobalGenericMCScheduler llf) {
		this.llf = llf;
	}

	public GlobalGenericMCScheduler getEdf() {
		return edf;
	}

	public void setEdf(GlobalGenericMCScheduler edf) {
		this.edf = edf;
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

	public boolean isSchedEzl() {
		return schedEzl;
	}

	public void setSchedEzl(boolean schedHybrid) {
		this.schedEzl = schedHybrid;
	}

	public boolean isSchedFedLax() {
		return schedFedLax;
	}

	public void setSchedFedLax(boolean schedFedLax) {
		this.schedFedLax = schedFedLax;
	}

	public boolean isSchedFedEdf() {
		return schedFedEdf;
	}

	public void setSchedFedEdf(boolean schedFedEdf) {
		this.schedFedEdf = schedFedEdf;
	}

	public boolean isSchedFedEzl() {
		return schedFedEzl;
	}

	public void setSchedFedEzl(boolean schedFedEzl) {
		this.schedFedEzl = schedFedEzl;
	}
	
}
