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
	
		setsHI(new String[maxD][getNbCores()]);
		setsLO(new String[maxD][getNbCores()]);
	}
	
	/**
	 * Recursively calculates the weight of an actor
	 * @param a
	 * @param deadline
	 * @param mode
	 * @return
	 */
	private void calcWeight (Actor a, int deadline, short mode) {
		if (a.isExitNode()) {
			if (mode == Actor.HI) {
				a.setEarDeadHI(deadline);
				a.setUrgencyHI(deadline - a.getCHI());
			} else {
				a.setEarDeadLO(deadline);
				a.setUrgencyLO(deadline - a.getCLO());
			}
		} else {
			int min = Integer.MAX_VALUE;
			if (mode == Actor.HI) {
				for (Edge e : a.getSndEdges()) {
					if ((min > e.getDest().getEarDeadHI())
							&& e.getDest().getCHI() != 0)
						min = e.getDest().getEarDeadHI();
				}
				a.setEarDeadHI(min);
				a.setUrgencyHI(min - a.getCHI());
			} else {
				for (Edge e : a.getSndEdges()) {
					if (min > e.getDest().getEarDeadLO())
						min = e.getDest().getEarDeadLO();
				}
				a.setEarDeadLO(min);
				a.setUrgencyLO(min - a.getCLO());
			}
			
		}
	}
	
	
	/**
	 * Calculates weights for tasks depending on the deadline
	 */
	private void calcWeights () {
		for (DAG d : getMcDags()) {
			// Go from the sinks to the sources
			for (Actor a : d.getNodes()) {
				if (a.getCHI() != 0)
					calcWeight(a, d.getDeadline(), Actor.HI);
				
				calcWeight(a, d.getDeadline(), Actor.LO);
			}
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
				while (!found) {
					if (lit.hasNext()) {
						a = lit.next();
						if (okDataDepend(a, sched) && !sched.contains(a))
							found = true;
					}
				}
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
		for (int s = 0; s < maxD; s++) {
			for (int c = 0; c < getNbCores(); c++) {
				System.out.print(sHI[s][c]+" | ");
			}
			System.out.print("\n");
		}
	}
	
	/**
	 * Prints the LO scheduling table
	 */
	public void printSLO () {
		for (int s = 0; s < maxD; s++) {
			for (int c = 0; c < getNbCores(); c++) {
				System.out.print(sLO[s][c]+" | ");
			}
			System.out.print("\n");
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

	
}
