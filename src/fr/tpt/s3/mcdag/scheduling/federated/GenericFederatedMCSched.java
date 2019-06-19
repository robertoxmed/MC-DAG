package fr.tpt.s3.mcdag.scheduling.federated;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.SchedulingException;

public abstract class GenericFederatedMCSched {
	
	// Set of MC-DAGs to schedule
	private Set<McDAG> mcDAGs;
	private Set<McDAG> heavyDAGs;
	
	// Set of scheduling tables
	private Hashtable<McDAG, String[][][]> schedTables;
	
	// Set of remaining times
	private Hashtable<McDAG,int[][]> remainingTime;
	
	private int nbCores;
	private int levels;
	
	// Preemption counters
	private Hashtable<VertexScheduling, Integer> preemptions;
	private int activations;
	
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
	 * Internal function that checks if all the sucessors of an actor are visited
	 * @param a
	 * @param level
	 * @return
	 */
	protected boolean succVisitedInLevel (VertexScheduling a, int level) {
		for (Edge e : a.getSndEdges()) {
			if (e.getDest().getWcet(level) != 0 && !((VertexScheduling) e.getDest()).getVisitedL()[level])
				return false;
		}
		return true;
	}
	
	/**
	 * Function to affect the proper deadline to a vertex
	 * @param v
	 * @param level
	 * @param deadline
	 */
	protected void calcDeadline (VertexScheduling v, int level, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (v.isSinkinL(level)) {
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : v.getSndEdges()) {
				test = ((VertexScheduling) e.getDest()).getDeadlines()[level] - e.getDest().getWcet(level);
				if (test < ret)
					ret = test;
			}
		}
		v.setDeadlineInL(ret, level);
	}
	
	/**
	 * Function to calculate deadlines in function of criticality levels
	 * @param d
	 */
	protected void calcDeadlines (McDAG d) {
		for (int i = 0; i < getLevels(); i++) {
			ArrayList<VertexScheduling> toVisit = new ArrayList<VertexScheduling>();
			
			for (Vertex v : d.getVertices()) {
				if (v.isSinkinL(i))
					toVisit.add((VertexScheduling) v);
			}
			
			while (!toVisit.isEmpty()) {
				VertexScheduling v = toVisit.get(0);
				
				calcDeadline(v, i, d.getDeadline());
				v.getVisitedL()[i] = true;
				
				for (Edge e : v.getRcvEdges()) {
					if (!((VertexScheduling) e.getSrc()).getVisitedL()[i]
							&& succVisitedInLevel((VertexScheduling) e.getSrc(), i)
							&& !toVisit.contains((VertexScheduling) e.getSrc())) {
						toVisit.add((VertexScheduling) e.getSrc());
					}
				}
				toVisit.remove(0);
			}
		}
	}

	/**
	 * Function to initialize tables with the respective
	 */
	protected void init () throws SchedulingException {
		// Check for heavy DAGs
		int coresQuota = getNbCores();
		heavyDAGs = new HashSet<McDAG>();
		
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
	
	/**
	 * Functions that adds new jobs when task have finished their execution
	 * @param ready
	 * @param level
	 */
	protected void checkJobActivations (List<VertexScheduling> ready, List<VertexScheduling> scheduled, McDAG d, int level) {		
		for (VertexScheduling v : scheduled) {
			
			for (Edge e :v.getSndEdges()) {
				VertexScheduling connectedVertex = (VertexScheduling) (e.getDest());
				boolean add = true;
					
				for (Edge e2 :connectedVertex.getRcvEdges()) {
					VertexScheduling checkedVertex = (VertexScheduling) (e2.getSrc());
						
					
					if (!scheduled.contains(checkedVertex)) {
						add = false;
						break;
					}
				}
					
				if (add && !ready.contains(connectedVertex)
						&& remainingTime.get(d)[level][connectedVertex.getId()] != 0) {
					ready.add(connectedVertex);
				}
			} 
		}
	}
	
	/**
	 * Function that computes a scheduling table for a given level
	 * @param d
	 * @param level
	 * @throws SchedulingException
	 */
	protected void buildTable (McDAG d, int level) throws SchedulingException {
		List<VertexScheduling> ready = new LinkedList<VertexScheduling>();
		List<VertexScheduling> scheduled = new LinkedList<VertexScheduling>();
		int cores = (int)(Math.ceil(d.getUmax()));
		
		// Add all source nodes
		for (Vertex v : d.getVertices()) {
			if (v.isSourceinL(level))
				ready.add((VertexScheduling) v);
		}
		
		sort(ready, 0, level);
		
		ListIterator<VertexScheduling> lit = ready.listIterator();
		boolean jobFinished = false;
		
		// Iterate through time indexes
		for (int timeIndex = 0; timeIndex < d.getDeadline(); timeIndex++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] Federated buildTable("+level+"): @t = "+timeIndex+", tasks activated: ");
				for (VertexScheduling v : ready)
					System.out.print("Prio("+v.getName()+") = "+v.getWeights()[level]+"; ");
				System.out.println("");
			}
			
			if (!verifyConstraints(ready, timeIndex, level)) {
				SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] buildTable("+level+"): Ready list not empty.");
				throw se;
			}
			
			// Allocate to cores
			for (int coreIndex = 0; coreIndex < cores; coreIndex++) {
				// Find next ready task
				if (lit.hasNext()) {
					VertexScheduling v = lit.next();
					int[][] val  = remainingTime.get(d);
					
					schedTables.get(d)[level][timeIndex][coreIndex] = v.getName();
					val[level][v.getId()]--;
					
					if (val[level][v.getId()] == 0) {
						scheduled.add(v);
						jobFinished = true;
						lit.remove();
					}
					
					remainingTime.put(d, val);
				}
			}
			
			// When a job finishes it could activate successors
			if (jobFinished)
				checkJobActivations(ready, scheduled, d, level);
			sort(ready, timeIndex, level);
			jobFinished = false;
			lit = ready.listIterator();
		}
		
		if (!ready.isEmpty()) {
			SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] buildTable("+level+"): Ready list not empty.");
			throw se;
		}
	}
	
	/**
	 * Method that schedules the system using a federated approach
	 * @throws SchedulingException
	 */
	public void scheduleSystems () throws SchedulingException {
		init();
		
		for (McDAG d : heavyDAGs) {
			calcDeadlines(d);
			if (debug) printDeadlines(d);
			
			for (int i = 0; i < getLevels(); i++) {
				buildTable(d, i);
			}
			
			if (debug) printTables(d);
		}
		
	}
	
	/*
	 * DEBUG FUNCTIONS
	 */
	
	/**
	 * Prints LFTs for all DAGs and all nodes in all the levels
	 * @param d
	 */
	protected void printDeadlines (McDAG d) {
		System.out.println("[DEBUG "+Thread.currentThread().getName()+"] Federated: DAG "+d.getId()+" printing deadlines");
		
		for (Vertex a : d.getVertices()) {
			System.out.print("[DEBUG "+Thread.currentThread().getName()+"]\t Actor "+a.getName()+", ");
			for (int i = 0; i < getLevels(); i++) {
				if (((VertexScheduling)a).getDeadlines()[i] != Integer.MAX_VALUE)
					System.out.print(((VertexScheduling)a).getDeadlines()[i]);
				System.out.print(" ");
			}
			System.out.println("");
		}
	}
	
	/**
	 * Prints the scheduling tables
	 */
	protected void printTables (McDAG d) {
		String [][][] sched = schedTables.get(d);
		int cores = (int) Math.ceil(d.getUmax());
		
		for (int i = getLevels() - 1; i >= 0; i--) {
			System.out.println("Scheduling table in mode "+ i+":");
			for (int c = 0; c < cores; c++) {
				for (int s = 0; s < d.getDeadline(); s++) {
					if (sched[i][s][c] != null)
						System.out.print(sched[i][s][c]+" | ");
					else
						System.out.print("-- | ");
				}
				System.out.print("\n");
			}
		}
		System.out.print("\n");
	}
	
	/*
	 * Getters & setters
	 */
	
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

	public Hashtable<VertexScheduling, Integer> getPreemptions() {
		return preemptions;
	}

	public void setPreemptions(Hashtable<VertexScheduling, Integer> preemptions) {
		this.preemptions = preemptions;
	}

	public Set<McDAG> getHeavyDAGs() {
		return heavyDAGs;
	}

	public void setHeavyDAGs(Set<McDAG> heavyDAGs) {
		this.heavyDAGs = heavyDAGs;
	}

	public int getActivations() {
		return activations;
	}

	public void setActivations(int activations) {
		this.activations = activations;
	}
}
