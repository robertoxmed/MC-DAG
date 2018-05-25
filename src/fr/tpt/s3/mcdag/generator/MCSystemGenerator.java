package fr.tpt.s3.mcdag.generator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Actor;
import fr.tpt.s3.mcdag.model.ActorSched;
import fr.tpt.s3.mcdag.model.DAG;
import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.util.RandomNumberGenerator;

public class MCSystemGenerator {

	// Set of generated graphs
	private Set<DAG> gennedDAGs;
	private int nbDAGs;
	
	// Parameters for the generation
	private double edgeProb;
	private double userMaxU;
	private int nbLevels;
	private int parallelismDegree;
	private int rfactor;
	private int nbTasks;
	
	// Utilities
	private RandomNumberGenerator rng;
	private boolean debug;
	
	private int possibleDeadlines[] = {30, 50, 60, 80, 100}; 
	
	public MCSystemGenerator (double maxU, int nbTasks,
			double eProb, int levels, int paraDegree, int nbDAGs,
			int rfactor, boolean debug) {
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
	private void debugNode (Actor a, String func) {
		
		System.out.print("[DEBUG "+Thread.currentThread().getName()+"] "+func+": Node "+a.getId());
		for (int i = nbLevels - 1; i >= 0; i--)
			System.out.print(" C("+i+") = "+a.getCI(i)+";");
		System.out.println(" Rank "+ ((ActorSched) a).getRank());
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
	private void uunifast (double uSet[], double u) {
		
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
	private void resetRanks (Set<Actor> nodes, int level) {
		for (Actor a : nodes) {
			if (a.getSndEdges().size() == 0 && a.getRcvEdges().size() == 0)
				((ActorSched) a).setRank(0);
		}
	}
	
	/**
	 * Method that generates a random graph
	 */
	protected void GenerateGraph(double utilization) {
		int id = 0;
		DAG d = new DAG();
		Set<Actor> nodes = new HashSet<Actor>();
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
			
			while (budgets[i] > 0 && tasksToGen > 0) {
				int nodesPerRank = rng.randomUnifInt(1, parallelismDegree);
				
				for (int j = 0; j < nodesPerRank || budgets[i] < 0; j++) {
					ActorSched n = new ActorSched(id, Integer.toString(id), nbLevels);
					
					// Transform uSet to budget
					if ((tasks[i] - tasksToGen) < tasks[i])
						n.getcIs()[i] = (int) Math.ceil((rDead * uSet[tasks[i] - tasksToGen]));
					else
						n.getcIs()[i] = budgets[i];
		
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
							if (rng.randomUnifDouble(0, 100) <= edgeProb
								&& n.getRank() > src.getRank()
								&& src.getCpFromNode()[i] + n.getCI(i) <= rDead) {
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
					if (i >= 1)
						n.getcIs()[i - 1] = n.getCI(i);
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
			// Nodes that have no edges become source nodes
			// their rank is reset
			resetRanks(nodes, i);
		}
		
		d.setNodes(nodes);
		d.setDeadline(rDead);
		d.setId(getGennedDAGs().size());
		getGennedDAGs().add(d);
		
		if (isDebug())
			System.out.println("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph(): DAG generation completed");
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
	
	/**
	 * Method that generates all DAGs in the system
	 * the utilization for the system is uniformly distributed between
	 * the set of DAGs
	 * @return
	 */
	protected void genAllDags () {
		
		// Apply Uunifast on the utilization for the DAGs
		double[] uSet =  new double[getNbDAGs()];
		uunifast(uSet, getUserMaxU());
		
		// Call genDAG with the utilization found
		for (int i = 0; i < getNbDAGs(); i++) {
			if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] Generating DAG #"+(i+1)+" of "+getNbDAGs());
			GenerateGraph(uSet[i]);
		}
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

	public int getRfactor() {
		return rfactor;
	}

	public void setRfactor(int rfactor) {
		this.rfactor = rfactor;
	}

	public int getNbTasks() {
		return nbTasks;
	}

	public void setNbTasks(int nbTasks) {
		this.nbTasks = nbTasks;
	}
}
