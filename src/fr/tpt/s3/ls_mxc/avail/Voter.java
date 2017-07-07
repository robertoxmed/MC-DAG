package fr.tpt.s3.ls_mxc.avail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Voter {
	// Nb of voters need to be odd to have a majority
	private int nbVot;
	private String name;
	private List<State> states;
	private List<Transition> transitions;
	private List<Transition> f_trans;

	public Voter (int nb_vot, String name) {
		this.nbVot = nb_vot;
		this.name = name;
		this.states = new LinkedList<State>();
		this.transitions = new LinkedList<Transition>();
		this.f_trans = new LinkedList<Transition>();
	}
	
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
					State src = this.states.get(prev);
					State s2 = new State(cur++, name, 0);
					State s3 = new State(cur++, name, 0);
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
		
		for (int i = 1; i <= nb_pred; i++) {
			int count = 0;
			Transition t = null;
			State src = this.getStates().get(end);
			// Mark
			if (i > (nb_pred/2)) {
				// Voter failed
				if (count != (nb_pred/2) + 1){
					t = new Transition(src, this.getStates().get(0), null);
					t.setName(name+"_ok");
				} else { // Voter suceeded
					t = new Transition(src, this.getStates().get(0), null);
					t.setName(name+"_failed");
				}

			} else {
				// Voter failed
				if (count == (nb_pred/2) - 1){
					t = new Transition(src, this.getStates().get(0), null);
					t.setName(name+"_ok");
				} else { // Voter suceeded
					t = new Transition(src, this.getStates().get(0), null);
					t.setName(name+"_failed");
				}
			}
			f_trans.add(t);
			count++;
			end--;
		}
		
	}
	
	
	public void printVoter () {
		System.out.println("module voter");
		System.out.println("\tv: [0..20] init 0;");
		Iterator<Transition> it = this.transitions.iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			System.out.println("\t["+t.getDestOk().getTask()+"_ok] v = "+t.getSrc().getId()+" -> "+t.getDestOk().getId());
			System.out.println("\t["+t.getDestOk().getTask()+"_fail] v = "+t.getSrc().getId()+" -> "+t.getDestFail().getId());
			System.out.println("");
		}
		
		it = this.f_trans.iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			System.out.println("\t["+t.getName()+"] v = "+t.getSrc().getId()+" -> "+t.getDestOk().getId());
		}
		System.out.println("");
		System.out.println("endmodule");
	}
	
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

	public List<Transition> getF_trans() {
		return f_trans;
	}

	public void setF_trans(List<Transition> f_trans) {
		this.f_trans = f_trans;
	}
}
