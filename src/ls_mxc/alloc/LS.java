package ls_mxc.alloc;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;


public class LS {
	
	// DAG to be scheduled
	private DAG mxc_dag;
	
	// Architecture, only nb cores atm
	private int nb_cores;
	private int deadline;
	
	// Weights to calculate HLFET levels
	private int weights_LO[];
	private int weights_HI[];
	
	// Scheduling tables, i: slot, j: task
	private String S_LO[][];
	private String S_HI[][];
	
	// Starting times of HI tasks in HI mode
	private int Start_HI[];
	
	//private List<Node> topo_Nodes;

	public LS(int dln, int cores, DAG d){
		this.setDeadline(dln);
		this.setNb_cores(cores);
		this.setMxcDag(d);
	}
	
	public LS() {}
	
	/**
	 * Calc weights for HLFET for both tables
	 */
	public void calcWeights(int mode) {
		
		weights_LO = new int[mxc_dag.getNodes().size()];
		weights_HI = new int[mxc_dag.getNodes().size()];
		
		Iterator<Node> it_n = mxc_dag.getNodes().iterator();
		while(it_n.hasNext()){
			Node n = it_n.next();
			if(mode == 0) { // LO mode
				weights_LO[n.getId()] = HLFET_level(n, mode);
			} else { // HI Mode
				weights_HI[n.getId()] = HLFET_level(n, mode);
			}
		}
	}
	
	/**
	 * Longest path from n to the sinks
	 * HLFET level
	 */
	public int HLFET_level(Node n, int mode) {
		
		int max = 0;
		
		// Final case the node is a sink
		if (n.isSink()){
			if (mode == 0) { // LO mode
				n.setWeight_LO(n.getC_LO());
				return n.getC_LO();
			} else {
				n.setWeight_HI(n.getC_HI());
				return n.getC_HI();
			}
		}
		
		// General case,  
		int[] tmp_max = new int[n.getSnd_edges().size()];
		
		Iterator<Edge> ie = n.getSnd_edges().iterator();
		int i = 0;
		while (ie.hasNext()) {
			
			Edge e = ie.next();
			tmp_max[i] = HLFET_level(e.getDest(), mode); 
			i++;
		}
		
		for(int j = 0; j < i; j++) {
			if (max < tmp_max[j])
				max = tmp_max[j];
		}
		
		if (mode == 0) { // LO mode
			n.setWeight_LO(max + n.getC_LO());
			return max + n.getC_LO();
		} else {
			n.setWeight_HI(max + n.getC_HI());
			return max + n.getC_HI();
		}
	}
	
