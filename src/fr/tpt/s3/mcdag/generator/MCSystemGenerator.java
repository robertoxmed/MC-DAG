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
package fr.tpt.s3.mcdag.generator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.util.RandomNumberGenerator;

public class MCSystemGenerator {

	// Set of generated graphs
	protected Set<McDAG> gennedDAGs;
	protected int nbDAGs;
	
	// Parameters for the generation
	protected double edgeProb;
	protected double userMaxU;
	protected int nbLevels;
	protected int parallelismDegree;
	protected double rfactor;
	protected int nbTasks;
	
	// Utilities
	protected RandomNumberGenerator rng;
	protected boolean debug;
	
	protected int possibleDeadlines[] = {100, 120, 150, 180, 200, 220, 250, 300, 400, 500}; 
	
	public MCSystemGenerator (double maxU, int nbTasks,
			double eProb, int levels, int paraDegree, int nbDAGs,
			double rfactor, boolean debug) {
		setUserMaxU(maxU);
		setNbTasks(nbTasks);
		setEdgeProb(eProb);
		setNbLevels(levels);
		setParallelismDegree(paraDegree);
		setNbDAGs(nbDAGs);
		setDebug(debug);
		setRfactor(rfactor);
		rng = new RandomNumberGenerator();
		gennedDAGs = new HashSet<>();
	}
	
	/**
	 * Function that prints the current parameters of the node
	 * @param a
	 */
	protected void debugNode (Vertex a, String func) {
		
		System.out.print("[DEBUG "+Thread.currentThread().getName()+"] "+func+": Node "+a.getId());
		for (int i = nbLevels - 1; i >= 0; i--)
			System.out.print(" C("+i+") = "+a.getWcet(i)+";");
		System.out.println(" Rank "+ ((VertexScheduling) a).getRank());
		for (Edge e : a.getRcvEdges())
			System.out.println("\t Rcv Edge "+e.getSrc().getId()+" -> "+a.getId());
		for (Edge e : a.getSndEdges())
			System.out.println("\t Snd Edge "+a.getId()+" -> "+e.getDest().getId());
	}
	
	/**
	 * UUnifast implementation
	 * @param uSet
	 * @param u
	 */
	protected void uunifast (double uSet[], double u) {
		
		double sum = u;
		double nextSum;
		
		for (int i = 0; i < uSet.length; i++) {
			nextSum = sum * Math.pow(rng.randomUnifDouble(0.0, 1.0), 1.0 / (uSet.length - (i + 1)));
			uSet[i] = sum - nextSum;
			sum = nextSum;
		}
	}
	
	/**
	
	/**
	 * UunifastDiscard implementation
	 * @param uSet
	 * @param u
	 * @return
	 */
	private boolean uunifastDiscard (double uSet[], double u) {
		
		double sum = u;
		double nextSum;
					
		for (int i = 0; i < uSet.length; i++) {
			nextSum = sum * Math.pow(rng.randomUnifDouble(0.0, 1.0), 1.0 / (uSet.length - i));
			uSet[i] = sum - nextSum;
			sum = nextSum;
		}
		
		for (int i = 0; i < uSet.length; i++) {
			if (uSet[i] > 1)
				return false;
		}
		
		return true;
	}
	
	/**
	 * Debug function for utilization given to tasks
	 * @param uSet
	 */
	private void printUset (double uSet[]) {
		System.out.print("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph():");
		for (int i = 0; i < uSet.length; i++)
			System.out.print(" U_Task["+ i + "] = " + uSet[i]);
		System.out.println("");
	}
	
	/**
	 * Method that resets Ranks on nodes that have no edges
	 * -> They become source edges
	 * @param level
	 */
	protected void resetRanks (Set<Vertex> nodes, int level) {
		for (Vertex a : nodes) {
			if (a.getSndEdges().size() == 0 && a.getRcvEdges().size() == 0)
				((VertexScheduling) a).setRank(0);
		}
	}
	
