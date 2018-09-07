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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Class to model the DAG in MxC
 * @author Roberto Medina
 *
 */
public class McDAG {
	
	private int id;
	private Set<Vertex> nodes;
	private Set<Vertex> nodesHI;
	private Set<Vertex> loOuts;
	private Set<Vertex> Outs;
	private int critPath;
	private int deadline;
	private int levels;
	
	public McDAG() {
		nodes = new HashSet<Vertex>();
		nodesHI = new HashSet<Vertex>();
		setLoOuts(new HashSet<Vertex>());
	}
	
	/**
	 * Method to get the critical Path.
	 * Needs to be called after getting HLFET levels.
	 */
	public int calcCriticalPath() {
		int cp = 0;
		
		for(int i = 0; i < this.getVertices().size(); i++) {
			if (cp < this.getNodebyID(i).getCpFromNode()[0])
				cp = this.getNodebyID(i).getCpFromNode()[0];
			if (cp < this.getNodebyID(i).getCpFromNode()[1])
				cp = this.getNodebyID(i).getCpFromNode()[1];
		}
		this.setCritPath(cp);
		return cp;
	}
	
	/**
	 * Sets HI nodes in the corresponding set
	 */
	public void setHINodes() {
		Iterator<Vertex> in = this.getVertices().iterator();
		while (in.hasNext()) {
			Vertex n = in.next();
			if (n.getWcet(1) != 0)
				this.getNodes_HI().add(n);
		}
	}
	
	/**
	 * Searches for the LO outputs in the DAG
	 */
	public void calcLOouts() {
		Iterator<Vertex> in = this.getVertices().iterator();
		while (in.hasNext()) {
			Vertex n = in.next();
			if (n.getSndEdges().size() == 0 &&
					n.getWcet(1) == 0) {
				this.getLoOuts().add(n);
			}
		}
	}
	
	/**
	 * Gets the outputs of the DAG
	 */
	public void calcOuts() {
		for (Vertex a : getVertices()) {
			if (a.getSndEdges().size() == 0)
				Outs.add(a);
		}
	}
	
	/**
	 * Returns the utilization in the LO mode
	 * @return
	 */
	public double getULO () {
		double ret = 0.0;
		
		for (Vertex a : getVertices())
			ret += a.getWcet(0);
		
		return ret / getDeadline();
	}
	
	/**
	 * Returns the utilization in the HI mode
	 * @return
	 */
	public double getUHI () {
		double ret = 0.0;
		
		for (Vertex a : getVertices()) {
			if (a.getWcet(1) != 0)
				ret += a.getWcet(1);
		}
		return ret / getDeadline();
	}
	
	/**
	 * Returns the maximum utilization of the DAG
	 * @return
	 */
	public double getUmax () {
		double ret = 0.0;
		
		for (int i = 0; i < getLevels(); i++) {
			double uL = 0.0;
			for (Vertex a : getVertices()) {
				uL += a.getWcet(i);
			}
			uL = uL / getDeadline();
			
			if (uL > ret)
				ret = uL;
		}
		
		return ret;
	}
	
	/**
	 * Returns the utilization value for the DAG at level i
	 * @param i
	 * @return
	 */
	public double getUi (int i) {
		double ret = 0;
		
		for (Vertex a : getVertices())
			ret += a.getWcet(i);
		
		ret = ret / getDeadline();
		
		return ret;
	}
	
	/**
	 * Returns the number of minimal cores required to schedule the DAG
	 * @return
	 */
	public int getMinCores() {
		int ret = 0;
		
		ret = (int) ((this.getUHI() > this.getULO()) ? Math.ceil(getUHI()) : Math.ceil(getULO()));
		
		return ret;
	}
	
	/*
	 * Getters & Setters
	 * 
	 */
	public Set<Vertex> getVertices() {
		return nodes;
	}
	public void setNodes(Set<Vertex> Nodes) {
		nodes = Nodes;
	}
	
	public Vertex getNodebyID(int id){
		Iterator<Vertex> it = nodes.iterator();
		while(it.hasNext()){
			Vertex n = it.next();
			if (n.getId() == id)
				return n; 
		}
		return null;
	}

	public Vertex getNodebyName(String name){
		Iterator<Vertex> it = nodes.iterator();
		while(it.hasNext()){
			Vertex n = it.next();
			if (n.getName().equalsIgnoreCase(name))
				return n; 
		}
		return null;
	}

	
	public Set<Vertex> getNodes_HI() {
		return nodesHI;
	}

	public void setNodes_HI(Set<Vertex> nodes_HI) {
		nodesHI = nodes_HI;
	}
	
	public Vertex getNodeHIbyID(int id){
		Iterator<Vertex> it = nodesHI.iterator();
		while(it.hasNext()){
			Vertex n = it.next();
			if (n.getId() == id)
				return n; 
		}
		return null;
	}

	public int getCritPath() {
		return critPath;
	}

	public void setCritPath(int critPath) {
		this.critPath = critPath;
	}

	public Set<Vertex> getLoOuts() {
		return loOuts;
	}

	public void setLoOuts(Set<Vertex> lO_outs) {
		loOuts = lO_outs;
	}

	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Vertex> getOuts() {
		return Outs;
	}

	public void setOuts(Set<Vertex> outs) {
		Outs = outs;
	}

	public int getLevels() {
		return levels;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}
}
