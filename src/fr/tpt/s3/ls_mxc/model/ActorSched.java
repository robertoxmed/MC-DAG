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
package fr.tpt.s3.ls_mxc.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class ActorSched extends Actor {
	
	private int wLO;
	private int wHI;
	private int wB;
	
	// Used for DAG generation
	private int rank;
	private int cpFromNode_LO;
	private int cpFromNode_HI;

	
	// Used for multi dag scheduling
	private int graphDead;
	private int LFTLO;
	private boolean promoted;
	private int LFTHI;
	private int urgencyLO;
	private int urgencyHI;
	private boolean visited;
	private boolean visitedHI;
	
	/**
	 * Constructors
	 */
	public ActorSched(int id, String name, int cLO, int cHI){
		super(id, name, 2);
		
		this.getcIs()[0] = cLO;
		this.getcIs()[1] = cHI;
		LFTHI = Integer.MAX_VALUE;
		LFTLO = Integer.MAX_VALUE;
		urgencyHI = Integer.MAX_VALUE;
		urgencyLO = Integer.MAX_VALUE;
		visited = false;
		visitedHI = false;
		promoted = false;
	}
	

	
	/**
	 * Calculates the critical Path from a given node
	 * @param n
	 * @param mode
	 * @return
	 */
	public int CPfromNode (short mode) {
		
		if (this.getRcvEdges().size() == 0) {
			if (mode == 0) {
				this.setCpFromNode_LO(this.getcIs()[0]);
				return this.getcIs()[0];
			} else {
				this.setCpFromNode_HI(this.getcIs()[1]);
				return this.getcIs()[1];
			}
		} else {
			int max = 0;
			int tmp = 0;
			Iterator<Edge> it_e = this.getRcvEdges().iterator();
			
			while (it_e.hasNext()){
				Edge e = it_e.next();
				if (mode == ActorSched.LO) {
					tmp = e.getSrc().CPfromNode(ActorSched.LO);
					if (max < tmp)
						max = tmp;
				} else {
					tmp = e.getSrc().CPfromNode(ActorSched.HI);
					if (max < tmp)
						max = tmp;
				}
			}
			if (mode == ActorSched.LO) {
				max += this.getcIs()[0];
				this.setCpFromNode_LO(max);
			} else {
				max += this.getcIs()[1];
				this.setCpFromNode_HI(max);
			}
			
			return max;
		}
			
	}
	

	/**
	 * Returns all LOpredecessors of a node
	 * @return
	 */
	public Set<ActorSched> getLOPred() {
		HashSet<ActorSched> result = new HashSet<ActorSched>();
		Iterator<Edge> ie = this.getRcvEdges().iterator();
		
		while (ie.hasNext()){
			Edge e = ie.next();
			if (e.getSrc().getcIs()[1] == 0) {
				result.add(e.getSrc());
				result.addAll(e.getSrc().getLOPred());
			}
		}
		return result;
	}

	
	/*
	 *  Getters & Setters
	 */
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

	public int getCpFromNode_LO() {
		return cpFromNode_LO;
	}

	public void setCpFromNode_LO(int cpFromNode_LO) {
		this.cpFromNode_LO = cpFromNode_LO;
	}

	public int getCpFromNode_HI() {
		return cpFromNode_HI;
	}

	public void setCpFromNode_HI(int cpFromNode_HI) {
		this.cpFromNode_HI = cpFromNode_HI;
	}

	public int getWeight_B() {
		return wB;
	}

	public void setWeight_B(int weight_B) {
		this.wB = weight_B;
	}

	public int getUrgencyLO() {
		return urgencyLO;
	}

	public void setUrgencyLO(int urgencyLO) {
		this.urgencyLO = urgencyLO;
	}

	public int getUrgencyHI() {
		return urgencyHI;
	}

	public void setUrgencyHI(int urgencyHI) {
		this.urgencyHI = urgencyHI;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public boolean isVisitedHI() {
		return visitedHI;
	}

	public void setVisitedHI(boolean visitedHI) {
		this.visitedHI = visitedHI;
	}

	public int getLFTLO() {
		return LFTLO;
	}

	public void setLFTLO(int lFTLO) {
		LFTLO = lFTLO;
	}

	public int getLFTHI() {
		return LFTHI;
	}

	public void setLFTHI(int lFTHI) {
		LFTHI = lFTHI;
	}

	public int getGraphDead() {
		return graphDead;
	}

	public void setGraphDead(int graphDead) {
		this.graphDead = graphDead;
	}

	public boolean isPromoted() {
		return promoted;
	}

	public void setPromoted(boolean promoted) {
		this.promoted = promoted;
	}

}
