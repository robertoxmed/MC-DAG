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

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.tpt.s3.ls_mxc.model.ActorSched;

public class FTM {
	// Nb of voters need to be odd to have a majority
	private int nbVot;
	private String name;
	private ActorSched votTask;
	private List<State> states;
	private List<Transition> transitions;
	private List<Transition> finTrans;
	
	// Used to create a MK firm mechanism
	private int m;
	private int k;
	
	private short type;

	/**
	 * Consturctor for a Voter FTM.
	 * @param nb_vot
	 * @param name
	 */
	public FTM (int nb_vot, String name) {
		this.nbVot = nb_vot;
		this.name = name;
		this.states = new LinkedList<State>();
		this.transitions = new LinkedList<Transition>();
		this.finTrans = new LinkedList<Transition>();
	}
	
	/**
	 * Constructor for a M-K firm mechanism.
	 * @param m
	 * @param k
	 * @param name
	 */
	public FTM (int m, int k, String name) {
		this.setM(m);
		this.setK(k);
		this.name = name;
		this.states = new LinkedList<State>();
		this.transitions = new LinkedList<Transition>();
		this.finTrans = new LinkedList<Transition>();
	}
	
	/**
	 * Creates the voting automaton
	 */
	public void createVoter () {
		int cur = 0;
		int width = 1;
		int prev = 0;
		int nb_pred = 1;

		// Init state
		State s = new State(cur++, name, 0);
		this.states.add(s);
		
		for (int i = 0; i < nbVot; i++) {
			for (int j = 0; j < nb_pred; j++) {
				for (int k = 0; k < width; k++) {
					String votName = (votTask.getName()+i);
					
					State src = this.states.get(prev);
					State s2 = new State(cur++, votName, 0);
					State s3 = new State(cur++, votName, 0);
					this.states.add(s2);
					this.states.add(s3);
					Transition t = new Transition(src, s2, s3);
					this.transitions.add(t);
				}
				prev++;
			}
			nb_pred = nb_pred * 2;
		}
		
		int end = cur - 1;
		int count = nb_pred;

		for (int i = 0; i < nb_pred; i++) {
			Transition t = null;
			State src = this.getStates().get(end);
			// Mark
			if (count > (nb_pred/2)) {
				int test = (nb_pred/2) + 1;
				// Voter failed
				if (count == test){
					t = new Transition(src, this.getStates().get(0), null);
					t.setName(name+"_ok");
				} else { // Voter suceeded
					t = new Transition(src, this.getStates().get(0), null);
					t.setName(name+"_fail");
				}

			} else {
				int test = (nb_pred/2);
				if (count == test){
					t = new Transition(src, this.getStates().get(0), null);
					t.setName(name+"_fail");
				} else { 
					t = new Transition(src, this.getStates().get(0), null);
					t.setName(name+"_ok");
				}
			}
			finTrans.add(t);
			end--;
			count--;
		}
	}
	
	/**
	 * Finds a state with the id given as a paremeter.
	 * @param id
	 * @return
	 */
	private State getStateByID(int id) {
		for (State s : getStates()) {
			if (s.getId() == id)
				return s;
		}
		return null;
	}
	
	/**
	 * Converts an int to a BitSet.
	 * @param k
	 * @return
	 */
	private BitSet convert(int k) {
		int idx = 0;

		BitSet bs = new BitSet(this.getK()+1);
		
		while (k != 0) {
			if (k % 2 != 0) {
				bs.set(idx);
			}
			idx++;
			k = k >>> 1;
		}
		return bs;
	}
	
	/**
	 * Converts a BitSet into an int.
	 * @param bs
	 * @return
	 */
	private int convert(BitSet bs) {
		int ret = 0;
		for (int i = 0; i < getK(); i++) {
			ret += bs.get(i) ? (1 << i) : 0;
		}
		return ret;
	}
	
	private BitSet shiftLeft (BitSet bs) {
		BitSet ret = new BitSet(getK());
		for (int i = 0; i < getK() - 1; i++) {
			if (bs.get(i))
				ret.set(i+1);
		}
		return ret;
	}

	
	/**
	 * Creates a M-k firm automaton
	 */
	public void createMKFirm() {
		BitSet bs = null;
		
		// K is the depth of the buffer
		for (int i = 0; i < (int)Math.pow(2, getK()); i++) {
			State s = new State(i, getName(), ActorSched.HI);
			getStates().add(s);
		}
		
		// Add the transitions between the states
		for (State is : getStates()) {
			// Check what happens when the bit operator is applied
			// Shift to the right + 1
			int calc = 0;
			bs = convert(is.getId());
			BitSet shifted = new BitSet(getK());
			if (is.getId() != 0) {
				shifted = shiftLeft(bs);
			}
			shifted.set(0);
			calc = convert(shifted);
			
			State dst = getStateByID(calc);
			Transition t1 = new Transition(is, dst, null);
			t1.setName(getName()+"_end_ok");
			getTransitions().add(t1);
			
			// Shift to the right + 0
			shifted = bs;
			if (is.getId() != 0) {
				shifted = shiftLeft(bs);
			}
			calc = convert(shifted);
			
			dst = getStateByID(calc);
			Transition t0 = new Transition(is, dst, null);
			t0.setName(getName()+"_end_fail");
			getTransitions().add(t0);
		}
		
		// M number of bites that need to be 1
		for (State is : getStates()) {
			// Add final transitions
			bs = convert(is.getId());

			Transition tf = new Transition(is, is, null);
			
			if (bs.cardinality() >= getM())
				tf.setName(getName()+"_ok");
			else
				tf.setName(getName()+"_fail");
			
			getFinTrans().add(tf);
		}
	}
	
	/*
	 * Debuggin functions
	 */
	
	public void printVoter () {
		System.out.println("module voter");
		System.out.println("\tv: [0..20] init 0");
		Iterator<Transition> it = this.transitions.iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			System.out.println("\t["+t.getDestOk().getTask()+"_ok] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestOk().getId()+");");
			System.out.println("\t["+t.getDestOk().getTask()+"_fail] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestFail().getId()+");");
			System.out.println("");
		}
		
		it = this.finTrans.iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			System.out.println("\t["+t.getName()+"] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestOk().getId()+");");
		}
		System.out.println("");
		System.out.println("endmodule");
		System.out.println("");

	}
	
	public void printMKFirm ( ) {
		System.out.println("module "+getM()+"-"+getK()+"firm");
		System.out.println("\tv: [0.."+getStates().size()+"] init 0;");
		for (Transition t : getTransitions())
			System.out.println("\t["+t.getName()+"] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestOk().getId()+");");
		
		System.out.println("");
		for (Transition t : getFinTrans()) 
			System.out.println("\t["+t.getName()+"] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestOk().getId()+");");
	}
	
	/*
	 * Getters + Setters
	 */
	
	
	public int getNbVot() {
		return nbVot;
	}

	public void setNbVot(int nbVot) {
		this.nbVot = nbVot;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Transition> getTransitions() {
		return transitions;
	}

	public void setTransitions(List<Transition> transitions) {
		this.transitions = transitions;
	}

	public List<State> getStates() {
		return states;
	}

	public void setStates(List<State> states) {
		this.states = states;
	}

	public ActorSched getVotTask() {
		return votTask;
	}

	public void setVotTask(ActorSched votTask) {
		this.votTask = votTask;
	}
	public List<Transition> getFinTrans() {
		return finTrans;
	}

	public void setFinTrans(List<Transition> finTrans) {
		this.finTrans = finTrans;
	}

	public int getM() {
		return m;
	}

	public void setM(int m) {
		this.m = m;
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

}
