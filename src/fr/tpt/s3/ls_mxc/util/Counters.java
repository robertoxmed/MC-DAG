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

public class Counters {

	
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
						
						for (int c = 0; c < nbCores; c++) {
							if (sched[l][s - 1][c].contains(a.getName())) {
								wasRunning = true;
								break;
							}
						}
						
						if (!wasRunning)
							refs.put(a, nbContSwitch + 1);	
					} else {
						refs.put(a, nbContSwitch + 1);
					}
				}
			}
		}
		
	}
	
	public static void countPreemptions (String sched[][][], Hashtable<ActorSched, Integer> refs, int level, int hPeriod, int nbCores) {
		
	}
}
