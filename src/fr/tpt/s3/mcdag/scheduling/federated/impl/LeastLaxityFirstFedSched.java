package fr.tpt.s3.mcdag.scheduling.federated.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.federated.GenericFederatedMCSched;

public class LeastLaxityFirstFedSched extends GenericFederatedMCSched{

	public LeastLaxityFirstFedSched(Set<McDAG> DAGs, int cores, int levels, boolean debug) {
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
				if (isDebug()) System.err.println("[DEBUG "+Thread.currentThread().getName()+"] FederatedLLF verifyConstraints(): negative laxity on task "+v.getName());
				return false;
			} else if (v.getWeights()[level] == 0) {
				sumZeroLax += 1;
			}
		}
		
		// More than m tasks have zero laxity
		if (sumZeroLax > getNbCores()) {
			if (isDebug()) System.err.println("[DEBUG "+Thread.currentThread().getName()+"] FederatedLLF verifyConstraints(): more than m zero laxity tasks");
			return false;
		}
		return true;
	}

	@Override
	protected void sort(List<VertexScheduling> ready, int slot, int level) {
		for (VertexScheduling v : ready)			
			v.setWeightInL(v.getDeadlines()[level] - slot - getRemainingTime().get(v.getDagRef())[level][v.getId()], level);
		
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
