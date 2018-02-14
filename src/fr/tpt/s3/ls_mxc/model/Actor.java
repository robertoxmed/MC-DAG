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
package fr.tpt.s3.ls_mxc.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class Actor {
	
	public static final short LO = 0;
	public static final short HI = 1;

	private int id;
	private String name;
	
	private int[] cIs;
	
	private int cpFromNodeLO;
	private int cpFromNodeHI;
	private int cpFromNode[];
	
	private Set<Edge> rcvEdges;
	private Set<Edge> sndEdges;
	
	private boolean sink;
	private boolean source;
	private boolean sinkHI;
	private boolean sourceHI;
	
	public Actor (int id, String name, int nbLevels) {
		this.setId(id);
		this.setName(name);
		cIs = new int[nbLevels];
		this.setSink(false);
		this.setSource(false);
		this.setSinkHI(false);
		this.setSourceHI(false);
		rcvEdges = new HashSet<Edge>();
		sndEdges = new HashSet<Edge>();
		cpFromNode = new int[nbLevels];
	}
	
	/**
	 * 	Utility methods
	 */
	public void checkifSource() {
		if (rcvEdges.size() == 0)
			this.setSource(true);
	}
	
	public void checkifSink() {
		if (sndEdges.size() == 0)
			this.setSink(true);
	}
	
	public void checkifSourceHI() {
		if (rcvEdges.size() == 0 && this.getcIs()[1] != 0)
			this.setSourceHI(true);
	}
	
	public void checkifSinkinHI() {
		
		if(this.getcIs()[1] == 0) {
			this.setSinkHI(false);
			return;
		}
		
		this.setSinkHI(true);
		
		Iterator<Edge> it_e = this.getSndEdges().iterator();
		while (it_e.hasNext()){
			Edge e = it_e.next();
			Actor dst = e.getDest();
			if (dst.getcIs()[1] != 0) {
				this.setSinkHI(false);
				break;
			}
		}
	}
	
	/**
	 * Returns the jth Ci(J)
	 * @param j
	 * @return
	 */
	public int getCI (int j) {
		return this.cIs[j];
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
				this.setCpFromNodeLO(this.getcIs()[0]);
				return this.getcIs()[0];
			} else {
				this.setCpFromNodeHI(this.getcIs()[1]);
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
				this.setCpFromNodeLO(max);
			} else {
				max += this.getcIs()[1];
				this.setCpFromNodeHI(max);
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
			this.getCpFromNode()[level] = this.getCI(level);
			return this.getCI(level);
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
			
			max += this.getCI(level);
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
			if (e.getSrc().getcIs()[1] == 0) {
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
		if (this.getCI(l) == 0)
			return false;
		
		for (Edge e : this.getRcvEdges()) {
			if (e.getSrc().getCI(l) != 0)
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
		if (this.getCI(l) == 0)
			return false;
		for (Edge e : this.getSndEdges()) {
			if (e.getDest().getCI(l) != 0)
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
	
	public int[] getcIs() {
		return cIs;
	}
	
	public void setcIs(int[] cIs) {
		this.cIs = cIs;
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
	
	public boolean isSink() {
		return sink;
	}
	
	public void setSink(boolean sink) {
		this.sink = sink;
	}
	
	public boolean isSource() {
		return source;
	}
	
	public void setSource(boolean source) {
		this.source = source;
	}
	
	public boolean isSinkHI() {
		return sinkHI;
	}
	
	public void setSinkHI(boolean sinkHI) {
		this.sinkHI = sinkHI;
	}
	
	public boolean isSourceHI() {
		return sourceHI;
	}
	
	public void setSourceHI(boolean sourceHI) {
		this.sourceHI = sourceHI;
	}

	public int getCpFromNodeLO() {
		return cpFromNodeLO;
	}

	public void setCpFromNodeLO(int cpFromNodeLO) {
		this.cpFromNodeLO = cpFromNodeLO;
	}

	public int getCpFromNodeHI() {
		return cpFromNodeHI;
	}

	public void setCpFromNodeHI(int cpFromNodeHI) {
		this.cpFromNodeHI = cpFromNodeHI;
	}

	public int[] getCpFromNode() {
		return cpFromNode;
	}

	public void setCpFromNode(int cpFromNode[]) {
		this.cpFromNode = cpFromNode;
	}
}