	/**
	 * Method that generates a random graph
	 */
	protected void GenerateGraph(double utilization) {
		int id = 0;
		McDAG d = new McDAG();
		Set<Vertex> nodes = new HashSet<Vertex>();
		int rank;
		int prevRank;
		
		int idxDeadline = rng.randomUnifInt(0, possibleDeadlines.length - 1);
		int rDead = possibleDeadlines[idxDeadline];
		
		/* Init phase:
		 * 	Utilization per mode + budgets per mode
		 *  Deadline given to graph
		 */
		double rU[] = new double[nbLevels];
		int budgets[] = new int[nbLevels];
		int cBounds[] = new int[nbLevels];
		int tasks[] = new int[nbLevels];
		
		/* Random distribution of utilization between the bounds
		 */

		for (int i = 0; i < nbLevels; i++) {
			tasks[i] = (int) (nbTasks / nbLevels);
			rU[i] = utilization;
			budgets[i] = (int) Math.ceil(rDead * rU[i]);
			cBounds[i] = (int) Math.ceil(rDead); 
		}
		
		if (isDebug()) {
			System.out.print("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph: Generating a graph with parameters ");
			for (int i = 0; i < nbLevels; i++)
				System.out.print("U["+i+"] = "+rU[i]+"; ");
			System.out.println("deadline = "+rDead);
			System.out.print("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph: Number of tasks per mode");
			for (int i = 0; i < nbLevels; i++)
				System.out.print("NbTasks["+i+"] = "+tasks[i]+"; ");
			System.out.println(" buget "+budgets[1]);
		}
		
		// Generate nodes for all levels
		prevRank = 0;
		for (int i = nbLevels - 1; i >= 0; i--) {
			
			// Node generation block
			rank = rng.randomUnifInt(0, prevRank);
			
			// Number of tasks to generate in mode i
			int tasksToGen = tasks[i];
			double uSet[] = new double[tasksToGen];
			double uInMode = (double) budgets[i] / rDead;
			
			while (!uunifastDiscard(uSet, uInMode)) {
				if (isDebug()) {
					System.out.println("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph: Running uunifastDiscard for mode "+i);
				}
			}
			
			if (isDebug()) printUset(uSet);
			
			while (tasksToGen > 0) {
				int nodesPerRank = rng.randomUnifInt(1, parallelismDegree);
				
				for (int j = 0; j < nodesPerRank || budgets[i] < 0; j++) {
					VertexScheduling n = new VertexScheduling(id, Integer.toString(id), nbLevels);
					
					// Transform uSet to budget
					if ((tasks[i] - tasksToGen) < tasks[i])
						n.getWcets()[i] = (int) Math.ceil((rDead * uSet[tasks[i] - tasksToGen]));
					else
						n.getWcets()[i] = budgets[i];
		
					if (budgets[i] - n.getWcet(i) > 0) {
						budgets[i] -= n.getWcet(i);
					} else {
						n.getWcets()[i] = budgets[i];
						budgets[i] = 0;
					}
					
					n.setRank(rank);
					// Not a source node
					if (rank != 0) {
						// Iterate through the nodes to create an edge
						Iterator<Vertex> it_n = nodes.iterator();
						while (it_n.hasNext()) {
							VertexScheduling src = (VertexScheduling) it_n.next();
							
							/* Roll a probability of having an edge between nodes
							 * Make sure that the deadline is not reached
							 */
							if (rng.randomUnifDouble(0, 100) <= edgeProb
								&& n.getRank() > src.getRank()
								&& src.getCpFromNode()[i] + n.getWcet(i) <= rDead
								){
								@SuppressWarnings("unused")
								Edge e = new Edge(src,n);
								
								/* Once the edge is added the critical path needs to
								 * be updated
								 */
								src.CPfromNode(i);
							}
						}
					}
					// Set the Ci for inferior levels
					if (i >= 1) {
						for (int x = i - 1; x >= 0; x--)
							n.getWcets()[x] = n.getWcet(i);
					}
					nodes.add(n);
					tasksToGen--;
					n.CPfromNode(i);
					id++;
					if (isDebug())
						debugNode(n, "GenerateGraph()");
				}
				rank++;
			}
			
			prevRank = rank;
			// Shrinking procedure only for HI tasks
			if (i >= 1) {
				// Shrinking depends on the reduction factor
				double minU = rU[i - 1] / getRfactor();
				int wantedBudget = (int) Math.ceil(minU * rDead);
				int actualBudget = 0;
				
				for (Vertex v : nodes)
					actualBudget += ((VertexScheduling) v).getWcet(i);
				
				while (wantedBudget < actualBudget && !allNodesAreMin(nodes, i)) {
					int idx = getRng().randomUnifInt(0, nodes.size() - 1);
					VertexScheduling n = (VertexScheduling) randomObjectIdxSet(nodes, idx);
						
					n.getWcets()[i - 1] = rng.randomUnifInt(1, n.getWcet(i));
					for (int x = i - 1; x >= 0; x--)
						n.getWcets()[x] = n.getWcets()[i - 1];
								
					actualBudget -= n.getWcet(i - 1);
					if (actualBudget < 0)
						actualBudget = 0;
				}
				
				if (isDebug()) {
					System.out.println("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph(): >>> Deflation of tasks in mode "+i+" finished");
					for (Vertex a : nodes) 
						debugNode(a, "GenerateGraph()");
				}
				
				Iterator<Vertex> it_n = nodes.iterator();
				actualBudget = 0;
				while (it_n.hasNext()) {
					Vertex a = it_n.next();
					a.CPfromNode(i - 1);
					actualBudget += a.getWcet(i - 1);
				}
				// Update remaining budgets
				budgets[i - 1] -= actualBudget;				
			}
			// Nodes that have no edges become source nodes
			// their rank is reset
			resetRanks(nodes, i);
		}
		
		d.setNodes(nodes);
		d.setDeadline(rDead);
		d.setLevels(nbLevels);
		d.setId(getGennedDAGs().size());
		getGennedDAGs().add(d);
		
		if (isDebug())
			System.out.println("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph(): DAG generation completed");
	}
	
	
	// Iterate until getting a random object from set
	protected Object randomObjectIdxSet (Set<Vertex> theSet, int idx) {
		int i = 0;
		
		for (Object o : theSet) {
			if (i == idx)
				return o;
			i++;
		}
		return null;
	}
	
