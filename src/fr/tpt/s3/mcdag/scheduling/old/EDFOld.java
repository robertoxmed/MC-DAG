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
package fr.tpt.s3.mcdag.scheduling.old;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.scheduling.SchedulingException;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.util.Counters;
import fr.tpt.s3.mcdag.util.MathMCDAG;

/**
 * Heuristic based on EDF to schedule Multiple MC-DAGs
 * @author roberto
 *
 */
public class EDFOld extends AbstractMixedCriticalityScheduler {
	
	// Set of MC-DAGs to be scheduled
	private Set<McDAG> mcDags;
	
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
	private Comparator<VertexScheduling> loComp;
	
	// To count preemptions
	private int activations;
	private Hashtable<VertexScheduling, Integer> preempts;
	
	// Debugging boolean
	private boolean debug;
	
	/**
	 * Constructor
	 * @param dags
	 * @param cores
	 * @param levels
	 * @param debug
	 */
	public EDFOld (Set<McDAG> dags, int cores, int levels, boolean debug) {
		setMcDags(dags);
		setNbCores(cores);
		setLevels(levels);
		setDebug(debug);
		remainingTime = new int[getLevels()][getMcDags().size()][];
		
		// Init remaining time for each DAG
		for (McDAG d : getMcDags()) {
			for (int i = 0; i < getLevels(); i++)
				remainingTime[i][d.getId()] = new int[d.getVertices().size()];
		}
		
		setLoComp(new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling arg0, VertexScheduling arg1) {
				if (arg0.getWeights()[0] - arg1.getWeights()[0] != 0)
					return arg0.getWeights()[0] - arg1.getWeights()[0];
				else
					return arg0.getId() - arg1.getId();
			}
		});
		
		setActivations(0);
		setPreempts(new Hashtable<VertexScheduling, Integer>());
	}
	
	/**
	 * Inits remaining time for tasks
	 */
	private void initRemainingTimes () {
		for (int i = 0; i < getLevels(); i++) {
			for (McDAG d : getMcDags()) {
				for (Vertex a : d.getVertices())
					remainingTime[i][d.getId()][a.getId()] = a.getWcet(i);
			}
		}
	}
	
	/**
	 * Initialization of tables
	 */
	@Override
	protected void initTables() {
		int[] input = new int[getMcDags().size()];
		int i = 0;
		
		for (McDAG d : getMcDags()) {
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
		
		// Calc number of activations
		for (McDAG d : getMcDags()) {
			for (Vertex a : d.getVertices()) {
				// Check if task runs in HI mode
				int nbActivations = (int) (hPeriod / d.getDeadline());
				if (a.getWcet(1) != 0)
					activations = activations + nbActivations * d.getLevels();
				else
					activations = activations + nbActivations;
			}
		}
	}
	

	private int scheduledUntilTinL (VertexScheduling a, int t, int l) {
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
	private int scheduledUntilTinLreverse (VertexScheduling a, int t, int l) {
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
	private void checkActivationHI (List<VertexScheduling> sched, List<VertexScheduling> ready, int level) {

		for (VertexScheduling a : sched) {
			// Check predecessors of task that was just allocated
			for (Edge e : a.getRcvEdges()) {
				VertexScheduling pred = (VertexScheduling) e.getSrc();
				boolean add = true;
				
				// Check if all successors of the predecessor have been allocated
				for (Edge e2 : pred.getSndEdges()) {
					if (e2.getDest().getWcet(level) > 0 && !sched.contains(e2.getDest())) {
						add = false;
						break;
					}
				}
				
				if (add && !ready.contains(pred) && remainingTime[level][pred.getGraphId()][pred.getId()] != 0) {
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
	private void checkActivationLO (List<VertexScheduling> sched, List<VertexScheduling> ready) {

		for (VertexScheduling a : sched) {
			// Check predecessors of task that was just allocated
			for (Edge e : a.getSndEdges()) {
				VertexScheduling succ = (VertexScheduling) e.getDest();
				boolean add = true;
				
				// Check if all successors of the predecessor have been allocated
				for (Edge e2 : succ.getRcvEdges()) {
					if (!sched.contains(e2.getSrc())) {
						add = false;
						break;
					}
				}
				
				if (add && !ready.contains(succ) && remainingTime[0][succ.getGraphId()][succ.getId()] != 0) {
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
	private void checkDAGActivation (List<VertexScheduling> sched, List<VertexScheduling> ready, int slot, int level) {
		for (McDAG d : getMcDags()) {
			// If the slot is a multiple of the deadline is a new activation
			if (slot % d.getDeadline() == 0) {
				ListIterator<VertexScheduling> it = sched.listIterator();
				
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] checkDAGActivation(): DAG (id. "+d.getId()+") activation at slot "+slot);
				for (Vertex a : d.getVertices()) {
					while (it.hasNext()) { // Remove nodes from the sched list
						VertexScheduling a2 = it.next();
						if (a.getName().contentEquals(a2.getName()))
							it.remove();
					}
					it = sched.listIterator();
					// Re-init execution time
					remainingTime[level][((VertexScheduling)a).getGraphId()][a.getId()] = a.getWcet(level);
					
					if (level >= 1 && a.isSinkinL(level)) {
						ready.add((VertexScheduling)a);
					} else if (level == 0 && a.isSourceinL(level)) {
						ready.add((VertexScheduling)a);
					}
				}
			}
		}
	}
	
	/**
	 * Function that calculates the priorities
	 * @param ready
	 * @param slot
	 * @param level
	 */
	private void calcPriorities (List<VertexScheduling> ready, int slot, int level) {
		for (VertexScheduling a : ready) {
			int dagId = a.getGraphId();
			if (level >= 1) {
				// It's not the highest criticality level
				if (level != getLevels() - 1) {
					// Checks for delays in hi tasks
					int deltaI = a.getWcet(level + 1) - a.getWcet(level);
					if (scheduledUntilTinLreverse(a, slot, level + 1) - deltaI < 0) {
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Task "+a.getName()+" needs to be delayed at slot @t = "+slot);
						a.setWeightInL(Integer.MAX_VALUE, level);
					} else if (scheduledUntilTinLreverse(a, slot, level) != 0 &&
							scheduledUntilTinLreverse(a, slot, level) - scheduledUntilTinLreverse(a, slot, level + 1) + deltaI == 0) {
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Task "+a.getName()+" needs to be delayed at slot @t = "+slot);
						a.setWeightInL(Integer.MAX_VALUE, level);
					} else {
						a.setWeightInL(a.getDeadlines()[level], level);
					}
				} else {
					a.setWeightInL(a.getDeadlines()[level], level);
				}
			} else {
				// If it's a HI task verify that mode transition is respected
				if (a.getWcet(level + 1) > 0) {
					// Promotion needed for the task
					if ((a.getWcet(level) - remainingTime[level][dagId][a.getId()]) - scheduledUntilTinL(a, slot, level + 1) < 0) {
						if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] calcLaxity(): Promotion of task "+a.getName()+" at slot @t = "+slot);
						a.setWeightInL(0, level);
					} else {
						a.setWeightInL(a.getDeadlines()[level], level);
					}
				} else {
					a.setWeightInL(a.getDeadlines()[level], level);
				}	
			}
		}
	}
	
	
	/**
	 * Builds the table in HI modes
	 * @param level
	 * @throws SchedulingException
	 */
	private void buildHITable (final int level) throws SchedulingException {
		List<VertexScheduling> ready = new LinkedList<VertexScheduling>();
		List<VertexScheduling> scheduled = new LinkedList<VertexScheduling>();
		
		// Add all sink nodes
		for (McDAG d : getMcDags()) {
			for (Vertex a : d.getVertices()) {
				if (a.isSinkinL(level))
					ready.add((VertexScheduling) a);
			}
		}
		
		calcPriorities(ready, 0, level);
		Collections.sort(ready, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling o1, VertexScheduling o2) {
				if (o1.getWeights()[level] - o2.getWeights()[level] != 0)
					return o1.getWeights()[level] - o2.getWeights()[level];
				else
					return o1.getId() - o2.getId();
			}
		});
		
		// Allocate slot by slot
		ListIterator<VertexScheduling> lit = ready.listIterator();
		boolean taskFinished = false;
		
		for (int s = hPeriod - 1; s >= 0; s--) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildHITable("+level+"): @t = "+s+", tasks activated: ");
				for (VertexScheduling a : ready)
					System.out.print("L("+a.getName()+") = "+a.getWeights()[level]+"; ");
				System.out.println("");
			}
			
			// Check if it's worth to continue the allocation
			
			for (int c = getNbCores() - 1; c >= 0; c--) {
				// Find a ready task
				if (lit.hasNext()) {
					VertexScheduling a = lit.next();
					
					int val = remainingTime[level][a.getGraphId()][a.getId()];
					sched[level][s][c] = a.getName();
					val--;
					
					// Task has been fully scheduled
					if (val == 0) {
						scheduled.add(a);
						taskFinished = true;
						lit.remove();
					}
					remainingTime[level][a.getGraphId()][a.getId()] = val;
				}
			}
			
			// A task finished its execution -> new tasks can be activated
			if (taskFinished)
				checkActivationHI(scheduled, ready, level);
			
			if (s != 0) {
				checkDAGActivation(scheduled, ready, s, level);
				calcPriorities(ready, gethPeriod() - s, level);
			}
			
			
			Collections.sort(ready, new Comparator<VertexScheduling>() {
				@Override
				public int compare(VertexScheduling o1, VertexScheduling o2) {
					if (o1.getWeights()[level] - o2.getWeights()[level] != 0)
						return o1.getWeights()[level] - o2.getWeights()[level];
					else
						return o1.getId() - o2.getId();
				}
			});
			
			taskFinished = false;
			lit = ready.listIterator();
		}
		if (!ready.isEmpty()) {
			SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] buildHITable("+level+"): Ready list not empty.");
			throw se;
		}
	}
	
	private void buildLOTable() throws SchedulingException {
		List<VertexScheduling> ready = new LinkedList<>();
		List<VertexScheduling> scheduled = new LinkedList<>();
		
		// Add all source nodes
		for (McDAG d : getMcDags()) {
			for (Vertex a : d.getVertices()) {
				if (a.isSourceinL(0))
					ready.add((VertexScheduling) a);
			}
		}
		
		calcPriorities(ready, 0, 0);
		Collections.sort(ready, loComp);
		
		// Allocate slot by slot
		ListIterator<VertexScheduling> lit = ready.listIterator();
		boolean taskFinished = false;
		
		for (int s = 0; s < gethPeriod(); s++) {
			if (isDebug()) {
				System.out.print("[DEBUG "+Thread.currentThread().getName()+"] buildLOTable(0): @t = "+s+", tasks activated: ");
				for (VertexScheduling a : ready)
					System.out.print("L("+a.getName()+") = "+a.getWeights()[0]+"; ");
				System.out.println("");
			}
			
			// Verify that is still worth to compute the sched table
			
			for (int c = 0; c < getNbCores(); c++) {
				// Get the next elt on the LO list
				if (lit.hasNext()) {
					VertexScheduling a = lit.next();
					int val = remainingTime[0][a.getGraphId()][a.getId()];
					
					sched[0][s][c] = a.getName();
					val--;
					
					if (val == 0) {
						scheduled.add(a);
						taskFinished = true;
						lit.remove();
					}
					remainingTime[0][a.getGraphId()][a.getId()] = val;
				}
			}
			
			if (taskFinished)
				checkActivationLO(scheduled, ready);
			
			if (s != hPeriod - 1) {
				checkDAGActivation(scheduled, ready, s + 1, 0);
				calcPriorities(ready, s + 1, 0);

			}
			
			Collections.sort(ready, loComp);
			taskFinished = false;
			lit = ready.listIterator();
		}
		if (!ready.isEmpty()) {
			SchedulingException se = new SchedulingException("[ERROR "+Thread.currentThread().getName()+"] buildLOlevel(0): Ready list not empty.");
			throw se;
		}
	}

	@Override
	public void buildAllTables() throws SchedulingException {
		initTables();
		initRemainingTimes();
		
		// Calculate deadlines for all DAGs
		for (McDAG d : getMcDags()) {
			calcDeadlines(d, getLevels());
			if (isDebug()) printDeadlines(d);
		}
		
		// Build HI tables first
		for (int i = getLevels() - 1; i >= 1; i--)
			buildHITable(i);
		
		// Build LO table
		buildLOTable();
		
		if (isDebug()) printTables();
		
		// Count preemptions
		for (McDAG d : getMcDags()) {
			for (Vertex a : d.getVertices()) {
				preempts.put((VertexScheduling) a, 0);
			}
		}
		
		Counters.countPreemptions(sched, preempts, 2, gethPeriod(), getNbCores());
	}
	
	/*
	 * DEBUG FUNCTIONS
	 */
	
	private void printDeadlines (McDAG d) {
		System.out.println("[DEBUG "+Thread.currentThread().getName()+"] DAG "+d.getId()+" printing LFTs");
		
		for (Vertex a : d.getVertices()) {
			System.out.print("[DEBUG "+Thread.currentThread().getName()+"]\t Actor "+a.getName()+", ");
			for (int i = 0; i < getLevels(); i++) {
				if (((VertexScheduling)a).getDeadlines()[i] != Integer.MAX_VALUE)
					System.out.print(((VertexScheduling)a).getDeadlines()[i]);
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

	public Comparator<VertexScheduling> getLoComp() {
		return loComp;
	}

	public void setLoComp(Comparator<VertexScheduling> loComp) {
		this.loComp = loComp;
	}

	public int getActivations() {
		return activations;
	}

	public void setActivations(int activations) {
		this.activations = activations;
	}

	public Hashtable<VertexScheduling, Integer> getPreempts() {
		return preempts;
	}

	public void setPreempts(Hashtable<VertexScheduling, Integer> preempts) {
		this.preempts = preempts;
	}

}
