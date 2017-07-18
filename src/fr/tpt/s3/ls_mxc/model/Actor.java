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


public class Actor {
	
	static final short LO = 0;
	static final short HI = 1;
	
	private int id;
	private String name;
	
	private int C_LO;
	private int C_HI;
	
	private boolean source;
	private boolean sink;
	private boolean sinkinHI;
	
	private int weight_LO;
	private int weight_HI;
	private int weight_B;
	
	private Set<Edge> rcv_edges;
	private Set<Edge> snd_edges;
	
	// Used for DAG generation
	private int rank;
	private int cpFromNode_LO;
	private int cpFromNode_HI;
	
	// Used for DAG availability analysis
	private double fProb;
	private boolean fMechanism;
	
	/**
	 * Constructors
	 */
	public Actor(int id, String name, int c_lo, int c_hi){
		this.setId(id);
		this.setC_LO(c_lo);
		this.setC_HI(c_hi);
		this.setName(name);
		this.setSink(false);
		this.setSource(false);
		
		rcv_edges = new HashSet<Edge>();
		snd_edges = new HashSet<Edge>();
	}
	
	/**
	 * 	Utility methods
	 */
	public void checkifSource() {
		if (rcv_edges.size() == 0)
			this.setSource(true);
	}
	
	public void checkifSink() {
		if (snd_edges.size() == 0)
			this.setSink(true);
	}
	
	public void checkifSinkinHI() {
		Iterator<Edge> it_e = this.getSnd_edges().iterator();
		this.setSinkinHI(true);
		
		while (it_e.hasNext()){
			Edge e = it_e.next();
			Actor dst = e.getDest();
			if (dst.getC_HI() != 0) {
				this.setSinkinHI(false);
				break;
			}
		}
	}
	
	/**
	 * 
	 * @param n
	 * @param mode
	 * @return
	 */
	public int CPfromNode (int mode) {
		
		if (this.getRcv_edges().size() == 0) {
			if (mode == 0) {
				this.setCpFromNode_LO(C_LO);
				return this.getC_LO();
			} else {
				this.setCpFromNode_HI(C_HI);
				return this.getC_HI();
			}
		} else {
			int max = 0;
			int tmp = 0;
			Iterator<Edge> it_e = this.getRcv_edges().iterator();
			while (it_e.hasNext()){
				Edge e = it_e.next();
				if (mode == 0) {
					tmp = e.getSrc().getCpFromNode_LO();
					if (max < tmp)
						max = tmp;
				} else {
					tmp = e.getSrc().getCpFromNode_HI();
					if (max < tmp)
						max = tmp;
				}
			}
			if (mode == 0)
				this.setCpFromNode_LO(max + this.getC_LO());
			else
				this.setCpFromNode_HI(max + this.getC_HI());
			
			return max;
		}
			
	}
	
	/**
	 * Tests if the node is an exit node
	 * @return
	 */
	public boolean isExitNode() {
		if (this.getSnd_edges().size() == 0)
			return true;
		else
			return false;
	}
	
	/**
	 * Returns all LOpredecessors of a node
	 * @return
	 */
	public Set<Actor> getLOPred() {
		HashSet<Actor> result = new HashSet<Actor>();
		Iterator<Edge> ie = this.getRcv_edges().iterator();
		
		while (ie.hasNext()){
			Edge e = ie.next();
			if (e.getSrc().getC_HI() == 0) {
				result.add(e.getSrc());
				result.addAll(e.getSrc().getLOPred());
			}
		}
		return result;
	}

	
	/**
	 *  Getters & Setters
	 */
	public int getC_LO() {
		return C_LO;
	}
	public void setC_LO(int c_LO) {
		C_LO = c_LO;
	}
	public int getC_HI() {
		return C_HI;
	}
	public void setC_HI(int c_HI) {
		C_HI = c_HI;
	}
	public boolean isSource() {
		return source;
	}
	public void setSource(boolean source) {
		this.source = source;
	}
	public boolean isSink() {
		return sink;
	}
	public void setSink(boolean sink) {
		this.sink = sink;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Set<Edge> getRcv_edges() {
		return rcv_edges;
	}
	public void setRcv_edges(Set<Edge> rcv_edges) {
		this.rcv_edges = rcv_edges;
	}
	public Set<Edge> getSnd_edges() {
		return snd_edges;
	}
	public void setSnd_edges(Set<Edge> snd_edges) {
		this.snd_edges = snd_edges;
	}
	public int getWeight_LO(){
		return this.weight_LO;
	}
	public void setWeight_LO(int w_lo){
		this.weight_LO = w_lo;
	}
	public int getWeight_HI(){
		return this.weight_HI;
	}
	public void setWeight_HI(int w_hi){
		this.weight_HI = w_hi;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public boolean isSinkinHI() {
		return sinkinHI;
	}

	public void setSinkinHI(boolean sinkinHI) {
		this.sinkinHI = sinkinHI;
	}

	public int getWeight_B() {
		return weight_B;
	}

	public void setWeight_B(int weight_B) {
		this.weight_B = weight_B;
	}

	public double getfProb() {
		return fProb;
	}

	public void setfProb(double fProb) {
		this.fProb = fProb;
	}

	public boolean isfMechanism() {
		return fMechanism;
	}

	public void setfMechanism(boolean fMechanism) {
		this.fMechanism = fMechanism;
	}

}
