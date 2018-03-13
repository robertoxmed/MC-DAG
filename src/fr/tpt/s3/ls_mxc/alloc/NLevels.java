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
package fr.tpt.s3.ls_mxc.alloc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.ActorSched;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.util.AlignScheduler;
import fr.tpt.s3.ls_mxc.util.Counters;
import fr.tpt.s3.ls_mxc.util.MathMCDAG;

/**
 * Allocator of DAGs for N levels of criticality
 * @author roberto
 *
 */
public class NLevels {
	
	// Set of DAGs to be scheduled
	private Set<DAG> mcDags;
	
	// Architecture + hyperperiod + nb of levels
	private int nbCores;
	private int hPeriod;
	private int levels;
	
	// Scheduling tables
	private String sched[][][];
	
	// Remaining time to be allocated for each node
	// Level, DAG id, Actor id
	private int remainingTime[][][];
	
	// Debugging boolean
	private boolean debug;
	
	// Comparators to order Actors
	private Comparator<ActorSched> loComp;
	
	// Counter of ctx switches & preemptions per task
	private int activations;
	private Hashtable<ActorSched, Integer> ctxSwitch;
	private Hashtable<ActorSched, Integer> preempts;
		
	/**
	 * Constructor
	 * @param dags
	 * @param cores
	 * @param levels
	 * @param debug
	 */
	public NLevels (Set<DAG> dags, int cores, int levels, boolean debug) {
		setMcDags(dags);
		setNbCores(cores);
		setLevels(levels);
		setDebug(debug);
		remainingTime = new int[getLevels()][getMcDags().size()][];
		
		// Init remaining scheduling time tables
		for (DAG d : getMcDags()) {
			for (int i = 0; i < getLevels(); i++) {
				remainingTime[i][d.getId()] = new int[d.getNodes().size()];
			}
		}
		
		setLoComp(new Comparator<ActorSched>() {
			@Override
			public int compare (ActorSched o1, ActorSched o2) {
				if (o1.getUrgencies()[0] - o2.getUrgencies()[0] != 0)
					return o1.getUrgencies()[0] - o2.getUrgencies()[0];
				else
					return o1.getId() - o2.getId();
			}
		});
	
		setCtxSwitch(new Hashtable<ActorSched, Integer>());
		setPreempts(new Hashtable<ActorSched, Integer>());
		setActivations(0);
	}
	
	/**
	 * Initializes the remaining time to be allocated for each node in each level
	 */
	private void initRemainTime () {
		for (int i = 0; i < getLevels(); i++) {
			for (DAG d : getMcDags()) {
				for (Actor a : d.getNodes()) {
					remainingTime[i][d.getId()][a.getId()] = a.getCI(i);
				}	
			}
		}
	}
	
