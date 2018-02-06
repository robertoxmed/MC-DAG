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
package fr.tpt.s3.ls_mxc.avail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.ls_mxc.alloc.SingleDAG;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.ActorAvail;
import fr.tpt.s3.ls_mxc.model.ActorSched;

public class Automata {

	private int nbStates;
	private List<State> loSched;
	private List<State> hiSched;
	private List<Transition> loTrans;
	private List<Transition> finTrans;
	private List<Transition> hiTrans;
	private List<FTM> ftms;
	private Set<Formula> loOutsForm;
	
	private SingleDAG ls;
	private DAG d;

	/**
	 *  Constructor of the Automata, needs the LO, HI tables,
	 *  the DAG with the data dependencies, deadline and number of cores
	 */
	public Automata (SingleDAG ls, DAG d) {
		this.setD(d);
		this.setLs(ls);
		this.loSched = new LinkedList<State>();
		this.hiSched = new LinkedList<State>();
		this.loTrans = new LinkedList<Transition>();
		this.setF_transitions(new LinkedList<Transition>());
		this.hiTrans = new LinkedList<Transition>();
		this.ftms = new LinkedList<FTM>();
		this.loOutsForm = new HashSet<Formula>();
	}
	
	/**
	 * Automata functions (creation of states + linking)
	 */
	
	// Calculate completion time of tasks and create a new state
	public void calcCompTimeLO (String task) {
		int c_t = 0;
		for (int i = 0; i < ls.getDeadline(); i++){
			for (int j = 0; j < ls.getNbCores(); j++) {
				if (ls.getSched()[0][i] != null && ls.getSched()[0][i][j] != null) {
					if (ls.getSched()[0][i][j].contentEquals(task))
						c_t = i;
				}
			}
		}

		Actor n = d.getNodebyName(task);
		State s;
		if (n.getCI(1) !=  0) {
			s = new State(nbStates++, task, ActorSched.HI);
			if (((ActorAvail) n).isfMechanism()) { // Test if it's a fault tolerant mechanism
				s.setfMechanism(true);
				if (((ActorAvail) n).getfMechType() == ActorAvail.VOTER) {
					FTM ftm = new FTM(3, n.getName());
					ftm.setNbVot(((ActorAvail) n).getNbReplicas());
					ftm.setVotTask((ActorSched) d.getNodebyName(((ActorAvail) n).getVotTask()));
					ftm.setType(ActorAvail.VOTER);
					ftm.createVoter();
					ftms.add(ftm);
				}
			}
		} else {
			s = new State(nbStates++, task, ActorSched.LO);
			
			if (((ActorAvail) n).isVoted())
				s.setVoted(true);
			if (((ActorAvail) n).getfMechType() == ActorAvail.MKFIRM) {
				FTM ftm = null;
				s.setfMechanism(true);
				ftm = new FTM(((ActorAvail) n).getM(), ((ActorAvail) n).getK(), n.getName());
				ftm.setVotTask((ActorSched) n);
				ftm.setType(ActorAvail.MKFIRM);				
				ftm.createMKFirm();
				ftms.add(ftm);
			}
		}
		s.setCompTime(c_t);
		addWithTime(loSched, (ActorAvail) n, s, c_t);
	}
	
	// Calculate completion time of tasks and create a new state HI mode
	public void calcCompTimeHI (String task) {
		int c_t = 0;
		for (int i = 0; i < ls.getDeadline(); i++){
			for (int j = 0; j < ls.getNbCores(); j++) {
				if (ls.getSched()[1][i][j].contentEquals(task))
					c_t = i;
			}
		}

		ActorSched n = (ActorSched) d.getNodebyName(task);
		State s;
		s = new State(nbStates++, task, ActorSched.HI);
		s.setCompTime(c_t);

		addWithTime(hiSched, (ActorAvail) n, s, c_t);
	}
	
