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

import java.util.HashSet;
import java.util.Set;

import fr.tpt.s3.ls_mxc.alloc.NLevels;
import fr.tpt.s3.ls_mxc.model.Actor;
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
		double[][] us = new double[mcp.getNbLevels()][getDags().size()];
		
		for (DAG d : getDags()) {
			input[i] = d.getDeadline();
			i++;
		}
		
		hPeriod = MathMCDAG.lcm(input);
		
		for (int l = 0; l < mcp.getNbLevels(); l++) {
			
			for (DAG d : getDags()) {
				us[l][d.getId()] = 0;
				int nbActivations = (int) (hPeriod / d.getDeadline());
			
				for (Actor a : d.getNodes()) {
					us[l][d.getId()] += a.getCI(l);
				}
				us[l][d.getId()] = us[l][d.getId()] / d.getDeadline();  
			}
		}
		
		return ret;
	}
	
	
	@Override
	public void run() {
		mcp.readXMLNlevels();
		NLevels nlvl = new NLevels(dags, minCores(), mcp.getNbLevels(), debug);
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
}
