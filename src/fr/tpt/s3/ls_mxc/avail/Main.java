package fr.tpt.s3.ls_mxc.avail;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.tpt.s3.ls_mxc.alloc.LS;
import fr.tpt.s3.ls_mxc.alloc.SchedulingException;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.model.Actor;

public class Main {
		
	@SuppressWarnings("unused")
	public static void main (String[] argv) throws IOException {
		
		System.out.println("========== LS Alloc BEGIN ==========");
		
		/*
		 * Example of DAG
		 */
		Actor A = new Actor(0, "At", 2, 3);
		Actor B = new Actor(1, "Bt", 2, 0);
		Actor C = new Actor(2, "Ct", 2, 0);
		Actor D = new Actor(3, "Dt", 2, 0);
		Actor E = new Actor(4, "Et", 2, 0);
		Actor F = new Actor(5, "Ft", 2, 0);
		Actor G = new Actor(6, "Gt", 2, 0);
		Actor H = new Actor(7, "Ht", 2, 0);
		Actor I = new Actor(8, "It", 2, 0);
		Actor J = new Actor(9, "Jt", 2, 0);
		Actor K = new Actor(10, "Kt", 2, 0);
		Actor L = new Actor(11, "Lt", 2, 0);
		Actor M = new Actor(12, "Mt", 2, 0);
		Actor N = new Actor(13, "Nt", 2, 0);
		
		Edge e0 = new Edge(A, B);
		Edge e1 = new Edge(B, C);
		Edge e2 = new Edge(C, D);
		Edge e3 = new Edge(E, F);
		Edge e4 = new Edge(F, G);
		Edge e5 = new Edge(H, I);
		Edge e6 = new Edge(K, L);
		Edge e7 = new Edge(L, M);
		
		DAG the_dag = new DAG();
		
		the_dag.getNodes().add(A);
		the_dag.getNodes().add(B);
		the_dag.getNodes().add(C);
		the_dag.getNodes().add(D);
		the_dag.getNodes().add(E);
		the_dag.getNodes().add(F);
		the_dag.getNodes().add(G);
		the_dag.getNodes().add(H);
		the_dag.getNodes().add(I);
		the_dag.getNodes().add(J);
		the_dag.getNodes().add(K);
		the_dag.getNodes().add(L);
		the_dag.getNodes().add(M);
		the_dag.getNodes().add(N);

		the_dag.setHINodes();
		the_dag.calcLOouts();
		
		LS alloc_problem = new LS(16, 2, the_dag);
		
		// Set booleans for sink and source
		Iterator<Actor> in = the_dag.getNodes().iterator();
		while (in.hasNext()){
			Actor n = in.next();
			n.checkifSink();
			n.checkifSinkinHI();
			n.checkifSource();
		}
		
		// HLFET Levels
		alloc_problem.calcWeights(0); // Weights in LO mode
		int[] w_lo = alloc_problem.getWeights_LO();
		
		alloc_problem.calcWeights(1); // Weights in HI mode
		int[] w_hi = alloc_problem.getWeights_HI();
		
		for(int i=0; i < 5; i++){
			System.out.println("LO weight "+ i +" = " + w_lo[i]);
		}
		
		System.out.println("-----------------------------------------");

		
		for(int i=0; i < 5; i++){
			System.out.println("HI weight "+ i +" = " + w_hi[i]);
		}
		
		/*
		 * Allocation, construction of tables
		 */
		
		try {
			alloc_problem.Alloc_HI();
		} catch (SchedulingException e) {
			System.out.println(e.getMessage());
		}
		
		System.out.println("\n-------------- S HI Table ---------------");
		alloc_problem.printS_HI();
		System.out.println("-----------------------------------------");

		try {
			alloc_problem.Alloc_LO();
		} catch (SchedulingException e) {
			System.out.println(e.getMessage());
		}
		
		System.out.println("\n-------------- S LO Table ---------------");
		alloc_problem.printS_LO();
		System.out.println("-----------------------------------------");
		
		System.out.println("------------- Construction of the Automata -------------");
		
		// Set failure probabilities
		in = the_dag.getNodes().iterator();
		while (in.hasNext()){
			Actor n = in.next();
			if (n.getC_HI() == 0)
				n.setfProb(0.01);
			else
				n.setfProb(0.001);
		}
		Voter v = new Voter(3, "VotA");
		v.createVoter();
		Automata auto = new Automata(alloc_problem, the_dag);
				
		auto.createAutomata();
		
		List<Voter> lv = new LinkedList<Voter>();
		lv.add(v);
		
		FileUtilities fu = new FileUtilities();
		fu.writeModelToFile("test.pm", lv, the_dag, auto);	
	}



}
