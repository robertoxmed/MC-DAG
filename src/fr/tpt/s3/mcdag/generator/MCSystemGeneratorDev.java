package fr.tpt.s3.mcdag.generator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import fr.tpt.s3.mcdag.model.Edge;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;

public class MCSystemGeneratorDev extends MCSystemGenerator{
 
		
		public MCSystemGeneratorDev (double maxU, int nbTasks,
				double eProb, int levels, int paraDegree, int nbDAGs,
				double rfactor, boolean debug) {
			super(maxU, nbTasks, eProb, levels, paraDegree, nbDAGs, rfactor, debug);
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
				
				// Uniform distribution for integer timing budgets
				int[] taskBudets = randIntSum(budgets[i], tasksToGen);
				
				
				while (tasksToGen > 0) {
					int nodesPerRank = rng.randomUnifInt(1, parallelismDegree);
					System.out.println("[DEBUG "+Thread.currentThread().getName()+"] GenerateGraph(): Nodes per rank "+nodesPerRank + " tasks to gen "+tasksToGen);

					
					for (int j = 0; j < nodesPerRank && tasksToGen > 0; j++) {
						VertexScheduling n = new VertexScheduling(id, Integer.toString(id), nbLevels);
						
						// Transform uSet to budget
						n.getWcets()[i] = taskBudets[tasksToGen - 1];						
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
		
}
