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
package fr.tpt.s3.ls_mxc.avail;

import java.util.LinkedList;
import java.util.List;

public class Transition {
	
	private State src;
	private State destOk;
	private State destFail;
	private double p;
	private List<Formula> bSet;
	private List<Formula> fSet;
	private String Name;
	
	/**
	 * Constructor the Transitions object
	 */
	public Transition (State src, State destOk, State destFail) {
		this.setSrc(src);
		this.setDestOk(destOk);
		this.setDestFail(destFail);
		bSet = new LinkedList<Formula>();
		setfSet(new LinkedList<Formula>());
	}
	

	public State getSrc() {
		return src;
	}
	public void setSrc(State src) {
		this.src = src;
	}
	public State getDestOk() {
		return destOk;
	}
	public void setDestOk(State dest) {
		this.destOk = dest;
	}
	public State getDestFail() {
		return destFail;
	}
	public void setDestFail(State destFail) {
		this.destFail = destFail;
	}
	public double getP() {
		return p;
	}
	public void setP(double p) {
		this.p = p;
	}
	public List<Formula> getbSet() {
		return bSet;
	}
	public void setbSet(List<Formula> bSet) {
		this.bSet = bSet;
	}
	public void setName (String Name) {
		this.Name = Name;
	}
	public String getName() {
		return Name;
	}
	public List<Formula> getfSet() {
		return fSet;
	}


	public void setfSet(List<Formula> fSet) {
		this.fSet = fSet;
	}

}
