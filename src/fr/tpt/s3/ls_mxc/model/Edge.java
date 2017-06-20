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

/**
 * 
 * @author roberto
 *
 */
public class Edge {
	
	// One edge has a src and a destination
	private Node src;
	private Node dest;
	
	private boolean visited;
	
	// To include cost in further implementations
	// private int cost;
	
	/**
	 * Constructor for the Edge
	 */
	public Edge(Node s, Node d, boolean v){
		this.setSrc(s);
		this.setDest(d);
		this.setVisited(v);
	}
	
	/**
	 * Getters & Setters	
	 * 
	 */
	public Node getSrc() {
		return src;
	}
	public void setSrc(Node src) {
		this.src = src;
	}
	public Node getDest() {
		return dest;
	}
	public void setDest(Node dest) {
		this.dest = dest;
	}
	public boolean isVisited() {
		return visited;
	}
	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	

}
