package fr.tpt.s3.mcdag.scheduling.galap.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.galap.GlobalGenericMCScheduler;

/**
 * Adaptation of the EZL scheduler
 * @author Roberto Medina
 *
 */
public class EarlistDeadlineZeroLaxityMCSched extends GlobalGenericMCScheduler {

	public EarlistDeadlineZeroLaxityMCSched(Set<McDAG> DAGs, int cores, int levels, boolean debug, boolean benchmark) {
		setMcDAGs(DAGs);
		setNbCores(cores);
		setLevels(levels);
		setCountPreempt(benchmark);
		setDebug(debug);
	}
	
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
		
		for (VertexScheduling v : ready) {
			// Task is activated and deadline was missed
			int relatSlot = slot % v.getDagRef().getDeadline();
			if (level >= 1)
				relatSlot = (gethPeriod() - slot - 1) % v.getDagRef().getDeadline();
			
			if (relatSlot > v.getModifiedDeadlines()[level]) {
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
			int relatSlot = slot % v.getDagRef().getDeadline();
			int dId = v.getDagRef().getId();
			
			// Check laxity first
			if (v.getModifiedDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()] == 0)
				v.setWeightInL(0, level);
			else
				v.setWeightInL(v.getModifiedDeadlines()[level], level);
			v.setDelayed(false);
			
			// Check if the tasks needs to be delayed
			if (level != getLevels() - 1 && v.getWcet(level + 1) != 0) {
				int delta = v.getWcet(level + 1) - v.getWcet(level);
				
				if (scheduledUntilTinLreverse(v, slot + 1, level + 1) <= delta) {
					if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Task "+v.getName()+" needs to be delayed at slot @t = "+slot);
					v.setDelayed(true);
					v.setWeightInL(Integer.MAX_VALUE, level);
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
			int relatSlot = slot % v.getDagRef().getDeadline();
			int dId = v.getDagRef().getId();
			
			if (v.getModifiedDeadlines()[level] - relatSlot - getRemainingTime()[level][dId][v.getId()] == 0)
				v.setWeightInL(0, level);
			else
				v.setWeightInL(v.getModifiedDeadlines()[level], level);
			
			// If it's a HI task check if it needs to be promoted
			if (v.getWcet(level + 1) > 0) {
				//Promotion needed for the task
				if ((v.getWcet(level) - getRemainingTime()[level][dId][v.getId()]) - scheduledUntilTinL(v, slot, level + 1) < 0) {
					if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Promotion of task "+v.getName()+" at slot @t = "+slot);
					v.setWeightInL(0, level);
				}
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
