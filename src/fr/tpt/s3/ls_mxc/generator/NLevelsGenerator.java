package fr.tpt.s3.ls_mxc.generator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.ActorSched;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.util.RandomNumberGenerator;

public class NLevelsGenerator {

	// Set of generated graphs
	private Set<DAG> gennedDAGs;
	private int nbDAGs;
	
	// Parameters for the generation
	private double edgeProb;
	private double userMinU;
	private double userMaxU;
	private int nbLevels;
	private int parallelismDegree;
	
	// Utilities
	private RandomNumberGenerator rng;
	private boolean debug;
	
	private int possibleDeadlines[] = {40, 20, 30, 50}; 
	
	public NLevelsGenerator (double minU, double maxU, double eProb, int levels, int paraDegree, int nbDAGs,
			boolean debug) {
		setUserMinU(minU);
		setUserMaxU(maxU);
		setEdgeProb(eProb);
		setNbLevels(levels);
		setParallelismDegree(paraDegree);
		setNbDAGs(nbDAGs);
		setDebug(debug);
		rng = new RandomNumberGenerator();
		gennedDAGs = new HashSet<>();
	}
	
	/**
	 * Function that prints the current parameters of the node
	 * @param a
	 */
	private void debugNode (Actor a, String func) {
		
		System.out.print("[DEBUG "+Thread.currentThread().getName()+"] "+func+": Node "+a.getId());
		for (int i = nbLevels - 1; i >= 0; i--)
			System.out.print(" C("+i+") = "+a.getCI(i)+";");
		System.out.println("");
		for (Edge e : a.getRcvEdges())
			System.out.println("\t Rcv Edge "+e.getSrc().getId()+" -> "+a.getId());
		for (Edge e : a.getSndEdges())
			System.out.println("\t Snd Edge "+a.getId()+" -> "+e.getDest().getId());
	}
	
	/**
	 * Method that generates a random graph
	 */
	public void GenerateGraph() {
		int id = 0;
		DAG d = new DAG();
		Set<Actor> nodes = new HashSet<Actor>();
		int rank;
		
		int idxDeadline = rng.randomUnifInt(0, possibleDeadlines.length - 1);
		int rDead = possibleDeadlines[idxDeadline];
		
		double rU[] = new double[nbLevels];
		int budgets[] = new int[nbLevels];
		int cBounds[] = new int[nbLevels];
			
		for (int i = 0; i < nbLevels; i++) {
			if (i != 0)
				rU[i] = rng.randomUnifDouble(rU[i - 1], rU[0] + ((userMaxU - rU[0]) / nbLevels) * (i+1));
			else 
				rU[i] = rng.randomUnifDouble(userMinU, userMaxU);
			budgets[i] = (int) Math.ceil(rDead * rU[i]);
			cBounds[i] = (int) Math.ceil(rDead / 4); 
		}
		
		if (isDebug()) {
			System.out.print("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph: Generating a graph with parameters ");
			for (int i = 0; i < nbLevels; i++)
				System.out.print("U["+i+"] = "+rU[i]+"; ");
			System.out.println("deadline = "+rDead);
		}
		
		// Generate nodes for all levels
		for (int i = nbLevels - 1; i >= 0; i--) {
			
			// Node generation block
			rank = 0;
			
			while (budgets[i] > 0) {
				int nodesPerRank = rng.randomUnifInt(1, parallelismDegree);
				
				for (int j = 0; j < nodesPerRank && budgets[i] > 0; j++) {
					ActorSched n = new ActorSched(id, Integer.toString(id), nbLevels);
					
					// Roll the Ci
					n.getcIs()[i] = rng.randomUnifInt(1, cBounds[i]);
					if (budgets[i] - n.getCI(i) > 0) {
						budgets[i] -= n.getCI(i);
					} else {
						n.getcIs()[i] = budgets[i];
						budgets[i] = 0;
					}
					
					n.setRank(rank);
					// Not a source node
					if (rank != 0) {
						// Iterate through the nodes to create an edge
						Iterator<Actor> it_n = nodes.iterator();
						while (it_n.hasNext()) {
							ActorSched src = (ActorSched) it_n.next();
							
							/* Roll a probability of having an edge between nodes
							 * Make sure that the deadline is not reached
							 */
							if (rng.randomUnifDouble(0, 100) <= edgeProb && n.getRank() > src.getRank()
								&& src.getCpFromNode()[i] + n.getCI(i) <= rDead) {
								@SuppressWarnings("unused")
								Edge e = new Edge(src,n);
							}
						}
					}
					// Set the Ci for inferior levels
					if (i >= 1)
						n.getcIs()[i - 1] = n.getCI(i);
					nodes.add(n);
					n.CPfromNode(i);
					id++;
					if (isDebug())
						debugNode(n, "GenerateGraph()");
				}
				rank++;
			}
			
			// Shrinking procedure only for HI tasks
			if (i >= 1) {
				System.out.println("Shrinking for mode "+i);

				double minU = rU[i - 1] / 2;
				int wantedBudget = (int) Math.ceil(minU * rDead);
				int actualBudget = (int) Math.ceil(rU[i] * rDead);
				Iterator<Actor> it_n;
				
				while (wantedBudget < actualBudget && !allNodesAreMin(nodes, i)) {
					it_n = nodes.iterator();
					while (it_n.hasNext()) {
						ActorSched n = (ActorSched) it_n.next();
						
						n.getcIs()[i - 1] = rng.randomUnifInt(1, n.getCI(i));
						actualBudget -= n.getCI(i - 1);
						if (actualBudget < 0)
							actualBudget = 0;
					}
				}
				
				if (isDebug()) {
					System.out.println("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph(): >>> Deflation of tasks in mode "+i+" finished");
					for (Actor a : nodes) 
						debugNode(a, "GenerateGraph()");
				}
				
				it_n = nodes.iterator();
				actualBudget = 0;
				while (it_n.hasNext()) {
					Actor a = it_n.next();
					a.CPfromNode(i - 1);
					actualBudget += a.getCI(i - 1);
				}
				// Update remaining budgets
				budgets[i - 1] -= actualBudget;				
			}
		}
		
		d.setNodes(nodes);
		d.setDeadline(rDead);
		d.setId(getGennedDAGs().size());
		getGennedDAGs().add(d);
		
		if (isDebug())
			System.out.println("[DEBUG"+Thread.currentThread().getName()+"] GenerateGraph(): DAG generation completed");
	}
	
	/**
	 * Verifies if all the nodes in the set have the minimum execution time
	 * @param nodes
	 * @param level
	 * @return
	 */
	private boolean allNodesAreMin(Set<Actor> nodes, int level) {
		for (Actor a : nodes) {
			if (a.getCI(level) != 1)
				return false;
		}
		return true;
	}
	
	
	/*
	 * Getters and setters 
	 */

	public Set<DAG> getGennedDAGs() {
		return gennedDAGs;
	}

	public void setGennedDAGs(Set<DAG> gennedDAGs) {
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

	public double getUserMinU() {
		return userMinU;
	}

	public void setUserMinU(double userMinU) {
		this.userMinU = userMinU;
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
}
