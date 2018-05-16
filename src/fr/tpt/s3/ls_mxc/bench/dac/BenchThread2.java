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
package fr.tpt.s3.ls_mxc.bench.dac;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import fr.tpt.s3.ls_mxc.alloc.SingleDAG;
import fr.tpt.s3.ls_mxc.alloc.MultiDAG;
import fr.tpt.s3.ls_mxc.alloc.SchedulingException;
import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.parser.MCParser;
import fr.tpt.s3.ls_mxc.util.MathMCDAG;

public class BenchThread2 implements Runnable {
	
	private Set<DAG> dags;
	private MCParser mcp;
	private String inputFile;
	private String outputFile;
	private boolean debug;
	
	private boolean schedFede;
	private boolean schedLax;
	
	public BenchThread2 (String input, String output, boolean debug) {
		setInputFile(input);
		dags = new HashSet<DAG>();
		setOutputFile(output);
		setDebug(debug);
		setSchedFede(false);
		setSchedLax(true);
		mcp = new MCParser(inputFile, null, dags, false);
	}
	
	/**
	 * Internal function that calculates the minimum number of cores
	 * to use with a laxity based scheduler
	 * @return
	 */
	private int minCoresLaxity () {
		int ret = 0;
		int hPeriod = 0;
		int[] input = new int[getDags().size()];
		int i = 0;
		double uLO = 0.0;
		double uHI = 0.0;
		double uMax = 0.0;
		
		for (DAG d : getDags()) {
			input[i] = d.getDeadline();
			i++;
		}		
	
		hPeriod = MathMCDAG.lcm(input);
		
		for (DAG d : getDags()) {
			int nbActivations = (int) (hPeriod / d.getDeadline());
			
			for (Actor a : d.getNodes()) {
				if (a.getCI(1) != 0)
					uHI += nbActivations * a.getCI(1);
				uLO += nbActivations * a.getCI(0);
			}
		}
		uLO = uLO / hPeriod;
		uHI = uHI / hPeriod;
		uMax = (uHI > uLO) ? uHI : uLO;
		ret = (int) (Math.ceil(uMax));
		
		return ret;
	}
	
	/**
	 * Checks if all DAGs will be scheduled using EDF
	 * @return
	 */
	private boolean allDAGsEDF (Set<DAG> dags) {
		
		for (DAG d : dags) {
			if (d.getUHI() >= 1 || d.getULO() >= 1)
				return false;
		}
		
		return true;
	}
	
	/**
	 * Writes the results of the thread in the text file
	 * @throws IOException 
	 */
	private synchronized void writeResults (boolean fSched, boolean lSched) throws IOException {
		Writer output;
		double uDAGs = 0.0;
		output = new BufferedWriter(new FileWriter(getOutputFile(), true));
		
		int outBFSched = 0;
		if (isSchedFede())
			outBFSched = 1;
		int outBLSched = 0;
		if (lSched)
			outBLSched = 1;
		
		for (DAG d : dags)
			uDAGs += d.getU();
		
		output.write(Thread.currentThread().getName()+"; "+getInputFile()+"; "+outBFSched+"; "+outBLSched+"; "+uDAGs+";\n");
		output.close();
	}
	
	/**
	 * Tests if all the DAGs are schedulable with the federated approach
	 * @param nbCores
	 * @return
	 * @throws SchedulingException 
	 */
	private void testFederated (int nbCores) {
		Set<DAG> clusteredDAGs = new HashSet<DAG>();
		int coresBudget = nbCores;
		double uRestLO = 0.0;
		double uRestHI = 0.0;
		double uRestMax = 0.0;
		
		for (DAG d : dags) {
			if (d.getUHI() >= 1 || d.getULO() >= 1) {
				clusteredDAGs.add(d);
				coresBudget -= d.getMinCores();
			} else {
				uRestLO += d.getULO();
				uRestHI += d.getUHI();
			}
		}
		uRestMax += (uRestHI > uRestLO) ? uRestHI : uRestLO;
		coresBudget -= (int) Math.ceil(uRestMax);
		
		if (coresBudget < 0) {
			setSchedFede(false);
			return;
		}
		
		for (DAG d : clusteredDAGs) {
			SingleDAG ls = new SingleDAG(d.getDeadline(), d.getMinCores(), d);
			try {
				ls.CheckBaruah();
			} catch (SchedulingException se) {
				setSchedFede(false);
				return;
			}
		}
		this.setSchedFede(true);
	}
	
	@Override
	public void run() {
		mcp.readXML();
		int nbCores = minCoresLaxity();
		
		// Test federated approach		
		if (allDAGsEDF(dags)) {
			setSchedFede(true);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] FEDERATED all DAGs with EDF.");
		} else {
			testFederated(nbCores);
		}
	
		// Test laxity
		MultiDAG mdag = new MultiDAG(dags, nbCores, false);
		
		try {
			mdag.allocAll();
		} catch (SchedulingException se) {
			setSchedLax(false);
			if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] LAXITY non schedulable with "+nbCores+" cores.");
		}
		
		// Write results
		try {
			writeResults(schedFede, schedLax);
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


}
