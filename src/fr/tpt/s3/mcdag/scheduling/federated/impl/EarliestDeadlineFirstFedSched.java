package fr.tpt.s3.mcdag.scheduling.federated.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.federated.GenericFederatedMCSched;

/**
 * Implementation of EDF priority ordering 
 * @author Roberto Medina
 *
 */
public class EarliestDeadlineFirstFedSched extends GenericFederatedMCSched {
	
	public EarliestDeadlineFirstFedSched(Set<McDAG> DAGs, int cores, int levels, boolean debug) {
		setMcDAGs(DAGs);
		setDebug(debug);
		setNbCores(cores);
		setLevels(levels);
		setSchedTables(new Hashtable<McDAG, String[][][]>());
		setRemainingTime(new Hashtable<McDAG,int[][]>());
	}

	@Override
	protected boolean verifyConstraints(List<VertexScheduling> ready, int slot, int level) {
		for (VertexScheduling v : ready) {
			if (slot > v.getDeadlines()[level]) {
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] verifyConstraints(): deadline not respected for "+v.getName());
				return false;
			}
		}
		return true;
	}

	@Override
	protected void sort(List<VertexScheduling> ready, int slot, int level) {
		for (VertexScheduling v : ready) {
			v.setWeightInL(v.getDeadlines()[level], level);
		}
		Collections.sort(ready, new Comparator<VertexScheduling>() {
			@Override
			public int compare (VertexScheduling o1, VertexScheduling o2) {
				if (o1.getWeights()[level] - o2.getWeights()[level] != 0)
					return o1.getWeights()[level] - o2.getWeights()[level];
				else
					return o1.getId() - o2.getId();
			}
		});
	}
	
}