	/**
	 * Inits the scheduling tables and calculates the hyper-period
	 */
	private void initTables () {
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
				for (int k = 0; k < getNbCores(); k++) {
					sched[i][j][k] = "-";
				}
			}
		}
		
		if (debug) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] initTables(): Sched tables initialized!");
	}
	
	/**
	 * Calculates the LFT of an actor in mode l which is a HI mode
	 * @param a
	 * @param l
	 */
	private void calcActorLFTrev (ActorSched a, int l, int dead) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSourceinL(l)) {
			ret = dead;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getRcvEdges()) {
				test = ((ActorSched) e.getSrc()).getLFTs()[l] - e.getSrc().getCI(l);
				if (test < ret)
					ret = test;
			}
		}
		a.setLFTinL(ret, l);
	}
	
	/**
	 * Calculates the LFT of an actor in the LO(west) mode
	 * @param a
	 * @param l
	 */
	private void calcActorLFT (ActorSched a, int l, int dead) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSinkinL(l)) {
			ret = dead;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getSndEdges()) {
				test = ((ActorSched) e.getDest()).getLFTs()[l] - e.getDest().getCI(l);
				if (test < ret)
					ret = test;
			}
		}
		a.setLFTinL(ret, l);
		a.setUrgencyinL(ret, l);
	}
	
	/**
	 * Internal function that tests if all successor of LO nodes in L mode have
	 * been visited.
	 * @param a
	 * @param l
	 * @return
	 */
	private boolean succVisitedinL (ActorSched a, int l) {
		for (Edge e : a.getSndEdges()) {
			if (e.getDest().getCI(l) != 0 && ((ActorSched) e.getDest()).getVisitedL()[l] == false) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Internal function that tests if all predecessors of HI nodes in L mode have
	 * been visited.
	 * @param a
	 * @param l
	 * @return
	 */
	private boolean predVisitedinL (ActorSched a, int l) {
		for (Edge e : a.getRcvEdges()) {
			if (e.getSrc().getCI(l) != 0 && ((ActorSched) e.getSrc()).getVisitedL()[l] == false) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Calculate LFTs on a DAG d
	 * @param d
	 */
	private void calcLFTs (DAG d) {
		
		// Calculate LFTs in HI modes
		for (int i = 1; i < levels; i++) {
			ArrayList<ActorSched> toVisit = new ArrayList<>();
			
			// Calculate sources in i mode
			for (Actor a : d.getNodes()) {
				if (a.isSourceinL(i)) {
					toVisit.add((ActorSched) a);
					((ActorSched) a).getVisitedL()[i] = true;
				}
			}
			
			// Visit all nodes iteratively
			while (!toVisit.isEmpty()) {
				ActorSched a = toVisit.get(0);
				
				calcActorLFTrev(a, i, d.getDeadline());
				for (Edge e : a.getSndEdges()) {
					if (e.getDest().getCI(i) != 0 && !((ActorSched) e.getDest()).getVisitedL()[i]
							&& predVisitedinL((ActorSched) e.getDest(), i)) {
						toVisit.add((ActorSched) e.getDest());
						((ActorSched) e.getDest()).getVisitedL()[i] = true;
					}
				}
				toVisit.remove(0);
			}
		}
		
		// Calculate LFT in LO mode
		ArrayList<ActorSched> toVisit = new ArrayList<>();
		
		for (Actor a : d.getNodes()) {
			if (a.isSinkinL(0)) {
				toVisit.add((ActorSched) a);
				((ActorSched) a).getVisitedL()[0] = true;
			}
		}
		
		while (!toVisit.isEmpty()) {
			ActorSched a = toVisit.get(0);
			
			calcActorLFT(a, 0, d.getDeadline());
			for (Edge e : a.getRcvEdges()) {
				if (!((ActorSched) e.getSrc()).getVisitedL()[0] && succVisitedinL(a, 0)) {
					toVisit.add((ActorSched) e.getSrc());
					((ActorSched) e.getSrc()).getVisitedL()[0] = true;
				}
			}
			toVisit.remove(0);
		}
	}
	
	/**
	 * Checks how many slots have been allocated for a in l mode until
	 * time slot t.
	 * @param a
	 * @param t
	 * @param l
	 * @return
	 */
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
	 * Resets temporary delays of HI tasks
	 */
	private void resetDelays() {
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.getCI(1) != 0)
					((ActorSched) a).setDelayed(false);
			}
		}
	}
	
	/**
	 * Calculates the laxity of the ready tasks that are on the list
	 * @param list
	 * @param slot
	 * @param level
	 */
	private void calcLaxity(List<ActorSched> list, int slot, int level) {
		for (ActorSched a : list) {
			int relatSlot = slot % a.getGraphDead();
			int dId = a.getGraphID();
			
			// The laxity has to be calculated for a HI mode
			if (level >= 1) {

				// It's not the highest criticality level -> perform checks
				if (level != getLevels() - 1) {
					int deltaI = a.getCI(level + 1) - a.getCI(level);
					//Check if in the higher table the Ci(L+1) - Ci(L) has been allocated
					if (scheduledUntilTinLreverse(a, slot, level + 1) - deltaI < 0) {
						a.setDelayed(true);
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Task "+a.getName()+" needs to be delayed at slot @t = "+slot);
						a.setUrgencyinL(Integer.MAX_VALUE, level);
					} else if (scheduledUntilTinLreverse(a, slot, level) != 0 &&
							scheduledUntilTinLreverse(a, slot, level) - scheduledUntilTinLreverse(a, slot, level + 1) + deltaI == 0) {
						a.setDelayed(true);
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Task "+a.getName()+" needs to be delayed at slot @t = "+slot);
						a.setUrgencyinL(Integer.MAX_VALUE, level);
					} else {
						a.setUrgencyinL(a.getLFTs()[level] - relatSlot - remainingTime[level][dId][a.getId()], level);
					}
				} else {
					a.setUrgencyinL(a.getLFTs()[level] - relatSlot - remainingTime[level][dId][a.getId()], level);
				}
			// Laxity in LO mode
			} else { 
				if (a.getCI(1) > 0) {
					if ((a.getCI(level) - remainingTime[level][dId][a.getId()]) - scheduledUntilTinL(a, slot, level + 1) < 0) {
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Promotion of task "+a.getName()+" at slot @t = "+slot);
						a.setUrgencyinL(0, level);
					}
				}
					
				a.setUrgencyinL(a.getLFTs()[level] - relatSlot - remainingTime[level][dId][a.getId()], level);
			}
		}
	}
	
	/**
	 * Functions that verifies if computing the scheduling table is worth it
	 * @param list
	 * @param slot
	 * @param level
	 * @return
	 */
	private boolean isPossible(List<ActorSched> list, int slot, int level) {
		int m = 0;
		ListIterator<ActorSched> lit = list.listIterator();
		
		while (lit.hasNext()) {
			ActorSched a = lit.next();
			
			if (a.getUrgencies()[level] == 0)
				m++;
			else if (a.getUrgencies()[level] < 0)
				return false;
			
			if (m > nbCores)
				return false;
		}
		
		return true;
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
					if (e2.getDest().getCI(level) != 0 && !sched.contains(e2.getDest()))
						add = false;
				}
				
				if (add && !ready.contains(pred) && remainingTime[level][pred.getGraphID()][pred.getId()] != 0) {
					ready.add(pred);
					activations++;
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
					if (!sched.contains(e2.getSrc()))
						add = false;
				}
				
				if (add && !ready.contains(succ) && remainingTime[0][succ.getGraphID()][succ.getId()] != 0) {
					ready.add(succ);
					activations++;
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
					remainingTime[level][((ActorSched)a).getGraphID()][a.getId()] = a.getCI(level);
					
					if (level >= 1 && a.isSinkinL(level)) {
						ready.add((ActorSched)a);
						activations++;
					} else if (level == 0 && a.isSourceinL(level)) {
						ready.add((ActorSched)a);
						activations++;
					}
				}
			}
		}
	}
	
	/**
	 * Builds the scheduling table of level l
	 * @param l
	 * @throws SchedulingException
	 */
	private void buildHITable (int l) throws SchedulingException {
		List<ActorSched> ready = new LinkedList<>();
		List<ActorSched> scheduled = new LinkedList<>();
		
		// Add all sink nodes
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSinkinL(l))
					ready.add((ActorSched) a);
			}
		}
		
		calcLaxity(ready, 0, l);
		ready.sort(new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched o1, ActorSched o2) {
				if (o1.getUrgencies()[l] - o2.getUrgencies()[l] != 0)
					return o1.getUrgencies()[l] - o2.getUrgencies()[l];
				else
					return o2.getId() - o1.getId();
			}
		});
		
		// Allocate slot by slot the HI scheduling tables
		ListIterator<ActorSched> lit = ready.listIterator();
		boolean taskFinished = false;
		
		for (int s = hPeriod - 1; s >= 0; s--) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildHITable("+l+"): @t = "+s+", tasks activated: ");
				for (ActorSched a : ready)
					System.out.print("L("+a.getName()+") = "+a.getUrgencies()[l]+"; ");
				System.out.println("");
			}
			
			// Check if it's worth to continue the allocation
			if (!isPossible(ready, s, l)) {
				SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] buildHITable("+l+"): Not enough slots left.");
				throw se;
			}
			
			for (int c = getNbCores() - 1; c >=0; c--) {
				// Find a top ready task
				if (lit.hasNext()) {
					ActorSched a = lit.next();
					if (a.isDelayed())
						break;
					int val = remainingTime[l][a.getGraphID()][a.getId()];
					
					sched[l][s][c] = a .getName();
					val--;
					
					// The task has been fully scheduled
					if (val == 0) {
						scheduled.add(a);
						taskFinished = true;
						lit.remove();
					}
					remainingTime[l][a.getGraphID()][a.getId()] = val;
				}
			}
			
			resetDelays();
			
			// It a task has been fully allocated check for new activations
			if (taskFinished)
				checkActivationHI(scheduled, ready, l);
			
			if (s != 0) {
				// Check for new DAG activations
				checkDAGActivation(scheduled, ready, s, l);
				// Update laxities for nodes
				calcLaxity(ready, gethPeriod() - s, l);
			}
			ready.sort(new Comparator<ActorSched>() {
				@Override
				public int compare(ActorSched o1, ActorSched o2) {
					if (o1.getUrgencies()[l] - o2.getUrgencies()[l] != 0)
						return o1.getUrgencies()[l] - o2.getUrgencies()[l];
					else
						return o2.getId() - o1.getId();
				}
			});
			taskFinished = false;
			lit = ready.listIterator();
		}
	}
	
	/**
	 * Builds the LO scheduling table
	 * @throws SchedulingException
	 */
	private void buildLOTable () throws SchedulingException {
		List<ActorSched> ready = new LinkedList<>();
		List<ActorSched> scheduled = new LinkedList<>();
		
		// Add all source nodes
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSourceinL(0)) {
					ready.add((ActorSched) a);
					activations++;
				}
			}
		}
		
		calcLaxity(ready, 0, 0);
		ready.sort(loComp);
		
		// Allocate slot by slot the scheduling table
		ListIterator<ActorSched> lit = ready.listIterator();
		boolean taskFinished = false;
		
		for (int s = 0; s < gethPeriod(); s++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildHITable(0): @t = "+s+", tasks activated: ");
				for (ActorSched a : ready)
					System.out.print("L("+a.getName()+") = "+a.getUrgencies()[0]+"; ");
				System.out.println("");
			}
			
			// Verify that is still worth trying to compute the sched table
			if (!isPossible(ready, s, 0)) {
				SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] buildHITable(0): Not enough slots left.");
				throw se;
			}
			
			for (int c = 0; c < getNbCores(); c++) {
				// Get the next element on the LO list
				if (lit.hasNext()) {
					ActorSched a = lit.next();
					int val = remainingTime[0][a.getGraphID()][a.getId()];
					
					sched[0][s][c] = a.getName();
					val--;
					
					if (val == 0) {
						lit.remove();
						scheduled.add(a);
						taskFinished = true;
					}
					
					remainingTime[0][a.getGraphID()][a.getId()] = val;
				}
			}
			
			if (taskFinished)
				checkActivationLO(scheduled, ready);
			
			if (s != hPeriod - 1) {
				checkDAGActivation(scheduled, ready, s + 1, 0);
				calcLaxity(ready, s + 1, 0);
			}
			ready.sort(loComp);
			taskFinished = false;
			lit = ready.listIterator();
		}
	}
	
	/**
	 * Builds all the scheduling tables for the system
	 */
	public void buildAllTables () throws SchedulingException {
		initRemainTime();
		initTables();
		
		// Calculate LFTs and urgencies in all DAGs
		for (DAG d : getMcDags()) {
			calcLFTs(d);
			if (isDebug()) printLFTs(d);
		}
		
		// Build tables: more critical tables first
		for (int i = getLevels() - 1; i >= 1; i--)			
			buildHITable(i);

		buildLOTable();		
		
		for (int i = 0; i < getLevels(); i++)
			AlignScheduler.align(sched, i, gethPeriod(), getNbCores());
		
		if (isDebug()) printTables();
		
		// Count preemptions
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				ctxSwitch.put((ActorSched) a, 0);
				preempts.put((ActorSched)a, 0);
			}
		}
		
		Counters.countContextSwitch(sched, ctxSwitch, getLevels(), hPeriod, nbCores);
		Counters.countPreemptions(sched, preempts, getLevels(), hPeriod, nbCores);
	}
	
	
	/*
	 * Non-preemptive version of the scheduler
	 */
	
	/**
	 * Checks if it's the first time a task is being scheduled
	 * @param l
	 * @return
	 */
	private boolean firstTimeRunning (int l, ActorSched a) {
		if (remainingTime[l][a.getGraphID()][a.getId()] == a.getCI(l))
			return true;
		return false;
	}
	
	/**
	 * Function that calculates laxities for a non preemptive version of the algorithm
	 * @param list
	 * @param slot
	 * @param level
	 */
	private void calcLaxitynopreempt (List<ActorSched> list, int slot, int level) {
		for (ActorSched a : list) {
			int relatSlot = slot % a.getGraphDead();
			int dId = a.getGraphID();
			
			// The laxity has to be calculated for a HI mode
			if (level >= 1) {

				// It's not the highest criticality level -> perform checks
				if (level != getLevels() - 1) {
					int deltaI = a.getCI(level + 1) - a.getCI(level);
					//Check if in the higher table the Ci(L+1) - Ci(L) has been allocated
					if (scheduledUntilTinLreverse(a, slot, level + 1) - deltaI < 0) {
						a.setDelayed(true);
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxitynopreempt(): Task "+a.getName()+" needs to be delayed at slot @t = "+slot);
						a.setUrgencyinL(Integer.MAX_VALUE, level);
					} else {
						a.setUrgencyinL(a.getLFTs()[level] - relatSlot - remainingTime[level][dId][a.getId()], level);
					}
				} else {
					a.setUrgencyinL(a.getLFTs()[level] - relatSlot - remainingTime[level][dId][a.getId()], level);
				}
			// Laxity in LO mode
			} else { 
				if (a.getCI(1) > 0) {
					if ((a.getCI(level) - remainingTime[level][dId][a.getId()]) - scheduledUntilTinL(a, slot, level + 1) < 0) {
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxitynopreempt(): Promotion of task "+a.getName()+" at slot @t = "+slot);
						a.setUrgencyinL(0, level);
					}
				}
					
				a.setUrgencyinL(a.getLFTs()[level] - relatSlot - remainingTime[level][dId][a.getId()], level);
			}
		}
	}
	
	
	/**
	 * Method to set running tasks at the beginning of the ready list
	 * @param list
	 */
	private void setRunningFirst (List<ActorSched> list) {
		List<ActorSched> promoted = new LinkedList<>();
		Iterator<ActorSched> lit = list.iterator();
		
		while (lit.hasNext()) {
			ActorSched a = lit.next();
			
			if (a.isRunning()) {
				lit.remove();
				promoted.add(a);
			}
		}
		
		for (ActorSched a : promoted)
			list.add(0, a);

	}
	
	/**
	 * Method that builds the HI scheduling table without preemption
	 * @param l
	 * @throws SchedulingException
	 */
	private void buildHInopreempt (int l) throws SchedulingException {
		List<ActorSched> ready = new LinkedList<>();
		List<ActorSched> scheduled = new LinkedList<>();
		
		// Add all sink nodes
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSinkinL(l))
					ready.add((ActorSched) a);
			}
		}
		
		calcLaxitynopreempt(ready, 0, l);
		ready.sort(new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched o1, ActorSched o2) {
				if (o1.getUrgencies()[l] - o2.getUrgencies()[l] != 0)
					return o1.getUrgencies()[l] - o2.getUrgencies()[l];
				else
					return o2.getId() - o1.getId();
			}
		});
		
		// Allocate slot by slot the HI scheduling tables
		ListIterator<ActorSched> lit = ready.listIterator();
		boolean taskFinished = false;
		
		for (int s = hPeriod - 1; s >= 0; s--) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildHInopreempt("+l+"): @t = "+s+", tasks activated: ");
				for (ActorSched a : ready)
					System.out.print("L("+a.getName()+") = "+a.getUrgencies()[l]+" R = "+a.isRunning()+"; ");
				System.out.println("");
			}
			
			// Check if it's worth to continue the allocation
			if (!isPossible(ready, s, l)) {
				SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] buildHInopreempt("+l+"): Not enough slots left.");
				throw se;
			}
			
			for (int c = getNbCores() - 1; c >=0; c--) {
				// Find a top ready task
				if (lit.hasNext()) {
					ActorSched a = lit.next();
					if (a.isDelayed())
						break;
					int val = remainingTime[l][a.getGraphID()][a.getId()];
					
					if (firstTimeRunning(l, a)) {
						a.setRunning(true);
						// Promote the task with the highest priority -> non preemptable
						a.setUrgencyinL(0, l);
					}
					
					sched[l][s][c] = a .getName();
					val--;
					
					// The task has been fully scheduled
					if (val == 0) {
						scheduled.add(a);
						taskFinished = true;
						lit.remove();
						a.setRunning(false);
					}
					remainingTime[l][a.getGraphID()][a.getId()] = val;
				}	
			}
			
			resetDelays();
			
			// It a task has been fully allocated check for new activations
			if (taskFinished)
				checkActivationHI(scheduled, ready, l);
			
			if (s != 0) {// Check for new DAG activations
				checkDAGActivation(scheduled, ready, s, l);
				// Update laxities for nodes only if there are free cores
				calcLaxitynopreempt(ready, gethPeriod() - s, l);
			}
		
			ready.sort(new Comparator<ActorSched>() {
				@Override
				public int compare(ActorSched o1, ActorSched o2) {
					if (o1.getUrgencies()[l] - o2.getUrgencies()[l] != 0)
						return o1.getUrgencies()[l] - o2.getUrgencies()[l];
					else
						return o2.getId() - o1.getId();
				}
			});
			setRunningFirst(ready);
			taskFinished = false;
			lit = ready.listIterator();
		}
	}
	
	/**
	 * Method that tries to schedule the system in LO mode with no preemption
	 * @throws SchedulingException
	 */
	private void buildLOnopreempt () throws SchedulingException {
		List<ActorSched> ready = new LinkedList<>();
		List<ActorSched> scheduled = new LinkedList<>();
		
		// Add all source nodes
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSourceinL(0))
					ready.add((ActorSched) a);
			}
		}
		
		calcLaxity(ready, 0, 0);
		ready.sort(loComp);
		
		// Allocate slot by slot the scheduling table
		ListIterator<ActorSched> lit = ready.listIterator();
		boolean taskFinished = false;
		
		for (int s = 0; s < gethPeriod(); s++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildHITable(0): @t = "+s+", tasks activated: ");
				for (ActorSched a : ready)
					System.out.print("L("+a.getName()+") = "+a.getUrgencies()[0]+"; ");
				System.out.println("");
			}
			
			// Verify that is still worth trying to compute the sched table
			if (!isPossible(ready, s, 0)) {
				SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] buildHITable(0): Not enough slots left.");
				throw se;
			}
			
			for (int c = 0; c < getNbCores(); c++) {
				// Get the next element on the LO list
				if (lit.hasNext()) {
					ActorSched a = lit.next();
					int val = remainingTime[0][a.getGraphID()][a.getId()];
					
					sched[0][s][c] = a.getName();
					val--;
					
					if (val == 0) {
						lit.remove();
						scheduled.add(a);
						taskFinished = true;
					}
					
					remainingTime[0][a.getGraphID()][a.getId()] = val;
				}
			}
						
			if (taskFinished)
				checkActivationLO(scheduled, ready);
			
			if (s != hPeriod - 1) {
				checkDAGActivation(scheduled, ready, s + 1, 0);
				calcLaxity(ready, s + 1, 0);
			}
			ready.sort(loComp);
			taskFinished = false;
			lit = ready.listIterator();
		}
	}
	
	/**
	 * Builds all scheduling tables with no preemption
	 */
	public void buildAllnonpreempt() throws SchedulingException {
		initRemainTime();
		initTables();
		
		// Calculate LFTs and urgencies in all DAGs
		for (DAG d : getMcDags()) {
			calcLFTs(d);
			if (isDebug()) printLFTs(d);
		}
		
		// Build tables: more critical tables first
		for (int i = getLevels() - 1; i >= 1; i--)
			buildHInopreempt(i);
		
		buildLOnopreempt();
				
		for (int i = 0; i < getLevels(); i++)
			AlignScheduler.align(sched, i, gethPeriod(), getNbCores());
		
		if(isDebug()) printTables();
	}
	
	/*
	 * DEBUG functions
	 */
	
	/**
	 * Prints LFTs for all DAGs and all nodes in all the levels
	 * @param d
	 */
	private void printLFTs (DAG d) {
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
	 * Prints information about the DAGs
	 */
	public void printDAGs () {
		System.out.println("[DEBUG "+Thread.currentThread().getName()+"] N levels: Number of DAGs "+getMcDags().size()+", on "+getLevels()+" levels.");
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"]\t Actor "+a.getName()+", ");
				for (int i = 0; i < getLevels(); i++)
					System.out.print(a.getCI(i)+" ");
				System.out.println("");
				
				for (Edge e : a.getSndEdges())
					System.out.println("[DEBUG "+Thread.currentThread().getName()+"]\t\t Edge "+e.getSrc().getName()+" -> "+e.getDest().getName());
			}
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
	 * Getters & Setters
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

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int[][][] getRemainingTime() {
		return remainingTime;
	}

	public void setRemainingTime(int remainingTime[][][]) {
		this.remainingTime = remainingTime;
	}

	public Comparator<ActorSched> getLoComp() {
		return loComp;
	}

	public void setLoComp(Comparator<ActorSched> loComp) {
		this.loComp = loComp;
	}

	public Hashtable<ActorSched, Integer> getPreempts() {
		return preempts;
	}

	public void setPreempts(Hashtable<ActorSched, Integer> preempts) {
		this.preempts = preempts;
	}

	public Hashtable<ActorSched, Integer> getCtxSwitch() {
		return ctxSwitch;
	}

	public void setCtxSwitch(Hashtable<ActorSched, Integer> ctxSwitch) {
		this.ctxSwitch = ctxSwitch;
	}

	public int getActivations() {
		return activations;
	}

	public void setActivations(int activations) {
		this.activations = activations;
	}

}
