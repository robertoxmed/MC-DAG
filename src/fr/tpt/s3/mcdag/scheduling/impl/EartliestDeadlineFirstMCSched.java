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
package fr.tpt.s3.mcdag.scheduling.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.GlobalGenericMCScheduler;

/**
 * Adaptation of the EDF scheduler
 * @author Roberto Medina
 *
 */
public class EartliestDeadlineFirstMCSched extends GlobalGenericMCScheduler {
	
	public EartliestDeadlineFirstMCSched(Set<McDAG> DAGs, int cores, int levels, boolean debug, boolean preemption) {
		setMcDAGs(DAGs);
		setNbCores(cores);
		setLevels(levels);
		setCountPreempt(preemption);
		setDebug(debug);
	}

	@Override
	protected boolean verifyConstraints(List<VertexScheduling> ready, int slot, int level) {
		int sumSlotsLeft = 0;
		
		for (VertexScheduling v : ready) {
			// Task is activated and its deadline has passed -> non schedulable system
			int relatSlot = slot % v.getGraphDead();
			if (level >= 1)
				relatSlot =  (gethPeriod() - slot - 1) % v.getGraphDead();
			
			if (relatSlot > v.getDeadlines()[level]) {
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] verifyConstraints(): deadline not respected for "+v.getName());
				return false;
			}
		}
		
		// Get the sum of remaining slots
		int relatSlot = 0;
		if (level >= 1)
			relatSlot = gethPeriod() - slot - 1;
		else
			relatSlot = slot;
		
		sumSlotsLeft = (gethPeriod() - relatSlot) * getNbCores();
		if (sumSlotsLeft < getSumRemainTimes()[level]) {
			if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] verifyConstraints(): Not enough slots left "+sumSlotsLeft+" for "+getSumRemainTimes()[level]);
			return false;
		}
		
		return true;
	}

	@Override
	protected void sortHI(List<VertexScheduling> ready, int slot, final int level) {
		// Check if tasks need to be delayed first
		for (VertexScheduling v : ready) {
			
			v.setWeightInL(v.getDeadlines()[level], level);
			v.setDelayed(false);
			
			if (level != getLevels() - 1) {
				int delta = v.getWcet(level + 1) - v.getWcet(level);
				
				if (scheduledUntilTinLreverse(v, slot, level + 1) <= delta) {
					if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] sortHI(): Task "+v.getName()+" needs to be delayed at slot @t = "+slot);
					v.setWeightInL(Integer.MAX_VALUE, level);
					v.setDelayed(true);
				}
			}
		}
		
		// Order the list accordingly
		Collections.sort(ready, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling o1, VertexScheduling o2) {
				if (o1.getWeights()[level] - o2.getWeights()[level] != 0)
					return o1.getWeights()[level] - o2.getWeights()[level];
				else
					return o1.getId() - o2.getId();
			}
		});
	}

	@Override
	protected void sortLO(List<VertexScheduling> ready, int slot, int level) {
		// If it's a HI task verify that mode transition is respected
		for (VertexScheduling v : ready) {
			int dagId = v.getGraphId();
			
			v.setWeightInL(v.getDeadlines()[level], level);
			
			if (v.getWcet(level + 1) > 0) {
				// Promotion needed for the task
				if ((v.getWcet(level) - getRemainingTime()[level][dagId][v.getId()]) - scheduledUntilTinL(v, slot, level + 1) < 0) {
					if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] sortLO(): Promotion of task "+v.getName()+" at slot @t = "+slot);
					v.setWeightInL(0, level);
				}
			}
		}
		// Sort the ready list
		Collections.sort(ready, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling arg0, VertexScheduling arg1) {
				if (arg0.getWeights()[0] - arg1.getWeights()[0] != 0)
					return arg0.getWeights()[0] - arg1.getWeights()[0];
				else
					return arg0.getId() - arg1.getId();
			}
		});
	}

}
