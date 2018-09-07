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
package fr.tpt.s3.mcdag.util;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.VertexScheduling;

/**
 * Utility class to count the number of context switches and preemptions
 * on a scheduling table
 * @author roberto
 *
 */
public class Counters {

	/**
	 * Method that counts the number of context switches for all tasks
	 * @param sched
	 * @param refs
	 * @param nbLevels
	 * @param hPeriod
	 * @param nbCores
	 */
	public static void countContextSwitch (String sched[][][], Hashtable<VertexScheduling, Integer> refs, int nbLevels, int hPeriod, int nbCores) {
		// Check for all tasks how many context switches it has
		Set<VertexScheduling> keys = refs.keySet();
		
		for (VertexScheduling a : keys) {
			int nbContSwitch = refs.get(a);
			
			// Iterate through the table to count context switches of a task
			for (int l = 0; l < nbLevels; l++) {
				for (int s = 0; s < hPeriod; s++) {
					if (s != 0) { // Check if task was running in the previous slot
						boolean wasRunning = false;
						boolean isRunning = false;
						
						for (int c = 0; c < nbCores; c++) {
							if (sched[l][s][c].contains(a.getName())) {
								isRunning = true;
								break;
							}
						}
						
						for (int c = 0; c < nbCores; c++) {
							if (sched[l][s - 1][c].contains(a.getName())) {
								wasRunning = true;
								break;
							}
						}
						
						if (!wasRunning && isRunning)
							nbContSwitch++;	
					}					
				}
			}
			refs.put(a, nbContSwitch);
		}
	}
	
	private static VertexScheduling lookForInList (List<VertexScheduling> listActors, String name) {
		for (VertexScheduling a: listActors) {
			if (a.getName().contentEquals(name))
				return a;
		}
		return null;
	}
	
	private static VertexScheduling lookForInKeys (Set<VertexScheduling> keys, String name) {
		for (VertexScheduling a: keys) {
			if (a.getName().contentEquals(name))
				return a;
		}
		return null;
	}
	
	/**
	 * Method to count the number of preemptions for each task
	 * @param sched
	 * @param refs
	 * @param levels
	 * @param hPeriod
	 * @param nbCores
	 */
	public static void countPreemptions (String sched[][][],
										 Hashtable<VertexScheduling, Integer> refs,
										 int levels, int hPeriod, int nbCores) {
		
		Set<VertexScheduling> keys = refs.keySet();
		List<VertexScheduling> runningPreviously = new LinkedList<>();
				
		for (int i = 0; i < levels; i++) {
			for (int j = 0; j < hPeriod; j++) {
				// Check if elements that were running in the previous slot are still running
				Iterator<VertexScheduling> lit = runningPreviously.listIterator();
				while (lit.hasNext()) {
					boolean stillRunning = false;
					VertexScheduling a = lit.next();
					
					for (int k = 0; k < nbCores; k++) {
						if (sched[i][j][k].contentEquals(a.getName())) {
							stillRunning = true;
							break;
						}
					}
					
					if (!stillRunning)
						lit.remove();	
				}
								
				// Add new tasks that are scheduled + increment their preemption count
				for (int k = 0; k < nbCores; k++) {
					if (!sched[i][j][k].contentEquals("-") &&
							lookForInList(runningPreviously, sched[i][j][k]) == null) {
						VertexScheduling toAdd = lookForInKeys(keys, sched[i][j][k]);
						int val = refs.get(toAdd);
						val++;
						refs.put(toAdd, val);
						runningPreviously.add(toAdd);
					}
						
				}
			}
		}
		
		// Decrement the preemption count by the nb of activations
		for (VertexScheduling a : keys) {
			int nbActivations = 0;
			if (a.getWcet(1) != 0)
				nbActivations = (int)(hPeriod / a.getGraphDead()) * levels;
			else
				nbActivations = (int)(hPeriod / a.getGraphDead());
			int val = refs.get(a);
			val -= nbActivations;
			refs.put(a, val);
		}
	}
	
	/**
	 * Method to count the number of preemptions for each task
	 * @param sched
	 * @param refs
	 * @param levels
	 * @param hPeriod
	 * @param nbCores
	 */
	public static void countPreemptions (String sched[][][],
										 Hashtable<VertexScheduling, Integer> refs,
										 int levels, int hPeriod, int deadline, int nbCores) {
		
		Set<VertexScheduling> keys = refs.keySet();
		List<VertexScheduling> runningPreviously = new LinkedList<>();
				
		for (int i = 0; i < levels; i++) {
			for (int j = 0; j < deadline; j++) {
				// Check if elements that were running in the previous slot are still running
				Iterator<VertexScheduling> lit = runningPreviously.listIterator();
				while (lit.hasNext()) {
					boolean stillRunning = false;
					VertexScheduling a = lit.next();
					
					for (int k = 0; k < nbCores; k++) {
						if (sched[i][j][k].contentEquals(a.getName())) {
							stillRunning = true;
							break;
						}
					}
					
					if (!stillRunning)
						lit.remove();	
				}
								
				// Add new tasks that are scheduled + increment their preemption count
				for (int k = 0; k < nbCores; k++) {
					if (!sched[i][j][k].contentEquals("-") &&
							lookForInList(runningPreviously, sched[i][j][k]) == null) {
						VertexScheduling toAdd = lookForInKeys(keys, sched[i][j][k]);
						int val = refs.get(toAdd);
						val++;
						refs.put(toAdd, val);
						runningPreviously.add(toAdd);
					}
						
				}
			}
		}
	}
}
