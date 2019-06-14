package fr.tpt.s3.mcdag.scheduling.federated;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.SchedulingException;

public abstract class GenericFederatedMCSched {
	
	// Set of MC-DAGs to schedule
	private Set<McDAG> mcDAGs;
	
	// Set of scheduling tables
	private Hashtable<McDAG, String[][][]> schedTables;
	
	// Set of remaining times
	private Hashtable<McDAG,int[][]> remainingTime;
	
	private int nbCores;
	private int levels;
	
	// Debugging boolean
	private boolean debug;
	
	/**
	 * Fucntion that verifies if the scheduling should continue
	 * @param ready
	 * @param slot
	 * @param level
	 * @return
	 */
	protected abstract boolean verifyConstraints (List<VertexScheduling> ready, int slot, int level);
	
	/**
	 * Function to sort the ready list
	 * @param ready
	 * @param slot
	 * @param level
	 */
	protected abstract void sort (List<VertexScheduling> ready, int slot, int level);

	/**
	 * Function to initialize tables with the respective
	 */
	protected void init () throws SchedulingException {
		
		// Check for heavy DAGs
		int coresQuota = getNbCores();
		Set<McDAG> heavyDAGs = new HashSet<McDAG>();
		
		// Separate heavy DAGs and check quota
		for (McDAG d : getMcDAGs()) {
			if (d.getUmax() < 1) {
				if (debug) System.err.println("[DEBUG "+Thread.currentThread().getName()+"] Scheduler does not check for light DAGs schedulability");
			} else {
				coresQuota -= Math.ceil(d.getUmax());
				heavyDAGs.add(d);
			}
		}
		
		if (coresQuota < 0) {
			SchedulingException se = new SchedulingException("Federated Scheduling > Not enough cores");
			throw se;
		}
		
		// Init variables 
		for (McDAG d : heavyDAGs) {
			int cores = (int)(Math.ceil(d.getUmax()));
			String[][][] dagTables = new String[getLevels()][d.getDeadline()][cores];
			
			// Initialize tables
			for (int i = 0; i < getLevels(); i++) {
				for (int j = 0; j < d.getDeadline(); j++) {
					for (int k = 0; k < cores; k++)
						dagTables[i][j][k] = "-";
				}
			}
			schedTables.put(d, dagTables);
			
			// Initialize remaining times
			int[][] remaining = new int[getLevels()][d.getVertices().size()];
			for (int i = 0; i < getLevels(); i++) {
				for (Vertex v : d.getVertices())
					remaining[i][v.getId()] = v.getWcet(i);
			}
			
			remainingTime.put(d, remaining);
		}
		
		if (debug) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] initTables(): Sched tables initialized!");	
	}
	
	protected void buildTable (int level) {
		
	}

	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}

	public Set<McDAG> getMcDAGs() {
		return mcDAGs;
	}

	public void setMcDAGs(Set<McDAG> mcDAGs) {
		this.mcDAGs = mcDAGs;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getLevels() {
		return levels;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}

	public Hashtable<McDAG, String[][][]> getSchedTables() {
		return schedTables;
	}

	public void setSchedTables(Hashtable<McDAG, String[][][]> schedTables) {
		this.schedTables = schedTables;
	}

	public Hashtable<McDAG,int[][]> getRemainingTime() {
		return remainingTime;
	}

	public void setRemainingTime(Hashtable<McDAG,int[][]> remainingTime) {
		this.remainingTime = remainingTime;
	}	
}
