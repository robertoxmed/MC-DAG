package fr.tpt.s3.mcdag.scheduling.federated.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.federated.GenericFederatedMCSched;

public class EarliestDeadlineZeroLaxityFedSched extends GenericFederatedMCSched{

	public EarliestDeadlineZeroLaxityFedSched(Set<McDAG> DAGs, int cores, int levels, boolean debug) {
		setMcDAGs(DAGs);
		setDebug(debug);
		setNbCores(cores);
		setLevels(levels);
		setSchedTables(new Hashtable<McDAG, String[][][]>());
		setRemainingTime(new Hashtable<McDAG, int[][]>());
	}
	
	@Override
	protected boolean verifyConstraints(List<VertexScheduling> ready, int slot, int level) {
		int sumZeroLax = 0;
		
		for (VertexScheduling v : ready) {
			if (v.getWeights()[level] < 0) {
				if (isDebug()) System.err.println("[DEBUG "+Thread.currentThread().getName()+"] FederatedEZL verifyConstraints(): negative laxity on task "+v.getName());
				return false;
			} else if (v.getWeights()[level] == 0) {
				sumZeroLax += 1;
			}
			
			if (slot > v.getDeadlines()[level]) {
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] FederatedEZL verifyConstraints(): deadline not respected for "+v.getName());
				return false;
			}
		}
		
		// More than m tasks have zero laxity
		if (sumZeroLax > getNbCores()) {
			if (isDebug()) System.err.println("[DEBUG "+Thread.currentThread().getName()+"] FederatedEZL verifyConstraints(): more than m zero laxity tasks");
			return false;
		}
		return true;
	}

	@Override
	protected void sort(List<VertexScheduling> ready, int slot, int level) {
		for (VertexScheduling v : ready) {
			int laxity = v.getDeadlines()[level] - slot - getRemainingTime().get(v.getDagRef())[level][v.getId()];
			
			if (laxity == 0)
				v.setWeightInL(0, level);
			else
				v.setWeightInL(v.getDeadlines()[level], level);
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
