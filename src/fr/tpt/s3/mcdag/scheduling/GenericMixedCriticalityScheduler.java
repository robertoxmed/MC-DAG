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
package fr.tpt.s3.mcdag.scheduling;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.util.Counters;
import fr.tpt.s3.mcdag.util.MathMCDAG;

/**
 * Generic implementation of the scheduling algorithms
 * Uses ALAP strategy for the HI criticality modes
 * @author roberto
 *
 */
public abstract class GenericMixedCriticalityScheduler {

	// Set of MC-DAGs to schedule
	private Set<McDAG> mcDAGs;
	
	// Architecture + hyper-period + nb of levels
	private int nbCores;
	private int hPeriod;
	private int levels;
	
	// Scheduling tables
	private String sched[][][];
	
	// Remaining time to be allocated for each node
	// Level, DAG id, Vertex Id
	private int remainingTime[][][];
	
	// Comprator to other vertices
	private Comparator<VertexScheduling> loComp;
	
	// Preemption counters
	private boolean countPreempt;
	private int activations;
	private Hashtable<VertexScheduling, Integer> preemptions;
	
	// Debugging boolean
	private boolean debug;
	
	/*
	 * SCHEDULING FUNCTIONS
	 */
	
	// Functions that need to be implemented (adaptation of the scheduler)
	/**
	 * Function that verifies if the scheduling should continue
	 * @return
	 */
	protected abstract boolean verifyConstraints (List<VertexScheduling> ready, int slot, int level);
	
	/**
	 * Function to sort the ready list in HI modes (uses the dual graph)
	 * @param ready
	 * @param slot
	 * @param level
	 */
	protected abstract void sortHI (List<VertexScheduling> ready, int slot, int level);
	
	/**
	 * Function to sort the ready list in the lower criticality mode
	 * @param ready
	 * @param slot
	 * @param level
	 */
	protected abstract void sortLO (List<VertexScheduling> ready, int slot, int level);
	
	/*
	 * Generic scheduling functions
	 */
	
	/**
	 * Initialize scheduling tables 
	 */
	protected void initTables() {
		int[] input = new int[getMcDAGs().size()];
		int i = 0;
		
		for (McDAG d : getMcDAGs()) {
			input[i] = d.getDeadline();
			i++;
		}
		
		sethPeriod(MathMCDAG.lcm(input));
		
		// Init scheduling tables
		sched = new String[getLevels()][gethPeriod()][getNbCores()];
		
		for (i = 0; i < getLevels(); i++) {
			for (int j = 0; j < gethPeriod(); j++) {
				for (int k = 0; k < getNbCores(); k++)
					sched[i][j][k] = "-";
			}
		}
		
		if (debug) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] initTables(): Sched tables initialized!");
		
