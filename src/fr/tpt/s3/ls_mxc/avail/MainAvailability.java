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
import fr.tpt.s3.ls_mxc.model.Actor;

public class MainAvailability {
	
	@SuppressWarnings("unused")
	public static void main (String[] argv) throws IOException {
		
		System.out.println("========== LS Alloc BEGIN ==========");
		
		/*
		 * Example of DAG
		 */
		Actor Avoid = new Actor(0, "Avoid", 3, 0);
		Actor Nav = new Actor(1, "Nav", 5, 6);
		Actor VotA = new Actor(2, "VotA", 1, 1);
		VotA.setfMechanism(true);
		Actor Stab = new Actor(3, "Stab", 2, 5);
		Actor Log = new Actor(4, "Log", 2, 0);
		Actor Shar = new Actor(5, "Shar", 3, 0);
		Actor Video = new Actor(6, "Video", 7, 0);
		Actor GPS = new Actor(7, "GPS", 2, 0);
		Actor Rec = new Actor(8, "Rec", 2, 0);
		
		Edge e0 = new Edge(Avoid, VotA);
		Edge e1 = new Edge(VotA, Nav);
		Edge e2 = new Edge(Nav, Stab);
		Edge e4 = new Edge(VotA, Log);
		Edge e5 = new Edge(Nav, Log);
		Edge e6 = new Edge(Stab, Log);
		Edge e7 = new Edge(Log, Shar);
		Edge e8 = new Edge(GPS, Rec);
		
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
		
		the_dag.sanityChecks();
		
		LS alloc_problem = new LS(15, 2, the_dag);
		
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
		Iterator <Actor> in = the_dag.getNodes().iterator();
		while (in.hasNext()){
			Actor n = in.next();
			if (n.getC_HI() == 0)
				n.setfProb(0.01);
			else
				n.setfProb(0.001);
		}
		
		FTM v = new FTM(3, "VotA");
		v.createVoter();
		v.printVoter();
		Automata auto = new Automata(alloc_problem, the_dag);
				
		auto.createAutomata();
		
		List<FTM> lv = new LinkedList<FTM>();
		lv.add(v);
		
		FileUtilities fu = new FileUtilities();
		fu.writeModelToFile("test.pm", lv, the_dag, auto);	
	}

}
