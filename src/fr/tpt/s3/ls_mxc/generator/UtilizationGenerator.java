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
package fr.tpt.s3.ls_mxc.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.model.Actor;

public class UtilizationGenerator {
	
	private double userU_LO;
	private double userU_HI;
	private int userCp;
	private int nbNodes;
	private int nbCores;
	private DAG genDAG;
	private int edgeProb;
	private double uHIinLO;
	private int paraDegree;
	private boolean HtoL;
	private RandomNumberGenerator rng;

	private int deadline;
	
	private int[][] adjMatrix;

	public UtilizationGenerator (double U_LO, double U_HI, int cp, int edgeProb, double UHIinLO, int para, int cores) {
		this.setUserU_LO(U_LO);
		this.setUserU_HI(U_HI);
		this.setUserCp(cp);
		this.setEdgeProb(edgeProb);
		this.setuHIinLO(UHIinLO);
		this.setParaDegree(para);
		this.setNbCores(cores);
		this.rng = new RandomNumberGenerator();
	}

	
	/**
	 * Generates a DAG
	 */
	public void GenenrateGraphCp() {
		// Variables
		int id = 0;
		setGenDAG(new DAG());
		Set<Actor> nodes = new HashSet<Actor>();
		boolean cpReached = false;
		int rank = 0;		
		// Budgets deduced by utilization and CP
		int budgetHI = (int) Math.ceil(userCp * userU_HI);
		int budgetLO = (int) Math.ceil(userCp * userU_LO);		
		int CHIBound = (int) Math.ceil(userCp / 2);
		int CLOBound = (int) Math.ceil(userCp / 2);
		
		// Generate the CP in HI mode
		Actor last = null;
		int toCP = userCp;
		while (!cpReached) {
			Actor n = new Actor(id, Integer.toString(id), 0, 0);
			n.setRank(rank);
			
			n.setC_HI(rng.randomUnifInt(1,CHIBound) + 2);
			
			
			// Add egde and update the CP (if not source)
			if (id != 0) {
				Edge e = new Edge(last, n);
				last.getSnd_edges().add(e);
				n.getRcv_edges().add(e);
			}
			
			nodes.add(n);
			n.CPfromNode(1);
			last = n;
			rank++;
			id++;
			
			if (toCP - n.getC_HI() > 0) {
				toCP = toCP  - n.getC_HI();
				budgetHI = budgetHI - n.getC_HI();
			} else {
				n.setC_HI(toCP);
				budgetHI = budgetHI - n.getC_HI();
				cpReached = true;
			}
			n.setC_LO(n.getC_HI());
		}		
		// Generate the other HI nodes and the arcs
		rank = 0;		
		while (budgetHI > 0) {
			// Roll a number of nodes to add to the level
			int nodesPerRank = rng.randomUnifInt(1, paraDegree);
			for (int j=0; j < nodesPerRank && budgetHI > 0; j++) {
				Actor n = new Actor(id, Integer.toString(id), 0, 0);
			
				// Roll a C_HI and test if budget is left
				n.setC_HI(rng.randomUnifInt(1, CHIBound) + 2);
				if (budgetHI - n.getC_HI() > 0) {
					budgetHI = budgetHI - n.getC_HI();
				} else {
					n.setC_HI(budgetHI);
					budgetHI = 0;
				}
				
				n.setRank(rank);
				if (rank != 0) {
					Iterator<Actor> it_n = nodes.iterator();
					while (it_n.hasNext()) {
						Actor src = it_n.next();
						// Test if the rank of the source is lower and if the CP
						// is not reached
						if (rng.randomUnifInt(1, 100) <= edgeProb && n.getRank() > src.getRank()
								&& src.getCpFromNode_HI() + n.getC_HI() <= userCp) {
							Edge e = new Edge(src, n);
							src.getSnd_edges().add(e);
							n.getRcv_edges().add(e);
						}
					}
				}
				n.setC_LO(n.getC_HI());
				nodes.add(n);
				n.CPfromNode(1);
				id++;
			}
			rank++;
		}
		
		// Deflate HI execution times
		int wantedHIinLO = (int) Math.ceil(uHIinLO * userCp);
		int actualBudget = (int) Math.ceil(userU_HI * userCp);
		Iterator<Actor> it_n;
		while (wantedHIinLO < actualBudget && !allHIareMin(nodes)) {
			it_n = nodes.iterator();
			while (it_n.hasNext()) {
				Actor n = it_n.next();
				
				n.setC_LO(rng.randomUnifInt(1, n.getC_LO()) + 1);				
				if (n.getC_LO() == 0)
					n.setC_LO(1);
				
				actualBudget = actualBudget - n.getC_LO();
				if (actualBudget < 0)
					actualBudget = 0;
			}
		}
		
		it_n = nodes.iterator();
		while (it_n.hasNext()){
			it_n.next().CPfromNode(0);
		}
				
		graphSanityCheck(1);
		
		// Add LO nodes
		actualBudget = 0;
		it_n = nodes.iterator();
		while (it_n.hasNext()) {
			actualBudget += it_n.next().getC_LO();
		}
			
		budgetLO = budgetLO - actualBudget;
					
		rank = 0;
		while (budgetLO > 0) {
			// Roll a number of nodes to add to the level
			int nodesPerRank = rng.randomUnifInt(1, ((int)(paraDegree / 2)));
			for (int j=0; j < nodesPerRank && budgetLO > 0; j++) {
				Actor n = new Actor(id, Integer.toString(id), 0, 0);
			
				// Roll a C_HI and test if budget is left
				n.setC_HI(0);
				n.setC_LO(rng.randomUnifInt(1, CLOBound) + 1);
				if (n.getC_LO() == 0)
					n.setC_LO(1); // Minimal execution time
				
				if (budgetLO - n.getC_LO() > 0) {
					budgetLO = budgetLO - n.getC_LO();
				} else {
					n.setC_LO(budgetLO);
					budgetLO = 0;
				}
				
				n.setRank(rank);
				if (rank != 0) {
					Iterator<Actor> it = nodes.iterator();
					while (it.hasNext()) {
						Actor src = it.next();
						// Test if the rank of the source is lower and if the CP
						// is not reached
						if (rng.randomUnifInt(1, 100) <= edgeProb && n.getRank() > src.getRank()
								&& src.getCpFromNode_LO() + n.getC_LO() <= userCp &&
								allowedCommunitcation(src, n)) {
							Edge e = new Edge(src, n);
							src.getSnd_edges().add(e);
							n.getRcv_edges().add(e);
						}
					}
				}
				nodes.add(n);
				n.CPfromNode(0);
				id++;
			}

			rank++;
		}
		
		// Have at least a 2 rank LO graph by adding one extra LO node
		if (rank == 1) {
			Actor n = new Actor(id, Integer.toString(id), 0, 0);
			n.setC_HI(0);
			n.setC_LO(rng.randomUnifInt(1, 1) + 1);
			
			Iterator<Actor> it = nodes.iterator();
			while (it.hasNext()) {
				Actor src = it.next();
				
				if (src.getC_HI() == 0) {
					Edge e = new Edge(src, n);
					src.getSnd_edges().add(e);
					n.getRcv_edges().add(e);
				} else if (rng.randomUnifInt(1, 100) <= edgeProb && n.getRank() > src.getRank()
						&& src.getCpFromNode_LO() + n.getC_LO() <= userCp &&
						allowedCommunitcation(src, n)) {
					Edge e = new Edge(src, n);
					src.getSnd_edges().add(e);
					n.getRcv_edges().add(e);
				}
			}
			nodes.add(n);
			n.CPfromNode(0);
			id++;
		}
		
		it_n = nodes.iterator();
		while (it_n.hasNext()){
			Actor n = it_n.next();
			n.checkifSink();
			n.checkifSource();
		}
		
		setNbNodes(id + 1);
		graphSanityCheck(0);
		genDAG.setNodes(nodes);
		setDeadline(genDAG.calcCriticalPath());
		createAdjMatrix();
	}
	
	
	/**
	 * Generates a DAG without making the CP in HI first
	 */
	public void GenenrateGraph() {
		// Variables
		int id = 0;
		setGenDAG(new DAG());
		Set<Actor> nodes = new HashSet<Actor>();
		boolean cpReached = false;
		int rank = 0;
		Random r = new Random(System.currentTimeMillis());
		
		// Budgets deduced by utilization and CP
		int budgetHI = (int) Math.ceil(userCp * userU_HI);
		int budgetLO = (int) Math.ceil(userCp * userU_LO);		
		int CHIBound = (int) Math.ceil(userCp / 2);
		int CLOBound = (int) Math.ceil(userCp / 2);
					
		// Generate HI nodes and the arcs
		// No hypothesis about the CP.
		rank = 0;		
		while (budgetHI > 0) {
			// Roll a number of nodes to add to the level
			int nodesPerRank = r.nextInt(paraDegree);
			for (int j=0; j < nodesPerRank && budgetHI > 0; j++) {
				Actor n = new Actor(id, Integer.toString(id), 0, 0);
			
				// Roll a C_HI and test if budget is left
				n.setC_HI(r.nextInt(CHIBound) + 2);
				if (budgetHI - n.getC_HI() > 0) {
					budgetHI = budgetHI - n.getC_HI();
				} else {
					n.setC_HI(budgetHI);
					budgetHI = 0;
				}
				
				n.setRank(rank);
				if (rank != 0) {
					Iterator<Actor> it_n = nodes.iterator();
					while (it_n.hasNext()) {
						Actor src = it_n.next();
						// Test if the rank of the source is lower and if the CP
						// is not reached
						if (r.nextInt(100) <= edgeProb && n.getRank() > src.getRank()
								&& src.getCpFromNode_HI() + n.getC_HI() <= userCp) {
							Edge e = new Edge(src, n);
							src.getSnd_edges().add(e);
							n.getRcv_edges().add(e);
							if (src.getCpFromNode_HI() + n.getC_HI() == userCp) {
								cpReached = true;
							}
						}
					}
				}
				n.setC_LO(n.getC_HI());
				nodes.add(n);
				n.CPfromNode(1);
				id++;
			}
			rank++;
		}
		
		// Deflate HI execution times
		int wantedHIinLO = (int) Math.ceil(uHIinLO * userCp);
		int actualBudget = (int) Math.ceil(userU_HI * userCp);
		Iterator<Actor> it_n;
		while (wantedHIinLO < actualBudget && !allHIareMin(nodes)) {
			it_n = nodes.iterator();
			while (it_n.hasNext()) {
				Actor n = it_n.next();
				
				n.setC_LO(r.nextInt(n.getC_LO()) + 1);				
				actualBudget = actualBudget - n.getC_LO();
				if (actualBudget < 0)
					actualBudget = 0;
			}
		}
		
		it_n = nodes.iterator();
		while (it_n.hasNext()){
			it_n.next().CPfromNode(0);
		}
				
		graphSanityCheck(1);
		
		// Add LO nodes
		actualBudget = 0;
		it_n = nodes.iterator();
		while (it_n.hasNext()) {
			actualBudget += it_n.next().getC_LO();
		}
			
		budgetLO = budgetLO - actualBudget;

		// Generate LO tasks
		rank = 0;
		while (budgetLO > 0) {
			// Roll a number of nodes to add to the level
			int nodesPerRank = r.nextInt((int)(paraDegree / 2));
			for (int j=0; j < nodesPerRank && budgetLO > 0; j++) {
				Actor n = new Actor(id, Integer.toString(id), 0, 0);
			
				// Roll a C_HI and test if budget is left
				n.setC_HI(0);
				n.setC_LO(r.nextInt(CLOBound) + 1);
				if (n.getC_LO() == 0)
					n.setC_LO(1); // Minimal execution time
				
				if (budgetLO - n.getC_LO() > 0) {
					budgetLO = budgetLO - n.getC_LO();
				} else {
					n.setC_LO(budgetLO);
					budgetLO = 0;
				}
				
				n.setRank(rank);
				if (rank != 0) {
					Iterator<Actor> it = nodes.iterator();
					while (it.hasNext()) {
						Actor src = it.next();
						// Test if the rank of the source is lower and if the CP
						// is not reached
						if (r.nextInt(100) <= edgeProb && n.getRank() > src.getRank()
								&& src.getCpFromNode_LO() + n.getC_LO() <= userCp &&
								allowedCommunitcation(src, n)) {
							Edge e = new Edge(src, n);
							src.getSnd_edges().add(e);
							n.getRcv_edges().add(e);
							if (src.getCpFromNode_HI() + n.getC_HI() == userCp) {
								cpReached = true;
							}
								
						}
					}
				}
				nodes.add(n);
				n.CPfromNode(0);
				id++;
			}

			rank++;
		}
		
		// Have at least a 2 rank LO graph by adding one extra LO node
		if (rank == 1) {
			Actor n = new Actor(id, Integer.toString(id), 0, 0);
			n.setC_HI(0);
			n.setC_LO(r.nextInt(1) + 1);
			
			Iterator<Actor> it = nodes.iterator();
			while (it.hasNext()) {
				Actor src = it.next();
				
				if (src.getC_HI() == 0) {
					Edge e = new Edge(src, n);
					src.getSnd_edges().add(e);
					n.getRcv_edges().add(e);
				} else if (r.nextInt(100) <= edgeProb && n.getRank() > src.getRank()
						&& src.getCpFromNode_LO() + n.getC_LO() <= userCp &&
						allowedCommunitcation(src, n)) {
					Edge e = new Edge(src, n);
					src.getSnd_edges().add(e);
					n.getRcv_edges().add(e);
				}
			}
			nodes.add(n);
			n.CPfromNode(0);
			id++;
		}
		

		// The Cp wasn't reached we need to add more tasks in LO mode
		if (!cpReached) {
			Actor n = new Actor(id, Integer.toString(id), 0, 0);
			
			// Get the closest node to the CP
			int max = 0;
			Actor node_max = null;
			Iterator<Actor> it = nodes.iterator();
			while (it.hasNext()) {
				Actor m = it.next();
				if (m.getCpFromNode_LO() > max) {
					max = m.getCpFromNode_LO();
					node_max = m;
				}
			}
			
			// Roll a C_HI and test if budget is left
			n.setC_HI(0);
			n.setC_LO(userCp - node_max.getCpFromNode_LO());
			Edge e = new Edge(node_max, n);
			node_max.getSnd_edges().add(e);
			n.getRcv_edges().add(e);
			
			n.CPfromNode(0);
			nodes.add(n);
			id++;
		}
		
		it_n = nodes.iterator();
		while (it_n.hasNext()){
			Actor n = it_n.next();
			n.checkifSink();
			n.checkifSource();
		}
		
		
		setNbNodes(id + 1);
		graphSanityCheck(0);
		genDAG.setNodes(nodes);
		setDeadline(genDAG.calcCriticalPath());
		createAdjMatrix();
	}
	
