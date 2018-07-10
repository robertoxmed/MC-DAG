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
package fr.tpt.s3.mcdag.bench.multidag;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import fr.tpt.s3.mcdag.alloc.EarlistDeadlineFirstMCSched;
import fr.tpt.s3.mcdag.alloc.FederatedMCSched;
import fr.tpt.s3.mcdag.alloc.LeastLaxityFirstMCSched;
import fr.tpt.s3.mcdag.alloc.SchedulingException;
import fr.tpt.s3.mcdag.model.Actor;
import fr.tpt.s3.mcdag.model.ActorSched;
import fr.tpt.s3.mcdag.model.DAG;
import fr.tpt.s3.mcdag.parser.MCParser;

public class BenchThread implements Runnable {
	
	private Set<DAG> dags;
	private MCParser mcp;
	private String inputFile;
	private String outputFile;
	private boolean debug;
	private int nbCores;
	private FederatedMCSched fedScheduler;
	private LeastLaxityFirstMCSched nlvlScheduler;
	private EarlistDeadlineFirstMCSched edfScheduler;
	
	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}

	private boolean schedFede;
	private boolean schedLax;
	private boolean schedEdf;
	
	public BenchThread (String input, String output, int cores, boolean debug) {
		setInputFile(input);
		dags = new HashSet<DAG>();
		setOutputFile(output);
		setNbCores(cores);
		setDebug(debug);
		setSchedFede(true);
		setSchedLax(true);
		setSchedEdf(true);
		mcp = new MCParser(inputFile, null, dags, false);
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
		int outPreemptsFed = 0;
		int outPreemptsLax = 0;
		int outPreemptsEdf = 0;
		int outActFed = 0;
		int outActLax = 0;
		int outActEdf = 0;
		
		if (isSchedFede()) {
			outBFSched = 1;
			Hashtable<ActorSched, Integer> pFed = fedScheduler.getPreempts();
			for (ActorSched task : pFed.keySet())
				outPreemptsFed += pFed.get(task);
			outActFed = fedScheduler.getActivations();
		}
		
		if (isSchedLax()) {
			outBLSched = 1;
			Hashtable<ActorSched, Integer> pLax = nlvlScheduler.getPreempts();
			for (ActorSched task : pLax.keySet())
				outPreemptsLax += pLax.get(task);
			outActLax = nlvlScheduler.getActivations();
		}
		
		if (isSchedEdf()) {
			outBEDFSched = 1;
			Hashtable<ActorSched, Integer> pEdf = edfScheduler.getPreempts();
			for (ActorSched task : pEdf.keySet())
				outPreemptsEdf += pEdf.get(task);
			outActEdf = edfScheduler.getActivations();
		}
		
		for (DAG d : dags)
			uDAGs += d.getUmax();
		
		output.write(Thread.currentThread().getName()+"; "+getInputFile()+"; "+outBFSched+"; "+outPreemptsFed+"; "+outActFed+"; "
		+outBLSched+"; "+outPreemptsLax+"; "+outActLax+"; "
		+outBEDFSched+"; "+outPreemptsEdf+"; "+outActEdf+"; "+uDAGs+"\n");
		output.close();
	}
	
	private void resetVisited (Set<DAG> sd) {
		for (DAG d : sd) {
			for (Actor a : d.getNodes()) {
				((ActorSched) a).getVisitedL()[0] = false;
				((ActorSched) a).getVisitedL()[1] = false;
			}
		}
	}
	
	@Override
	public void run() {
		mcp.readXML();
		
		// Test federated approach
		// Make a copy of the system instance
		Set<DAG> fedDAGs = new HashSet<DAG>(dags);
		fedScheduler = new FederatedMCSched(fedDAGs, nbCores, debug);
		
		try {
			fedScheduler.buildAllTables();
		} catch (SchedulingException se) {
			setSchedFede(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] FEDERATED non schedulable with "+nbCores+" cores.");
		}
		
		// Test edf
		// Make another copy of the system instance
		Set<DAG> edfDAGs = new HashSet<DAG>(dags);
		edfScheduler = new EarlistDeadlineFirstMCSched(edfDAGs, nbCores, 2, debug);
		
		try {
			resetVisited(dags);
			edfScheduler.buildAllTables();
		} catch (SchedulingException se) {
			setSchedEdf(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] EDF non schedulable with "+nbCores+" cores.");
		}
	
		// Test laxity
		// MultiDAG mdag = new MultiDAG(dags, nbCores, debug);
		nlvlScheduler = new LeastLaxityFirstMCSched(dags, nbCores, 2, debug);
		
		try {
			resetVisited(dags);
			//mdag.buildAllTables();
			nlvlScheduler.buildAllTables();
		} catch (SchedulingException se) {
			setSchedLax(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] LAXITY non schedulable with "+nbCores+" cores.");
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
	public Set<DAG> getDags() {
		return dags;
	}

	public void setDags(Set<DAG> dags) {
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
}
