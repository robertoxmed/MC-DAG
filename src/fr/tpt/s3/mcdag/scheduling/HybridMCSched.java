package fr.tpt.s3.mcdag.scheduling;

import java.util.List;

import fr.tpt.s3.mcdag.model.VertexScheduling;

public class HybridMCSched extends GenericMixedCriticalityScheduler {

	@Override
	protected boolean verifyConstraints(List<VertexScheduling> ready, int slot, int level) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void sortHI(List<VertexScheduling> ready, int slot, int level) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void sortLO(List<VertexScheduling> ready, int slot, int level) {
		// TODO Auto-generated method stub
		
	}

}
