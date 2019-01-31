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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.VertexScheduling;

/**
 * Least-laxity first adaptation for the MC meta-heuristic
 * @author Roberto Medina
 *
 */
public class LeastLaxityFirstMCSched extends GlobalGenericMCScheduler{
	
	// Map to implement the modified version of LLF
	private Map<Integer, List<VertexScheduling>> equalityMap;
	private int lastEqLax = -1;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public LeastLaxityFirstMCSched (Set<McDAG> DAGs, int cores, int levels, boolean debug, boolean preemption) {
		setMcDAGs(DAGs);
		setNbCores(cores);
		setLevels(levels);
		setCountPreempt(preemption);
		setDebug(debug);
		equalityMap = new HashMap();
		
		for (McDAG d : getMcDAGs()) {
			calcDedlines(d);
			if (isDebug()) printDeadlines(d);
		}
		
		if (isCountPreempt())
			setPreemptions(new Hashtable<VertexScheduling, Integer>());
	}
	
	/**
	 * Function that verifies if the scheduling tables can still be obtained 
	 */
	@Override
	protected boolean verifyConstraints(List<VertexScheduling> ready, int slot, int level) {
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
		if (sumSlotsLeft < getSumRemainTimes()[level]) {
			if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] verifyConstraints(): Not enough slots left "+sumSlotsLeft+" for "+getSumRemainTimes());
			return false;
		}
		
		return true;
	}

	@Override
	protected void sortHI(List<VertexScheduling> ready, int slot, int level) {
		for (VertexScheduling v : ready) {
			int relatSlot = slot % v.getGraphDead();
			int dId = v.getGraphId();
			
			// It's not the highest criticality level -> perform checks
			if (level != getLevels() - 1 && v.getWcet(level + 1) != 0) {
				int deltaI = v.getWcet(level + 1) - v.getWcet(level);
				//Check if in the higher table the Ci(L+1) - Ci(L) has been allocated
				if (scheduledUntilTinLreverse(v, slot + 1, level + 1) <= deltaI) {
					if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Task "+v.getName()+" needs to be delayed at slot @t = "+slot);
					v.setDelayed(true);
					v.setWeightInL(Integer.MAX_VALUE, level);
				} else {
					v.setWeightInL(v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()], level);
				}
			} else {
				v.setWeightInL(v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()], level);
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
		//checkForEqualities(ready, level);
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
		//checkForEqualities(ready, level);
	}
	
	/**
	 * Method to prevent preemptions when tasks have the same laxity
	 * the equality is only interesting on the last element m of the list
	 * where m is the number of available cores
	 * @param ready
	 * @param level
	 */
	private void checkForEqualities (List<VertexScheduling> ready, int level) {
		// There is no current equality
		if (lastEqLax == -1) {
			int eqLax = ready.get(getNbCores() - 1).getWeights()[level]; // Check the laxity of the last element
			boolean eq = (ready.get(getNbCores()).getWeights()[level] == eqLax) ? true : false;
			int index = getNbCores() - 2;

			// The system is a state were there is an equality on tasks are there are not enough cores
			// Initialize the map with the first task
			if (eq) {
				lastEqLax = eqLax;
				equalityMap.put(eqLax, new ArrayList<VertexScheduling>());
				equalityMap.get(eqLax).add(ready.get(getNbCores() - 1));
				
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] \t\t\t\t checkForEqualities: equality with laxity " + eqLax);
			
				// Look for the tasks that will be able to be scheduled
				eq = (ready.get(index).getWeights()[level] == eqLax) ? true : false;
				while (eq && index >= 0) {
					// Add the ready task to the beginning of the list in the map
					equalityMap.get(eqLax).add(0, ready.get(index));
					index--;
					
					if (index > 0)
						eq = (ready.get(index).getWeights()[level] == eqLax) ? true : false;
					else
						eq = false;
				}
				
				// Mark tasks that will be scheduled
				for (VertexScheduling v : equalityMap.get(eqLax))
					v.setSticky(true);
				
				// Look for tasks that will not be scheduled
				index = getNbCores();
				eq = (ready.get(getNbCores()).getWeights()[level] == eqLax) ? true : false;
				while (eq && index < ready.size()) {
					// Add the ready task to the beginning of the list in the map
					equalityMap.get(eqLax).add(0, ready.get(index));
					index++;
					
					if (index < ready.size())
						eq = (ready.get(index).getWeights()[level] == eqLax) ? true : false;
					else
						eq = false;
				}
				
				// Mark tasks that will not be scheduled
				for (VertexScheduling v : equalityMap.get(eqLax))
					v.setLaxityDelayed(true);
			}
		} else { // The system is an equality state
			// Grab the list from the Map
			ArrayList<VertexScheduling> eqList = (ArrayList<VertexScheduling>) equalityMap.get(lastEqLax);
			
			// TODO: Update the map by removing or adding new tasks
			Iterator<VertexScheduling> itL = eqList.iterator();
			while (itL.hasNext()) {
				VertexScheduling v = itL.next();
				
				if (!ready.contains(v))
					itL.remove();
			}
			
			if (eqList.size() == 0) {
				equalityMap.remove(lastEqLax);
				lastEqLax = -1;
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] \t\t\t\t checkForEqualities: removing list in hashmap");
				return;
			}
				
			
			// Reorder the ready list accordingly
			for (int i = 0; i < ready.size(); i++) {
				// Look for previous scheduled task
				if (eqList.contains(ready.get(i)) && ready.get(i).isLaxityDelayed()) {
					for (int j = i; j < ready.size(); j++) {
						Collections.swap(ready, i, j); // Swap in the ready list
					}
				}
			}
		}
	}
}