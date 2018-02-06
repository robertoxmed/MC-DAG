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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.tpt.s3.ls_mxc.alloc.SingleDAG;
import fr.tpt.s3.ls_mxc.alloc.MultiDAG;
import fr.tpt.s3.ls_mxc.alloc.SchedulingException;
import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.parser.MCParser;
import fr.tpt.s3.ls_mxc.util.MathMCDAG;

public class BenchThread implements Runnable {

	private Set<DAG> dags;
	private MCParser mcp;
	private String inputFile;
	private String outputFile;
	private boolean debug;
	
	private boolean schedFede;
	
	public BenchThread (String input, String output, boolean debug) {
		setInputFile(input);
		dags = new HashSet<DAG>();
		setOutputFile(output);
		setDebug(debug);
		setSchedFede(true);
		mcp = new MCParser(inputFile, null, dags, false);
	}
	
	/**
	 * Internal function that calculates the minimum number of cores to use
	 * with a federated scheduler
	 * @return
	 */
	private int minCoresBaruah () {
		int ret = 0;
		double uRest = 0.0;
		
		for (DAG d : dags) {
			double uMax = 0.0;
			double uLO = 0.0;
			double uHI = 0.0;
			
			uLO = d.getULO();
			uHI = d.getUHI();
			uMax = (uHI > uLO) ? uHI : uLO;
			
			if (uMax > 1) 
				ret += (int) Math.ceil(uMax);
			else 
				uRest += uMax;
		}
		
		return ret += (int) Math.ceil(uRest);
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
	 * Writes the results of the thread in the text file
	 * @throws IOException 
	 */
	private synchronized void writeResults (int fCores, boolean fSched, int lCores, boolean lSched) throws IOException {
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
		
		output.write(Thread.currentThread().getName()+"; "+getInputFile()+"; "+fCores+"; "+outBFSched+"; "+lCores+"; "+outBLSched+"; "+uDAGs+";\n");
		output.close();
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
	
	
	private boolean testFederated (DAG d, int cores) throws SchedulingException {
		boolean ret = false;
		
		SingleDAG ls = new SingleDAG(d.getDeadline(), cores, d);
		
		ret = ls.CheckBaruah();
		
		return ret;
	}
	
	private int testSystemFederated (int maxCores) {
		int ret = 0;
		int testedCores = 0;
		Set<DAG> clusteredDAGs = new HashSet<DAG>();
		Map<DAG, Boolean> dagMap = new HashMap<DAG, Boolean>();
		
		// Test schedulability of all DAGs with the federated approach
		for (DAG d : dags) {
			if (d.getUHI() >= 1 || d.getULO() >= 1) {
				Boolean b = new Boolean(false);
				clusteredDAGs.add(d);
				dagMap.put(d,b);
				testedCores += d.getMinCores();
			}
		}
		
		while (testedCores < maxCores) {
			for (DAG d : clusteredDAGs) {
				int maxQuota = d.getMinCores() * 2;
				int addedQuota = 0;
				boolean schedFed = false;
				
				while (!schedFed && addedQuota < maxQuota - d.getMinCores()) {
					try {
						schedFed = testFederated(d, d.getMinCores() + addedQuota);
					} catch (SchedulingException se) {
						addedQuota++;
						if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Incrementing the number of cores Federated: " + addedQuota);
					}
				}
				
				if (schedFed)
					dagMap.put(d, true);
				
				ret += d.getMinCores() + addedQuota;
				testedCores += ret;
			}
		}
		
		for (DAG d : dags) {
			if (dagMap.get(d) != null) {
				if (dagMap.get(d).booleanValue() == false)
					setSchedFede(false);
			}
		}
		
		return ret;
	}
	
	@Override
	public void run () {
		int bcores = 0;
		int lcores = 0;
		
		// Read the file
		if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Reading file "+inputFile);

		mcp.readXML();
		
		// Calc the min number of cores for Baruah
		lcores = minCoresLaxity();
		bcores = minCoresBaruah();
		if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Minimum number of cores Federated = " + bcores);
		
		// Allocate with Baruah -> check if schedulable or not
		int maxFCores = 2 * lcores;
		double uRestLO = 0.0;
		double uRestHI = 0.0;
		double uRestMax = 0.0;
		
		for (DAG d : dags) {
			if (d.getUHI() < 1 && d.getULO() < 1) {
				uRestLO += d.getULO();
				uRestHI += d.getUHI();
			}
		}
		uRestMax += (uRestHI > uRestLO) ? uRestHI: uRestLO;
		maxFCores -= (int) Math.ceil(uRestMax);
		
		if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] DAGs with U < 1 use " + (int) Math.ceil(uRestMax)+" cores.");
		
		/*
		 *  If all DAGs are scheduled using EDF and we gave the minimum number
		 *  the set is schedulable
		 */
		if (allDAGsEDF(dags)) {
			maxFCores = (int) Math.ceil(uRestMax);
		} else {
			maxFCores = testSystemFederated(maxFCores);		
			maxFCores += (int) Math.ceil(uRestMax);
		}
		
		// Maximum num of cores reached but still non schedulable
		if (!isSchedFede())
			System.out.println("[BENCH "+Thread.currentThread().getName()+"] Non-schedulable with Federated approach and " + maxFCores);
		
		// Calc min number of cores for our method
		int maxLCores = 2 * lcores;
		if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Minimum number of cores Laxity = " + lcores+"; max cores = "+maxLCores);
		
		// Allocate with our method.
		boolean schedLax = false;
		MultiDAG mdag = new MultiDAG(dags, lcores, false);
		
		while (!schedLax && lcores <= maxLCores) {
			try {
				mdag.setNbCores(lcores);
				schedLax = mdag.allocAll();
			} catch (SchedulingException se) {
				lcores++;
				if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] LAXITY incrementing number of cores: " + lcores);
			}
		}
			
		// Write results
		try {
			writeResults(maxFCores, schedFede, lcores, schedLax);
		} catch (IOException e) {
			e.printStackTrace();
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

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public boolean isSchedFede() {
		return schedFede;
	}

	public void setSchedFede(boolean schedFede) {
		this.schedFede = schedFede;
	}
}
