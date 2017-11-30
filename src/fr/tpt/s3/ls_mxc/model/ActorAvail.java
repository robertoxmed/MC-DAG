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

public class ActorAvail extends ActorSched {

	public static final short VOTER = 2;
	public static final short MKFIRM = 3;
	
	// Used for DAG availability analysis
	private String votTask;
	private boolean fMechanism;
	private short fMechType;
	private boolean isVoted;
	private int nbReplicas;
	private int M;
	private int K;
	
	public ActorAvail (int id, String name, int cLO, int cHI) {
		super(id, name, cLO, cHI);
	}
	
	/*
	 * Getters and Setters
	 */

	public String getVotTask() {
		return votTask;
	}
	public void setVotTask(String votTask) {
		this.votTask = votTask;
	}
	public boolean isfMechanism() {
		return fMechanism;
	}
	public void setfMechanism(boolean fMechanism) {
		this.fMechanism = fMechanism;
	}
	public short getfMechType() {
		return fMechType;
	}
	public void setfMechType(short fMechType) {
		this.fMechType = fMechType;
	}
	public boolean isVoted() {
		return isVoted;
	}
	public void setVoted(boolean isVoted) {
		this.isVoted = isVoted;
	}
	public int getNbReplicas() {
		return nbReplicas;
	}
	public void setNbReplicas(int nbReplicas) {
		this.nbReplicas = nbReplicas;
	}
	public int getM() {
		return M;
	}
	public void setM(int m) {
		M = m;
	}
	public int getK() {
		return K;
	}
	public void setK(int k) {
		K = k;
	}
}