	/**
	 * Verifies if all the nodes in the set have the minimum execution time
	 * @param nodes
	 * @param level
	 * @return
	 */
	protected boolean allNodesAreMin(Set<Vertex> nodes, int level) {
		for (Vertex a : nodes) {
			if (a.getWcet(level) != 1)
				return false;
		}
		return true;
	}
	
	private boolean thresholdUtilization () {
		double uSys = 0;
		
		for (McDAG d : getGennedDAGs()) {
			uSys += d.getUmax();
		}		
		System.out.println(">>>>>>>>>>>>>>> U generated " + uSys);
		if (userMaxU * 0.99 <= uSys && uSys <= userMaxU * 1.01)
			return true;
		
		return false;
	}
	
	/**
	 * Method that generates all DAGs in the system
	 * the utilization for the system is uniformly distributed between
	 * the set of DAGs
	 * @return
	 */
	protected void genAllDags () {
		boolean done = false;
		
		while (!done) {
			// 	Apply Uunifast on the utilization for the DAGs
			double[] uSet =  new double[getNbDAGs()];
			uunifast(uSet, getUserMaxU());
		
			// Call genDAG with the utilization found
			for (int i = 0; i < getNbDAGs(); i++) {
				if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] Generating DAG #"+(i+1)+" of "+getNbDAGs());
				GenerateGraph(uSet[i]);
			}
			removeNilNodes();

			done = thresholdUtilization();
			
			if (!done) {
				
				getGennedDAGs().clear();
			}
		}
		
	}
	
	/**
	 * Tests if the wcet are null for a node
	 * @param a
	 * @return
	 */
	private boolean nilValuesInAllModes (Vertex a) {
		
		for (int i = 0; i < nbLevels; i++) {
			if (a.getWcet(i) != 0)
				return false;
		}
		return true;
	}
	
	
	private void removeNilNodes () {
		for (McDAG d : gennedDAGs) {
			Iterator<Vertex> ita = d.getVertices().iterator();
			while (ita.hasNext()) {
				Vertex a = ita.next();
				if (nilValuesInAllModes(a)) {
					for (Edge e : a.getRcvEdges())
						e.getSrc().getSndEdges().remove(e);
					for (Edge e : a.getSndEdges())
						e.getDest().getRcvEdges().remove(e);
						
					a.getRcvEdges().clear();
					a.getSndEdges().clear();
					ita.remove();
				}
			}
		}
	}
	
	/**
	 * Function to randomly generate a vector of integers that add to a given sum
	 * Placeholder until randFixedSum is implemented in Java
	 * @param sum
	 * @param n
	 * @return
	 */
	protected int[] randIntSum (int sum, int n) {
		int vals[] = new int[n];
		
		for (int i = 0; i < n - 1; ++i)
			vals[i] = rng.randomUnifInt(0, sum); 
		
		vals[n - 1] = sum;
		
		Arrays.sort(vals);
		for (int i = n - 1; i > 0; --i)
			vals[i] -= vals[i - 1];
			
		for (int i = 0; i < n; ++i)
			++vals[i];
			
		return vals;
	}
	
	/*
	 * Getters and setters 
	 */

	public Set<McDAG> getGennedDAGs() {
		return gennedDAGs;
	}

	public void setGennedDAGs(Set<McDAG> gennedDAGs) {
		this.gennedDAGs = gennedDAGs;
	}

	public double getEdgeProb() {
		return edgeProb;
	}

	public void setEdgeProb(double edgeProb) {
		this.edgeProb = edgeProb;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getParallelismDegree() {
		return parallelismDegree;
	}

	public void setParallelismDegree(int parallelismDegree) {
		this.parallelismDegree = parallelismDegree;
	}

	public double getUserMaxU() {
		return userMaxU;
	}

	public void setUserMaxU(double userMaxU) {
		this.userMaxU = userMaxU;
	}

	public RandomNumberGenerator getRng() {
		return rng;
	}

	public void setRng(RandomNumberGenerator rng) {
		this.rng = rng;
	}
	
	public int getNbLevels() {
		return nbLevels;
	}

	public void setNbLevels(int nbLevels) {
		this.nbLevels = nbLevels;
	}

	public int getNbDAGs() {
		return nbDAGs;
	}

	public void setNbDAGs(int nbDAGs) {
		this.nbDAGs = nbDAGs;
	}

	public double getRfactor() {
		return rfactor;
	}

	public void setRfactor(double rfactor) {
		this.rfactor = rfactor;
	}

	public int getNbTasks() {
		return nbTasks;
	}

	public void setNbTasks(int nbTasks) {
		this.nbTasks = nbTasks;
	}
}
