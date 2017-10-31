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

import java.util.HashSet;
import java.util.Iterator;
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
	private int edgeProb;
	private double uHIinLO;
	private int paraDegree;
	private boolean HtoL;
	private RandomNumberGenerator rng;
	private Set<DAG> genDAG;
	private int nbDags;
	private int deadline;
	private boolean debug;
	
	public UtilizationGenerator (double U_LO, double U_HI, int cp, int edgeProb, double UHIinLO, int para, int cores, int nbDags, boolean debug) {
		this.setUserU_LO(U_LO);
		this.setUserU_HI(U_HI);
		this.setUserCp(cp);
		this.setEdgeProb(edgeProb);
		this.setuHIinLO(UHIinLO);
		this.setParaDegree(para);
		this.setNbCores(cores);
		this.setGenDAG(new HashSet<DAG>());
		this.setNbDags(nbDags);
		this.rng = new RandomNumberGenerator();
		this.setDebug(debug);
	}
	
	/**
	 * Function that prints the current parameters of the node
	 * @param a
	 */
	private void debugNode (Actor a, String func) {
		
		System.out.println("[DEBUG] "+func+": Node "+a.getId()+" Ci(HI) = "+a.getCHI()+" Ci(LO) = "+a.getCLO());
		for (Edge e : a.getRcvEdges())
			System.out.println("\t Rcv Edge "+e.getSrc().getId()+" -> "+a.getId());
		for (Edge e : a.getSndEdges())
			System.out.println("\t Snd Edge "+a.getId()+" -> "+e.getDest().getId());
	}
	
	/**
	 * Generates a DAG without making the CP in HI first
	 */
	public void GenenrateGraph() {
		// Variables
		int id = 0;
		DAG d = new DAG();
		Set<Actor> nodes = new HashSet<Actor>();
		int rank = 0;
		
		// Budgets deduced by utilization and deadline
		// Randomly generate a deadline
		int rDead = rng.randomUnifInt(userCp/2, userCp);
		double rULO = rng.randomUnifDouble(userU_LO / 2, userU_LO);
		double rUHI = rng.randomUnifDouble(userU_HI / 2, userU_LO);
		
		int budgetHI = (int) Math.ceil(rDead * rUHI);
		int budgetLO = (int) Math.ceil(rDead * rULO);		
		int CHIBound = (int) Math.ceil(rDead / 2);
		int CLOBound = (int) Math.ceil(rDead / 2);
		
		if (isDebug()) {
			System.out.println("[DEBUG] GenerateGraph: Generating a graph with parameters, ULO = "+rULO+", UHI = "+rUHI+
					" deadline = "+rDead);
			System.out.println("[DEBUG] GenerateGraph: >>> Generating HI tasks first.");
		}
					
		// Generate HI nodes and the arcs
		// No hypothesis about the CP.
		rank = 0;		
		while (budgetHI > 0) {
			// Roll a number of nodes to add to the level
			int nodesPerRank = rng.randomUnifInt(1,paraDegree);
			for (int j=0; j < nodesPerRank && budgetHI > 0; j++) {
				Actor n = new Actor(id, Integer.toString(id), 0, 0);
			
				// Roll a C_HI and test if budget is left
				n.setCHI(rng.randomUnifInt(2, CHIBound));
				if (budgetHI - n.getCHI() > 0) {
					budgetHI = budgetHI - n.getCHI();
				} else {
					n.setCHI(budgetHI);
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
								&& src.getCpFromNode_HI() + n.getCHI() <= rDead) {
							Edge e = new Edge(src, n);
							src.getSndEdges().add(e);
							n.getRcvEdges().add(e);
						}
					}
				}
				n.setCLO(n.getCHI());
				nodes.add(n);
				n.CPfromNode(Actor.HI);
				id++;
				if (isDebug()) {
					String func = Thread.currentThread().getStackTrace()[1].getMethodName();
					debugNode(n, func);
				}
			}
			rank++;
		}
		
		// Deflate HI execution times
		double minU = Math.min(rUHI, rULO) / 2;
		int wantedHIinLO = (int) Math.ceil(minU * rDead);
		int actualBudget = (int) Math.ceil(rUHI * rDead);
		Iterator<Actor> it_n;
		
		while (wantedHIinLO < actualBudget && !allHIareMin(nodes)) {
			it_n = nodes.iterator();
			while (it_n.hasNext()) {
				Actor n = it_n.next();
				
				n.setCLO(rng.randomUnifInt(1, n.getCLO()));				
				actualBudget = actualBudget - n.getCLO();
				if (actualBudget < 0)
					actualBudget = 0;
			}
		}

		// Debugging block
		if (isDebug()) {
			System.out.println("[DEBUG] GenerateGraph(): >>> Deflation of HI tasks finished");
			for (Actor a : nodes) 
				debugNode(a, "GenerateGraph()");
		}
		
		it_n = nodes.iterator();
		while (it_n.hasNext()){
			it_n.next().CPfromNode(Actor.LO);
		}
				
		graphSanityCheck(d, Actor.HI);
		
		// Add LO nodes
		actualBudget = 0;
		it_n = nodes.iterator();
		while (it_n.hasNext()) {
			actualBudget += it_n.next().getCLO();
		}
			
		budgetLO = budgetLO - actualBudget;

		// Generate LO tasks
		rank = 0;
		while (budgetLO > 0) {
			// Roll a number of nodes to add to the level
			int nodesPerRank = rng.randomUnifInt(1, (int)(paraDegree / 2));
			for (int j=0; j < nodesPerRank && budgetLO > 0; j++) {
				Actor n = new Actor(id, Integer.toString(id), 0, 0);
			
				// Roll a C_HI and test if budget is left
				n.setCHI(0);
				n.setCLO(rng.randomUnifInt(1, CLOBound));
				if (n.getCLO() == 0)
					n.setCLO(1); // Minimal execution time
				
				if (budgetLO - n.getCLO() > 0) {
					budgetLO = budgetLO - n.getCLO();
				} else {
					n.setCLO(budgetLO);
					budgetLO = 0;
				}
				
				n.setRank(rank);
				if (rank != 0) {
					Iterator<Actor> it = nodes.iterator();
					while (it.hasNext()) {
						Actor src = it.next();
						// Test if the rank of the source is lower and if the CP
						// is not reached
						if (rng.randomUnifInt(1,100) <= edgeProb && n.getRank() > src.getRank()
								&& src.getCpFromNode_LO() + n.getCLO() <= rDead &&
								allowedCommunitcation(src, n)) {
							Edge e = new Edge(src, n);
							src.getSndEdges().add(e);
							n.getRcvEdges().add(e);
						}
					}
				}
				nodes.add(n);
				n.CPfromNode(Actor.LO);
				id++;
			}
			rank++;
		}
		
		// Debugging block
		if (isDebug()) {
			System.out.println("[DEBUG] GenerateGraph(): >>> LO tasks added.");
			for (Actor a : nodes) 
				debugNode(a, "GenerateGraph()");
		}
		
		it_n = nodes.iterator();
		while (it_n.hasNext()){
			Actor n = it_n.next();
			n.checkifSink();
			n.checkifSource();
		}
		
		graphSanityCheck(d, Actor.LO);
		d.setNodes(nodes);
		d.setDeadline(rDead);
		d.setId(getGenDAG().size());
		getGenDAG().add(d);
	}
	
	/**
	 * Tests if all HI nodes are minimal execution <=> C LO = 1
	 * @param nodes
	 * @return
	 */
	public boolean allHIareMin (Set<Actor> nodes) {
		Iterator<Actor> it = nodes.iterator();
		while (it.hasNext()){
			if (it.next().getCLO() != 1)
				return false;
		}
		return true;
	}
	
	/**
	 * Calculate the minimum number of cores for the Graph.
	 */
	public void calcMinCores(DAG d) {
		int sumClo = 0;
		int sumChi = 0;
		int max;
		Iterator<Actor> it_n = d.getNodes().iterator();
		
		while (it_n.hasNext()) {
			Actor n = it_n.next();
			sumChi += n.getCHI();
			sumClo += n.getCLO();
		}
		
		if (sumChi >= sumClo)
			max = sumChi;
		else
			max = sumClo;
		
		this.setNbCores((int)Math.ceil(max/this.getDeadline()));
	}
	
	public boolean allowedCommunitcation (Actor src, Actor dest) {
		if ((src.getCHI() > 0 && dest.getCHI() >= 0) ||
		(src.getCHI() == 0 && dest.getCHI() == 0))
			return true;
		
		return false;	
	}
	
	/**
	 * Sanity check for the graph:
	 * 	- Each node has to have at least one edge
	 */
	public void graphSanityCheck(DAG d, short mode) {
		boolean added = false;
		Iterator<Actor> it_n = d.getNodes().iterator();
		
		while (it_n.hasNext()) {
			Actor n = it_n.next();
			
			// It is an independent node with no edges
			if (n.getRcvEdges().size() == 0 && n.getSndEdges().size() == 0) {
				Iterator<Actor> it_n2 = d.getNodes().iterator();
				while (it_n2.hasNext() && added == false) {
					if (mode == Actor.LO) {
						Actor n2 = it_n2.next(); 
						if (n.getRank() < n2.getRank() &&
								allowedCommunitcation(n, n2) &&
								n.getCpFromNode_LO() + n2.getCLO() <= userCp){
							Edge e = new Edge(n, n2);
							n.getSndEdges().add(e);
							n2.getRcvEdges().add(e);
							added = true;
							if ((n.getCHI() == 0 && n2.getCHI() != 0) ||
									(n.getCHI() != 0 && n2.getCHI() == 0))
								this.setHtoL(true);
							n2.CPfromNode(mode);
						} else if (n.getRank() > n2.getRank() &&
								allowedCommunitcation(n2,n) &&
								n2.getCpFromNode_LO() + n.getCLO() <= userCp) {
							Edge e = new Edge(n2, n);
							n.getRcvEdges().add(e);
							n2.getSndEdges().add(e);
							added = true;
							if ((n.getCHI() == 0 && n2.getCHI() != 0) ||
									(n.getCHI() != 0 && n2.getCHI() == 0))
								this.setHtoL(true);
							n.CPfromNode(mode);
						}
					} else {
						Actor n2 = it_n2.next(); 
						if (n.getRank() < n2.getRank() &&
								allowedCommunitcation(n, n2) &&
								n.getCpFromNode_HI() + n2.getCHI() <= userCp){
							Edge e = new Edge(n, n2);
							n.getSndEdges().add(e);
							n2.getRcvEdges().add(e);
							added = true;
							n.CPfromNode(mode);
						} else if (n.getRank() > n2.getRank() &&
								allowedCommunitcation(n2,n) &&
								n2.getCpFromNode_HI() + n.getCHI() <= userCp) {
							Edge e = new Edge(n2, n);
							n.getRcvEdges().add(e);
							n2.getSndEdges().add(e);
							added = true;
							n.CPfromNode(mode);
						}
					}
				}
				added = false;
			}
		}
	}
	
	public void addHtoL(DAG d) {
		Actor hi = null;
		Actor lo = null;
		Iterator<Actor> it_n = d.getNodes().iterator();
		
		while (HtoL == false) {

			while (it_n.hasNext()) { // Find a HI task
				Actor n = it_n.next();
				if (n.getRank() < 2 && n.getCHI() > 0) {
					hi = n;
				}
			}

			it_n = d.getNodes().iterator();
			while (it_n.hasNext()) { // Find a HI task
				Actor n = it_n.next();
				if (n.getRank() > 2 && n.getCHI() == 0) {
					lo = n;
				}
			}

			if (hi.getRank() < lo.getRank() &&
					allowedCommunitcation(hi, lo) &&
					hi.getCpFromNode_LO() + lo.getCLO() <= userCp) {
				Edge e = new Edge(hi, lo);
				hi.getSndEdges().add(e);
				lo.getRcvEdges().add(e);
				lo.CPfromNode(Actor.LO);
				this.setHtoL(true);
			}
		}
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

	public Set<DAG> getGenDAG() {
		return genDAG;
	}

	public void setGenDAG(Set<DAG> genDAG) {
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

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getNbDags() {
		return nbDags;
	}

	public void setNbDags(int nbDags) {
		this.nbDags = nbDags;
	}
}
