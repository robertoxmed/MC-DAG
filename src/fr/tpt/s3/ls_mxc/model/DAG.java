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

import fr.tpt.s3.ls_mxc.alloc.LS;

/**
 * Class to model the DAG in MxC
 * @author Roberto Medina
 *
 */
public class DAG {
	
	private Set<Actor> Nodes;
	private Set<Actor> Nodes_HI;
	private Set<Actor> LO_outs;
	private int critPath;
	
	public DAG() {
		Nodes = new HashSet<Actor>();
		Nodes_HI = new HashSet<Actor>();
		setLO_outs(new HashSet<Actor>());
	}
	
	
	/**
	 * Method to get the critical Path.
	 * Needs to be called after getting HLFET levels.
	 */
	public int calcCriticalPath() {
		int cp = 0;
		
		LS ls = new LS();
		ls.setMxcDag(this);
		ls.calcWeights(0);
		ls.calcWeights(1);
		
		for(int i = 0; i < this.getNodes().size(); i++) {
			if (cp < this.getNodebyID(i).getWeight_LO())
				cp = this.getNodebyID(i).getWeight_LO();
			if (cp < this.getNodebyID(i).getWeight_HI())
				cp = this.getNodebyID(i).getWeight_HI();
		}
		this.setCritPath(cp);
		return cp;
	}
	
	/**
	 * Sets HI nodes in the corresponding set
	 */
	public void setHINodes() {
		Iterator<Actor> in = this.getNodes().iterator();
		while (in.hasNext()) {
			Actor n = in.next();
			if (n.getC_HI() != 0)
				this.getNodes_HI().add(n);
		}
	}
	
	/**
	 * Searches for the LO outputs in the DAG
	 */
	public void calcLOouts() {
		Iterator<Actor> in = this.getNodes().iterator();
		while (in.hasNext()) {
			Actor n = in.next();
			if (n.getSnd_edges().size() == 0 &&
					n.getC_HI() == 0) {
				this.getLO_outs().add(n);
			}
		}
	}
	
	/**
	 * Getters & Setters
	 * 
	 */
	public Set<Actor> getNodes() {
		return Nodes;
	}
	public void setNodes(Set<Actor> nodes) {
		Nodes = nodes;
	}
	
	public Actor getNodebyID(int id){
		Iterator<Actor> it = Nodes.iterator();
		while(it.hasNext()){
			Actor n = it.next();
			if (n.getId() == id)
				return n; 
		}
		return null;
	}

	public Actor getNodebyName(String name){
		Iterator<Actor> it = Nodes.iterator();
		while(it.hasNext()){
			Actor n = it.next();
			if (n.getName().equalsIgnoreCase(name))
				return n; 
		}
		return null;
	}

	
	public Set<Actor> getNodes_HI() {
		return Nodes_HI;
	}

	public void setNodes_HI(Set<Actor> nodes_HI) {
		Nodes_HI = nodes_HI;
	}
	
	public Actor getNodeHIbyID(int id){
		Iterator<Actor> it = Nodes_HI.iterator();
		while(it.hasNext()){
			Actor n = it.next();
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

	public Set<Actor> getLO_outs() {
		return LO_outs;
	}

	public void setLO_outs(Set<Actor> lO_outs) {
		LO_outs = lO_outs;
	}

	
}
