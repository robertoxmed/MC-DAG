/*******************************************************************************
 * Copyright (c) 2018 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
	public Federated (Set<DAG> system, int architecture, boolean debug) {
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
			remainingTime[a.getId()] = a.getWcet(level);
	}
	
	private void calcHLFETs (DAG d, int level, List<ActorSched> prioOrder) {
		
		ArrayList<ActorSched> toVisit = new ArrayList<ActorSched>();
		
		// Look for sinks first
		for (Actor a : d.getNodes()) {
			if (a.getWcet(level) != 0) {
				if (a.getSndEdges().size() == 0 && a.getWcet(level) != 0) {
					toVisit.add((ActorSched)a);
				} else {
					boolean add = true;
					
					for (Edge e : a.getSndEdges()) {
						if (e.getDest().getWcet(level) != 0)
							add = false;
					}
					if (add) {
						toVisit.add((ActorSched)a);
					}
				}
			}
		}
		
		// Iterate through nodes and compute their HLFET
		while (toVisit.size() != 0) {
			ActorSched a = toVisit.get(0);
			
			// Look for the max on the SndEdges
			int max = 0;
				
			for (Edge e : a.getSndEdges()) {
				ActorSched dest = (ActorSched) e.getDest();
				if (dest.getWcet(level) != 0 &&
						dest.getHlfet()[level] > max) {
					max = dest.getHlfet()[level];
				}
			}
			a.getHlfet()[level] = max + a.getWcet(level);
			a.getVisitedL()[level] = true;

			for (Edge e : a.getRcvEdges()) {
				boolean allSuccVisited = true;
				Actor test = e.getSrc();
				
				for (Edge e2 : test.getSndEdges()) {
					ActorSched dest = (ActorSched) e2.getDest();
					if (!dest.getVisitedL()[level] && dest.getWcet(level) != 0) {
						allSuccVisited = false;
						break;
					}
				}
				
				if (allSuccVisited && test.getWcet(level) != 0 && !toVisit.contains((ActorSched)test))
					toVisit.add((ActorSched)test);
			}
			toVisit.remove(0);
		}
		
		// Create the list with the priority ordering
		for (Actor a : d.getNodes()) {
			if (a.getWcet(level) != 0)
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
		ListIterator<ActorSched> lit = scheduled.listIterator();
		while (lit.hasNext()) {
			ActorSched sched = lit.next();
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
				
				if (add && remainingTime[dest.getId()] != 0) {
					ready.add(dest);
				}
			}
			lit.remove();
			ready.remove(sched);
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
					a.getWcet(1) != 0)
				ready.add((ActorSched)a);
		}
		
		ready.sort(hiComp);
		
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
			
			while (pit.hasNext() && coreBudget > 0) {
				ActorSched a = pit.next();
				
				// 	The priority actor is in the ready queue
				if (ready.contains(a)) {
					if (a.isRunning()) { // Task was already running previous slot
						coreBudget--;
						toSched.add(a);
					} else  { // Check if other tasks were running
						for (ActorSched check : ready) {
							if (check.isRunning() && !toSched.contains(check)) {
								coreBudget--;
								toSched.add(check);
							}
						}
					} 
					// If no other task was running start scheduling
					if (coreBudget > 0 && !toSched.contains(a)) {
						toSched.add(a);
						a.setRunning(true);
						coreBudget--;
					}
				}
			}
			
			// Allocate
			int c = 0;
			for (ActorSched a : toSched) {
				sched[1][s][c] = a.getName();	
				remainingTime[a.getId()] = remainingTime[a.getId()] - 1;

				if (remainingTime[a.getId()] == 0) {
					scheduled.add(a);
					taskFinished = true;
				}
				c++;
			}
			
			// Check if we have new activations
			if (taskFinished) {
				checkNewActivations(scheduled, ready, remainingTime);
				ready.sort(hiComp);
				taskFinished = false;
			}
			pit = prioOrder.listIterator();
		}
		
	}
	
	private void buildLOTable (DAG d, String sched[][][], List<ActorSched> loPrioOrder, List<ActorSched> hiPrioOrder) throws SchedulingException {
		List<ActorSched> ready = new LinkedList<ActorSched>();
		List<ActorSched> scheduled = new LinkedList<ActorSched>();
		int[] remainingTime = new int[d.getNodes().size()];
		boolean taskFinished = false;
		
		// Init remaining time and tables
		initRemainingTimes(d, remainingTime, 0);
		
		for (Actor a : d.getNodes()) {
			if (a.getRcvEdges().size() == 0)
				ready.add((ActorSched)a);
		}
		
		ready.sort(loComp);
		ListIterator<ActorSched> hpit = hiPrioOrder.listIterator();
		ListIterator<ActorSched> lpit = loPrioOrder.listIterator();

		// Iterate through the number of cores
		for (int s = 0; s < d.getDeadline(); s++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildLOTable(): @t = "+s+", tasks activated: ");
				for (ActorSched a : ready)
					System.out.print("H("+a.getName()+") = "+a.getHlfet()[0]+"; ");
				System.out.println("");
			}
			
			// There aren't enough slots to continue the allocation
			if (!enoughSlots(ready, s, d.getDeadline(), d.getMinCores(), remainingTime)) {
				SchedulingException se = new SchedulingException("[ERROR"+Thread.currentThread().getName()+"] buildLOTable(): Failed to schedule at time "+s);
				throw se;
			}
			
			// Construct the schedulable elements
			ArrayList<ActorSched> toSched = new ArrayList<>();
			int coreBudget = d.getMinCores();
			
			// Check the priority ordering of HI tasks first
			while (hpit.hasNext() && coreBudget > 0) {
				ActorSched a = hpit.next();
				
				// Priority task is in the ready queue
				if (ready.contains(a)) { 
					if (a.isRunning()) { // Task was already running previous slot
						coreBudget--;
						toSched.add(a);
					}  else  { // Check if other HI tasks were running
						for (ActorSched check : ready) {
							if (check.isRunning() && !toSched.contains(check) 
									&& check.getWcet(1) != 0 && coreBudget > 0) {
								coreBudget--;
								toSched.add(check);
							}
						}
					}
					// In this case HI tasks can preempt LO ones
					if (coreBudget > 0 && !toSched.contains(a)) {
						toSched.add(a);
						a.setRunning(true);
						coreBudget--;
					}
				}
			}
			
			// Check if we can allocate LO tasks
			while (lpit.hasNext() && coreBudget > 0) {
				ActorSched a = lpit.next();
				
				if (ready.contains(a)) { // LO task is ready
					if (a.isRunning()) {
						coreBudget--;
						toSched.add(a);
					} else { // Check if other LO tasks were already running
						for (ActorSched check : ready) {
							if (check.isRunning() && check.getWcet(1) == 0 &&
									!toSched.contains(check) && coreBudget > 0) {
								coreBudget--;
								toSched.add(check);
							} else if (check.isRunning() && coreBudget <= 0) {
								check.setRunning(false);
							}
						}
					}
					// LO task can start being scheduled
					if (coreBudget > 0 && !toSched.contains(a)) {
						toSched.add(a);
						a.setRunning(true);
						coreBudget--;
					}
				}
			}
			
			// Allocate
			int c = 0;
			for (ActorSched a : toSched) {
				sched[0][s][c] = a.getName();
				remainingTime[a.getId()] = remainingTime[a.getId()] - 1;

				if (remainingTime[a.getId()] == 0) {
					scheduled.add(a);
					taskFinished = true;
				}
				c++;
			}
			
			// Check if we have new activations
			if (taskFinished) {
				checkNewActivations(scheduled, ready, remainingTime);
				ready.sort(loComp);
				taskFinished = false;
			}
			hpit = hiPrioOrder.listIterator();
			lpit = loPrioOrder.listIterator();
		}
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
			if (isDebug()) printDAG(d);
			
			calcHLFETs(d, 1, hiPrioOrder);
			calcHLFETs(d, 0, loPrioOrder);
			
			ListIterator<ActorSched> lit = loPrioOrder.listIterator();
			while (lit.hasNext()) {
				ActorSched a = lit.next();
				if (a.getWcet(1) > 0)
					lit.remove();
			}
			loPrioOrder.sort(loComp);
			
			
			if (isDebug()) printHLFETLevels(d);
			
			buildHITable(d, sched, hiPrioOrder);
			buildLOTable(d, sched, loPrioOrder, hiPrioOrder);
		}
	}
	
	/*
	 * DEBUG FUNCTIONS
	 */
	private void printHLFETLevels (DAG d) {
		for (Actor a : d.getNodes())
			System.out.println("Node "+a.getName()+" HLEFT(HI) "+((ActorSched)a).getHlfet()[1]+" HLFET(LO) "+((ActorSched)a).getHlfet()[0]);
	}
	
	private void printDAG (DAG d) {
		for (Actor a : d.getNodes())
			System.out.println("Node "+a.getName()+" Ci(HI) "+((ActorSched)a).getWcet(1)+" Ci(LO) "+((ActorSched)a).getWcet(0));

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
