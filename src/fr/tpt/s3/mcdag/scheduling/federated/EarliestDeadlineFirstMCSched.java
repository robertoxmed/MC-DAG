package fr.tpt.s3.mcdag.scheduling.federated;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.VertexScheduling;

/**
 * Implementation of EDF priority ordering 
 * @author Roberto Medina
 *
 */
public class EarliestDeadlineFirstMCSched extends GenericFederatedMCSched {
	
	public EarliestDeadlineFirstMCSched(Set<McDAG> DAGs, int cores, int levels, boolean debug) {
		setMcDAGs(DAGs);
		setDebug(debug);
		setNbCores(cores);
		setLevels(levels);
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
		Collections.sort(ready, new Comparator<VertexScheduling>() {
			@Override
			public int compare (VertexScheduling o1, VertexScheduling o2) {
				if (o1.getDeadlines()[level] - o2.getDeadlines()[level] != 0)
					return o1.getDeadlines()[level] - o2.getDeadlines()[level];
				else
					return o1.getId() - o2.getId();
			}
		});
	}
	
}