	/**
	 * Tests if all HI nodes are minimal execution <=> C LO = 1
	 * @param nodes
	 * @return
	 */
	public boolean allHIareMin (Set<Actor> nodes) {
		Iterator<Actor> it = nodes.iterator();
		while (it.hasNext()){
			if (it.next().getC_LO() != 1)
				return false;
		}
		return true;
	}
	
	/**
	 * Calculate the minimum number of cores for the Graph.
	 */
	public void calcMinCores() {
		int sumClo = 0;
		int sumChi = 0;
		int max;
		
		Iterator<Actor> it_n = genDAG.getNodes().iterator();
		
		while (it_n.hasNext()) {
			Actor n = it_n.next();
			sumChi += n.getC_HI();
			sumClo += n.getC_LO();
		}
		
		if (sumChi >= sumClo)
			max = sumChi;
		else
			max = sumClo;
		
		this.setNbCores((int)Math.ceil(max/this.getDeadline()));
	}
	
	public boolean allowedCommunitcation (Actor src, Actor dest) {
		if ((src.getC_HI() > 0 && dest.getC_HI() >= 0) ||
		(src.getC_HI() == 0 && dest.getC_HI() == 0))
			return true;
		
		return false;	
	}
	
	/**
	 * Sanity check for the graph:
	 * 	- Each node has to have at least one edge
	 */
	public void graphSanityCheck(int mode) {
		boolean added = false;
		Iterator<Actor> it_n = genDAG.getNodes().iterator();
		
		while (it_n.hasNext()) {
			Actor n = it_n.next();
			
			// It is an independent node with no edges
			if (n.getRcv_edges().size() == 0 && n.getSnd_edges().size() == 0) {
				Iterator<Actor> it_n2 = genDAG.getNodes().iterator();
				while (it_n2.hasNext() && added == false) {
					if (mode == 0) {
						Actor n2 = it_n2.next(); 
						if (n.getRank() < n2.getRank() &&
								allowedCommunitcation(n, n2) &&
								n.getCpFromNode_LO() + n2.getC_LO() <= userCp){
							Edge e = new Edge(n, n2);
							n.getSnd_edges().add(e);
							n2.getRcv_edges().add(e);
							added = true;
							if ((n.getC_HI() == 0 && n2.getC_HI() != 0) ||
									(n.getC_HI() != 0 && n2.getC_HI() == 0))
								this.setHtoL(true);
							n2.CPfromNode(mode);
						} else if (n.getRank() > n2.getRank() &&
								allowedCommunitcation(n2,n) &&
								n2.getCpFromNode_LO() + n.getC_LO() <= userCp) {
							Edge e = new Edge(n2, n);
							n.getRcv_edges().add(e);
							n2.getSnd_edges().add(e);
							added = true;
							if ((n.getC_HI() == 0 && n2.getC_HI() != 0) ||
									(n.getC_HI() != 0 && n2.getC_HI() == 0))
								this.setHtoL(true);
							n.CPfromNode(mode);
						}
					} else {
						Actor n2 = it_n2.next(); 
						if (n.getRank() < n2.getRank() &&
								allowedCommunitcation(n, n2) &&
								n.getCpFromNode_HI() + n2.getC_HI() <= userCp){
							Edge e = new Edge(n, n2);
							n.getSnd_edges().add(e);
							n2.getRcv_edges().add(e);
							added = true;
							n.CPfromNode(mode);
						} else if (n.getRank() > n2.getRank() &&
								allowedCommunitcation(n2,n) &&
								n2.getCpFromNode_HI() + n.getC_HI() <= userCp) {
							Edge e = new Edge(n2, n);
							n.getRcv_edges().add(e);
							n2.getSnd_edges().add(e);
							added = true;
							n.CPfromNode(mode);
						}
					}
				}
				added = false;
			}
		}
	}
	
