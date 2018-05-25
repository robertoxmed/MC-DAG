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
	private int wLO;
	private int wHI;
	private int wB;
	
	// Used for DAG generation
	private int rank;

	// Used for multiDAG scheduling N level
	private int graphDead;
	
	// Used for N levels scheduling
	private int LFTs[];
	private int laxity[];
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
		
		LFTs = new int[nbLevels];
		laxity = new int[nbLevels];
		visitedL = new boolean[nbLevels];
		
		for (int i = 0; i < nbLevels; i++) {
			LFTs[i] = Integer.MAX_VALUE;
			laxity[i] = Integer.MAX_VALUE;
			visitedL[i] = false;
		}
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
	public int getWeightLO(){
		return this.wLO;
	}
	public void setWeightLO(int w_lo){
		this.wLO = w_lo;
	}
	public int getWeightHI(){
		return this.wHI;
	}
	public void setWeightHI(int w_hi){
		this.wHI = w_hi;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public int getWeightB() {
		return wB;
	}

	public void setWeightB(int weight_B) {
		this.wB = weight_B;
	}

	public int getGraphDead() {
		return graphDead;
	}

	public void setGraphDead(int graphDead) {
		this.graphDead = graphDead;
	}

	public int[] getLFTs() {
		return LFTs;
	}

	public void setLFTs(int lFTs[]) {
		LFTs = lFTs;
	}

	public void setLFTinL (int val, int l) {
		this.getLFTs()[l] = val;
	}

	public int[] getLaxities() {
		return laxity;
	}


	public void setLaxities(int urgencies[]) {
		this.laxity = urgencies;
	}
	
	public void setLaxityinL (int val, int level) {
		this.laxity[level] = val;
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

}
