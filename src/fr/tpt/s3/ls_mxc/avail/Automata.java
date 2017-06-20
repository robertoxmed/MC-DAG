package fr.tpt.s3.ls_mxc.avail;

import java.util.Iterator;
import java.util.List;
import fr.tpt.s3.ls_mxc.model.Node;

public class Automata {

	private int nb_tasks;
	private List<Double> f_prob;
	private int[] lo_rtime;
	private List<State> lo_sched;
	private List<State> hi_sched;
	private List<Transition> l_transitions;
	private List<Transition> h_transitions;
	
	
	// This procedures creates the automata
	public void createAutomata () {
		System.out.println("module proc");
		System.out.println("\t s : [0..50] init 0");
		
		// Create all necessary booleans
		Iterator<State> is = lo_sched.iterator();
		while (is.hasNext()) {
			State s = is.next();
			if (s.getMode() == 0) // It is a LO task
				System.out.println("\t"+s.getId()+": bool init false;\n");
		}
		
		
		// Create the LO scheduling zone
		Iterator<Transition> it = l_transitions.iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			System.out.println("\t["+t.getSrc().getTask()+"_lo] s = " + t.getSrc().getId()
					+ " -> 1 - "+ t.getP() +": (s' = " + t.getDestOk().getId() + ") +"
					+ t.getP() + ": (s' =" + t.getDestFail().getId() +");\n");
			
		}

		
		// Create the HI scheduling zone
		// Need to iterate through transitions
		it = h_transitions.iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			System.out.println("["+t.getSrc().getTask()+"_hi] s = " + t.getSrc().getId() + " -> (s' =" + t.getDestOk().getId() +");\n");
			
		}
		

		
		System.out.println("end module");
	}
}
