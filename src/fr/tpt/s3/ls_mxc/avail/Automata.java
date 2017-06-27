package fr.tpt.s3.ls_mxc.avail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Node;

public class Automata {

	private int deadline;
	private int nb_cores;
	private int nb_states;
	private List<Double> f_prob;
	private int[] lo_rtime;
	private List<State> lo_sched;
	private List<State> hi_sched;
	private List<Transition> l_transitions;
	private List<Transition> h_transitions;

	private String S_LO[][];
	private String S_HI[][];
	private DAG d;

	/**
	 *  Constructor of the Automata, needs the LO, HI tables,
	 *  the DAG with the data dependencies, deadline and number of cores
	 */
	public Automata (String S_LO[][], String S_HI[][], DAG d, int ded, int nb_cores) {
		this.deadline = ded;
		this.nb_cores = nb_cores;
		this.setS_LO(S_LO);
		this.setS_HI(S_HI);
		this.setD(d);
		this.lo_sched = new LinkedList<State>();
		this.hi_sched = new LinkedList<State>();
	}
	
	/**
	 * Automata functions (creation of states + linking)
	 */
	
	// Calculate completion time of tasks and create a new state
	public void calcCompTimeLO (String task) {
		int c_t = 0;
		for (int i = 0; i < deadline; i++){
			for (int j = 0; j < nb_cores; j++) {
				if (S_LO[i][j].contentEquals(task))
					c_t = i;
			}
		}

		Node n = d.getNodebyName(task);
		State s;

		s = new State(nb_states++, task, 0);
		s.setC_t(c_t);
	
		addWithTime(lo_sched, n, s, c_t);
	}
	
	// Calculate completion time of tasks and create a new state HI mode
	public void calcCompTimeHI (String task) {
		int c_t = 0;
		for (int i = 0; i < deadline; i++){
			for (int j = 0; j < nb_cores; j++) {
				if (S_HI[i][j].contentEquals(task))
					c_t = i;
			}
		}

		Node n = d.getNodebyName(task);
		State s;
		s = new State(nb_states++, task, 1);
		s.setC_t(c_t);

		addWithTime(hi_sched, n, s, c_t);
	}
	
	/**
	 * Procedure that adds the state to a list in the right order.
	 * @param l
	 * @param n
	 * @param s
	 * @param c_t
	 */
	public void addWithTime(List<State> l, Node n, State s, int c_t) {
		int idx = 0;
		Iterator<State> is = l.iterator();
		State s2;
		
		// Iterate until the first task that has the same or a higher completion time
		while (is.hasNext()) {
			s2 = is.next();
			if (s2.getC_t() < c_t)
				idx++;
			else
				break;
		}
		
		// Breaking ties, LO exit nodes first
		// then HI tasks, then LO tasks
		if (n.isExitNode() && n.getC_HI() == 0) {
			l.add(idx, s);
		} else if (n.getC_HI() != 0) {
			int cur_ct = c_t;
			while (is.hasNext() && cur_ct == c_t) {
				s2 = is.next();
				cur_ct = s2.getC_t();
				if (s2.getMode() == 1)
					idx++;
			}
			l.add(idx, s);
		} else {
			int cur_ct = c_t;
			while (is.hasNext() && cur_ct == c_t) {
				s2 = is.next();
				cur_ct = s2.getC_t();
				idx++;
			}
			l.add(idx, s);
		}
		
	}
	
	/**
	 * Procedure links the states by creating Transitions objects
	 * after the scheduling lists were created.
	 */
	public void linkStates() {
		
		Iterator<State> it = hi_sched.iterator();
		Iterator<State> it2 = hi_sched.iterator();

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
		
		
		it = lo_sched.iterator();
		it2 = lo_sched.iterator();
		s2 = it2.next();
		while (it2.hasNext()) {
			State s = it.next();
			if (it2.hasNext()) {
				s2 = it2.next();
				Transition t;
				if (s.getMode() == 1) { // If it's a HI task
					// Find the HI task that corresponds to s
					State S = findStateHI(s.getTask());
					t = new Transition(s, s2, S);
					t.setP(d.getNodebyName(s.getTask()).getfProb());
				} else { // It is a LO task
					t = new Transition(s, s2, s2);
					t.setP(d.getNodebyName(s.getTask()).getfProb());
				}
				getL_transitions().add(t);
			}
		}
		
		// Add final transition in HI mode (recovery mechanism)
		State s0 = lo_sched.get(0);
		State Sf = hi_sched.get(hi_sched.size() - 1);
		Transition t = new Transition(Sf, s0, null);
		getH_transitions().add(t);
		
		// Add final transitions in LO mode
		// We need to add 2^n transitions depending on the number of outputs
		
		
	}
	
	/**
	 * Finds the corresponding state of a HI task in HI automata zone
	 * @param task
	 * @return
	 */
	public State findStateHI(String task) {
		State ret = null;
		boolean found = false;
		
		Iterator<State> it = hi_sched.iterator();
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
	 *  This procedures prints the automata
	 */
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
	
	/**
	 * Print functions
	 */
	public void printLOList() {
		Iterator<State> it = lo_sched.iterator();
		while (it.hasNext()){
			System.out.print(it.next().getTask()+ " ");
		}		
		System.out.println(" ");
	}
	
	public void printHIList() {
		Iterator<State> it = hi_sched.iterator();
		while (it.hasNext()){
			System.out.print(it.next().getTask()+ " ");
		}
		System.out.println(" ");
	}
	
	/**
	 * Getters and setters
	 */
	
	public List<Double> getF_prob() {
		return f_prob;
	}

	public void setF_prob(List<Double> f_prob) {
		this.f_prob = f_prob;
	}

	public int[] getLo_rtime() {
		return lo_rtime;
	}

	public void setLo_rtime(int[] lo_rtime) {
		this.lo_rtime = lo_rtime;
	}

	public List<State> getLo_sched() {
		return lo_sched;
	}

	public void setLo_sched(List<State> lo_sched) {
		this.lo_sched = lo_sched;
	}

	public List<State> getHi_sched() {
		return hi_sched;
	}

	public void setHi_sched(List<State> hi_sched) {
		this.hi_sched = hi_sched;
	}

	public List<Transition> getL_transitions() {
		return l_transitions;
	}

	public void setL_transitions(List<Transition> l_transitions) {
		this.l_transitions = l_transitions;
	}

	public List<Transition> getH_transitions() {
		return h_transitions;
	}

	public void setH_transitions(List<Transition> h_transitions) {
		this.h_transitions = h_transitions;
	}

	public String[][] getS_LO() {
		return S_LO;
	}

	public void setS_LO(String[][] s_LO) {
		S_LO = s_LO;
	}

	public String[][] getS_HI() {
		return S_HI;
	}

	public void setS_HI(String[][] s_HI) {
		S_HI = s_HI;
	}

	public DAG getD() {
		return d;
	}

	public void setD(DAG d) {
		this.d = d;
	}



}
