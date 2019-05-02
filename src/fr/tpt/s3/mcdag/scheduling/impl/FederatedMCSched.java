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
package fr.tpt.s3.mcdag.scheduling.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.SchedulingException;
import fr.tpt.s3.mcdag.scheduling.old.AbstractMixedCriticalityScheduler;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.util.Counters;
import fr.tpt.s3.mcdag.util.MathMCDAG;

/**
 * Class implementing Federated MC-DAG scheduling
 * @author roberto
 *
 */
public class FederatedMCSched extends AbstractMixedCriticalityScheduler{
	
	// Set of DAGs to be scheduled 
	private Set<McDAG> mcDags;
	
	// Architecture
	private int nbCores;
	private int hPeriod;
	
	private Comparator<VertexScheduling> loComp;
	private Comparator<VertexScheduling> hiComp;
	
	private int activations;
	private Hashtable<VertexScheduling, Integer> preempts;
	
	private boolean debug;
	
	/**
	 * Constructor
	 * @param system
	 * @param architecture
	 */
	public FederatedMCSched (Set<McDAG> system, int architecture, boolean debug) {
		setMcDags(system);
		setNbCores(architecture);
		setDebug(debug);
		setLoComp(new Comparator<VertexScheduling>() {
			@Override
			public int compare (VertexScheduling o1, VertexScheduling o2) {
				if (o1.getHlfet()[0] - o2.getHlfet()[0] != 0)
					return o1.getHlfet()[0] - o2.getHlfet()[0];
				else
					return o1.getId() - o2.getId();
			}
		});
		setHiComp(new Comparator<VertexScheduling>() {
			@Override
			public int compare (VertexScheduling o1, VertexScheduling o2) {
				if (o1.getHlfet()[1] - o2.getHlfet()[1] != 0)
					return o1.getHlfet()[1] - o2.getHlfet()[1];
				else
					return o1.getId() - o2.getId();
			}
		});
		
		int[] input = new int[getMcDags().size()];
		int i = 0;
		
		for (McDAG d : getMcDags()) {
			input[i] = d.getDeadline();
			i++;
		}
		
		sethPeriod(MathMCDAG.lcm(input));
		
		preempts = new Hashtable<VertexScheduling, Integer>();
	}
	
	private void initRemainingTimes (McDAG d, int remainingTime[], int level) {
		
		for (int i = 0; i < remainingTime.length; i++)
			remainingTime[i] = 0;
		
		for (Vertex a : d.getVertices()) {
			remainingTime[a.getId()] = a.getWcet(level);
			if (a.getWcet(1) != 0)
				activations += (int)(hPeriod / d.getDeadline()) * 2;
			else
				activations += (int)(hPeriod / d.getDeadline());
		}
	}
	