	public void addHtoL() {
		Actor hi = null;
		Actor lo = null;
		Iterator<Actor> it_n = genDAG.getNodes().iterator();
		
		while (HtoL == false) {

			while (it_n.hasNext()) { // Find a HI task
				Actor n = it_n.next();
				if (n.getRank() < 2 && n.getC_HI() > 0) {
					hi = n;
				}
			}

			it_n = genDAG.getNodes().iterator();
			while (it_n.hasNext()) { // Find a HI task
				Actor n = it_n.next();
				if (n.getRank() > 2 && n.getC_HI() == 0) {
					lo = n;
				}
			}

			if (hi.getRank() < lo.getRank() &&
					allowedCommunitcation(hi, lo) &&
					hi.getCpFromNode_LO() + lo.getC_LO() <= userCp) {
				Edge e = new Edge(hi, lo);
				hi.getSnd_edges().add(e);
				lo.getRcv_edges().add(e);
				lo.CPfromNode(0);
				this.setHtoL(true);
			}
		}
	}
	
	/**
	 * Creates the matrix to be written in the files
	 */
	public void createAdjMatrix(){
		adjMatrix = new int[nbNodes][];
		for (int i = 0; i < nbNodes; i++)
			adjMatrix[i] = new int[nbNodes];
		
		Iterator<Actor> it_n = genDAG.getNodes().iterator();
		while (it_n.hasNext()){
			Actor n = it_n.next();
			
			Iterator<Edge> it_e = n.getRcv_edges().iterator();
			while (it_e.hasNext()){
				Edge e = it_e.next();
				adjMatrix[e.getDest().getId()][e.getSrc().getId()] = 1;
			}
		}
	}
	
