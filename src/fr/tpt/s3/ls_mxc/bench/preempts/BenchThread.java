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
package fr.tpt.s3.ls_mxc.bench.preempts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import fr.tpt.s3.ls_mxc.alloc.NLevels;
import fr.tpt.s3.ls_mxc.alloc.SchedulingException;
import fr.tpt.s3.ls_mxc.model.ActorSched;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.parser.MCParser;
import fr.tpt.s3.ls_mxc.util.MathMCDAG;

/**
 * Benchmarking thread that allocates a system with a preemptive and
 * non preemptive algorithm
 * @author roberto
 *
 */
public class BenchThread implements Runnable{

	private Set<DAG> dags;
	private MCParser mcp;
	private String inputFile;
	private String outputFile;
	private boolean schedPreempt;
	private boolean schedNoPreempt;
	private boolean debug;
	
	public BenchThread (String input, String output, boolean debug) {
		setInputFile(input);
		setOutputFile(output);
		setDebug(debug);
		dags = new HashSet<DAG>();
		mcp = new MCParser(inputFile, null, dags, false);		
	}
	
	/**
	 * Internal function to count the number of cores needed to schedule the system
	 * @return
	 */
	private int minCores () {
		int ret = 0;
		int hPeriod = 0;
		int[] input = new int[getDags().size()];
		int i = 0;
		double[] us = new double[mcp.getNbLevels()];
		
		for (DAG d : getDags()) {
			input[i] = d.getDeadline();
			i++;
		}
		
		hPeriod = MathMCDAG.lcm(input);
		
		// Calculate utilization of the system per level
		for (int l = 0; l < mcp.getNbLevels(); l++) {
			
			for (DAG d : getDags()) {
				int nbActivations = (int) (hPeriod / d.getDeadline());

				us[l] += d.getUi(l) * nbActivations;  
			}
			us[l] = us[l] / hPeriod;
		}
		
		// Get the max utilization
		for (int l = 0; l < mcp.getNbLevels(); l++) {
			if (ret < us[l])
				ret = (int) (Math.ceil(us[l]));
		}
		
		return ret;
	}
	
	/**
	 * Function that writes the results of number of preemptions on a file
	 * @param nlvl
	 * @param nlvlno
	 * @throws IOException
	 */
	private synchronized void writeResults(NLevels nlvl, NLevels nlvlno) throws IOException{
		Writer output;
		int ctxtSwitchP = 0;
		int ctxtSwitchNP = 0;
		int preemptsP = 0;
		int preemptsNP = 0;
		Set<ActorSched> actorKeys = nlvl.getCtxSwitch().keySet();
		output = new BufferedWriter(new FileWriter(getOutputFile(), true));
		
		for (ActorSched a : actorKeys) {
			ctxtSwitchNP += nlvl.getCtxSwitch().get(a);
			ctxtSwitchNP += nlvlno.getCtxSwitch().get(a);
			preemptsP += nlvl.getPreempts().get(a);
			preemptsNP += nlvl.getPreempts().get(a);
		}
		
		output.write(Thread.currentThread().getName()+"; "+inputFile+"; "+isSchedPreempt()+"; "+nlvl.getActivations()+"; "+
				     ctxtSwitchP+"; "+preemptsP+"; "+isSchedNoPreempt()+"; "+nlvlno.getActivations()+"; "+
					 ctxtSwitchNP+"; "+preemptsNP+"\n");
		
		output.close();
	}
	
	@Override
	public void run() {
		// Read from the file to get the system
		mcp.readXMLNlevels();
		
		int minCores = minCores();
		NLevels nlvlPreempt = new NLevels(dags, minCores, mcp.getNbLevels(), debug);
		NLevels nlvlNoPreempt = new NLevels(dags, minCores, mcp.getNbLevels(), debug);
		
		if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Minimum number of cores = "+minCores);
		
		// Build the preemptive version of the algorithm
		try {
			nlvlPreempt.buildAllTables();
		} catch (SchedulingException se) {
			se.printStackTrace();
			setSchedPreempt(false);
		}
		
		// Build the non preemptive version of the algorithm
		try {
			nlvlNoPreempt.buildAllnonpreempt();
		} catch (SchedulingException se) {
			se.printStackTrace();
			setSchedNoPreempt(false);
		}
		
		// Try to write the results on a file
		try {
			writeResults(nlvlPreempt, nlvlNoPreempt);
		} catch (IOException ie) {
			ie.printStackTrace();
			System.exit(20);
		}
	}

	/*
	 * Getters and setters
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

	public boolean isSchedPreempt() {
		return schedPreempt;
	}

	public void setSchedPreempt(boolean schedPreempt) {
		this.schedPreempt = schedPreempt;
	}

	public boolean isSchedNoPreempt() {
		return schedNoPreempt;
	}

	public void setSchedNoPreempt(boolean schedNoPreempt) {
		this.schedNoPreempt = schedNoPreempt;
	}
}
