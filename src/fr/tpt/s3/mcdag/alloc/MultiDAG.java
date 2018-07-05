/*******************************************************************************
 * Copyright (c) 2017, 2018 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Actor;
import fr.tpt.s3.mcdag.model.ActorSched;
import fr.tpt.s3.mcdag.model.DAG;
import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.util.MathMCDAG;

public class MultiDAG extends AbstractMixedCriticalityScheduler{
	
	// Set of DAGs to be scheduled
	private Set<DAG> mcDags;
	
	// Architecture + Hyperperiod
	private int nbCores;
	private int hPeriod;
	
	// List of DAG nodes to order them
	private Comparator<ActorSched> lHIComp;
	private Comparator<ActorSched> lLOComp;
	
	// Scheduling tables
	private String sched[][][];

	// Remaining time to be allocated for each node
	// Level, DAG id, Actor id
	private int remainingTime[][][];
	
	private boolean debug;
	
	/**
	 * Constructor of the Multi DAG scheduler
	 * @param sd
	 * @param cores
	 */
	public MultiDAG (Set<DAG> sd, int cores, boolean debug) {
		setMcDags(sd);
		setNbCores(cores);
		
		remainingTime = new int[2][getMcDags().size()][];
		
		// Init remaining time
		for (DAG d : getMcDags()) {
			remainingTime[0][d.getId()] = new int[d.getNodes().size()];
			remainingTime[1][d.getId()] = new int[d.getNodes().size()];
		}
		
		lHIComp = new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched o1, ActorSched o2) {
				if (o1.getLaxities()[1] - o2.getLaxities()[1] != 0)
					return o1.getLaxities()[1] - o2.getLaxities()[1];
				else
					return o2.getId() - o1.getId();
			}			
		};
		
		lLOComp = new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched o1, ActorSched o2) {
				if (o1.getLaxities()[0] - o2.getLaxities()[0] != 0)
					return o1.getLaxities()[0] - o2.getLaxities()[0];
				else
					return o1.getId() - o2.getId();
			}		
		};
		setDebug(debug);
	}
	
	/**
	 * Allocates the scheduling tables
	 */
	protected void initTables () {
		int[] input = new int[getMcDags().size()];
		int i = 0;
	
		// Look for the maximum deadline
		for (DAG d : getMcDags()) {
			input[i] = d.getDeadline();
			i++;
		}
		
		sethPeriod(MathMCDAG.lcm(input));
		
		// Init scheduling tables
		sched = new String[2][gethPeriod()][getNbCores()];
		
		for (i = 0; i < 2; i++) {
			for (int j = 0; j < gethPeriod(); j++) {
				for (int k = 0; k < getNbCores(); k++)
					sched[i][j][k] = "-";
			}
		}
		
		if (debug) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] initTables(): Hyper-period of the graph: "+gethPeriod()+"; tables initialized.");
	}
	
	/**
	 * Calculates the LFT of an Actor (HI and LO mode)
	 * @param a
	 * @param deadline
	 * @param mode
	 * @return
	 */
	private void calcActorLFTHI (ActorSched a, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.getRcvEdges().size() == 0) {
			ret = a.getGraphDead();
		} else {
			for (Edge e : a.getRcvEdges()) {
				if (e.getSrc().getWcet(1) != 0) {
					int test = ((ActorSched) e.getSrc()).getLFTs()[1] - e.getSrc().getWcet(1);
					
					if (test < ret)
						ret = test;
				}
			}
		}
		a.setLFTinL(ret, 1);
	}
	
	private void calcActorLFTLO (ActorSched a, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.getSndEdges().size() == 0) {
			ret = a.getGraphDead();
		} else {
			for (Edge e : a.getSndEdges()) {
				int test = ((ActorSched) e.getDest()).getLFTs()[0] - e.getDest().getWcet(0);
				if (test < ret)
					ret = test;
			}
		}
		a.setLFTinL(ret, 0);
	}
	
	/**
	 * Internal function that tests if all successors of a node have
	 * been visited.
	 * @param a
	 * @return
	 */
	private boolean succVisited (ActorSched a) {
		for (Edge e : a.getSndEdges()) {
			if (!((ActorSched) e.getDest()).getVisitedL()[0])
				return false;
		}
		return true;
	}
	
	/**
	 * Internal function that tests if all predecessors of a HI node have
	 * been visited.
	 * @param a
	 * @return
	 */
	private boolean predVisitedHI (ActorSched a) {
		boolean ret = true;
		
		for (Edge e : a.getRcvEdges()) {
			if (e.getSrc().getWcet(1) != 0) {
				if (!((ActorSched) e.getSrc()).getVisitedL()[1])
					return false;
			}
		}
		
		return ret;
	}
	
	/**
	 * Recursively calculates LFTs of an actor
	 */
	private void calcLFTs (DAG d) {
		// Add a list to add the nodes that have to be visited
		ArrayList<ActorSched> toVisit = new ArrayList<>();
		ArrayList<ActorSched> toVisitHI = new ArrayList<>();
		
		// Add sink LO nodes
		for (Actor a : d.getNodes()) {
			if (a.getSndEdges().size() == 0)
				toVisit.add((ActorSched) a);	
		}
		
		// Add source HI tasks
		for (Actor a : d.getNodes()) {
			if (a.getWcet(1) != 0) { // It's a HI task
				if (a.getRcvEdges().size() == 0) {
					toVisitHI.add((ActorSched) a);
				} else { // This should never happen atm
					boolean add = true;
					
					for (Edge e : a.getRcvEdges()) {
						if (e.getSrc().getWcet(1) != 0) {
							add = false;
							break;
						}
					}
					if (add)
						toVisitHI.add((ActorSched) a);
				}
			}
		}
		
		while (toVisit.size() != 0) {
			ActorSched a = toVisit.get(0);
			
			calcActorLFTLO(a, d.getDeadline());
			a.getVisitedL()[0] = true;

			for (Edge e : a.getRcvEdges()) {
				if (!((ActorSched) e.getSrc()).getVisitedL()[0] && succVisited((ActorSched) e.getSrc())) {
					toVisit.add((ActorSched) e.getSrc());
				}
			}
			toVisit.remove(0);
		}
		
		while (toVisitHI.size() != 0) {
			ActorSched a = toVisitHI.get(0);
			
			calcActorLFTHI(a, d.getDeadline());
			a.getVisitedL()[1] = true;
			for (Edge e : a.getSndEdges()) {
				if (e.getDest().getWcet(1) != 0 && predVisitedHI((ActorSched) e.getDest())) {
					toVisitHI.add((ActorSched) e.getDest());
				}
			}
			toVisitHI.remove(0);
		}
	}
	
	/**
	 * Calculates weights for tasks depending on the deadline
	 */
	private void calcWeights () {
		for (DAG d : getMcDags()) {
			calcLFTs(d);
		}
	}

	/**
	 * Checks for new activations in the HI mode for a given DAG
	 * @param a
	 * @param sched
	 */
	private void checkActorActivationHI (List<ActorSched> sched, List<ActorSched> ready) {
		// Check all predecessor of actor a that just finished
		for (ActorSched a : sched) {
			for (Edge e : a.getRcvEdges()) {
				ActorSched pred = (ActorSched) e.getSrc();
				boolean add = true;
			
				// 	Check all successors of the predecessor
				for (Edge e2 : pred.getSndEdges()) {
					if (e2.getDest().getWcet(1) > 0 && !sched.contains(e2.getDest())) {
						add = false;
						break;
					}
				}
			
				if (add && !ready.contains(pred) && remainingTime[1][pred.getGraphID()][pred.getId()] != 0)
					ready.add(pred);					
			}
		}
	}
	
	/**
	 * Checks for activations in the LO mode
	 * @param a
	 * @param sched
	 */
	private void checkActorActivationLO (List<ActorSched> sched, List<ActorSched> ready) {
		// Check all predecessor of actor a that just finished
		for (ActorSched a : sched) {
			for (Edge e : a.getSndEdges()) {
				ActorSched succ = (ActorSched) e.getDest();
				boolean add = true;
			
				// 	Check all successors of the predecessor
				for (Edge e2 : succ.getRcvEdges()) {
					if (!sched.contains(e2.getSrc())) {
						add = false;
						break;
					}
				}
			
				if (add && !ready.contains(succ) && remainingTime[0][succ.getGraphID()][succ.getId()] != 0)
					ready.add(succ);
			}
		}
	}
	
	/**
	 * Checks for the activation of a new DAG during the hyper-period
	 * @param slot
	 */
	private void checkDAGActivation (List<ActorSched> sched, List<ActorSched> ready, int slot, short mode) {
		for (DAG d : getMcDags()) {
			// If the slot is a multiple of the deadline there is a new activation
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
					// Re-init remaining execution time to be allocated
					
					remainingTime[(int)mode][((ActorSched)a).getGraphID()][a.getId()] = a.getWcet(mode);
					
					if (mode == ActorSched.HI && a.isSinkinL(1)) {
						ready.add((ActorSched) a);
					} else if (mode == ActorSched.LO && a.isSourceinL(0)){
						ready.add((ActorSched) a);
					}
				}			
			}
		}
	}
	
	/**
	 * Inits the remaining time to be allocated to each Actor
	 */
	private void initRemainT () {
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.getWcet(1) != 0)
					remainingTime[1][((ActorSched)a).getGraphID()][a.getId()] = a.getWcet(1);
				else
					remainingTime[1][((ActorSched)a).getGraphID()][a.getId()] = 0;
				
				remainingTime[0][((ActorSched)a).getGraphID()][a.getId()] = a.getWcet(0);
			}
		}
	}
	
	/**
	 * Checks how many slots have been allocated in the HI scheduling table
	 * @param a
	 * @param t
	 * @return
	 */
	private int scheduledUntilT (ActorSched a, int t) {
		int ret = 0;
		int start = (int)(t / a.getGraphDead()) * a.getGraphDead();
		
		for (int i = start; i <= t; i++) {
			for (int c = 0; c < nbCores; c++) {
				if (sched[1][i][c] != null) {
					if (sched[1][i][c].contentEquals(a.getName()))
						ret++;
				}
			}
		}
		return ret; 
	}
	
	/**
	 * Updates the laxity of each actor that is currently activated
	 * @param list
	 * @param slot
	 * @param mode
	 */
	private void calcLaxity (List<ActorSched> list, int slot, short mode) {
		for (ActorSched a : list) {
			int relatSlot = slot % a.getGraphDead();
			int dId = a.getGraphID();
					
			if (mode == ActorSched.HI) { // Laxity in HI mode
				a.setLaxityinL(a.getLFTs()[1] - relatSlot - remainingTime[1][dId][a.getId()], 1);
			} else  {// Laxity in LO mode
				// Promote HI tasks that need to be scheduled at this slot
				if (a.getWcet(1) > 0) {
					if ((a.getWcet(0) - remainingTime[0][dId][a.getId()] - scheduledUntilT(a, slot)) < 0) {
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Promotion of task "+a.getName()+" at slot @t = "+slot);
						a.setLaxityinL(0, 0);
					} else {
						a.setLaxityinL(a.getLFTs()[0] - relatSlot - remainingTime[0][dId][a.getId()], 0);
					}
				} else {
					a.setLaxityinL(a.getLFTs()[0] - relatSlot - remainingTime[0][dId][a.getId()], 0);
				}
			}
		}
	}
	
	/**
	 * Verifies if the scheduling table is worth computing
	 * @param slot
	 * @return
	 */
	private boolean isPossible (List<ActorSched> list, int slot, int level) {
		int m = 0;
		ListIterator<ActorSched> lit = list.listIterator();
		
		while (lit.hasNext()) {
			ActorSched a = lit.next();
			
			if (a.getLaxities()[level] == 0)
				m++;
			else if (a.getLaxities()[level] < 0)
				return false;
			
			if (m > nbCores)
				return false;
		}
		
		return true;
	}
	
	/**
	 * Allocates the DAGs in the HI mode and registers virtual deadlines
	 * @throws SchedulingException
	 */
	public void allocHI () throws SchedulingException {
		List<ActorSched> scheduled = new LinkedList<>();
		List<ActorSched> ready = new LinkedList<>();
		
		// Add all exit HI nodes to the ready list.
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSinkinL(1))
					ready.add((ActorSched) a);
			}
		}

		calcLaxity(ready, 0, ActorSched.HI);
		Collections.sort(ready, lHIComp);
		 
		// Allocate all slots of the HI scheduling table
		ListIterator<ActorSched> lit = ready.listIterator();
		boolean taskFinished = false;
		
		for (int s = hPeriod - 1; s >= 0; s--) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] allocHI(): @t = "+s+", tasks activated: ");
				for (ActorSched a : ready)
					System.out.print("L("+a.getName()+") = "+a.getLaxities()[1]+"; ");
				System.out.println("");
			}
			
			// Check if it's worth to continue the allocation
			if (!isPossible(ready, s, 1)) {
				SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] allocHI() MultiDAG: Not enough slots left.");
				throw se;
			}
			
			for (int c = getNbCores() - 1; c >= 0; c--) {
				// Find a ready task in the HI list
				if (lit.hasNext()) {
					ActorSched a = lit.next();
					int val = remainingTime[1][a.getGraphID()][a.getId()];
					
					sched[1][s][c] = a.getName();
					val--;
					
					// The task has been fully scheduled
					if (val == 0) {
						scheduled.add(a);
						taskFinished = true;
						lit.remove();
					}
					remainingTime[1][a.getGraphID()][a.getId()] = val;
				}
			}
			
			if (taskFinished)
				checkActorActivationHI(scheduled, ready);

			if (s != 0) {
				// Check if DAGs need to be activated at the next slot
				checkDAGActivation(scheduled, ready, s, ActorSched.HI); 
				// Update laxities for nodes in the ready list
				calcLaxity(ready, gethPeriod() - s, ActorSched.HI);
			}
			Collections.sort(ready, lHIComp);
			taskFinished = false;
			lit = ready.listIterator();
		}
		if (ready.size() != 0) {
			SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] allocHI() MultiDAG: Not enough slots left.");
			throw se;
		}
	}
	
	/**
	 * Allocates the DAGs in LO mode
	 * @throws SchedulingException
	 */
	public void allocLO () throws SchedulingException {
		List<ActorSched> ready = new LinkedList<>();
		List<ActorSched> scheduled = new LinkedList<>();
		
		// Add all HI nodes to the list.
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSourceinL(0))
					ready.add((ActorSched)a);
			}
		}
		
		calcLaxity(ready, 0, ActorSched.LO);
		Collections.sort(ready, lLOComp);
		
		// Allocate all slots of the LO scheduling table
		ListIterator<ActorSched> lit = ready.listIterator();
		boolean taskFinished = false;
		
		for (int s = 0; s < hPeriod; s++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] allocLO(): @t = "+s+", tasks activated: ");
				for (ActorSched a : ready)
					System.out.print("L("+a.getName()+") = "+a.getLaxities()[0]+"; ");
				System.out.println("");
			}
			
			// Verify that there are enough slots to continue the scheduling
			if (!isPossible(ready, s, 0)) {
				SchedulingException se = new SchedulingException("[WARNING "+Thread.currentThread().getName()+"] allocLO() MultiDAG: Not enough slot left");
				throw se;
			}
			
			for (int c = 0; c < getNbCores(); c++) {
				// Find a ready task in the LO list
				if (lit.hasNext()) {
					ActorSched a = lit.next();
					int val = remainingTime[0][a.getGraphID()][a.getId()];
					
					sched[0][s][c] = a.getName();
					val--;
					
					if (val == 0) {
						scheduled.add(a);
						taskFinished = true;
						lit.remove();
					}
					remainingTime[0][a.getGraphID()][a.getId()] = val;
				}
			}
						
			if (taskFinished)
				checkActorActivationLO(scheduled, ready);
			
			if (s != hPeriod - 1) {
				checkDAGActivation(scheduled, ready, s + 1, ActorSched.LO);
				calcLaxity(ready, s + 1, ActorSched.LO);
			}
			Collections.sort(ready, lLOComp);
			taskFinished = false;
			lit = ready.listIterator();
		}
		if (ready.size() != 0) {
			SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] allocLO() Ready list not empty");
			throw se;
		}
	}
	
	/**
	 * Tries to allocate all DAGs in the number of cores given
	 * @param debug
	 * @throws SchedulingException
	 */
	public void buildAllTables () throws SchedulingException {
		initTables();
		initRemainT();

		calcWeights();
		
		if (isDebug()) printLFT();
		
		allocHI();
		if (isDebug()) printSHI();
		
		allocLO();
		if (isDebug()) printSLO();
	}

	/*
	 * Debugging functions
	 */
	/**
	 * Prints urgency values for the multi DAG scheduling
	 */
	public void printLFT () {
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] printLFT(): DAG "+d.getId()+"; Actor "+a.getName()
									+"; LFT LO "+((ActorSched) a).getLFTs()[0]);
				if (a.getWcet(1) != 0) System.out.print("; LFT HI "+((ActorSched) a).getLFTs()[1]);
				System.out.println(".");
			}
		}
		System.out.println("");
	}
	
	/**
	 * Prints the HI scheduling table
	 */
	public void printSHI () {
		for (int c = 0; c < getNbCores(); c++) {
			for (int s = 0; s < gethPeriod(); s++) {
				if (sched[1][s][c] != null)
					System.out.print(sched[1][s][c]+" | ");
				else
					System.out.print("-- | ");
			}
			System.out.print("\n");
		}
		
		System.out.print("\n");
	}
	
	/**
	 * Prints the LO scheduling table
	 */
	public void printSLO () {
		for (int c = 0; c < getNbCores(); c++) {
			for (int s = 0; s < gethPeriod(); s++) {
				if (sched[0][s][c] !=  null)
					System.out.print(sched[0][s][c]+" | ");
				else
					System.out.print("-- | ");
			}
			System.out.print("\n");
		}
	}
	
	/*
	 * Getters and setters
	 */

	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}

	public Set<DAG> getMcDags() {
		return mcDags;
	}

	public void setMcDags(Set<DAG> mcDags) {
		this.mcDags = mcDags;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int gethPeriod() {
		return hPeriod;
	}

	public void sethPeriod(int hPeriod) {
		this.hPeriod = hPeriod;
	}

	public Comparator<ActorSched> getlLOComp() {
		return lLOComp;
	}

	public void setlLOComp(Comparator<ActorSched> lLOComp) {
		this.lLOComp = lLOComp;
	}

	public Comparator<ActorSched> getlHIComp() {
		return lHIComp;
	}

	public void setlHIComp(Comparator<ActorSched> lHIComp) {
		this.lHIComp = lHIComp;
	}

	public String[][][] getSched() {
		return sched;
	}

	public void setSched(String sched[][][]) {
		this.sched = sched;
	}
}
