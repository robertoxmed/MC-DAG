/*******************************************************************************
 * Copyright (c) 2017, 2018 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package fr.tpt.s3.mcdag.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class Actor {
	
	public static final short LO = 0;
	public static final short HI = 1;

	private int id;
	private String name;
	
	private int[] wcets;
	
	private int cpFromNode[];
	
	private Set<Edge> rcvEdges;
	private Set<Edge> sndEdges;

	
	public Actor (int id, String name, int nbLevels) {
		this.setId(id);
		this.setName(name);
		wcets = new int[nbLevels];
		rcvEdges = new HashSet<Edge>();
		sndEdges = new HashSet<Edge>();
		cpFromNode = new int[nbLevels];
	}
	
	/**
	 * Returns the jth Ci(J)
	 * @param j
	 * @return
	 */
	public int getWcet (int level) {
		return this.wcets[level];
	}
	
	
	/**
	 * Tests if the node is an exit node
	 * @return
	 */
	public boolean isExitNode() {
		if (this.getSndEdges().size() == 0)
			return true;
		else
			return false;
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
				getCpFromNode()[mode] = this.getWcets()[0];
				return this.getWcets()[0];
			} else {
				getCpFromNode()[mode] = this.getWcets()[1];
				return this.getWcets()[1];
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
				max += this.getWcets()[0];
				getCpFromNode()[mode] = max;
			} else {
				max += this.getWcets()[1];
				getCpFromNode()[mode] = max;
			}
			
			return max;
		}
	}
	
	/**
	 * Calculates the critical Path from a given node
	 * @param n
	 * @param mode
	 * @return
	 */
	public int CPfromNode (int level) {
		
		if (this.getRcvEdges().size() == 0) {
			this.getCpFromNode()[level] = this.getWcet(level);
			return this.getWcet(level);
		} else {
			int max = 0;
			int tmp = 0;
			Iterator<Edge> it_e = this.getRcvEdges().iterator();
			
			while (it_e.hasNext()){
				Edge e = it_e.next();
				
				tmp = e.getSrc().getCpFromNode()[level];
				if (max < tmp)
					max = tmp;
			}
			
			max += this.getWcet(level);
			this.getCpFromNode()[level] = max;

			return max;
		}
	}
	
	/**
	 * Returns all LOpredecessors of a node
	 * @return
	 */
	public Set<Actor> getLOPred() {
		HashSet<Actor> result = new HashSet<Actor>();
		Iterator<Edge> ie = this.getRcvEdges().iterator();
		
		while (ie.hasNext()){
			Edge e = ie.next();
			if (e.getSrc().getWcets()[1] == 0) {
				result.add(e.getSrc());
				result.addAll(e.getSrc().getLOPred());
			}
		}
		return result;
	}
	
	/**
	 * Returns true if the actor is a source in L mode
	 * @param l
	 * @return
	 */
	public boolean isSourceinL (int l) {
		if (this.getWcet(l) == 0)
			return false;
		
		for (Edge e : this.getRcvEdges()) {
			if (e.getSrc().getWcet(l) != 0)
				return false;
		}
		return true;
	}
	
	/**
	 * Returns true if the actor is a sink in L mode
	 * @param l
	 * @return
	 */
	public boolean isSinkinL (int l) {
		if (this.getWcet(l) == 0)
			return false;
		for (Edge e : this.getSndEdges()) {
			if (e.getDest().getWcet(l) != 0)
				return false;
		}
		return true;
	}
	
	/*
	 * Getters and setters
	 *
	 */
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int[] getWcets() {
		return wcets;
	}
	
	public void setWcets(int[] cIs) {
		this.wcets = cIs;
	}
	
	public Set<Edge> getRcvEdges() {
		return rcvEdges;
	}
	
	public void setRcvEdges(Set<Edge> rcvEdges) {
		this.rcvEdges = rcvEdges;
	}
	
	public Set<Edge> getSndEdges() {
		return sndEdges;
	}
	
	public void setSndEdges(Set<Edge> sndEdges) {
		this.sndEdges = sndEdges;
	}

	public int[] getCpFromNode() {
		return cpFromNode;
	}

	public void setCpFromNode(int cpFromNode[]) {
		this.cpFromNode = cpFromNode;
	}
}
