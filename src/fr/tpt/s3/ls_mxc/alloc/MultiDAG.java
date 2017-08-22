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
import java.util.Hashtable;
import java.util.Iterator;
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
	 * Adds an Actor at the right spot in the List
	 * @param la
	 * @param a
	 */
	private void addInList (List<Actor> la, Actor a, short mode) {
		int idx = 0;
		
		if (mode == Actor.HI) {
			Iterator<Actor> ia = la.iterator();
			while (ia.hasNext() ) {
				Actor t = ia.next();
				if (t.getUrgencyHI() > a.getUrgencyHI())
					idx++;
				else
					break;
			}
		} else {
			ListIterator<Actor> ia = la.listIterator();
			while (ia.hasNext() ) {
				Actor t = ia.next();
				if (t.getUrgencyLO() < a.getUrgencyLO())
					idx++;
				else
					break;
			}
		}
		la.add(idx, a);
	}
	
	/**
	 * Checks if predecessors of Actor a has been allocated 
	 * @param a
	 * @param sched
	 * @return
	 */
	private boolean okDataDepend (Actor a, List<Actor> sched) {
		for(Edge e : a.getRcvEdges()) {
			if (!sched.contains(e.getSrc()))
				return false;
		}
		return true;
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
		
		setlHI(new ArrayList<Actor>());
		// Add all HI nodes to the list.
		for (DAG d : getMcDags()) {
			for (Actor a : d.getNodes()) {
				if (a.getCHI() != 0)
					addInList(lHI, a, Actor.HI);
			}
		}
		
		// Allocate all slots of the HI scheduling table
		ListIterator<Actor> lit = lHI.listIterator();
		boolean found = false;
		for (int s = maxD - 1; s >= 0; s--) {
			
			for (int c = 0; c < getNbCores(); c++) {
				// Find a ready task in the HI list
				Actor a = null;
				while (!found && lit.hasNext()) {
					a = lit.next();
					if (okDataDepend(a, sched) && !sched.contains(a))
						found = true;
	
				}
				if (found) {
					sHI[s][c] = a.getName();
					int val = remainTHI.get(a.getName());
					val--;
					if (val == 0) {
							startHI.put(a.getName(), s);
							sched.add(a);
							lit.remove();
						}
					remainTHI.put(a.getName(), val);
				}
				found = false;
			}
			
			lit = lHI.listIterator(lHI.size());
		}
	}
	
	/**
	 * Allocates the DAGs in LO mode
	 * @throws SchedulingException
	 */
	public void allocLO () throws SchedulingException {
		
	}
	
	/**
	 * Tries to allocate all DAGs in the number of cores given
	 * @throws SchedulingException
	 */
	public void allocAll () throws SchedulingException {
		initTables();
		calcWeights();
		initRemainT();
		allocHI();
		allocLO();
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
									+"; Urgency HI "+a.getUrgencyHI()+"; Urgency LO "+a.getUrgencyLO());
			}
		}
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
}
