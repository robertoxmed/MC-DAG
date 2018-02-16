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

/**
 * Class to model the DAG in MxC
 * @author Roberto Medina
 *
 */
public class DAG {
	
	private int id;
	private Set<Actor> nodes;
	private Set<Actor> nodesHI;
	private Set<Actor> loOuts;
	private Set<Actor> Outs;
	private Set<Actor> sinks;
	private Set<Actor> sinksHI;
	private Set<Actor> sourcesHI;
	private int critPath;
	private int deadline;
	private int levels;
	
	public DAG() {
		nodes = new HashSet<Actor>();
		nodesHI = new HashSet<Actor>();
		setLoOuts(new HashSet<Actor>());
		sinks = new HashSet<Actor>();
		sinksHI = new HashSet<Actor>();
		sourcesHI = new HashSet<Actor>();
	}
	
	/**
	 * Method to set all flags once the DAG has been instantiated
	 * and initialized
	 */
	public void sanityChecks () {
		this.setHINodes();
		this.calcLOouts();
		Iterator<Actor> in = this.getNodes().iterator();
		
		while (in.hasNext()){
			Actor n = in.next();
			
			n.checkifSink();
			n.checkifSinkinHI();
			n.checkifSource();
			n.checkifSourceHI();
			
			if (n.isSink())
				getSinks().add(n);
			if (n.isSinkHI())
				getSinksHI().add(n);
			if (n.isSourceHI())
				getSourcesHI().add(n);
		}
	}
	
	/**
	 * Method to get the critical Path.
	 * Needs to be called after getting HLFET levels.
	 */
	public int calcCriticalPath() {
		int cp = 0;
		
		for(int i = 0; i < this.getNodes().size(); i++) {
			if (cp < this.getNodebyID(i).getCpFromNodeLO())
				cp = this.getNodebyID(i).getCpFromNodeLO();
			if (cp < this.getNodebyID(i).getCpFromNodeHI())
				cp = this.getNodebyID(i).getCpFromNodeHI();
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
			if (n.getCI(1) != 0)
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
			if (n.getSndEdges().size() == 0 &&
					n.getCI(1) == 0) {
				this.getLoOuts().add(n);
			}
		}
	}
	
	/**
	 * Gets the outputs of the DAG
	 */
	public void calcOuts() {
		for (Actor a : getNodes()) {
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
		
		for (Actor a : getNodes())
			ret += a.getCI(0);
		
		return ret / getDeadline();
	}
	
	/**
	 * Returns the utilization in the HI mode
	 * @return
	 */
	public double getUHI () {
		double ret = 0.0;
		
		for (Actor a : getNodes()) {
			if (a.getCI(1) != 0)
				ret += a.getCI(1);
		}
		return ret / getDeadline();
	}
	
	/**
	 * Returns the maximum utilization of the DAG
	 * @return
	 */
	public double getU () {
		double ret = 0.0;
		
		for (int i = 0; i < getLevels(); i++) {
			double uL = 0.0;
			for (Actor a : getNodes()) {
				uL += a.getCI(i);
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
		
		for (Actor a : getNodes())
			ret += a.getCI(i);
		
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
	public Set<Actor> getNodes() {
		return nodes;
	}
	public void setNodes(Set<Actor> Nodes) {
		nodes = Nodes;
	}
	
	public Actor getNodebyID(int id){
		Iterator<Actor> it = nodes.iterator();
		while(it.hasNext()){
			Actor n = it.next();
			if (n.getId() == id)
				return n; 
		}
		return null;
	}

	public Actor getNodebyName(String name){
		Iterator<Actor> it = nodes.iterator();
		while(it.hasNext()){
			Actor n = it.next();
			if (n.getName().equalsIgnoreCase(name))
				return n; 
		}
		return null;
	}

	
	public Set<Actor> getNodes_HI() {
		return nodesHI;
	}

	public void setNodes_HI(Set<Actor> nodes_HI) {
		nodesHI = nodes_HI;
	}
	
	public Actor getNodeHIbyID(int id){
		Iterator<Actor> it = nodesHI.iterator();
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

	public Set<Actor> getLoOuts() {
		return loOuts;
	}

	public void setLoOuts(Set<Actor> lO_outs) {
		loOuts = lO_outs;
	}

	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}

	public Set<Actor> getSinks() {
		return sinks;
	}

	public void setSinks(Set<Actor> sinks) {
		this.sinks = sinks;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Actor> getOuts() {
		return Outs;
	}

	public void setOuts(Set<Actor> outs) {
		Outs = outs;
	}

	public Set<Actor> getSinksHI() {
		return sinksHI;
	}

	public void setSinksHI(Set<Actor> sinksHI) {
		this.sinksHI = sinksHI;
	}

	public Set<Actor> getSourcesHI() {
		return sourcesHI;
	}

	public void setSourcesHI(Set<Actor> sourcesHI) {
		this.sourcesHI = sourcesHI;
	}

	public int getLevels() {
		return levels;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}
}
