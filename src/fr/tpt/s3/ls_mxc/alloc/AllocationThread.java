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
package fr.tpt.s3.ls_mxc.alloc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import fr.tpt.s3.ls_mxc.avail.Automata;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.parser.MCParser;

/**
 * Threads used by the framework to schedule and write to files
 * @author roberto
 *
 */
public class AllocationThread implements Runnable{
	
	private Set<DAG> dags;
	private MCParser mcp;
	private String inputFile;
	private boolean outSchedFile;
	private boolean outPRISMFile;
	private boolean levels;
	
	private SingleDAG ls;
	private MultiDAG msched;
	private NLevels nlvl;
	private Automata auto;
	private boolean debug;
	
	public AllocationThread(String iFile, boolean oSF, boolean oPF, boolean debug) {
		dags = new HashSet<DAG>();
		mcp = new MCParser(iFile, null, dags, oPF);
		setOutPRISMFile(oPF);
		if (isOutPRISMFile()) mcp.setOutPrismFile(iFile.substring(0, iFile.lastIndexOf('.')).concat(".pm"));
		setOutSchedFile(oSF);
		if (isOutSchedFile()) mcp.setOutSchedFile(iFile.substring(0, iFile.lastIndexOf('.')).concat("-sched.xml"));
		setDebug(debug);
	}

	@Override
	public void run() {
		
		// File has a dual-criticality system
		if (!isLevels()) {
			mcp.readXML();
		
			if (!isOutSchedFile())
				System.err.println("[WARNING] No output file has been specified for the scheduling tables.");
		
			// Only one DAG has to be scheduled in the multi-core architecture
			if (dags.size() == 1) {
				DAG dag = dags.iterator().next();
				ls = new SingleDAG();
				ls.setMxcDag(dag);
				ls.setDeadline(dag.getDeadline());
				ls.setNbCores(mcp.getNbCores());
				ls.setDebug(debug);
				
				try {
					ls.AllocAll();
				} catch (SchedulingException e1) {
					System.out.println("[ERROR] UniDAG: unable to schedule the example: "+this.getInputFile());
					System.out.println(e1.getMessage());
					System.exit(1);
				}
				mcp.setNbCores(ls.getNbCores());
				mcp.sethPeriod(ls.getDeadline());
				mcp.setSched(ls.getSched());
				
				if (isOutPRISMFile()) {
					if (debug) System.out.println("[DEBUG] UniDAG: Creating the automata object.");
					auto = new Automata(ls, dag);
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
			// Multiple DAGs neede to be scheduled
			} else if (dags.size() > 1) {
				msched = new MultiDAG(dags, mcp.getNbCores(), debug);
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] MultiDAG: "+dags.size()+" DAGs are going to be scheduled in "+mcp.getNbCores()+" cores.");
				
				try {
					msched.allocAll();
				} catch (SchedulingException e) {
					System.err.println(e.getMessage());
					System.err.println("[ERROR] MultiDAG: unable to schedule the example: "+mcp.getInputFile());
					System.exit(1);
				}
				mcp.setNbCores(msched.getNbCores());
				mcp.sethPeriod(msched.gethPeriod());
				mcp.setSched(msched.getSched());
			}
			
		} else { // The model is a N-level MCS
			mcp.readXMLNlevels();
			
			setNlvl(new NLevels(dags, mcp.getNbCores(), mcp.getNbLevels(), debug));
			if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] N levels: "+dags.size()+" DAGs are going to be scheduled in "+mcp.getNbCores()+" cores.");
			nlvl.printDAGs();
			try {
				nlvl.buildAllTables();
			} catch (SchedulingException e) {
				e.printStackTrace();
			}
			mcp.setSched(nlvl.getSched());
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

	public MultiDAG getMsched() {
		return msched;
	}

	public void setMsched(MultiDAG msched) {
		this.msched = msched;
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

	public NLevels getNlvl() {
		return nlvl;
	}

	public void setNlvl(NLevels nlvl) {
		this.nlvl = nlvl;
	}
}