	/**
	 * Creates the S_HI table 
	 */
	public void Alloc_HI(){
		
		/* =============================================
		 *  Initialization of variables used by the class
		 ================================================*/
		S_HI = new String[deadline][nb_cores];
		// Initialize with 0s
		for (int c = 0; c < nb_cores; c++) {
			for(int t = 0; t < deadline; t++) {
				S_HI[t][c] = "0";
			}
		}
			
		Start_HI = new int[mxc_dag.getNodes().size()];
		int[] t_hi = new int[mxc_dag.getNodes().size()];
		
		LinkedList<Node> li_hi = new LinkedList<Node>();
		Iterator<Node> it_n = mxc_dag.getNodes().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<Node> ready_hi = new LinkedList<Node>();
		
		// Add HI nodes to the list
		while(it_n.hasNext()){
			Node n = it_n.next();
			if (n.getC_HI() != 0) {
				System.out.println("Adding "+n.getName()+ " CiHI = "+ n.getC_HI());
				t_hi[n.getId()] = n.getC_HI();
				li_hi.add(n);
				if (n.isSource()) // At the beginning only source nodes are added
					ready_hi.add(n);
			}
		}

		// Sort lists
		Collections.sort(li_hi, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				return n2.getWeight_HI() - n1.getWeight_HI();
			}
		});
		
		Collections.sort(ready_hi, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				return n2.getWeight_HI()- n1.getWeight_HI();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		for(int t = 0; t < deadline; t++){
			ListIterator<Node> li_it = ready_hi.listIterator();
			for(int c = 0; c < nb_cores; c++) {
				if (li_it.hasNext()){
					Node n = li_it.next(); // Get head of the list
					S_HI[t][c] = n.getName(); // Give the slot to the task
					
					// Check if it's the first slot allocated
					// Start time
					if (t_hi[n.getId()] == n.getC_HI())
						Start_HI[n.getId()] = t;
					
					// Decrement slots left for the task
					t_hi[n.getId()] = t_hi[n.getId()] - 1;
				
					if (t_hi[n.getId()] == 0){ // Task has ended its execution
						li_it.remove();
					
						// Check for new activations
						checkActivation(li_it, n, t_hi, 1);
						
						// Heavier tasks can be activated -> needs a new sort
						Collections.sort(ready_hi, new Comparator<Node>() {
							@Override
							public int compare(Node n1, Node n2) {
								return n2.getWeight_HI() - n1.getWeight_HI();
							}
						});
					}
				}
			}
			li_it = ready_hi.listIterator(); // Restart the iterator for the next slot
		}

	}
	
	/**
	 * S LO allocation
	 */
	public void Alloc_LO(){
		/* =============================================
		 *  Initialization of variables used by the method
		 ================================================*/
		S_LO = new String[deadline][nb_cores];
		// Initialize with 0s
		for (int c = 0; c < nb_cores; c++) {
			for(int t = 0; t < deadline; t++) {
				S_LO[t][c] = "0";
			}
		}
			
		int[] t_lo = new int[mxc_dag.getNodes().size()];
		
		LinkedList<Node> li_lo = new LinkedList<Node>();
		Iterator<Node> it_n = mxc_dag.getNodes().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<Node> ready_lo = new LinkedList<Node>();
		
		// Add LO nodes to the list
		while(it_n.hasNext()){
			Node n = it_n.next();
			t_lo[n.getId()] = n.getC_LO();
			li_lo.add(n);
			if (n.isSource()) // At the beginning only source nodes are added
				ready_lo.add(n);
		}

		// Sort lists
		Collections.sort(li_lo, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				return n2.getWeight_LO() - n1.getWeight_LO();
			}
		});
		
		Collections.sort(ready_lo, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				return n2.getWeight_LO()- n1.getWeight_LO();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		for(int t = 0; t < deadline; t++){
			ListIterator<Node> li_it = ready_lo.listIterator();
			for(int c = 0; c < nb_cores; c++) {
				if (li_it.hasNext() && S_LO[t][c].contains("0")){
					Node n = li_it.next(); // Get head of the list
					
					// Check if slot is a HI start time
					checkStartHI(t, Start_HI, t_lo, n);
					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_lo, new Comparator<Node>() {
						@Override
						public int compare(Node n1, Node n2) {
							return n2.getWeight_LO() - n1.getWeight_LO();
						}
					});
					
					S_LO[t][c] = n.getName(); // Give the slot to the task

					// Decrement slots left for the task
					t_lo[n.getId()] = t_lo[n.getId()] - 1;

					if (t_lo[n.getId()] == 0){ // Task has ended its execution
						li_it.remove();

						// Check for new activations
						checkActivation(li_it, n, t_lo, 0);

						// Heavier tasks can be activated -> needs a new sort
						Collections.sort(ready_lo, new Comparator<Node>() {
							@Override
							public int compare(Node n1, Node n2) {
								return n2.getWeight_LO() - n1.getWeight_LO();
							}
						});
					}
				}
			}
			li_it = ready_lo.listIterator(); // Restart the iterator for the next slot
		}
	}
	
	/**
	 * Check if t is a start time and allocates what's left of the HI task
	 */
	public boolean checkStartHI(int t, int[] start_hi, int[] t_lo, Node n){
		boolean ret = false;
		if (start_hi[n.getId()] == t && t_lo[n.getId()] != 0) { // Task i has to start/finish its execution
			// If it's an activation time promote weight
			n.setWeight_LO(Integer.MAX_VALUE);
		}
		return ret;
	}
	
	/**
	 * Checks if successors of node n are activated/
	 */
	public void checkActivation(ListIterator<Node> li_r, Node n, int[] t_hi, int mode){
		
		// Check all successors
		Iterator<Edge> it_e = n.getSnd_edges().iterator();
		while (it_e.hasNext()){
			Edge e = it_e.next();
			Node suc = e.getDest();
			boolean ready = true;
			
			if (mode == 1 && suc.getC_HI() == 0) // Don't activate LO tasks in HI mode
				ready = false;
			
			Iterator<Edge> it_e_rcv = suc.getRcv_edges().iterator();
			while (it_e_rcv.hasNext()){ // For each successor we check its dependencies
				
				Edge e2 = it_e_rcv.next();
				Node pred = e2.getSrc();
				if (t_hi[pred.getId()] != 0){
					ready = false;
					break;
				}
			}
			
			if (ready) {
				li_r.add(suc);
			}
		}
	}
	
	/**
	 * Prints the S_HI table & start times
	 */
	public void printS_HI(){
		for (int c = 0; c < nb_cores; c++) {
			for(int t = 0; t < deadline; t++) {
				System.out.print(S_HI[t][c]+" | ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");

		Iterator<Node> it_n = mxc_dag.getNodes().iterator();
		while(it_n.hasNext()){
			Node n = it_n.next();
			if (n.getC_HI() != 0)
				System.out.println("Start HI ["+n.getName()+"] = "+ Start_HI[n.getId()]);
		}
			
	}
	
	/**
	 * Prints the S_LO table
	 */
	public void printS_LO(){
		for (int c = 0; c < nb_cores; c++) {
			for(int t = 0; t < deadline; t++) {
				System.out.print(S_LO[t][c]+" | ");
			}
			System.out.print("\n");
		}

	}
	
	public void Alloc_All(){
		
		this.calcWeights(1);
		this.Alloc_HI();
		
		this.printS_HI();
		
		this.calcWeights(0);
		this.Alloc_LO();
	
		this.printS_LO();
	}

	
	/**
	 * 
	 * Getters & Setters
	 */
	public void setMxcDag(DAG d){
		this.mxc_dag = d;
	}
	public DAG getMxcDag(){
		return this.mxc_dag;
	}
	
	public int getNb_cores() {
		return nb_cores;
	}

	public void setNb_cores(int nb_cores) {
		this.nb_cores = nb_cores;
	}

	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}
	
	public int[] getWeights_LO() {
		return weights_LO;
	}

	public void setWeights_LO(int weights_LO[]) {
		this.weights_LO = weights_LO;
	}
	
	public int[] getWeights_HI() {
		return weights_HI;
	}

	public void setWeights_HI(int weights_HI[]) {
		this.weights_HI = weights_HI;
	}
	public String[][] getS_HI() {
		return S_HI;
	}

	public void setS_HI(String[][] s_HI) {
		S_HI = s_HI;
	}

	public int[] getStart_HI() {
		return Start_HI;
	}

	public void setStart_HI(int start_HI[]) {
		Start_HI = start_HI;
	}

	public String[][] getS_LO() {
		return S_LO;
	}

	public void setS_LO(String s_LO[][]) {
		S_LO = s_LO;
	}

}
