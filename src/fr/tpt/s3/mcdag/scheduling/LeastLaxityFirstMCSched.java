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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;

/**
 * Least-laxity first adaptation for the MC meta-heuristic
 * @author Roberto Medina
 *
 */
public class LeastLaxityFirstMCSched extends GlobalGenericMCScheduler{
	
	public LeastLaxityFirstMCSched (Set<McDAG> DAGs, int cores, int levels, boolean preemption, boolean debug) {
		setMcDAGs(DAGs);
		setNbCores(cores);
		setLevels(levels);
		setCountPreempt(preemption);
		setDebug(debug);
		
		for (McDAG d : getMcDAGs()) {
			calcDedlines(d);
			if (isDebug()) printDeadlines(d);
		}
		
		if (isCountPreempt())
			setPreemptions(new Hashtable<VertexScheduling, Integer>());
	}
	
	/**
	 * Calculates deadline of an actor by considering the dual of the graph
	 * @param a
	 * @param level
	 * @param deadline
	 */
	protected void calcDeadlineReverse (VertexScheduling a, int level, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSourceinLReverse(level)) {
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getSndEdges()) {
				test = ((VertexScheduling) e.getDest()).getDeadlines()[level] - e.getDest().getWcet(level);
				if (test < ret)
					ret = test;
			}
		}
		a.setDeadlineInL(ret, level);
	}
	
	/**
	 * Calculates the deadline of an actor, successors should be have their value assigned
	 * first
	 * @param a
	 * @param level
	 * @param deadline
	 */
	protected void calcDeadline (VertexScheduling a, int level, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSinkinL(level)) {
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getSndEdges()) {
				test = ((VertexScheduling) e.getDest()).getDeadlines()[level] - e.getDest().getWcet(level);
				if (test < ret)
					ret = test;
			}
		}
		a.setDeadlineInL(ret, level);
	}
	
	/**
	 * Function that calculates deadlines in all criticality modes for DAG
	 * @param d
	 */
	private void calcDedlines (McDAG d) {
		// Start by calculating deadlines in HI modes
		for (int i = 1; i < getLevels(); i++) {
			ArrayList<VertexScheduling> toVisit = new ArrayList<VertexScheduling>();
			
			// Calculate sources in i mode
			for (Vertex v : d.getVertices()) {
				if (v.isSourceinLReverse(i)) {
					toVisit.add((VertexScheduling) v);
				}
			}
			
			// Visit all nodes iteratively
			while (!toVisit.isEmpty()) {
				VertexScheduling a = toVisit.get(0);
				
				calcDeadlineReverse(a, i, d.getDeadline());
				a.getVisitedL()[i] = true;
				
				for (Edge e: a.getRcvEdges()) {
					if (e.getSrc().getWcet(i) != 0 && !((VertexScheduling) e.getSrc()).getVisitedL()[i]
							&& succVisitedInLevel((VertexScheduling) e.getSrc(), i)
							&& !toVisit.contains((VertexScheduling) e.getSrc())) {
						toVisit.add((VertexScheduling) e.getSrc());
					}
				}
				toVisit.remove(0);
			}
		}
		
		// Calculate deadlines in LO mode
		ArrayList<VertexScheduling> toVisit = new ArrayList<VertexScheduling>();
		// Calculate sources in i mode
		for (Vertex a : d.getVertices()) {
			if (a.isSinkinL(0))
				toVisit.add((VertexScheduling) a);
		}
					
		// Visit all nodes iteratively
		while (!toVisit.isEmpty()) {
			VertexScheduling a = toVisit.get(0);
						
			calcDeadline(a, 0, d.getDeadline());
			a.getVisitedL()[0] = true;
						
			for (Edge e: a.getRcvEdges()) {
				if (!((VertexScheduling) e.getSrc()).getVisitedL()[0]
						&& succVisitedInLevel((VertexScheduling) e.getSrc(), 0)
						&& !toVisit.contains((VertexScheduling) e.getSrc())) {
					toVisit.add((VertexScheduling) e.getSrc());
				}
			}
			toVisit.remove(0);
		}
	}
	
	/**
	 * Checks the amount of execution time that has been allocated to a vertex
	 * @param a
	 * @param t
	 * @param l
	 * @return
	 */
	private int scheduledUntilTinL (VertexScheduling a, int t, int l) {
		int ret = 0;
		int start = (int)(t / a.getGraphDead()) * a.getGraphDead();
		
		for (int i = start; i <= t; i++) {
			for (int c = 0; c < getNbCores(); c++) {
				if (getSched()[l][i][c] !=  null) {
					if (getSched()[l][i][c].contentEquals(a.getName()))
						ret++;
				}
			}
		}
		
		return ret;
	}

	/**
	 * Checks how many slots have been allocated for a in l mode in reverse
	 * from the deadline until the current slot
	 * @param a
	 * @param t
	 * @param l
	 * @return
	 */
	private int scheduledUntilTinLreverse (VertexScheduling a, int t, int l) {
		int ret = 0;
		int end = 0;
		
		int realSlot = gethPeriod() - t;
		
		if (t == 0)
			return 0;
		
		if ((int)(realSlot/a.getGraphDead()) <= 0 || realSlot % a.getGraphDead() == 0) {
			end = a.getGraphDead() - 1;
		} else {
			end = ((int)(realSlot / a.getGraphDead()) + 1)  * a.getGraphDead() - 1;
		}
		
		//System.out.println("\t\t\t [schedut] task "+a.getName()+" end "+end+" slot "+realSlot);
		
		for (int i = end; i > realSlot; i--) {
			for (int c = 0; c < getNbCores(); c++) {
				if (getSched()[l][i][c] !=  null) {
					if (getSched()[l][i][c].contentEquals(a.getName()))
						ret++;
				}
			}
		}
		return ret;
	}
	
	/**
	 * Function that verifies if the scheduling tables can still be obtained 
	 */
	@Override
	protected boolean verifyConstraints(List<VertexScheduling> ready, int slot, int level) {
		int sumRemainTimes = 0;
		int sumSlotsLeft = 0;
		int sumZeroLax = 0;
		
		for (VertexScheduling v : ready) {
			// Task has negative laxity -> non schedulable system
			if (v.getWeights()[level] < 0) {
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] verifyConstraints(): negative laxity on task "+v.getName());
				return false;
			} else if (v.getWeights()[level] == 0) {
				sumZeroLax += 1;
			}
			
			sumRemainTimes += getRemainingTime()[level][v.getGraphId()][v.getId()];
		}
		
		// More than m zero laxity tasks
		if (sumZeroLax > getNbCores()) {
			if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] verifyConstraints(): more than m zero laxity tasks");
			return false;
		}
		
		// Get the sum of remaining slots
		int relatSlot = 0;
		if (level >= 1)
			relatSlot = gethPeriod() - slot - 1;
		else
			relatSlot = slot;
		
		sumSlotsLeft = (gethPeriod() - relatSlot) * getNbCores();
		if (sumSlotsLeft < sumRemainTimes) {
			if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] verifyConstraints(): Not enough slots left "+sumSlotsLeft+" for "+sumRemainTimes);
			return false;
		}
		
		return true;
	}

	@Override
	protected void sortHI(List<VertexScheduling> ready, int slot, int level) {
		for (VertexScheduling v : ready) {
			int relatSlot = slot % v.getGraphDead();
			int dId = v.getGraphId();
			
			// The laxity has to be calculated for a HI mode
			if (level >= 1) {

				// It's not the highest criticality level -> perform checks
				if (level != getLevels() - 1 && v.getWcet(level + 1) != 0) {
					int deltaI = v.getWcet(level + 1) - v.getWcet(level);
					//Check if in the higher table the Ci(L+1) - Ci(L) has been allocated
					if (scheduledUntilTinLreverse(v, slot + 1, level + 1) <= deltaI) {
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Task "+v.getName()+" needs to be delayed at slot @t = "+slot);
						v.setWeightInL(Integer.MAX_VALUE, level);
					} else {
						v.setWeightInL(v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()], level);
					}
				} else {
					v.setWeightInL(v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()], level);
				}
			}
		}
		// Sort the ready list
		Collections.sort(ready, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling o1, VertexScheduling o2) {
				if (o1.getWeights()[level] - o2.getWeights()[level] != 0)
					return o1.getWeights()[level] - o2.getWeights()[level];
				else
					return o2.getId() - o1.getId();
			}
		});
	}

	@Override
	protected void sortLO(List<VertexScheduling> ready, int slot, int level) {
		for (VertexScheduling v : ready) {
			int relatSlot = slot % v.getGraphDead();
			int dId = v.getGraphId();
			
			// If it's a HI task
			if (v.getWcet(level + 1) > 0) {
				// Promotion needed for the task
				if ((v.getWcet(level) - getRemainingTime()[level][dId][v.getId()]) - scheduledUntilTinL(v, slot, level + 1) < 0) {
					if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Promotion of task "+v.getName()+" at slot @t = "+slot);
					v.setWeightInL(0, level);
				} else {
					v.setWeightInL(v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()], level);
				}
			} else {
				v.setWeightInL(v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()], level);
			}
		}
		// Sort the list
		Collections.sort(ready, new Comparator<VertexScheduling>() {
			@Override
			public int compare (VertexScheduling o1, VertexScheduling o2) {
				if (o1.getWeights()[0] - o2.getWeights()[0] != 0)
					return o1.getWeights()[0] - o2.getWeights()[0];
				else
					return o1.getId() - o2.getId();
			}
		});
	}

}
