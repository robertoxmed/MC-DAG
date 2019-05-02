package fr.tpt.s3.mcdag.scheduling.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.GlobalGenericMCScheduler;

/**
 * Adaptation of the EZL scheduler
 * @author Roberto Medina
 *
 */
public class EarlistDeadlineZeroLaxity extends GlobalGenericMCScheduler {

	public EarlistDeadlineZeroLaxity(Set<McDAG> DAGs, int cores, int levels, boolean debug, boolean benchmark) {
		setMcDAGs(DAGs);
		setNbCores(cores);
		setCountPreempt(benchmark);
		setDebug(debug);
		
		for (McDAG d : getMcDAGs()) {
			calcDedlines(d);
			if (isDebug()) printDeadlines(d);
		}
		
		if (isCountPreempt())
			setPreemptions(new Hashtable<VertexScheduling, Integer>());
	}
	
	@Override
	protected boolean verifyConstraints(List<VertexScheduling> ready, int slot, int level) {
		int sumSlotsLeft = 0;
		
		for (VertexScheduling v : ready) {
			// Task is activated and deadline was missed
			int relatSlot = slot % v.getGraphDead();
			if (level >= 1)
				relatSlot = (gethPeriod() - slot - 1) % v.getGraphDead();
			
			if (relatSlot > v.getDeadlines()[level]) {
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] verifyConstraints(): deadline not respected for "+v.getName());
				return false;
			}
		}
		
		// Calculate the sum of remaining slots
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
	protected void sortHI(List<VertexScheduling> ready, int slot, int level) {
		// Check if tasks need to be delayed first
		for (VertexScheduling v : ready) {
			int relatSlot = slot % v.getGraphDead();
			int dId = v.getGraphId();
			
			if (level != getLevels() - 1) {
				
				int delta = v.getWcet(level + 1) - v.getWcet(level);
				// Check if the tasks needs to be delayed
				if (scheduledUntilTinL(v, slot + 1, level + 1) <= delta) {
					if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Task "+v.getName()+" needs to be delayed at slot @t = "+slot);
					v.setDelayed(true);
					v.setWeightInL(Integer.MAX_VALUE, level);
				} else {
					// Check laxity first
					if (v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()] == 0)
						v.setWeightInL(0, level);
					else
						v.setWeightInL(v.getDeadlines()[level], level);
					v.setDelayed(false);
				}
			} else {
				// Check laxity first
				if (v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()] == 0)
					v.setWeightInL(0, level);
				else
					v.setWeightInL(v.getDeadlines()[level], level);
				v.setDelayed(false);
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
			
			// If it's a HI task check if it needs to be promoted
			if (v.getWcet(level + 1) > 0) {
				//Promotion needed for the task
				if ((v.getWcet(level) - getRemainingTime()[level][dId][v.getId()]) - scheduledUntilTinL(v, slot, level + 1) < 0) {
					if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Promotion of task "+v.getName()+" at slot @t = "+slot);
					v.setWeightInL(0, level);
				} else {
					// Verify laxity first
					if (v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()] == 0)
						v.setWeightInL(0, level);
					else
						v.setWeightInL(v.getDeadlines()[level], level);
				}
			} else {
				// Verify laxity first
				if (v.getDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()] == 0)
					v.setWeightInL(0, level);
				else
					v.setWeightInL(v.getDeadlines()[level], level);
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