	/**
	 * Procedure that adds the state to a list in the right order.
	 * @param l
	 * @param n
	 * @param s
	 * @param c_t
	 */
	public void addWithTime(List<State> l, ActorAvail n, State s, int c_t) {
		int idx = 0;
		Iterator<State> is = l.iterator();
		State s2 = null;
		
		// Iterate until the first task that has the same or a higher completion time
		while (is.hasNext()) {
			s2 = is.next();
			if (s2.getCompTime() >= c_t)
				break;
			else
				idx++;
		}
		
		// Breaking ties, LO exit nodes first
		// then HI tasks, then LO tasks
		if (s2 != null && s2.getCompTime() == c_t) {
			
			if (n.getCI(1) != 0) {	
				int cur_ct = c_t;
				while (is.hasNext() && cur_ct == c_t) {
					s2 = is.next();
					cur_ct = s2.getCompTime();
					if (s2.getMode() == ActorSched.HI)
						idx++;
				}
			} else if (n.getCI(1) == 0) {
				int cur_ct = c_t;
				while (is.hasNext() && cur_ct == c_t) {
					s2 = is.next();
					cur_ct = s2.getCompTime();
					idx++;
				}
			}
		}
		l.add(idx,s);
		if (n.isfMechanism() && n.getfMechType() == ActorAvail.MKFIRM) {
			State s0 = new State(nbStates++, n.getName(), ActorSched.LO);
			s0.setCompTime(c_t);
			s0.setSynched(true);
			l.add(idx+1, s0);
		}
		// If it is an exit LO node
		if (n.getCI(1) == 0 && n.getSndEdges().size() == 0) {
			State s0 = new State(nbStates++, n.getName(), ActorSched.LO);
			s0.setCompTime(c_t);
			s0.setExit(true);
			l.add(idx+1, s0);
		}
	}
	
	/**
	 * Finds the corresponding state of a LO task in LO automata zone
	 * @param task
	 * @return
	 */
	public State findStateLO(String task) {
		State ret = null;
		boolean found = false;
		
		Iterator<State> it = loSched.iterator();
		while (it.hasNext() && !found) {
			State s = it.next();
			if (s.getTask().contentEquals(task)) {
				ret = s;
				found = true;
			}
		}
		
		return ret;
	}
	
	/**
	 * Finds the corresponding state of a HI task in HI automata zone
	 * @param task
	 * @return
	 */
	public State findStateHI(String task) {
		State ret = null;
		boolean found = false;
		
		Iterator<State> it = hiSched.iterator();
		while (it.hasNext() && !found) {
			State s = it.next();
			if (s.getTask().contentEquals(task)) {
				ret = s;
				found = true;
			}
		}
		return ret;
	}
	
	/**
	 * Procedure that calculates all the sets of booleans
	 * for each output formula in the DAG
	 */
	public void calcOutputSets() {
		Iterator<Actor> in = d.getLoOuts().iterator();
		while (in.hasNext()) {
			ActorSched n = (ActorSched) in.next();
			
			// Create the Formula
			LinkedList<AutoBoolean> bSet = new LinkedList<AutoBoolean>();
			Formula f = new Formula(n.getName(), bSet);
			
			Set<Actor> nPred = n.getLOPred();
			
			// Create the boolean set for the LO output
			AutoBoolean a = new AutoBoolean(n.getName(), n.getName());
			bSet.add(a);
			Iterator<Actor> in2 = nPred.iterator();
			while (in2.hasNext()) {
				ActorSched n2 = (ActorSched) in2.next();
				AutoBoolean ab = new AutoBoolean(n2.getName(), n.getName());
				bSet.add(ab);
			}
			loOutsForm.add(f);
		}
	}
	
