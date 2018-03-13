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
package fr.tpt.s3.ls_mxc.alloc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.ActorSched;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.util.MathMCDAG;

public class MultiDAG{
	
	// Set of DAGs to be scheduled
	private Set<DAG> mcDags;
	
	// Architecture + Hyperperiod
	private int nbCores;
	private int hPeriod;
	
	// List of DAG nodes to order them
	private List<ActorSched> lLO;
	private List<ActorSched> lHI;
	private Comparator<ActorSched> lHIComp;
	private Comparator<ActorSched> lLOComp;
	
	// Scheduling tables
	private String sched[][][];

	// Remaining time for all nodes
	private Hashtable<String, Integer> remainTLO;
	private Hashtable<String, Integer> remainTHI;
	
	private boolean debug;
	
	/**
	 * Constructor of the Multi DAG scheduler
	 * @param sd
	 * @param cores
	 */
	public MultiDAG (Set<DAG> sd, int cores, boolean debug) {
		setMcDags(sd);
		setNbCores(cores);
		remainTLO = new Hashtable<>();
		remainTHI = new Hashtable<>();
		
		lHIComp = new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched o1, ActorSched o2) {
				if (o1.getUrgencyHI() - o2.getUrgencyHI() != 0)
					return o1.getUrgencyHI() - o2.getUrgencyHI();
				else
					return o2.getId() - o1.getId();
			}			
		};
		
		lLOComp = new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched o1, ActorSched o2) {
				if (o1.getUrgencyLO() - o2.getUrgencyLO() != 0)
					return o1.getUrgencyLO() - o2.getUrgencyLO();
				else
					return o1.getId() - o2.getId();
			}		
		};
		setDebug(debug);
	}
	
	/**
	 * Allocates the scheduling tables
	 */
	private void initTables () {
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
		
		if (a.isSource()) {
			ret = deadline;			
		} else {
			int test = Integer.MAX_VALUE;

			for (Edge e : a.getRcvEdges()) {
				test = ((ActorSched) e.getSrc()).getLFTHI() - e.getSrc().getCI(1);
				
				if (test < ret)
					ret = test;
			}
		}
		a.setLFTHI(ret);
	}
	
	private void calcActorLFTLO (ActorSched a, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSink()) {			
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;

			for (Edge e : a.getSndEdges()) {
				test = ((ActorSched) e.getDest()).getLFTLO() - e.getDest().getCI(0);
				if (test < ret)
					ret = test;
			}
		}
		a.setLFTLO(ret);
		a.setUrgencyLO(ret);
	}
	
	/**
	 * Internal function that tests if all successors of a node have
	 * been visited.
	 * @param a
	 * @return
	 */
	private boolean succVisited (ActorSched a) {
		boolean ret = true;
		
		for (Edge e : a.getSndEdges()) {
			if (!((ActorSched) e.getDest()).isVisited())
				return false;
		}
		return ret;
	}
	
	/**
	 * Internal function that tests if all predecessors of a HI node have
	 * been visited.
	 * @param a
	 * @return
	 */
	private boolean succVisitedHI (ActorSched a) {
		boolean ret = true;
		
		for (Edge e : a.getRcvEdges()) {
			if (e.getSrc().getCI(1) != 0 && !((ActorSched) e.getSrc()).isVisitedHI())
				return false;
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
		
		for (Actor a : d.getSinks()) {
			toVisit.add((ActorSched) a);
			((ActorSched) a).setVisited(true);
		}
		
		for (Actor a : d.getSourcesHI()) {
			toVisitHI.add((ActorSched) a);
			((ActorSched) a).setVisitedHI(true);
		}
		
		while (toVisit.size() != 0) {
			ActorSched a = toVisit.get(0);
			
			calcActorLFTLO(a, d.getDeadline());
			for (Edge e : a.getRcvEdges()) {
				if (!((ActorSched) e.getSrc()).isVisited() && succVisited((ActorSched) e.getSrc())) {
					toVisit.add((ActorSched) e.getSrc());
					((ActorSched) e.getSrc()).setVisited(true);
				}
			}
			toVisit.remove(0);
		}
		
		while (toVisitHI.size() != 0) {
			ActorSched a = toVisitHI.get(0);
			
			calcActorLFTHI(a, d.getDeadline());
			for (Edge e : a.getSndEdges()) {
				if (e.getDest().getCI(1) != 0 && !((ActorSched) e.getDest()).isVisitedHI()
						&& succVisitedHI((ActorSched) e.getDest())) {
					toVisitHI.add((ActorSched) e.getDest());
					((ActorSched) e.getDest()).setVisitedHI(true);
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
					if (e2.getDest().getCI(1) != 0 && !sched.contains(e2.getDest())) {
						add = false;
						break;
					}
				}
			
				if (add && !ready.contains(pred) && remainTHI.get(pred.getName()) != 0)
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
					if (!sched.contains(e2.getSrc()))
						add = false;
				}
			
				if (add && !ready.contains(succ) && remainTLO.get(succ.getName()) != 0)
					ready.add(succ);
			}
		}
	}
	
	/**
	 * Checks for the activation of a new DAG during the hyper-period
	 * @param slot
	 */
	private void checkDAGActivation (int slot, List<ActorSched> sched, List<ActorSched> lMode, short mode) {
		for (DAG d : getMcDags()) {
			// If the slot is a mulitple of the deadline there is a new activation
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
					if (a.getCI(1) != 0)
						remainTHI.put(a.getName(), a.getCI(1));
					remainTLO.put(a.getName(), a.getCI(0));
					
					if (mode == ActorSched.HI) {
						if (a.isSinkHI())
							lMode.add((ActorSched) a);
					} else {
						if (a.isSource())
							lMode.add((ActorSched) a);
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
				if (a.getCI(1) != 0)
					remainTHI.put(a.getName(), a.getCI(1));
				remainTLO.put(a.getName(), a.getCI(0));
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
					
			if (mode == ActorSched.HI) { // Laxity in HI mode
				a.setUrgencyHI(a.getLFTHI() - relatSlot - remainTHI.get(a.getName()));
			} else  {// Laxity in LO mode
				// Promote HI tasks that need to be scheduled at this slot
				if (a.getCI(1) != 0) {
					if ((a.getCI(0) - remainTLO.get(a.getName())) - scheduledUntilT(a, slot) < 0) {
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Promotion of task "+a.getName()+" at slot @t = "+slot);
						a.setUrgencyLO(0);
					} else {
						a.setUrgencyLO(a.getLFTLO() - relatSlot - remainTLO.get(a.getName()));
					}
				} else {
					a.setUrgencyLO(a.getLFTLO() - relatSlot - remainTLO.get(a.getName()));
				}
			}
		}
	}
	
	/**
	 * Verifies if the scheduling table is worth computing
	 * @param slot
	 * @return
	 */
	private boolean isPossible (int slot, List<ActorSched> lMode, short mode) {
		int m = 0;
		ListIterator<ActorSched> it = lMode.listIterator();
		
		while (it.hasNext()) {
			ActorSched a = it.next();
			
			if (mode == ActorSched.HI) {
				if (a.getUrgencyHI() == 0)
					m++;
				else if (a.getUrgencyHI() < 0)
					return false;					
			} else {
				if (a.getUrgencyLO() == 0)
					m++;
				else if (a.getUrgencyLO() < 0)
					return false;
			}
			
			if (m > nbCores)
				return false;
		}
		
		return true;
	}
	
	/**
	 * Allocates the DAGs in the HI mode and registers virtual deadlines
	 * @throws SchedulingException
	 */
	public boolean allocHI () throws SchedulingException {
		List<ActorSched> completed = new LinkedList<>();
		lHI = new ArrayList<ActorSched>();
		
		// Add all exit HI nodes to the ready list.
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSinkHI())
					lHI.add((ActorSched) a);
			}
		}

		calcLaxity(lHI, 0, ActorSched.HI);
		lHI.sort(lHIComp);
		 
		// Allocate all slots of the HI scheduling table
		ListIterator<ActorSched> lit = lHI.listIterator();
		boolean taskFinished = false;
		
		for (int s = hPeriod - 1; s >= 0; s--) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] allocHI(): @t = "+s+", tasks activated: ");
				for (ActorSched a : lHI)
					System.out.print("L("+a.getName()+") = "+a.getUrgencyHI()+"; ");
				System.out.println("");
			}
			
			// Check if it's worth to continue the allocation
			if (!isPossible(s, lHI, ActorSched.HI)) {
				SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] allocHI() MultiDAG: Not enough slots left.");
				throw se;
			}
			
			for (int c = getNbCores() - 1; c >= 0; c--) {
				// Find a ready task in the HI list
				if (lit.hasNext()) {
					ActorSched a = lit.next();
					int val = remainTHI.get(a.getName());
					
					sched[1][s][c] = a.getName();
					val--;
					
					// The task has been fully scheduled
					if (val == 0) {
						completed.add(a);
						taskFinished = true;
						lit.remove();
					}
					remainTHI.put(a.getName(), val);
				}
			}
			
			if (taskFinished)
				checkActorActivationHI(completed, lHI);

			if (s != 0) {
				// Check if DAGs need to be activated at the next slot
				checkDAGActivation(s, completed, lHI, ActorSched.HI); 
				// Update laxities for nodes in the ready list
				calcLaxity(lHI, gethPeriod() - s, ActorSched.HI);
			}
			lHI.sort(lHIComp);
			taskFinished = false;
			lit = lHI.listIterator();
		}
		return true;
	}
	
	/**
	 * Allocates the DAGs in LO mode
	 * @throws SchedulingException
	 */
	public boolean allocLO () throws SchedulingException{
		List<ActorSched> completed = new LinkedList<>();
		lLO = new ArrayList<ActorSched>();
		
		// Add all HI nodes to the list.
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSource())
					lLO.add((ActorSched) a);
			}
		}
		
		calcLaxity(lLO, 0, ActorSched.LO);
		lLO.sort(lLOComp);
		
		// Allocate all slots of the LO scheduling table
		ListIterator<ActorSched> lit = lLO.listIterator();
		boolean taskFinished = false;
		
		for (int s = 0; s < hPeriod; s++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] allocLO(): @t = "+s+", tasks activated: ");
				for (ActorSched a : lLO)
					System.out.print("L("+a.getName()+") = "+a.getUrgencyLO()+"; ");
				System.out.println("");
			}
			
			// Verify that there are enough slots to continue the scheduling
			if (!isPossible(s, lLO, ActorSched.LO)) {
				SchedulingException se = new SchedulingException("[WARNING "+Thread.currentThread().getName()+"] allocLO() MultiDAG: Not enough slot left");
				throw se;
			}			
			
			for (int c = 0; c < getNbCores(); c++) {
				// Find a ready task in the LO list
				if (lit.hasNext()) {
					ActorSched a = lit.next();
					int val = remainTLO.get(a.getName());
					
					sched[0][s][c] = a.getName();
					val--;
					
					if (val == 0) {
						lit.remove();
						completed.add(a);
						taskFinished = true;
					}
					remainTLO.put(a.getName(), val);
				}
			}
						
			if (taskFinished)
				checkActorActivationLO(completed, lLO);
			
			if (s != hPeriod - 1) {
				checkDAGActivation(s + 1, completed, lLO, ActorSched.LO);
				calcLaxity(lLO, s + 1, ActorSched.LO);
			}
			lLO.sort(lLOComp);
			taskFinished = false;
			lit = lLO.listIterator();
		}
		return true;
	}
	
	/**
	 * Tries to allocate all DAGs in the number of cores given
	 * @param debug
	 * @throws SchedulingException
	 */
	public boolean allocAll () throws SchedulingException {
		boolean ret = true;
		this.setDebug(debug);
		initTables();
		calcWeights();
		
		if (isDebug()) printLFT();
		
		initRemainT();
		if (!allocHI())
			return false;
		//AlignScheduler.align(getSched(), 1, hPeriod, nbCores);
		if (isDebug()) printSHI();
		
		if (!allocLO())
			return false;
		//AlignScheduler.align(getSched(), 0, hPeriod, nbCores);
		if (isDebug()) printSLO();
		
		
		return ret;
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
									+"; LFT LO "+((ActorSched) a).getLFTLO());
				if (a.getCI(1) != 0) System.out.print("; LFT HI "+((ActorSched) a).getLFTHI());
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

	public List<ActorSched> getlLO() {
		return lLO;
	}

	public void setlLO(List<ActorSched> lLO) {
		this.lLO = lLO;
	}

	public List<ActorSched> getlHI() {
		return lHI;
	}

	public void setlHI(List<ActorSched> lHI) {
		this.lHI = lHI;
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
