package fr.tpt.s3.ls_mxc.generator;

import java.util.HashSet;
import java.util.Set;

import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.util.RandomNumberGenerator;

public class NLevelsGenerator {

	// Set of generated graphs
	private Set<DAG> gennedDAGs;
	
	// Parameters for the generation
	private double edgeProb;
	private double userMinU;
	private double userMaxU;
	private int nbLevels;
	private int parallelismDegree;
	
	// Utilities
	private RandomNumberGenerator rng;
	private boolean debug;
	
	private int possibleDeadlines[] = {10, 15, 20, 30, 14, 12}; 
	
	public NLevelsGenerator (double minU, double maxU, double eProb, int levels, int paraDegree, boolean debug) {
		setUserMinU(minU);
		setUserMaxU(maxU);
		setEdgeProb(eProb);
		setNbLevels(levels);
		setParallelismDegree(paraDegree);
		setDebug(debug);
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
				rU[i] = rng.randomUnifDouble(rU[i - 1], userMinU + ((userMaxU - userMinU) / nbLevels) * (i+1));
			else 
				rU[i] = rng.randomUnifDouble(userMinU, userMinU + ((userMaxU - userMinU) / nbLevels) * (i+1));
			budgets[i] = (int) Math.ceil(rDead * rU[i]);
			cBounds[i] = (int) Math.ceil(rDead / 2); 
		}
		
		if (isDebug()) {
			System.out.print("[DEBUG] GenerateGraph: Generating a graph with parameters ");
			for (int i = 0; i < nbLevels; i++)
				System.out.print("U["+i+"] = "+rU[i]+"; ");
			System.out.println("deadline = "+rDead);
			System.out.println("[DEBUG] GenerateGraph: >>> Generating HI tasks first.");
		}
		
		// Generate nodes for all levels
		for (int i = 0; i < nbLevels; i++) {
			// Node generation block
			
			// Shrinking procedure
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
}
