package fr.tpt.s3.mcdag.alloc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Actor;
import fr.tpt.s3.mcdag.model.ActorSched;
import fr.tpt.s3.mcdag.model.DAG;
import fr.tpt.s3.mcdag.model.Edge;

/**
 * Class implementing Federated MC-DAG scheduling
 * @author roberto
 *
 */
public class Federated {
	
	// Set of DAGs to be scheduled 
	private Set<DAG> mcDags;
	
	// Architecture
	private int nbCores;
	
	private Comparator<ActorSched> loComp;
	private Comparator<ActorSched> hiComp;
	
	private boolean debug;
	
	/**
	 * Constructor
	 * @param system
	 * @param architecture
	 */
	public Federated (Set<DAG> system, int architecture) {
		setMcDags(system);
		setNbCores(architecture);
		setDebug(debug);
		setLoComp(new Comparator<ActorSched>() {
			@Override
			public int compare (ActorSched o1, ActorSched o2) {
				if (o1.getHlfet()[0] - o2.getHlfet()[0] != 0)
					return o1.getHlfet()[0] - o2.getHlfet()[0];
				else
					return o1.getId() - o2.getId();
			}
		});
		setHiComp(new Comparator<ActorSched>() {
			@Override
			public int compare (ActorSched o1, ActorSched o2) {
				if (o1.getHlfet()[1] - o2.getHlfet()[1] != 0)
					return o1.getHlfet()[1] - o2.getHlfet()[1];
				else
					return o1.getId() - o2.getId();
			}
		});
	}
	
	private void initRemainingTimes (DAG d, int remainingTime[], int level) {
		
		for (int i = 0; i < remainingTime.length; i++)
			remainingTime[i] = 0;
		
		for (Actor a : d.getNodes())
			remainingTime[a.getId()] = a.getcIs()[level];		
	}
	
