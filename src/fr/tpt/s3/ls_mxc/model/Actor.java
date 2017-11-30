/*******************************************************************************
 * Copyright (c) 2017 Roberto Medina
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

public class Actor {
	
	public static final short LO = 0;
	public static final short HI = 1;

	private int id;
	private String name;
	
	private int[] cIs;
	
	private Set<Edge> rcvEdges;
	private Set<Edge> sndEdges;
	
	private boolean sink;
	private boolean source;
	private boolean sinkHI;
	private boolean sourceHI;
	
	public Actor (int id, String name, int nbModes) {
		this.setId(id);
		this.setName(name);
		cIs = new int[nbModes];
		this.setSink(false);
		this.setSource(false);
		this.setSinkHI(false);
		this.setSourceHI(false);
		rcvEdges = new HashSet<Edge>();
		sndEdges = new HashSet<Edge>();
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
			ActorSched dst = e.getDest();
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
}