	private void calcHLFETs (McDAG d, final int level, List<VertexScheduling> prioOrder) {
		
		ArrayList<VertexScheduling> toVisit = new ArrayList<VertexScheduling>();
		
		// Look for sinks first
		for (Vertex a : d.getVertices()) {
			if (a.getWcet(level) != 0) {
				if (a.getSndEdges().size() == 0 && a.getWcet(level) != 0) {
					toVisit.add((VertexScheduling)a);
				} else {
					boolean add = true;
					
					for (Edge e : a.getSndEdges()) {
						if (e.getDest().getWcet(level) != 0)
							add = false;
					}
					if (add) {
						toVisit.add((VertexScheduling)a);
					}
				}
			}
		}
		
		// Iterate through nodes and compute their HLFET
		while (toVisit.size() != 0) {
			VertexScheduling a = toVisit.get(0);
			
			// Look for the max on the SndEdges
			int max = 0;
				
			for (Edge e : a.getSndEdges()) {
				VertexScheduling dest = (VertexScheduling) e.getDest();
				if (dest.getWcet(level) != 0 &&
						dest.getHlfet()[level] > max) {
					max = dest.getHlfet()[level];
				}
			}
			a.getHlfet()[level] = max + a.getWcet(level);
			a.getVisitedL()[level] = true;

			for (Edge e : a.getRcvEdges()) {
				boolean allSuccVisited = true;
				Vertex test = e.getSrc();
				
				for (Edge e2 : test.getSndEdges()) {
					VertexScheduling dest = (VertexScheduling) e2.getDest();
					if (!dest.getVisitedL()[level] && dest.getWcet(level) != 0) {
						allSuccVisited = false;
						break;
					}
				}
				
				if (allSuccVisited && test.getWcet(level) != 0 && !toVisit.contains((VertexScheduling)test))
					toVisit.add((VertexScheduling)test);
			}
			toVisit.remove(0);
		}
		
		// Create the list with the priority ordering
		for (Vertex a : d.getVertices()) {
			if (a.getWcet(level) != 0)
				prioOrder.add((VertexScheduling) a);
		}
		Collections.sort(prioOrder, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling o1, VertexScheduling o2) {
				if (o1.getHlfet()[level] - o2.getHlfet()[level] != 0)
					return o1.getHlfet()[level] - o2.getHlfet()[level];
				else
					return o1.getId() - o2.getId();
			}	
		});	
	}
	
	private boolean enoughSlots (List<VertexScheduling> ready, int slot, int deadline, int cores, int remainingTime[]) {
		int sumReady = 0;
		int rSlots = ((deadline -1) - slot) * cores;
		
		for (VertexScheduling a : ready)
			sumReady += remainingTime[a.getId()];
				
		if (sumReady > rSlots)
			return false;
		
		return true;
	}
	
	private void checkNewActivations (List<VertexScheduling> scheduled, List<VertexScheduling> ready, int remainingTime[]) {
		// Check from the scheduled tasks the new activations
		ListIterator<VertexScheduling> lit = scheduled.listIterator();
		while (lit.hasNext()) {
			VertexScheduling sched = lit.next();
			// Check destination nodes
			for (Edge eDest : sched.getSndEdges()) {
				VertexScheduling dest = (VertexScheduling) eDest.getDest();
				
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
	
	private void buildHITable (McDAG d, String sched[][][], List<VertexScheduling> prioOrder) throws SchedulingException {
		
		List<VertexScheduling> ready = new LinkedList<VertexScheduling>();
		List<VertexScheduling> scheduled = new LinkedList<VertexScheduling>();
		int[] remainingTime = new int[d.getVertices().size()];
		boolean taskFinished = false;

		
		// Init remaining time and tables
		initRemainingTimes(d, remainingTime, 1);
		
		for (Vertex a : d.getVertices()) {
			if (a.getRcvEdges().size() == 0 &&
					a.getWcet(1) != 0)
				ready.add((VertexScheduling)a);
		}
		
		Collections.sort(ready, hiComp);
		
		ListIterator<VertexScheduling> pit = prioOrder.listIterator();

		// Iterate through the number of cores
		for (int s = 0; s < d.getDeadline(); s++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildHITable(): @t = "+s+", tasks activated: ");
				for (VertexScheduling a : ready)
					System.out.print("H("+a.getName()+") = "+a.getHlfet()[1]+"; ");
				System.out.println("");
			}
			
			// There aren't enough slots to continue the allocation
			if (!enoughSlots(ready, s, d.getDeadline(), getNbCores(), remainingTime)) {
				SchedulingException se = new SchedulingException("[ERROR"+Thread.currentThread().getName()+"] buildHITable(): Failed to schedule at time "+s);
				throw se;
			}
			
			// Check the priority ordering
			ArrayList<VertexScheduling> toSched = new ArrayList<>();
			int coreBudget = getNbCores();
			
			while (pit.hasNext() && coreBudget > 0) {
				VertexScheduling a = pit.next();
				
				// 	The priority actor is in the ready queue
				if (ready.contains(a)) {
					if (a.isRunning()) { // Task was already running previous slot
						coreBudget--;
						toSched.add(a);
					} else  { // Check if other tasks were running
						for (VertexScheduling check : ready) {
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
			for (VertexScheduling a : toSched) {
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
				Collections.sort(ready, hiComp);
				taskFinished = false;
			}
			pit = prioOrder.listIterator();
		}
		if (!ready.isEmpty()) {
			SchedulingException se = new SchedulingException("[ERROR"+Thread.currentThread().getName()+"] buildHITable(): ready list not empty");
			throw se;
		}
	}
	
	private void buildLOTable (McDAG d, String sched[][][], List<VertexScheduling> loPrioOrder, List<VertexScheduling> hiPrioOrder) throws SchedulingException {
		List<VertexScheduling> ready = new LinkedList<VertexScheduling>();
		List<VertexScheduling> scheduled = new LinkedList<VertexScheduling>();
		int[] remainingTime = new int[d.getVertices().size()];
		boolean taskFinished = false;
		
		// Init remaining time and tables
		initRemainingTimes(d, remainingTime, 0);
		
		for (Vertex a : d.getVertices()) {
			if (a.getRcvEdges().size() == 0)
				ready.add((VertexScheduling)a);
		}
		
		Collections.sort(ready, loComp);
		ListIterator<VertexScheduling> hpit = hiPrioOrder.listIterator();
		ListIterator<VertexScheduling> lpit = loPrioOrder.listIterator();

		// Iterate through the number of slots
		for (int s = 0; s < d.getDeadline(); s++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildLOTable(): @t = "+s+", tasks activated: ");
				for (VertexScheduling a : ready)
					System.out.print("H("+a.getName()+") = "+a.getHlfet()[0]+"; ");
				System.out.println("");
			}
			
			// There aren't enough slots to continue the allocation
			if (!enoughSlots(ready, s, d.getDeadline(), getNbCores(), remainingTime)) {
				SchedulingException se = new SchedulingException("[ERROR"+Thread.currentThread().getName()+"] buildLOTable(): Failed to schedule at time "+s);
				throw se;
			}
			
			// Construct the schedulable elements
			ArrayList<VertexScheduling> toSched = new ArrayList<>();
			int coreBudget = getNbCores();
			
			// Check the priority ordering of HI tasks first
			while (hpit.hasNext() && coreBudget > 0) {
				VertexScheduling a = hpit.next();
				
				// Priority task is in the ready queue
				if (ready.contains(a)) { 
					if (a.isRunning()) { // Task was already running previous slot
						coreBudget--;
						toSched.add(a);
					}  else  { // Check if other HI tasks were running
						for (VertexScheduling check : ready) {
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
				VertexScheduling a = lpit.next();
				
				if (ready.contains(a)) { // LO task is ready
					if (a.isRunning()) {
						coreBudget--;
						toSched.add(a);
					} else { // Check if other LO tasks were already running
						for (VertexScheduling check : ready) {
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
			for (VertexScheduling a : toSched) {
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
				Collections.sort(ready, loComp);
				taskFinished = false;
			}
			hpit = hiPrioOrder.listIterator();
			lpit = loPrioOrder.listIterator();
		}
		if (!ready.isEmpty()) {
			SchedulingException se = new SchedulingException("[ERROR"+Thread.currentThread().getName()+"] buildHITable(): ready list not empty");
			throw se;
		}
	}
	
	protected void initTables () {}
	
	private void checkLightTaskActivation (Set<VertexScheduling> lightTasks,
										   List<VertexScheduling> ready,
										   Hashtable<VertexScheduling, Integer> remainingTime,
										   int slot, int level) {
		for (VertexScheduling a : lightTasks) {
			if (slot % a.getDeadlines()[level] == 0) {
				ready.add(a);
				remainingTime.put(a, a.getWcet(level));
			}
		}
		
	}
	
	private void buildLight (Set<VertexScheduling> lightTasks, String sched[][][], final int level, int hPeriod, int cores)
	throws SchedulingException {
		List<VertexScheduling> ready = new LinkedList<>();
		Hashtable<VertexScheduling, Integer> remainingTime = new Hashtable<VertexScheduling, Integer>();
		
		// Init remainingTimes
		for (VertexScheduling a : lightTasks) {
			remainingTime.put(a, a.getWcet(level));
			ready.add(a);
		}
		Collections.sort(ready, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling o1, VertexScheduling o2) {
				if (o1.getDeadlines()[level] - o2.getDeadlines()[level] != 0)
					return o1.getDeadlines()[level] - o2.getDeadlines()[level];
				else
					return o1.getId() - o2.getId();
			}
		});
		
		ListIterator<VertexScheduling> lit = ready.listIterator();
		for (int s = 0; s < hPeriod; s++) {
			for (int c = 0; c < cores; c++) {
				if (lit.hasNext()) {
					VertexScheduling a = lit.next();
					int val = remainingTime.get(a);
					
					sched[level][s][c] = a.getName();
					val--;
					
					remainingTime.put(a, val);
				}
			}
			
			if (s != hPeriod - 1)
				checkLightTaskActivation(lightTasks, ready, remainingTime, s + 1, level);
			
			Collections.sort(ready, new Comparator<VertexScheduling>() {
				@Override
				public int compare(VertexScheduling o1, VertexScheduling o2) {
					if (o1.getDeadlines()[level] - o2.getDeadlines()[level] != 0)
						return o1.getDeadlines()[level] - o2.getDeadlines()[level];
					else
						return o1.getId() - o2.getId();
				}
			});
			lit = ready.listIterator();
		}
	}
	
	public void buildAllTables () throws SchedulingException {
		
		int coresQuota = getNbCores();
		double uLightDAGs = 0.0;
		Set<McDAG> heavyDAGs = new HashSet<McDAG>();
		Set<McDAG> lightDAGs = new HashSet<McDAG>();
			
		// Separate heavy and light DAGs
		// Check if we have enough cores in the architecture
		for (McDAG d : getMcDags()) {
			if (d.getUmax() < 1) {
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
		for (McDAG d : lightDAGs)
			uLightDAGs += d.getUmax();
		
		if (Math.ceil(uLightDAGs) > coresQuota) {
			SchedulingException se = new SchedulingException("Not enough cores in federated");
			throw se;
		}
		
		// Check for scheduling of heavy DAGs
		for (McDAG d : heavyDAGs) {			
			List<VertexScheduling> hiPrioOrder = new LinkedList<>();
			List<VertexScheduling> loPrioOrder = new LinkedList<>();
			// Init sched table
			String sched[][][] = new String[2][d.getDeadline()][getNbCores()];
			
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < d.getDeadline(); j++) {
					for (int k = 0; k < d.getMinCores(); k++) {
						sched[i][j][k] = "-";
					}
				}
			}
			if (isDebug()) printDAG(d);
			
			calcHLFETs(d, 1, hiPrioOrder);
			calcHLFETs(d, 0, loPrioOrder);
			
			ListIterator<VertexScheduling> lit = loPrioOrder.listIterator();
			while (lit.hasNext()) {
				VertexScheduling a = lit.next();
				if (a.getWcet(1) > 0)
					lit.remove();
			}
			Collections.sort(loPrioOrder, loComp);
			Collections.sort(hiPrioOrder, hiComp);
			
			if (isDebug()) printHLFETLevels(d);
			
			buildHITable(d, sched, hiPrioOrder);
			buildLOTable(d, sched, loPrioOrder, hiPrioOrder);
			
			for (Vertex a : d.getVertices()) {
				VertexScheduling task = (VertexScheduling) a;
				preempts.put(task, 0);
			}
			Counters.countPreemptions(sched, preempts, 2, gethPeriod(), d.getDeadline(), d.getMinCores());
		}

		// Build tables for light DAGs
		int coresLight = (int) Math.ceil(uLightDAGs);
		String sched[][][] = new String[2][gethPeriod()][coresLight];
		Set<VertexScheduling> lightTasks = new HashSet<VertexScheduling>();

		// Transform DAGs to independent tasks and add them to set
		for (McDAG d : lightDAGs) {
			VertexScheduling indTask = new VertexScheduling(d.getId(), "it_d"+d.getId(), 2);
			int cilo = 0, cihi = 0;
			
			for (Vertex a : d.getVertices()) {
				if (a.getWcet(1) != 0)
					cihi += a.getWcet(1);
				cilo += a.getWcet(0);
			}
			int wcets[] = {cilo, cihi};
			indTask.setWcets(wcets);
			 
			indTask.setDeadlineInL(d.getDeadline(), 0);
			indTask.setDeadlineInL(d.getDeadline(), 1);
			lightTasks.add(indTask);
			preempts.put(indTask, 0);
			activations += (int) hPeriod / indTask.getDeadlines()[0];
		}
		
		// Calculate the hyperperiod of the light DAGs ?????
		
		// Build table in LO & HI
		buildLight(lightTasks, sched, 0, gethPeriod(), coresLight);
		buildLight(lightTasks, sched, 1, gethPeriod(), coresLight);
		Counters.countPreemptions(sched, preempts, 2, gethPeriod(), gethPeriod(), coresLight);

		
		if (debug) printPreempts();
	}
	
	/*
	 * DEBUG FUNCTIONS
	 */
	private void printHLFETLevels (McDAG d) {
		for (Vertex a : d.getVertices())
			System.out.println("Node "+a.getName()+" HLEFT(HI) "+((VertexScheduling)a).getHlfet()[1]+" HLFET(LO) "+((VertexScheduling)a).getHlfet()[0]);
	}
	
	private void printDAG (McDAG d) {
		for (Vertex a : d.getVertices())
			System.out.println("Node "+a.getName()+" Ci(HI) "+((VertexScheduling)a).getWcet(1)+" Ci(LO) "+((VertexScheduling)a).getWcet(0));
	}
	
	private void printPreempts () {
		int total = 0;
		System.out.println("[DEBUG "+Thread.currentThread().getName()+"] Printing preemption data...");

		for (VertexScheduling a : preempts.keySet()) {
			System.out.println("[DEBUG "+Thread.currentThread().getName()+"]\t Task "+a.getName()+" peempted "+preempts.get(a)+" times.");
			total += preempts.get(a);
		}
		System.out.println("[DEBUG "+Thread.currentThread().getName()+"] Total number of preemptions = "+total+" for "+activations+" activations");
	}

	/*
	 * Getters and setters
	 */
	public Set<McDAG> getMcDags() {
		return mcDags;
	}

	public void setMcDags(Set<McDAG> mcDags) {
		this.mcDags = mcDags;
	}

	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}

	public Comparator<VertexScheduling> getLoComp() {
		return loComp;
	}

	public void setLoComp(Comparator<VertexScheduling> loComp) {
		this.loComp = loComp;
	}

	public Comparator<VertexScheduling> getHiComp() {
		return hiComp;
	}

	public void setHiComp(Comparator<VertexScheduling> hiComp) {
		this.hiComp = hiComp;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public Hashtable<VertexScheduling, Integer> getPreempts() {
		return preempts;
	}

	public void setPreempts(Hashtable<VertexScheduling, Integer> preempts) {
		this.preempts = preempts;
	}

	public int gethPeriod() {
		return hPeriod;
	}

	public void sethPeriod(int hPeriod) {
		this.hPeriod = hPeriod;
	}

	public int getActivations() {
		return activations;
	}

	public void setActivations(int activations) {
		this.activations = activations;
	}
}
