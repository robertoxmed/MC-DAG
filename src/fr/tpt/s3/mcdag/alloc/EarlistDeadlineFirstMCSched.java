package fr.tpt.s3.mcdag.alloc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Actor;
import fr.tpt.s3.mcdag.model.ActorSched;
import fr.tpt.s3.mcdag.model.DAG;
import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.util.MathMCDAG;

/**
 * Heuristic based on EDF to schedule Multiple MC-DAGs
 * @author roberto
 *
 */
public class EarlistDeadlineFirstMCSched extends AbstractMixedCriticalityScheduler {
	
	// Set of MC-DAGs to be scheduled
	private Set<DAG> mcDags;
	
	// Architecture + hyper-period + nb of levels
	private int nbCores;
	private int hPeriod;
	private int levels;
	
	// Scheduling tables
	private String sched[][][];
	
	// Remaining time to be allocated for each node
	// Level, DAG id, Actor id
	private int remainingTime[][][];
	
	// Comparator to order Actors
	private Comparator<ActorSched> loComp;
	
	// Debugging boolean
	private boolean debug;
	
	
	public EarlistDeadlineFirstMCSched (Set<DAG> dags, int cores, int levels, boolean debug) {
		setMcDags(dags);
		setNbCores(cores);
		setLevels(levels);
		setDebug(debug);
		remainingTime = new int[getLevels()][getMcDags().size()][];
		
		// Init remaining time for each DAG
		for (DAG d : getMcDags()) {
			for (int i = 0; i < getLevels(); i++)
				remainingTime[i][d.getId()] = new int[d.getNodes().size()];
		}
		
		setLoComp(new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched arg0, ActorSched arg1) {
				if (arg0.getLFTs()[0] - arg1.getLFTs()[0] != 0)
					return arg0.getLFTs()[0] - arg1.getLFTs()[0];
				else
					return arg0.getId() - arg1.getId();
			}
		});
		
	}
	
	private void initRemainingTimes () {
		for (int i = 0; i < getLevels(); i++) {
			for (DAG d : getMcDags()) {
				for (Actor a : d.getNodes())
					remainingTime[i][d.getId()][a.getId()] = a.getWcet(i);
			}
		}
	}
	
	@Override
	protected void initTables() {
		int[] input = new int[getMcDags().size()];
		int i = 0;
		
		for (DAG d : getMcDags()) {
			input[i] = d.getDeadline();
			i++;
		}
		
		sethPeriod(MathMCDAG.lcm(input));
		
		// Init scheduling tables
		sched = new String[getLevels()][gethPeriod()][getNbCores()];
		
		for (i = 0; i < getLevels(); i++) {
			for (int j = 0; j < gethPeriod(); j++) {
				for (int k = 0; k < getNbCores(); k++)
					sched[i][j][k] = "-";
			}
		}
		
		if (debug) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] initTables(): Sched tables initialized!");
	}
	
	
	private void calcDeadlineReverse (ActorSched a, int level, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSourceinL(level)) {
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getRcvEdges()) {
				test = ((ActorSched) e.getSrc()).getLFTs()[level] - e.getSrc().getWcet(level);
				if (test < ret)
					ret = test;
			}
		}
		a.setLFTinL(ret, level);
	}
	
	private void calcDeadline (ActorSched a, int level, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSinkinL(level)) {
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getSndEdges()) {
				test = ((ActorSched) e.getDest()).getLFTs()[level] - e.getDest().getWcet(level);
				if (test < ret)
					ret = test;
			}
		}
		a.setLFTinL(ret, level);
	}
	
	private boolean predVisitedInLevel (ActorSched a, int level) {
		for (Edge e : a.getRcvEdges()) {
			if (e.getSrc().getWcet(level) != 0 && !((ActorSched) e.getSrc()).getVisitedL()[level])
				return false;
		}
		return true;
	}
	
	private boolean succVisitedInLevel (ActorSched a, int level) {
		for (Edge e : a.getSndEdges()) {
			if (e.getDest().getWcet(level) != 0 && !((ActorSched) e.getDest()).getVisitedL()[level])
				return false;
		}
		return true;
	}
	
	/**
	 * Calculate deadlines for a DAG in all its mode
	 * @param d The MC-DAG
	 */
	private void calcDeadlines (DAG d) {
		
		// Start by calculating deadlines in HI modes
		for (int i = 1; i < getLevels(); i++) {
			ArrayList<ActorSched> toVisit = new ArrayList<ActorSched>();
			
			// Calculate sources in i mode
			for (Actor a : d.getNodes()) {
				if (a.isSourceinL(i))
					toVisit.add((ActorSched) a);
			}
			
			// Visit all nodes iteratively
			while (!toVisit.isEmpty()) {
				ActorSched a = toVisit.get(0);
				
				calcDeadlineReverse(a, i, d.getDeadline());
				a.getVisitedL()[i] = true;
				
				for (Edge e: a.getSndEdges()) {
					if (e.getDest().getWcet(i) != 0 && !((ActorSched) e.getDest()).getVisitedL()[i]
							&& predVisitedInLevel((ActorSched) e.getDest(), i)
							&& !toVisit.contains((ActorSched) e.getDest())) {
						toVisit.add((ActorSched) e.getDest());
					}
				}
				toVisit.remove(0);
			}
		}
		
		// Calculate deadlines in LO mode
		ArrayList<ActorSched> toVisit = new ArrayList<ActorSched>();
		// Calculate sources in i mode
		for (Actor a : d.getNodes()) {
			if (a.isSinkinL(0))
				toVisit.add((ActorSched) a);
		}
					
		// Visit all nodes iteratively
		while (!toVisit.isEmpty()) {
			ActorSched a = toVisit.get(0);
						
			calcDeadline(a, 0, d.getDeadline());
			a.getVisitedL()[0] = true;
						
			for (Edge e: a.getRcvEdges()) {
				if (!((ActorSched) e.getSrc()).getVisitedL()[0]
						&& succVisitedInLevel((ActorSched) e.getSrc(), 0)
						&& !toVisit.contains((ActorSched) e.getSrc())) {
					toVisit.add((ActorSched) e.getSrc());
				}
			}
			toVisit.remove(0);
		}
	}

	private int scheduledUntilTinL (ActorSched a, int t, int l) {
		int ret = 0;
		int start = (int)(t / a.getGraphDead()) * a.getGraphDead();
		
		for (int i = start; i <= t; i++) {
			for (int c = 0; c < nbCores; c++) {
				if (sched[l][i][c] !=  null) {
					if (sched[l][i][c].contentEquals(a.getName()))
						ret++;
				}
			}
		}
		
		return ret;
	}

	/**
	 * Checks how many slots have been allocated for a in l mode in reverse
	 * from the deadline until the current slot
	 * @param a
	 * @param t
	 * @param l
	 * @return
	 */
	private int scheduledUntilTinLreverse (ActorSched a, int t, int l) {
		int ret = 0;
		int end = 0;
		
		int realSlot = gethPeriod() - t;
		
		if (t == 0)
			return 0;
		
		if ((int)(realSlot/a.getGraphDead()) <= 0 || realSlot % a.getGraphDead() == 0) {
			end = a.getGraphDead() - 1;
		} else {
			end = ((int)(realSlot / a.getGraphDead()) + 1)  * a.getGraphDead() - 1;
		}
		
		//System.out.println("\t\t\t [schedut] task "+a.getName()+" end "+end+" slot "+realSlot);
		
		for (int i = end; i > realSlot; i--) {
			for (int c = 0; c < nbCores; c++) {
				if (sched[l][i][c] !=  null) {
					if (sched[l][i][c].contentEquals(a.getName()))
						ret++;
				}
			}
		}
		return ret;
	}
	
	/**
	 * Checks for new activations in HI modes
	 * @param sched
	 * @param ready
	 * @param level
	 */
	private void checkActivationHI (List<ActorSched> sched, List<ActorSched> ready, int level) {

		for (ActorSched a : sched) {
			// Check predecessors of task that was just allocated
			for (Edge e : a.getRcvEdges()) {
				ActorSched pred = (ActorSched) e.getSrc();
				boolean add = true;
				
				// Check if all successors of the predecessor have been allocated
				for (Edge e2 : pred.getSndEdges()) {
					if (e2.getDest().getWcet(level) > 0 && !sched.contains(e2.getDest())) {
						add = false;
						break;
					}
				}
				
				if (add && !ready.contains(pred) && remainingTime[level][pred.getGraphID()][pred.getId()] != 0) {
					ready.add(pred);
				}
			}
		}
	}
	
	/**
	 * Checks for new activations in the LO mode
	 * @param sched
	 * @param ready
	 * @param level
	 */
	private void checkActivationLO (List<ActorSched> sched, List<ActorSched> ready) {

		for (ActorSched a : sched) {
			// Check predecessors of task that was just allocated
			for (Edge e : a.getSndEdges()) {
				ActorSched succ = (ActorSched) e.getDest();
				boolean add = true;
				
				// Check if all successors of the predecessor have been allocated
				for (Edge e2 : succ.getRcvEdges()) {
					if (!sched.contains(e2.getSrc())) {
						add = false;
						break;
					}
				}
				
				if (add && !ready.contains(succ) && remainingTime[0][succ.getGraphID()][succ.getId()] != 0) {
					ready.add(succ);
				}
			}
		}
	}
	
	/**
	 * Checks for new activations of DAGs
	 * @param sched
	 * @param ready
	 * @param slot
	 * @param level
	 */
	private void checkDAGActivation (List<ActorSched> sched, List<ActorSched> ready, int slot, int level) {
		for (DAG d : getMcDags()) {
			// If the slot is a multiple of the deadline is a new activation
			if (slot % d.getDeadline() == 0) {
				ListIterator<ActorSched> it = sched.listIterator();
				
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] checkDAGActivation(): DAG (id. "+d.getId()+") activation at slot "+slot);
				for (Actor a : d.getNodes()) {
					while (it.hasNext()) { // Remove nodes from the sched list
						ActorSched a2 = it.next();
						if (a.getName().contentEquals(a2.getName()))
							it.remove();
					}
					it = sched.listIterator();
					// Re-init execution time
					remainingTime[level][((ActorSched)a).getGraphID()][a.getId()] = a.getWcet(level);
					
					if (level >= 1 && a.isSinkinL(level)) {
						ready.add((ActorSched)a);
					} else if (level == 0 && a.isSourceinL(level)) {
						ready.add((ActorSched)a);
					}
				}
			}
		}
	}
	
	private void buildHITable (final int level) throws SchedulingException {
		List<ActorSched> ready = new LinkedList<ActorSched>();
		List<ActorSched> scheduled = new LinkedList<ActorSched>();
		
		// Add all sink nodes
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSinkinL(level))
					ready.add((ActorSched) a);
			}
		}
		
		Collections.sort(ready, new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched o1, ActorSched o2) {
				if (o1.getLFTs()[level] - o2.getLFTs()[level] != 0)
					return o1.getLFTs()[level] - o2.getLFTs()[level];
				else
					return o1.getId() - o2.getId();
			}
		});
		
		// Allocate slot by slot
		ListIterator<ActorSched> lit = ready.listIterator();
		boolean taskFinished = false;
		
		for (int s = hPeriod - 1; s >= 0; s--) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildHITable("+level+"): @t = "+s+", tasks activated: ");
				for (ActorSched a : ready)
					System.out.print("L("+a.getName()+") = "+a.getLaxities()[level]+"; ");
				System.out.println("");
			}
			
			for (int c = getNbCores() - 1; c >= 0; c--) {
				// Find a ready task
				if (lit.hasNext()) {
					ActorSched a = lit.next();
				}
			}
		}
	}

	@Override
	public void buildAllTables() throws SchedulingException {
		initTables();
		initRemainingTimes();
		
		// Calculate deadlines for all DAGs
		for (DAG d : getMcDags()) {
			calcDeadlines(d);
			if (isDebug()) printDeadlines(d);
		}
	}
	
	/*
	 * DEBUG FUNCTIONS
	 */
	
	private void printDeadlines (DAG d) {
		System.out.println("[DEBUG "+Thread.currentThread().getName()+"] DAG "+d.getId()+" printing LFTs");
		
		for (Actor a : d.getNodes()) {
			System.out.print("[DEBUG "+Thread.currentThread().getName()+"]\t Actor "+a.getName()+", ");
			for (int i = 0; i < getLevels(); i++) {
				if (((ActorSched)a).getLFTs()[i] != Integer.MAX_VALUE)
					System.out.print(((ActorSched)a).getLFTs()[i]);
				System.out.print(" ");
			}
			System.out.println("");
		}
	}
	
	/**
	 * Prints the scheduling tables
	 */
	public void printTables () {
		for (int i = getLevels() - 1; i >= 0; i--) {
			System.out.println("Scheduling table in mode "+ i+":");
			for (int c = 0; c < getNbCores(); c++) {
				for (int s = 0; s < gethPeriod(); s++) {
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

	public Set<DAG> getMcDags() {
		return mcDags;
	}


	public void setMcDags(Set<DAG> mcDags) {
		this.mcDags = mcDags;
	}


	public int getNbCores() {
		return nbCores;
	}


	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}


	public int gethPeriod() {
		return hPeriod;
	}


	public void sethPeriod(int hPeriod) {
		this.hPeriod = hPeriod;
	}


	public int getLevels() {
		return levels;
	}


	public void setLevels(int levels) {
		this.levels = levels;
	}


	public String[][][] getSched() {
		return sched;
	}


	public void setSched(String[][][] sched) {
		this.sched = sched;
	}


	public int[][][] getRemainingTime() {
		return remainingTime;
	}


	public void setRemainingTime(int[][][] remainingTime) {
		this.remainingTime = remainingTime;
	}


	public boolean isDebug() {
		return debug;
	}


	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public Comparator<ActorSched> getLoComp() {
		return loComp;
	}

	public void setLoComp(Comparator<ActorSched> loComp) {
		this.loComp = loComp;
	}

}
