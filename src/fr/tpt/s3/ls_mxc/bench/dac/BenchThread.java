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

import java.util.HashSet;
import java.util.Set;

import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.parser.MCParser;

public class BenchThread implements Runnable {

	private Set<DAG> dags;
	private MCParser mcp;
	private String inputFile;
	
	public BenchThread (String input) {
		setInputFile(input);
		dags = new HashSet<DAG>();
		mcp = new MCParser(inputFile, null, null, dags);
	}
	
	/**
	 * Internal functions that calculates the minimum number of cores to use
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
	
	private synchronized void writeResults () {
		
	}
	
	@Override
	public void run () {
		int bcores = 0;
		
		// Read the file
		mcp.readXML();
		
		// Calc the min number of cores for Baruah
		bcores = minCoresBaruah();
		System.out.println("[BENCH "+Thread.currentThread().getName()+"] Minimum number of cores Baruah = "+bcores);
		
		// Allocate with Baruah -> check if schedulable or not
		
		// Calc min number of cores for our method
		
		// Allocate with our method.
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
}
