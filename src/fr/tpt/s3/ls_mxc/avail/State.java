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

public class State {
	
	private String name;
	private int mode;
	private int id;
	private int compTime;
	private boolean fMechanism;
	
	public State (int id, String t, int m) {
		setId(id);
		setTask(t);
		setMode(m);
		t = null;
	}
	
	public String getTask() {
		return name;
	}
	public void setTask(String task) {
		this.name = task;
	}
	public int getMode() {
		return mode;
	}
	public void setMode(int mode) {
		this.mode = mode;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public int getCompTime() {
		return compTime;
	}

	public void setCompTime(int c_t) {
		this.compTime = c_t;
	}

	public boolean isfMechanism() {
		return fMechanism;
	}

	public void setfMechanism(boolean fMechanism) {
		this.fMechanism = fMechanism;
	}
}
