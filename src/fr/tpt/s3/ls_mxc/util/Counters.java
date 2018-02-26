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
package fr.tpt.s3.ls_mxc.util;

import java.util.Hashtable;
import java.util.Set;

import fr.tpt.s3.ls_mxc.model.ActorSched;

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
	public static void countContextSwitch (String sched[][][], Hashtable<ActorSched, Integer> refs, int nbLevels, int hPeriod, int nbCores) {
		// Check for all tasks how many context switches it has
		Set<ActorSched> keys = refs.keySet();
		
		for (ActorSched a : keys) {
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
	
	/**
	 * Method to count the number of preemptions for each task
	 * @param sched
	 * @param refs
	 * @param levels
	 * @param hPeriod
	 * @param nbCores
	 */
	public static void countPreemptions (String sched[][][], Hashtable<ActorSched, Integer> refs, int levels, int hPeriod, int nbCores) {
		
		Set<ActorSched> keys = refs.keySet();
		@SuppressWarnings("unchecked")
		Hashtable<ActorSched, Integer>[] remaining = new Hashtable[levels];
		
		for (int i = 0; i < levels; i++)
			remaining[i] = new Hashtable<ActorSched, Integer>();
		
		// Init remaining times
		for (ActorSched a : keys) {
			for (int i = 0; i < levels; i++)
				remaining[i].put(a, a.getCI(i));
		}
		
		// Iterate through actors to check the number of preemptions
		for (ActorSched a : keys) {
			int preempts = refs.get(a);
			// Iterate through all the levels
			for (int l = 0; l < levels; l++) {
				// Check the number of activations a task has
				int nbActivations = (int)(a.getGraphDead() / hPeriod);
				
				for (int s = 1; s < hPeriod; s++) {
					int val = remaining[l].get(a);
					
					for (int c = 0; c < nbCores; c++) {
						// The task is running
						if (sched[l][s][c].contentEquals(a.getName())) {
							boolean wasRunning = false;
							
							// Check if the task was running in the previous slot
							for (int c2 = 0; c2 < nbCores; c2++) {
								if (sched[l][s - 1][c2].contentEquals(a.getName())) {
									wasRunning = true;
									break;
								}
							}
							// The task wasn't running and it isn't the first it is executed was a preemption at this point
							if (!wasRunning && val != a.getCI(l))
								preempts++;
							
							val--;							
							if (val == 0 && nbActivations != 0) {
								remaining[l].put(a, a.getCI(l));
								nbActivations--;
							} else {
								remaining[l].put(a, val);
							}
						}
					}
				}
			}
			refs.put(a, preempts);
		}
	}
}
