package fr.tpt.s3.ls_mxc.generator;

import java.util.Set;

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
