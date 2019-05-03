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
package fr.tpt.s3.mcdag.scheduling;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import fr.tpt.s3.mcdag.avail.Automata;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.parser.MCParser;

/**
 * Threads used by the framework to schedule and write to files
 * @author roberto
 *
 */
public class SchedulingThread implements Runnable{
	
	private Set<GlobalGenericMCScheduler> schedulers;
	private Set<McDAG> dags;
	private MCParser mcp;
	private String inputFile;
	private boolean outSchedFile;
	private boolean outPRISMFile;
	private boolean levels;
	
	private SingleDAG ls;
	private Automata auto;
	private boolean debug;
	private boolean preempt;
	
	public SchedulingThread(String iFile, boolean oSF, boolean oPF, boolean debug, boolean preempt) {
		schedulers = new HashSet<GlobalGenericMCScheduler>();
		dags = new HashSet<McDAG>();
		mcp = new MCParser(iFile, null, schedulers, dags, oPF);
		setOutPRISMFile(oPF);
		setPreempt(preempt);
		
		if (isOutPRISMFile()) mcp.setOutPrismFile(iFile.substring(0, iFile.lastIndexOf('.')).concat(".pm"));
		setOutSchedFile(oSF);
		if (isOutSchedFile()) mcp.setOutSchedFile(iFile.substring(0, iFile.lastIndexOf('.')).concat("-sched.xml"));
		setDebug(debug);
	}

	@Override
	public void run() {
		mcp.readXML();
		
		if (!isOutSchedFile())
			System.err.println("[WARNING] No output file has been specified for the scheduling tables.");
		
		// Only one DAG has to be scheduled in the multi-core architecture
			
		if (isOutPRISMFile()) {
			if (debug) System.out.println("[DEBUG] UniDAG: Creating the automata object.");
			auto = new Automata(ls, dags.iterator().next());
			auto.createAutomata();
			mcp.setAuto(auto);
			try {
				mcp.writePRISM();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("[WARNING] Error writting PRISM files "+outPRISMFile);
			}
			System.out.println("["+Thread.currentThread().getName()+"] PRISM file written.");
		}
		
		// Test for schedulability
		for (GlobalGenericMCScheduler scheduler : schedulers) {
			scheduler.setDebug(debug);
			scheduler.setCountPreempt(preempt);
			try {
				scheduler.scheduleSystem();
			} catch (SchedulingException e) {
				System.err.println("[ERROR] Unable to schedule the system");
				e.printStackTrace();
			}
		}
		
		/* =============== Write results ================ */
		if (isOutSchedFile()) {
			try {
				mcp.writeSched();
			} catch (IOException e) {
				System.err.println("[WARNING] Error writting scheduling tables to file "+outSchedFile);
				e.printStackTrace();
			}
		}
	}

	/*
	 * Getters and setters
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
	
	public boolean isOutSchedFile() {
		return outSchedFile;
	}

	public void setOutSchedFile(boolean outSchedFile) {
		this.outSchedFile = outSchedFile;
	}

	public boolean isOutPRISMFile() {
		return outPRISMFile;
	}

	public void setOutPRISMFile(boolean outPRISMFile) {
		this.outPRISMFile = outPRISMFile;
	}

	public SingleDAG getLs() {
		return ls;
	}

	public void setLs(SingleDAG ls) {
		this.ls = ls;
	}

	public Automata getAuto() {
		return auto;
	}

	public void setAuto(Automata auto) {
		this.auto = auto;
	}

	public boolean isLevels() {
		return levels;
	}

	public void setLevels(boolean levels) {
		this.levels = levels;
	}

	public boolean isPreempt() {
		return preempt;
	}

	public void setPreempt(boolean preempt) {
		this.preempt = preempt;
	}

	public Set<GlobalGenericMCScheduler> getSchedulers() {
		return schedulers;
	}

	public void setSchedulers(Set<GlobalGenericMCScheduler> schedulers) {
		this.schedulers = schedulers;
	}
}
