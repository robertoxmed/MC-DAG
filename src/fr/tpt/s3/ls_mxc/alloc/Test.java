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
package fr.tpt.s3.ls_mxc.alloc;

import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.model.Node;

public class Test {
	
	public static void main (String args[]){
		
		System.out.println("========== LS Alloc BEGIN ==========");
		
		/*
		 * Example of DAG
		 */
		Node A = new Node(0, "A", 3, 3);
		Node B = new Node(1, "B", 2, 4);
		Node C = new Node(2, "C", 3, 0);
		Node D = new Node(3, "D", 3, 5);
		Node E = new Node(4, "E", 1, 0);
		
		Edge e1 = new Edge(A, B, false);
		Edge e2 = new Edge(B, C, false);
		Edge e3 = new Edge(D, B, false);
		Edge e4 = new Edge(B, E, false);
		
		A.getSnd_edges().add(e1);
		B.getRcv_edges().add(e1);
		B.getSnd_edges().add(e2);
		C.getRcv_edges().add(e2);
		D.getSnd_edges().add(e3);
		B.getRcv_edges().add(e3);
		B.getSnd_edges().add(e4);
		E.getRcv_edges().add(e4);
		
		DAG the_dag = new DAG();
		
		the_dag.getNodes().add(A);
		the_dag.getNodes().add(B);
		the_dag.getNodes().add(C);
		the_dag.getNodes().add(D);
		the_dag.getNodes().add(E);
		
		LS alloc_problem = new LS(9, 2, the_dag);
		
		// Set booleans for sink and source
		A.checkifSink();
		A.checkifSource();
		B.checkifSink();
		B.checkifSource();
		C.checkifSink();
		C.checkifSource();
		D.checkifSink();
		D.checkifSource();
		E.checkifSink();
		E.checkifSource();
		B.checkifSinkinHI();
		
		/*
		 * HLFET Levels
		 */
		
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
		
		
		/*
		 * Reading from file
		 */
		System.out.println("\n-------------- Reading from file ---------------");

		FileUtilities fu = new FileUtilities();
			
		LS l3 = new LS();
		
		fu.ReadAndInit("/home/roberto/workspace/LS_mxc/tests/ex3.test", l3);

		try {
			l3.Alloc_All();
			l3.printS_HI();
			l3.printS_LO();
		} catch (SchedulingException e) {
			System.out.println(e.getMessage());
		}
		
		
		System.out.println("\n-------------- Reading from file ---------------");

		
		LS l4 = new LS();
		
		fu.ReadAndInit("/home/roberto/workspace/LS_mxc/tests/ex2.test", l4);
		
		try {
			l4.Alloc_All();
			l3.printS_HI();
			l3.printS_LO();
		} catch (SchedulingException e) {
			System.out.println(e.getMessage());
		}


		
		System.out.println("\n========== LS Alloc END ==========");
	}
}