	/**
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void toFile(String filename) throws IOException {
		BufferedWriter out = null;
		try {
			File f = new File(filename);
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			
			// Write number of nodes
			out.write("#NbNodes\n");
			out.write(Integer.toString(this.getNbNodes() - 1) + "\n\n");
			
			// Write number of cores
			out.write("#NbCores\n");
			out.write(Integer.toString(this.getNbCores()) + "\n\n");
			
			// Write number of cores
			out.write("#Deadline\n");
			out.write(Integer.toString(this.getDeadline()) + "\n\n");
			
			//Write C LOs
			out.write("#C_LO\n");
			for (int i = 0; i < nbNodes - 1; i++) {
				Actor n = genDAG.getNodebyID(i);
				out.write(Integer.toString(n.getC_LO()) + "\n");
			}
			out.write("\n");
			
			//Write C HIs
			out.write("#C_HI\n");
			for (int i = 0; i < nbNodes - 1; i++) {
				Actor n = genDAG.getNodebyID(i);
				out.write(Integer.toString(n.getC_HI()) + "\n");
			}
			out.write("\n");
			
			//Write precedence matrix
			out.write("#Pred\n");
			for (int i = 0; i < nbNodes - 1; i++) {
				for (int j = 0; j < nbNodes - 1; j++){
					out.write(Integer.toString(adjMatrix[i][j]));
					if (j < nbNodes - 2)
						out.write(",");
				}
				out.write("\n");
			}
			out.write("\n");
			
		}catch (IOException e){
			System.out.print("To File : " + e.getMessage());
		}finally{
			if(out != null)
				out.close();
		}
	}
	

	/**
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void toDZN(String filename) throws IOException {
		
	}

	/**
	 * Getters and setters
	 */
	public double getUserU_LO() {
		return userU_LO;
	}

