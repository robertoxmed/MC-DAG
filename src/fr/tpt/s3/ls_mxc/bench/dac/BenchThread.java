/*******************************************************************************
 * Copyright (c) 2017 Roberto Medina
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

import fr.tpt.s3.ls_mxc.alloc.LS;
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
	
	public BenchThread (String input, String output, boolean debug) {
		setInputFile(input);
		dags = new HashSet<DAG>();
		setOutputFile(output);
		setDebug(debug);
		mcp = new MCParser(inputFile, null, null, dags);
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
			
			for (Actor a : d.getNodes()) {
				if (a.getCHI() != 0)
					uHI += a.getCHI();
				uLO += a.getCLO();
			}
			
			uLO = uLO / d.getDeadline();
			uHI = uHI / d.getDeadline();
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
				if (a.getCHI() != 0)
					uHI += nbActivations * a.getCHI();
				uLO += nbActivations * a.getCLO();
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
		output = new BufferedWriter(new FileWriter(getOutputFile(), true));
		
		output.write(Thread.currentThread().getName()+"; "+getInputFile()+"; "+fCores+"; "+fSched+"; "+lCores+"; "+lSched+";\n");
		output.close();
	}
	
	@Override
	public void run () {
		int bcores = 0;
		int lcores = 0;
		
		// Read the file
		if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Reading file "+inputFile);

		mcp.readXML();
		
		// Calc the min number of cores for Baruah
		bcores = minCoresBaruah();
		if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Minimum number of cores Federated = " + bcores);
		
		// Allocate with Baruah -> check if schedulable or not
		boolean schedFede = false;
		int maxFCores = 2 * bcores;
		double uRestLO = 0.0;
		double uRestHI = 0.0;
		double uRestMax = 0.0;
		
		for (DAG d : dags) {
			if (d.getUHI() < 1 && d.getULO() < 1) {
				uRestLO += d.getULO();
				uRestHI += d.getUHI();
			}
			uRestMax = (uRestHI > uRestLO) ? uRestHI: uRestLO;
			maxFCores -= (int) Math.ceil(uRestMax);
		}
		
		if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] DAGs with U < 1 use " + (int) Math.ceil(uRestMax)+" cores.");
		
		while (!schedFede && bcores < maxFCores) {
			for (DAG d : dags) {
				// Utilization of the DAG is higher than 1
				if (d.getUHI() >= 1 || d.getULO() >= 1) {
					try {
						LS ls = new LS(d.getDeadline(), bcores, d);
						schedFede = ls.CheckBaruah();			
					} catch (SchedulingException se) {
						bcores++;
						if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Incrementing the number of cores Federated:" + bcores);
					}
				}
			}
		}
		
		// Maximum num of cores reached but still non schedulable
		if (!schedFede) {
			System.out.println("[BENCH "+Thread.currentThread().getName()+"] Non-schedulable with Federated approach and " + maxFCores);
		}
		
		// Calc min number of cores for our method
		lcores = minCoresLaxity();
		int maxLCores = 2 * lcores;
		if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Minimum number of cores Laxity = " + lcores);
		
		// Allocate with our method.
		boolean schedLax = false;
		MultiDAG mdag = new MultiDAG(dags, lcores, false);
		
		while (!schedLax && lcores < maxLCores) {
			try {
				mdag.setNbCores(lcores);
				schedLax = mdag.allocAll();
			} catch (SchedulingException se) {
				lcores++;
				if (isDebug()) System.out.println("[BENCH "+Thread.currentThread().getName()+"] Incrementing the number of cores Laxity: " + lcores);
			}
		}
			
		// Write results
		try {
			writeResults(bcores + (int) Math.ceil(uRestMax), schedFede, lcores, schedLax);
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
}