	private void calcHLFETs (DAG d, int level, List<ActorSched> prioOrder) {
		
		ArrayList<ActorSched> toVisit = new ArrayList<ActorSched>();
		
		// Look for sinks first
		for (Actor a : d.getNodes()) {
			if (a.getSndEdges().size() == 0 &&
					a.getCI(level) != 0)
				toVisit.add((ActorSched)a);
		}
		
		// Iterate through nodes and compute their HLFET
		while (toVisit.size() != 0) {
			ActorSched a = toVisit.get(0);
			
			if (a.getSndEdges().size() == 0) {
				a.getHlfet()[level] = a.getCI(level);
			} else {
				// Look for the max on the SndEdges
				int max = 0;
				
				for (Edge e : a.getSndEdges()) {
					ActorSched dest = (ActorSched) e.getDest();
					if (dest.getHlfet()[level] > max)
						max = a.getHlfet()[level];
				}
				a.getHlfet()[level] = max + a.getCI(level);
			}
			
			a.getVisitedL()[level] = true;
	
			for (Edge e : a.getRcvEdges()) {
				boolean allSuccVisited = true;
				Actor test = e.getSrc();
				
				for (Edge e2 : test.getSndEdges()) {
					ActorSched dest = (ActorSched) e2.getDest();
					if (!dest.getVisitedL()[level]) {
						allSuccVisited = false;
						break;
					}
				}
				
				if (allSuccVisited && a.getCI(level) != 0 && !toVisit.contains((ActorSched)a))
					toVisit.add((ActorSched)a);
			}
			toVisit.remove(0);
		}
		
		// Create the list with the priority ordering
		for (Actor a : d.getNodes()) {
			if (a.getCI(level) != 0)
				prioOrder.add((ActorSched) a);
		}
		prioOrder.sort(new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched o1, ActorSched o2) {
				if (o1.getHlfet()[level] - o2.getHlfet()[level] != 0)
					return o1.getHlfet()[level] - o2.getHlfet()[level];
				else
					return o1.getId() - o2.getId();
			}	
		});	
	}
	
	private boolean enoughSlots (List<ActorSched> ready, int slot, int deadline, int cores, int remainingTime[]) {
		int sumReady = 0;
		int rSlots = ((deadline -1) - slot) * cores;
		
		for (ActorSched a : ready)
			sumReady += remainingTime[a.getId()];
				
		if (sumReady > rSlots)
			return false;
		
		return true;
	}
	
	private void checkNewActivations (List<ActorSched> scheduled, List<ActorSched> ready, int remainingTime[]) {
		// Check from the scheduled tasks the new activations
		for (ActorSched sched : scheduled) {
			// Check destination nodes
			for (Edge eDest : sched.getSndEdges()) {
				ActorSched dest = (ActorSched) eDest.getDest();
				
				// Check all the predecessors of the destination
				boolean add = true;
				
				for (Edge ePred : dest.getRcvEdges()) {
					if (remainingTime[ePred.getSrc().getId()] != 0) {
						add = false;
						break;
					}
				}
				
				if (add)
					ready.add(dest);
			}
			scheduled.remove(sched);
		}
	}
	
	private void buildHITable (DAG d, String sched[][][], List<ActorSched> prioOrder) throws SchedulingException {
		
		List<ActorSched> ready = new LinkedList<ActorSched>();
		List<ActorSched> scheduled = new LinkedList<ActorSched>();
		int[] remainingTime = new int[d.getNodes().size()];
		boolean taskFinished = false;

		
		// Init remaining time and tables
		initRemainingTimes(d, remainingTime, 1);
		
		for (Actor a : d.getNodes()) {
			if (a.getRcvEdges().size() == 0 &&
					a.getCI(1) != 0)
				ready.add((ActorSched)a);
		}
		
		ready.sort(hiComp);
		
		ListIterator<ActorSched> rit = ready.listIterator();
		ListIterator<ActorSched> pit = prioOrder.listIterator();

		
		// Iterate through the number of cores
		for (int s = 0; s < d.getDeadline(); s++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildHITable(): @t = "+s+", tasks activated: ");
				for (ActorSched a : ready)
					System.out.print("H("+a.getName()+") = "+a.getHlfet()[1]+"; ");
				System.out.println("");
			}
			
			// There aren't enough slots to continue the allocation
			if (!enoughSlots(ready, s, d.getDeadline(), d.getMinCores(), remainingTime)) {
				SchedulingException se = new SchedulingException("[ERROR"+Thread.currentThread().getName()+"] buildHITable(): Failed to schedule at time "+s);
				throw se;
			}
			
			// Check the priority ordering
			ArrayList<ActorSched> toSched = new ArrayList<>();
			int coreBudget = d.getMinCores();
			
			while (coreBudget != 0) {
				if (pit.hasNext()) {
					ActorSched a = pit.next();
				
					// 	The priority actor is in the ready queue
					if (ready.contains(a)) {
						if (a.isRunning()) { // Task was already running previous slot
							coreBudget--;
							toSched.add(a);
						} else  { // Check if other tasks were running
							for (ActorSched check : ready) {
								if (check.isRunning()) {
									coreBudget--;
									toSched.add(a);
								}
							}
						} 
						// If no other task was running start scheduling
						if (coreBudget > 0) {
							toSched.add(a);
							a.setRunning(true);
						}
					}
				}
			}
			
			// Allocate
			int c = 0;
			for (ActorSched a : toSched) {
				sched[1][s][c] = a.getName();
				
				remainingTime[a.getId()]--;
				
				if (remainingTime[a.getId()] == 0) {
					scheduled.add(a);
					taskFinished = true;
					pit.remove();
					rit.remove();
				}
				
				c++;
			}
			
			
			// Check if we have new activations
			if (taskFinished) {
				checkNewActivations(scheduled, ready, remainingTime);
				ready.sort(hiComp);
				taskFinished = false;
			}
		}
		
	}
	
	private void buildLOTable (DAG d, String sched[][][], List<ActorSched> prioOrder) throws SchedulingException {
		
	}
	
	public void buildTables () throws SchedulingException {
		
		int coresQuota = getNbCores();
		double uLightDAGs = 0.0;
		Set<DAG> heavyDAGs = new HashSet<DAG>();
		Set<DAG> lightDAGs = new HashSet<DAG>();
		
		// Separate heavy and light DAGs
		// Check if we have enough cores in the architecture
		for (DAG d : getMcDags()) {
			if (d.getUmax() <= 1) {
				lightDAGs.add(d);
			} else {
				coresQuota -= d.getUmax();
				heavyDAGs.add(d);
			}
		}
		
		if (coresQuota < 0) {
			SchedulingException se = new SchedulingException("Not enough cores in federated");
			throw se;
		}
		
		// Check for scheduling of light DAGs
		for (DAG d : lightDAGs)
			uLightDAGs += d.getUmax();
		
		if (Math.ceil(uLightDAGs) > coresQuota) {
			SchedulingException se = new SchedulingException("Not enough cores in federated");
			throw se;
		}
		
		// Check for scheduling of heavy DAGs
		for (DAG d : heavyDAGs) {
			
			List<ActorSched> hiPrioOrder = new LinkedList<>();
			List<ActorSched> loPrioOrder = new LinkedList<>();
			// Init sched table
			String sched[][][] = new String[2][d.getDeadline()][d.getMinCores()];
			
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < d.getDeadline(); j++) {
					for (int k = 0; k < d.getMinCores(); k++) {
						sched[i][j][k] = " - ";
					}
				}
			}
			
			calcHLFETs(d, 1, hiPrioOrder);
			calcHLFETs(d, 0, loPrioOrder);
			
			buildHITable(d, sched, hiPrioOrder);
			
			buildLOTable(d, sched, loPrioOrder);
		}
	}

	/*
	 * Getters and setters
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

	public Comparator<ActorSched> getLoComp() {
		return loComp;
	}

	public void setLoComp(Comparator<ActorSched> loComp) {
		this.loComp = loComp;
	}

	public Comparator<ActorSched> getHiComp() {
		return hiComp;
	}

	public void setHiComp(Comparator<ActorSched> hiComp) {
		this.hiComp = hiComp;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

}