	public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
	    Set<Set<T>> sets = new HashSet<Set<T>>();
	    if (originalSet.isEmpty()) {
	        sets.add(new HashSet<T>());
	        return sets;
	    }
	    List<T> list = new ArrayList<T>(originalSet);
	    T head = list.get(0);
	    Set<T> rest = new HashSet<T>(list.subList(1, list.size())); 
	    for (Set<T> set : powerSet(rest)) {
	        Set<T> newSet = new HashSet<T>();
	        newSet.add(head);
	        newSet.addAll(set);
	        sets.add(newSet);
	        sets.add(set);
	    }       
	    return sets;
	}  
	
	
	/**
	 * Procedure links the states by creating Transitions objects
	 * after the scheduling lists were created.
	 */
	public void linkStates() {
		
		Iterator<State> it = hiSched.iterator();
		Iterator<State> it2 = hiSched.iterator();

		// Construct the HI zone of the automata
		State s2 = it2.next(); 
		while (it2.hasNext()) {
			State s = it.next();
			if (it2.hasNext()) {
				s2 = it2.next();
				Transition t = new Transition(s, s2, null);
				getH_transitions().add(t);
			}
		}
				
		// Construct the LO zone of the automata
		State sk = new State(nbStates++, "FinalLO", 0);
		loSched.add(sk);
		it = loSched.iterator();
		it2 = loSched.iterator();
		s2 = it2.next();
		while (it2.hasNext()) {
			State s = it.next();
			if (it2.hasNext()) {
				s2 = it2.next();
				Transition t;
				if (s.getMode() == ActorSched.HI) { // If it's a HI task
					// Find the HI task that corresponds to s
					State S = hiSched.get(0);
					t = new Transition(s, s2, S);
					if(!s.isfMechanism()) // If it's not a fault tolerant mechanism
						t.setP(((ActorSched) d.getNodebyName(s.getTask())).getfProb());
				} else { // It is a LO task
					if (s.isVoted()) {
						t = new Transition(s,s2, s2);
						t.setP(((ActorSched) d.getNodebyName(s.getTask())).getfProb());
					} else if (s.isSynched()) {
						t = new Transition(s, s2, s2);
					} else {
						t = new Transition(s, s2, s2);
						if (s.getCompTime() != 0)
							t.setP(((ActorSched) d.getNodebyName(s.getTask())).getfProb());
					}
				}
				getL_transitions().add(t);
			}
		}
		
		// Add final transition in HI mode (recovery mechanism)
		State s0 = loSched.get(0);
		State Sf = hiSched.get(hiSched.size() - 1);
		Transition t = new Transition(Sf, s0, null);
		getH_transitions().add(t);
		
		// Add final transitions in LO mode
		// We need to add 2^n transitions depending on the number of outputs
		calcOutputSets();
		
		Transition tfinal = new Transition(sk, s0, s0);
		finTrans.add(tfinal);
	
	}

	/**
	 *  This procedures creates the automata for the PRISM model
	 */
	public void createAutomata () {
		
		// Calculate completion times for all nodes in LO and HI mode		
		State s0 = new State(nbStates++, "Init", 0);
		s0.setCompTime(0);
		loSched.add(s0);
		
		Iterator<Actor> in = d.getNodes().iterator();
		while (in.hasNext()) {
			ActorSched n = (ActorSched) in.next();
			this.calcCompTimeLO(n.getName());
		}
		
		in = d.getNodes_HI().iterator();
//		while (in.hasNext()) {
//			Actor n = in.next();
//			this.calcCompTimeHI(n.getName());
//		}
		State sH = new State(nbStates++, "SHI", 0);
		hiSched.add(sH);
				
		this.linkStates();
	}
	
	/**
	 * Print functions
	 */
	public void printLOList() {
		Iterator<State> it = loSched.iterator();
		while (it.hasNext()){
			System.out.print(it.next().getTask()+ " ");
		}		
		System.out.println(" ");
	}
	
	public void printHIList() {
		Iterator<State> it = hiSched.iterator();
		while (it.hasNext()){
			System.out.print(it.next().getTask()+ " ");
		}
		System.out.println(" ");
	}
	
	/**
	 * Getters and setters
	 */
	
	public FTM getFTMbyName (String name) {
		FTM ret = null;
		boolean found = false;
		Iterator<FTM> iftm = getFtms().iterator();
		
		while (iftm.hasNext() && !found) {
			FTM f = iftm.next();
			if (f.getName().contains(name))
				ret = f;
		}
		return ret;
	}

	public List<State> getLo_sched() {
		return loSched;
	}

	public void setLo_sched(List<State> lo_sched) {
		this.loSched = lo_sched;
	}

	public List<State> getHi_sched() {
		return hiSched;
	}

	public void setHi_sched(List<State> hi_sched) {
		this.hiSched = hi_sched;
	}

	public List<Transition> getL_transitions() {
		return loTrans;
	}

	public void setL_transitions(List<Transition> l_transitions) {
		this.loTrans = l_transitions;
	}

	public List<Transition> getH_transitions() {
		return hiTrans;
	}

	public void setH_transitions(List<Transition> h_transitions) {
		this.hiTrans = h_transitions;
	}

	public DAG getD() {
		return d;
	}

	public void setD(DAG d) {
		this.d = d;
	}

	public SingleDAG getLs() {
		return ls;
	}

	public void setLs(SingleDAG ls) {
		this.ls = ls;
	}

	public Set<Formula> getL_outs_b() {
		return loOutsForm;
	}

	public void setL_outs_b(Set<Formula> l_outs_b) {
		this.loOutsForm = l_outs_b;
	}

	public List<Transition> getF_transitions() {
		return finTrans;
	}

	public void setF_transitions(List<Transition> f_transitions) {
		this.finTrans = f_transitions;
	}

	public List<FTM> getFtms() {
		return ftms;
	}

	public void setFtms(List<FTM> ftms) {
		this.ftms = ftms;
	}
	public int getNbStates() {
		return nbStates;
	}

	public void setNbStates(int nbStates) {
		this.nbStates = nbStates;
	}
}
