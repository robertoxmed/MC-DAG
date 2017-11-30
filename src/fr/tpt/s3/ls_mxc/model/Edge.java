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
	private Actor src;
	private Actor dest;

	
	/**
	 * Constructor for the Edge
	 */
	
	public Edge(Actor s, Actor d) {
		this.setSrc(s);
		this.setDest(d);
		s.getSndEdges().add(this);
		d.getRcvEdges().add(this);
	}

	/*
	 * Getters & Setters	
	 * 
	 */
	public Actor getSrc() {
		return src;
	}
	public void setSrc(Actor s) {
		this.src = s;
	}
	public Actor getDest() {
		return dest;
	}
	public void setDest(Actor dest) {
		this.dest = dest;
	}
}
