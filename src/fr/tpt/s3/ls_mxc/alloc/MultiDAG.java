/*******************************************************************************
 * Copyright (c) 2017 Roberto Medina
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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;

public class MultiDAG {
	
	// Set of DAGs to be scheduled
	private Set<DAG> mcDags;
	
	// Architecture
	private int nbCores;
	
	// List of DAG nodes to order them
	private List<Actor> lLO;
	private List<Actor> lHI;
	
	// Scheduling tables
	private String sLO[][];
	private String sHI[][];
	
	// LSAIs of HI tasks
	private Hashtable<String, Integer> startHI;
	
	// Max deadline
	private int maxD;

	// Remaining time for all nodes
	private Hashtable<String, Integer> remainTLO;
	private Hashtable<String, Integer> remainTHI;
	
	private boolean debug;
	
	/**
	 * Constructor of the Multi DAG scheduler
	 * @param sd
	 * @param cores
	 */
	public MultiDAG (Set<DAG> sd, int cores) {
		setMcDags(sd);
		setNbCores(cores);
		startHI = new Hashtable<>();
		remainTLO = new Hashtable<>();
		remainTHI = new Hashtable<>();
	}
	
	/**
	 * Allocates the scheduling tables & the LSAIs
	 */
	private void initTables () {
		maxD = 0;
		// Look for the maximum deadline
		for (DAG d : getMcDags()) {
			if (d.getDeadline() > maxD)
				maxD = d.getDeadline();				
		}
	
		sHI = new String[maxD][getNbCores()];
		sLO = new String[maxD][getNbCores()];
	}
	
	/**
	 * Gives the urgency of an Actor
	 * @param a
	 * @param deadline
	 * @param mode
	 * @return
	 */
	private int calcActorUrgencyLO (Actor a, int deadline, short mode) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSink()) {
			a.setUrgencyLO(deadline - a.getCLO());			
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;

			for (Edge e : a.getSndEdges()) {
				test = e.getDest().getUrgencyLO() - a.getCLO();
				
				if (test < ret)
					ret = test;
			}
			a.setUrgencyLO(ret);
		}
		return ret;
	}
	
	private int calcActorUrgencyHI (Actor a, int deadline, short mode) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSinkinHI()) {
			a.setUrgencyHI(deadline - a.getCLO());			
			ret = deadline;
			
		} else {
			int test = Integer.MAX_VALUE;

			for (Edge e : a.getSndEdges()) {
				test = e.getDest().getUrgencyHI() - a.getCHI();
				
				if (test < ret)
					ret = test;
			}
			a.setUrgencyHI(ret);
		}
		return ret;
	}
	
	/**
	 * Recursively calculates the weight of an actor
	 * @param a
	 * @param deadline
	 * @param mode
	 * @return
	 */
	private void calcDAGUrgency (DAG d) {
		// Add a list to add the nodes that have to be visited
		ArrayList<Actor> toVisit = new ArrayList<>();
		ArrayList<Actor> toVisitHI = new ArrayList<>();
		
		for (Actor a : d.getSinks()) {
			toVisit.add(a);
			a.setVisited(true);
		}
		
		for (Actor a : d.getSinksHI()) {
			toVisitHI.add(a);
			a.setVisitedHI(true);
		}
		
		while (toVisit.size() != 0 && toVisitHI.size() != 0) {
			Actor a = toVisit.get(0);
			calcActorUrgencyLO(a, d.getDeadline(), Actor.LO);
			
			for (Edge e : a.getRcvEdges()) {
				if (!e.getSrc().isVisited()) {
					toVisit.add(e.getSrc());
					e.getSrc().setVisited(true);
				}
			}
			toVisit.remove(0);
		}
		
		while (toVisitHI.size() != 0) {
			Actor a = toVisitHI.get(0);
			calcActorUrgencyHI(a, d.getDeadline(), Actor.HI);
			
			for (Edge e : a.getRcvEdges()) {
				if (e.getSrc().getCHI() != 0 && !e.getSrc().isVisitedHI()) {
					toVisitHI.add(e.getSrc());
					e.getSrc().setVisitedHI(true);
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
			// Go from the sinks to the sources
			calcDAGUrgency(d);
		}
	}

	/**
	 * Checks for new activations in the HI mode 
	 * @param a
	 * @param sched
	 */
	private void checkActivationHI (List<Actor> sched, List<Actor> ready, short mode) {
		// Check all predecessor of actor a that just finished
		for (Actor a : sched) {
			for (Edge e : a.getRcvEdges()) {
				Actor pred = e.getSrc();
				boolean add = true;
			
				// 	Check all successors of the predecessor
				for (Edge e2 : pred.getSndEdges()) {
					if (mode == Actor.HI) {
						if (e2.getDest().getCHI() != 0 && !sched.contains(e2.getDest()))
							add = false;
					} else {
						if (!sched.contains(e2.getDest()))
							add = false;
					}
				}
			
				if (add && !ready.contains(pred) && remainTHI.get(pred.getName()) != 0) {
					ready.add(pred);
					if (isDebug()) {
						System.out.println("Activation HI => Task "+pred.getName()+" has been added to the ready list");
					}
				}
			}
		}
	}
	
	/**
	 * Checks for activations in the LO mode
	 * @param a
	 * @param sched
	 */
	private void checkActivationLO (List<Actor> sched, List<Actor> ready, short mode) {
		// Check all predecessor of actor a that just finished
		for (Actor a : sched) {
			for (Edge e : a.getSndEdges()) {
				Actor succ = e.getDest();
				boolean add = true;
			
				// 	Check all successors of the predecessor
				for (Edge e2 : succ.getRcvEdges()) {
					if (!sched.contains(e2.getSrc()))
						add = false;
				}
			
				if (add && !ready.contains(succ) && remainTLO.get(succ.getName()) != 0) {
					ready.add(succ);
					if (isDebug()) {
						System.out.println("Activation LO => Task "+succ.getName()+" has been added to the ready list");
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
				if (a.getCHI() != 0)
					remainTHI.put(a.getName(), a.getCHI());
				
				remainTLO.put(a.getName(), a.getCLO());
			}
		}
	}
	
	

	/**
	 * Allocates the DAGs in the HI mode and registers LSAIs
	 * @throws SchedulingException
	 */
	public void allocHI () throws SchedulingException {
		List<Actor> sched = new LinkedList<>();
		lHI = new ArrayList<Actor>();
		
		// Add all HI nodes to the list.
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.getCHI() != 0 && a.isSinkinHI())
					lHI.add(a);
			}
		}
		
		lHI.sort(new Comparator<Actor>() {
			@Override
			public int compare(Actor o1, Actor o2) {
				if (o1.getUrgencyHI() - o2.getUrgencyHI() != 0)
					return o2.getUrgencyHI() - o1.getUrgencyHI();
				else
					return o2.getId() - o1.getId();
			}			
		});
		
		// Allocate all slots of the HI scheduling table
		ListIterator<Actor> lit = lHI.listIterator();
		boolean taskFinished = false;
		
		for (int s = maxD - 1; s >= 0; s--) {	
			for (int c = 0; c < getNbCores(); c++) {
				// Find a ready task in the HI list
				if (lit.hasNext()) {
					Actor a = lit.next();
					int val = remainTHI.get(a.getName());
					
					sHI[s][c] = a.getName();
					val--;
					
					if (val == 0) {
						lit.remove();
						startHI.put(a.getName(), s);
						sched.add(a);
						taskFinished = true;
					}
					remainTHI.put(a.getName(), val);
				}
			}
			
			if (taskFinished) {
				checkActivationHI(sched, lHI, Actor.HI);
				lHI.sort(new Comparator<Actor>() {
					@Override
					public int compare(Actor o1, Actor o2) {
						if (o1.getUrgencyHI() - o2.getUrgencyHI() != 0)
							return o2.getUrgencyHI() - o1.getUrgencyHI();
						else
							return o2.getId() - o1.getId();
					}			
				});
			}
			taskFinished = false;
			lit = lHI.listIterator();
		}
	}
	
	/**
	 * Checks if the deadlines can still be satisfied
	 * @return
	 */
	private boolean checkDeadlines (int slot) {
		boolean satisfiable = true;
		
		for (DAG d : getMcDags()) {
			int sum = 0;
			int bLeft = (d.getDeadline() - slot) * getNbCores();
			
			for (Actor a : d.getNodes())
				sum += remainTLO.get(a.getName());
			
			if (sum > bLeft) {
				satisfiable = false;
				break;
			}
		}
		
		return satisfiable;
	}
	
	/**
	 * Checks if HI tasks need to be activated
	 * @param ready
	 * @param slot
	 */
	private void checkLSAIHI (List<Actor> ready, int slot) {
		Enumeration<String> enumKey = startHI.keys();
		
		while (enumKey.hasMoreElements()) {
			String task = enumKey.nextElement();
			Integer val = startHI.get(task);
			
			if (slot == val) { // The slot corresponds to an LSAI
				for (DAG d : getMcDags()) {
					Actor a = d.getNodebyName(task);
					
					if (a != null && remainTLO.get(a.getName()) == a.getCLO()) {
						if (!ready.contains(a)) // Check if the task is in the ready list (it should be)
							System.out.println("WARNING: HI task is promoted but not in the ready queue.");
						a.setUrgencyLO(0);
						ready.sort(new Comparator<Actor>() {
							@Override
							public int compare(Actor o1, Actor o2) {
								if (o1.getUrgencyLO() - o2.getUrgencyLO() != 0)
									return o1.getUrgencyLO() - o2.getUrgencyLO();
								else
									return o1.getId() - o2.getId();
							}			
						});
						if (isDebug())
							System.out.println("LSAI => LSAI of task "+a.getName()+"; task has been promoted.");
					}
				}
			}
		}
	}
	
	/**
	 * Allocates the DAGs in LO mode
	 * @throws SchedulingException
	 */
	public void allocLO () throws SchedulingException {
		List<Actor> sched = new LinkedList<>();
		lLO = new ArrayList<Actor>();
		
		// Add all HI nodes to the list.
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.isSource())
					lLO.add(a);
			}
		}
		
		lLO.sort(new Comparator<Actor>() {
			@Override
			public int compare(Actor o1, Actor o2) {
				if (o1.getUrgencyLO() - o2.getUrgencyLO() != 0)
					return o1.getUrgencyLO() - o2.getUrgencyLO();
				else
					return o1.getId() - o2.getId();
			}			
		});
		
		// Allocate all slots of the LO scheduling table
		ListIterator<Actor> lit = lLO.listIterator();
		boolean taskFinished = false;
		
		for (int s = 0; s < maxD; s++) {
			// Verify that there are enough slots to continue the scheduling
			if (!checkDeadlines(s)) {
				SchedulingException se = new SchedulingException("Alloc LO MultiDAG: Not enough slot left");
				throw se;
			}
			
			for (int c = 0; c < getNbCores(); c++) {
				// Find a ready task in the HI list
				if (lit.hasNext()) {
					Actor a = lit.next();
					int val = remainTLO.get(a.getName());
					
					sLO[s][c] = a.getName();
					System.out.println("SLO => Task "+a.getName()+"; exec remain "+val);
					val--;
					
					if (val == 0) {
						lit.remove();
						
						sched.add(a);
						taskFinished = true;
					}
					remainTLO.put(a.getName(), val);
				}
			}
			
			if (taskFinished) {
				checkActivationLO(sched, lLO, Actor.LO);
				lLO.sort(new Comparator<Actor>() {
					@Override
					public int compare(Actor o1, Actor o2) {
						if (o1.getUrgencyLO() - o2.getUrgencyLO() != 0)
							return o1.getUrgencyLO() - o2.getUrgencyLO();
						else
							return o1.getId() - o2.getId();
					}			
				});
			}
			taskFinished = false;
			// Check for HI activation of tasks
			checkLSAIHI(lLO, s+1);
			lit = lLO.listIterator();
		}
	}
	
	/**
	 * Tries to allocate all DAGs in the number of cores given
	 * @throws SchedulingException
	 */
	public void allocAll (boolean debug) throws SchedulingException {
		this.setDebug(debug);
		initTables();
		calcWeights();
		if (isDebug())
			printUrgencies();
		initRemainT();
		allocHI();
		if (isDebug())
			printSHI();
		allocLO();
		if (isDebug())
			printSLO();
	}
	
	/*
	 * Debugging functions
	 */
	/**
	 * Prints urgency values for the multi DAG scheduling
	 */
	public void printUrgencies () {
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				System.out.println("DAG "+d.getId()+"; Actor "+a.getName()
									+"; Urgency LO "+a.getUrgencyLO()+"; Urgency HI "+a.getUrgencyHI());
			}
		}
		System.out.println("");
	}
	
	/**
	 * Prints the HI scheduling table
	 */
	public void printSHI () {
		for (int c = 0; c < getNbCores(); c++) {
			for (int s = 0; s < maxD; s++) {
				System.out.print(sHI[s][c]+" | ");
			}
			System.out.print("\n");
		}
		
		System.out.print("\n");
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes_HI())
				System.out.println("Task "+a.getName()+" LSAI = "+getStartHI().get(a.getName()));
		}
	}
	
	/**
	 * Prints the LO scheduling table
	 */
	public void printSLO () {
		for (int c = 0; c < getNbCores(); c++) {
			for (int s = 0; s < maxD; s++) {
				System.out.print(sLO[s][c]+" | ");
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

	public String[][] getsLO() {
		return sLO;
	}

	public void setsLO(String[][] sLO) {
		this.sLO = sLO;
	}

	public String[][] getsHI() {
		return sHI;
	}

	public void setsHI(String[][] sHI) {
		this.sHI = sHI;
	}

	public List<Actor> getlLO() {
		return lLO;
	}

	public void setlLO(List<Actor> lLO) {
		this.lLO = lLO;
	}

	public List<Actor> getlHI() {
		return lHI;
	}

	public void setlHI(List<Actor> lHI) {
		this.lHI = lHI;
	}

	public Set<DAG> getMcDags() {
		return mcDags;
	}

	public void setMcDags(Set<DAG> mcDags) {
		this.mcDags = mcDags;
	}

	public Hashtable<String, Integer> getStartHI() {
		return startHI;
	}

	public void setStartHI(Hashtable<String, Integer> startHI) {
		this.startHI = startHI;
	}

	public int getMaxD() {
		return maxD;
	}

	public void setMaxD(int maxD) {
		this.maxD = maxD;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}
