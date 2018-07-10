/*******************************************************************************
 * Copyright (c) 2017, 2018 Roberto Medina
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
package fr.tpt.s3.mcdag.model;

public class ActorSched extends Actor {
	
	// Used for singleDAG scheduling
	private int[] hlfet;
	
	// Used for DAG generation
	private int rank;

	// Used for multiDAG scheduling N level
	private int graphDead;
	
	// Used for N levels scheduling
	private int deadlines[];
	private int weights[];
	private boolean visitedL[];
	private int graphID;
	private boolean delayed;
	
	// Non preemptive version
	private boolean running;
	
	private double fProb;

	/**
	 * Constructor whithout CIs
	 * @param id
	 * @param name
	 */
	public ActorSched (int id, String name, int nbLevels) {
		super(id, name, nbLevels);
		
		hlfet = new int[nbLevels];
		deadlines = new int[nbLevels];
		weights = new int[nbLevels];
		visitedL = new boolean[nbLevels];
		
		for (int i = 0; i < nbLevels; i++) {
			hlfet[i] = 0;
			deadlines[i] = Integer.MAX_VALUE;
			weights[i] = Integer.MAX_VALUE;
			visitedL[i] = false;
		}
		running = false;
		delayed = false;
	}
	
	/*
	 *  Getters & Setters
	 */
	public double getfProb() {
		return fProb;
	}
	public void setfProb(double fProb) {
		this.fProb = fProb;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public int getGraphDead() {
		return graphDead;
	}

	public void setGraphDead(int graphDead) {
		this.graphDead = graphDead;
	}

	public int[] getDeadlines() {
		return deadlines;
	}

	public void setLFTs(int lFTs[]) {
		deadlines = lFTs;
	}

	public void setLFTinL (int val, int l) {
		this.getDeadlines()[l] = val;
	}

	public int[] getLaxities() {
		return weights;
	}

	public void setLaxities(int urgencies[]) {
		this.weights = urgencies;
	}
	
	public void setLaxityinL (int val, int level) {
		this.weights[level] = val;
	}
	

	public boolean[] getVisitedL() {
		return visitedL;
	}

	public void setVisitedL(boolean visitedL[]) {
		this.visitedL = visitedL;
	}

	public int getGraphID() {
		return graphID;
	}

	public void setGraphID(int graphID) {
		this.graphID = graphID;
	}

	public boolean isDelayed() {
		return delayed;
	}

	public void setDelayed(boolean delayed) {
		this.delayed = delayed;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public int[] getHlfet() {
		return hlfet;
	}

	public void setHlfet(int[] hlfet) {
		this.hlfet = hlfet;
	}

}