		// Calc number of activations
		if (isCountPreempt()) {
			for (McDAG d : getMcDAGs()) {
				for (Vertex a : d.getVertices()) {
					// Check if task runs in HI mode
					int nbActivations = (int) (hPeriod / d.getDeadline());
					if (a.getWcet(1) != 0)
						activations = activations + nbActivations * d.getLevels();
					else
						activations = activations + nbActivations;
				}
			}
		}
	}
	
	/**
	 * Inits remaining time for tasks
	 */
	private void initRemainingTimes () {
		remainingTime = new int[getLevels()][getMcDAGs().size()][];
		
		// Init remaining time for each DAG
		for (McDAG d : getMcDAGs()) {
			for (int i = 0; i < getLevels(); i++)
				remainingTime[i][d.getId()] = new int[d.getVertices().size()];
		}

		for (int i = 0; i < getLevels(); i++) {
			for (McDAG d : getMcDAGs()) {
				for (Vertex a : d.getVertices())
					remainingTime[i][d.getId()][a.getId()] = a.getWcet(i);
			}
		}
	}
	
	/**
	 * Internal function that checks if all the predecessors of an actor are visited
	 * @param a
	 * @param level
	 * @return
	 */
	protected boolean predVisitedInLevel (VertexScheduling a, int level) {
		for (Edge e : a.getRcvEdges()) {
			if (e.getSrc().getWcet(level) != 0 && !((VertexScheduling) e.getSrc()).getVisitedL()[level])
				return false;
		}
		return true;
	}
	
	/**
	 * Internal function that checks if all the sucessors of an actor are visited
	 * @param a
	 * @param level
	 * @return
	 */
	protected boolean succVisitedInLevel (VertexScheduling a, int level) {
		for (Edge e : a.getSndEdges()) {
			if (e.getDest().getWcet(level) != 0 && !((VertexScheduling) e.getDest()).getVisitedL()[level])
				return false;
		}
		return true;
	}
	
	
	/**
	 * Functions that adds new jobs when task have finished their execution
	 * @param ready
	 * @param level
	 */
	protected void checkJobActivations (List<VertexScheduling> ready, List<VertexScheduling> scheduled, int level) {
		final boolean forward = level == 0;
		
		for (VertexScheduling v : scheduled) {
			
			for (Edge e : forward ? v.getSndEdges() : v.getRcvEdges()) {
				VertexScheduling connectedVertex = (VertexScheduling) (forward ? e.getDest() : e.getSrc());
				boolean add = true;
					
				for (Edge e2 : forward ? connectedVertex.getRcvEdges() : connectedVertex.getSndEdges()) {
					VertexScheduling checkedVertex = (VertexScheduling) (forward ? e2.getSrc() : e2.getDest());
						
					
					if (forward && !scheduled.contains(checkedVertex)) {
						add = false;
						break;
					} else if (!forward && checkedVertex.getWcet(level) != 0 && !scheduled.contains(checkedVertex)) {
						add = false;
						break;
					}
				}
					
				if (add && !ready.contains(connectedVertex)
						&& remainingTime[level][connectedVertex.getGraphId()][connectedVertex.getId()] != 0) {
					ready.add(connectedVertex);
				}
			} 
		}
	}
	
	/**
	 * Function that adds entry vertices when the period of a MC-DAG is reached
	 * @param ready
	 * @param slot
	 * @param level
	 */
	protected void checkDagActivations (List<VertexScheduling> ready, List<VertexScheduling> scheduled, int slot, int level) {		
		for (McDAG d : getMcDAGs()) {
			
			if (slot % d.getDeadline() == 0) {
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] checkDAGActivation(): DAG (id. "+d.getId()+") activation at slot "+slot);
				
				for (Vertex v : d.getVertices()) {
					// Remove nodes from the scheduled list
					scheduled.remove(v);
					
					remainingTime[level][((VertexScheduling)v).getGraphId()][v.getId()] = v.getWcet(level);
					
					if (level >= 1 && v.isSinkinL(level))
						ready.add((VertexScheduling) v);
					else if (level == 0 && v.isSourceinL(level))
						ready.add((VertexScheduling) v);
				}
			}
		}
	}
	
	/**
	 * Function that computes the scheduling tables on the dual 
	 * @param level
	 * @throws SchedulingException
	 */
	protected void buildTable (final int level) throws SchedulingException {
		List<VertexScheduling> ready = new LinkedList<VertexScheduling>();
		List<VertexScheduling> scheduled = new LinkedList<VertexScheduling>();
		final boolean forward = level == 0;
		
		// Add all sink nodes
		for (McDAG d : getMcDAGs()) {
			for (Vertex v : d.getVertices()) {
				if (forward) {
					if (v.isSourceinL(level))
						ready.add((VertexScheduling) v);
				} else {
					if (v.isSourceinLReverse(level)) 
						ready.add((VertexScheduling) v);
				}
			}
		}
		
		if (forward)
			sortLO(ready, 0, level);
		else
			sortHI(ready, 0, level);
		
		// Allocate slot by slot
		ListIterator<VertexScheduling> lit = ready.listIterator();
		boolean jobFinished = false;
		
		final int increment = forward ? 1 : -1;
		final int startTimeIndex  = forward ? 0 : hPeriod - 1;
		final int startCoreIndex = forward ? 0 : nbCores - 1;
		
		for (int timeIndex = startTimeIndex; timeIndex < hPeriod && timeIndex >= 0; timeIndex = timeIndex + increment) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildHiTable("+level+"): @t = "+timeIndex+", tasks activated: ");
				for (VertexScheduling v : ready)
					System.out.print("Prio("+v.getName()+") = "+v.getWeights()[level]+"; ");
				System.out.println("");
			}
			
			if (!verifyConstraints(ready, timeIndex, level)) {
				SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] buildHITable("+level+"): Ready list not empty.");
				throw se;
			}
			
			// Allocate to cores
			for (int coreIndex = startCoreIndex; coreIndex >= 0 && coreIndex < nbCores; coreIndex = coreIndex + increment) {
				// Find next ready tasks that is not delayed
				boolean notDelayed = false;
				
				while (!notDelayed && lit.hasNext()) {
					if (lit.hasNext()) {
						VertexScheduling v = lit.next();
					
						if (!v.isDelayed()) {
							notDelayed = true;
							int val = remainingTime[level][v.getGraphId()][v.getId()];
						
							sched[level][timeIndex][coreIndex] = v.getName();
							val--;
							
							// Task has been fully scheduled
							if (val == 0) {
								scheduled.add(v);
								jobFinished = true;
								lit.remove();
							}
							remainingTime[level][v.getGraphId()][v.getId()] = val;
						} 
					}
				}
			}
			
			// A job finished its execution -> new tasks can be activated
			if (jobFinished)
				checkJobActivations(ready, scheduled, level);
			
			if (forward) {
				if (timeIndex != hPeriod - 1) {
					checkDagActivations(ready, scheduled, timeIndex + 1, level);
					sortLO(ready, timeIndex + 1, level);
				}
			} else {
				if (timeIndex !=  0) {
					checkDagActivations(ready, scheduled, timeIndex, level);
					sortHI(ready, gethPeriod() - timeIndex, level);
				}
			}
			
			jobFinished = false;
			lit = ready.listIterator();	
		}
	}
	
	/**
	 * Function that schedules the system in all the criticality modes
	 * @throws SchedulingException
	 */
	protected void scheduleSystem () throws SchedulingException {
		initTables();
		initRemainingTimes();
		
		// Start by the highest tables first
		for (int i = getLevels() - 1; i >= 0; i--)
			buildTable(i);
		
		if (isDebug()) printTables();
		
		// Count preemption if the boolean is true
		if (isCountPreempt()) {
			for (McDAG d : getMcDAGs()) {
				for (Vertex v : d.getVertices())
					preemptions.put((VertexScheduling) v, 0);
			}
			Counters.countPreemptions(sched, preemptions, getLevels(), hPeriod, nbCores);
			
			if (isDebug()) printPreempts();
		}
	}
	
	/*
	 * DEBUG FUNCTIONS
	 */
	
	/**
	 * Prints the scheduling tables
	 */
	public void printTables () {
		for (int i = getLevels() - 1; i >= 0; i--) {
			System.out.println("Scheduling table in mode "+ i+":");
			for (int c = 0; c < getNbCores(); c++) {
				for (int s = 0; s < gethPeriod(); s++) {
					if (sched[i][s][c] != null)
						System.out.print(sched[i][s][c]+" | ");
					else
						System.out.print("-- | ");
				}
				System.out.print("\n");
			}
		}
		System.out.print("\n");
	}
	
	/**
	 * Prints preemption statistics
	 */
	public void printPreempts () {
		int total = 0;
		System.out.println("[DEBUG "+Thread.currentThread().getName()+"] Printing preemption data...");

		for (VertexScheduling a : preemptions.keySet()) {
			System.out.println("[DEBUG "+Thread.currentThread().getName()+"]\t Task "+a.getName()+" peempted "+preemptions.get(a)+" times.");
			total += preemptions.get(a);
		}
		System.out.println("[DEBUG "+Thread.currentThread().getName()+"] Total number of preemptions = "+total+" for "+getActivations()+" activations");
	}

	/*
	 * Getters & Setters
	 */
	public Set<McDAG> getMcDAGs() {
		return mcDAGs;
	}

	public void setMcDAGs(Set<McDAG> mcDAGs) {
		this.mcDAGs = mcDAGs;
	}

	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}

	public int gethPeriod() {
		return hPeriod;
	}

	public void sethPeriod(int hPeriod) {
		this.hPeriod = hPeriod;
	}

	public int getLevels() {
		return levels;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}

	public String[][][] getSched() {
		return sched;
	}

	public void setSched(String[][][] sched) {
		this.sched = sched;
	}

	public int[][][] getRemainingTime() {
		return remainingTime;
	}

	public void setRemainingTime(int[][][] remainingTime) {
		this.remainingTime = remainingTime;
	}

	public Comparator<VertexScheduling> getLoComp() {
		return loComp;
	}

	public void setLoComp(Comparator<VertexScheduling> loComp) {
		this.loComp = loComp;
	}

	public int getActivations() {
		return activations;
	}

	public void setActivations(int activations) {
		this.activations = activations;
	}

	public Hashtable<VertexScheduling, Integer> getPreemptions() {
		return preemptions;
	}

	public void setPreemptions(Hashtable<VertexScheduling, Integer> preemptions) {
		this.preemptions = preemptions;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isCountPreempt() {
		return countPreempt;
	}

	public void setCountPreempt(boolean countPreempt) {
		this.countPreempt = countPreempt;
	}
	
}
