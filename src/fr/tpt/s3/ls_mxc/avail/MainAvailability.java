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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.tpt.s3.ls_mxc.alloc.LS;
import fr.tpt.s3.ls_mxc.alloc.SchedulingException;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.model.Node;

public class MainAvailability {
	
	@SuppressWarnings("unused")
	public static void main (String[] argv) throws IOException {
		
		System.out.println("========== LS Alloc BEGIN ==========");
		
		/*
		 * Example of DAG
		 */
		Node Avoid = new Node(0, "Avoid", 3, 0);
		Node Nav = new Node(1, "Nav", 5, 6);
		Node VotA = new Node(2, "VotA", 1, 1);
		VotA.setfMechanism(true);
		Node Stab = new Node(3, "Stab", 2, 5);
		Node Log = new Node(4, "Log", 2, 0);
		Node Shar = new Node(5, "Shar", 3, 0);
		Node Video = new Node(6, "Video", 7, 0);
		Node GPS = new Node(7, "GPS", 2, 0);
		Node Rec = new Node(8, "Rec", 2, 0);
		
		Edge e0 = new Edge(Avoid, VotA, false);
		Edge e1 = new Edge(VotA, Nav, false);
		Edge e2 = new Edge(Nav, Stab, false);
		Edge e4 = new Edge(VotA, Log, false);
		Edge e5 = new Edge(Nav, Log, false);
		Edge e6 = new Edge(Stab, Log, false);
		Edge e7 = new Edge(Log, Shar, false);
		Edge e8 = new Edge(GPS, Rec, false);
		
		DAG the_dag = new DAG();
		
		the_dag.getNodes().add(Avoid);
		the_dag.getNodes().add(VotA);
		the_dag.getNodes().add(Nav);
		the_dag.getNodes().add(Stab);
		the_dag.getNodes().add(Log);
		the_dag.getNodes().add(Shar);
		the_dag.getNodes().add(Video);
		the_dag.getNodes().add(GPS);
		the_dag.getNodes().add(Rec);
		
		the_dag.setHINodes();
		the_dag.calcLOouts();
		
		LS alloc_problem = new LS(15, 2, the_dag);
		
		// Set booleans for sink and source
		Iterator<Node> in = the_dag.getNodes().iterator();
		while (in.hasNext()){
			Node n = in.next();
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
			Node n = in.next();
			if (n.getC_HI() == 0)
				n.setfProb(0.01);
			else
				n.setfProb(0.001);
		}
		
		Voter v = new Voter(3, "VotA");
		v.createVoter();
		v.printVoter();
		Automata auto = new Automata(alloc_problem, the_dag);
				
		auto.createAutomata();
		
		List<Voter> lv = new LinkedList<Voter>();
		lv.add(v);
		
		FileUtilities fu = new FileUtilities();
		fu.writeModelToFile("test.pm", lv, the_dag, auto);	
	}

}