	public void setUserU_LO(double userU_LO) {
		this.userU_LO = userU_LO;
	}

	public int getUserCp() {
		return userCp;
	}

	public void setUserCp(int userCp) {
		this.userCp = userCp;
	}

	public double getUserU_HI() {
		return userU_HI;
	}

	public void setUserU_HI(double userU_HI) {
		this.userU_HI = userU_HI;
	}

	public int getNbNodes() {
		return nbNodes;
	}

	public void setNbNodes(int nbNodes) {
		this.nbNodes = nbNodes;
	}

	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}

	public DAG getGenDAG() {
		return genDAG;
	}

	public void setGenDAG(DAG genDAG) {
		this.genDAG = genDAG;
	}
	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}
	public int getEdgeProb() {
		return edgeProb;
	}

	public void setEdgeProb(int edgeProb) {
		this.edgeProb = edgeProb;
	}

	public double getuHIinLO() {
		return uHIinLO;
	}

	public void setuHIinLO(double uHIinLO) {
		this.uHIinLO = uHIinLO;
	}


	public int getParaDegree() {
		return paraDegree;
	}

	public void setParaDegree(int paraDegree) {
		this.paraDegree = paraDegree;
	}


	public boolean isHtoL() {
		return HtoL;
	}


	public void setHtoL(boolean htoL) {
		HtoL = htoL;
	}
}
