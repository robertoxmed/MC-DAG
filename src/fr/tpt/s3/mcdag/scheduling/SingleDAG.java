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
package fr.tpt.s3.mcdag.scheduling;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.model.McDAG;
import fr.tpt.s3.mcdag.model.Edge;

/**
 * List scheduling algorithm + construction of tables
 * @author Roberto Medina
 *
 */
public class SingleDAG extends AbstractMixedCriticalityScheduler {
	
	// DAG to be scheduled
	private McDAG mcDag;
	
	// Architecture, only nb cores atm
	private int nbCores;
	private int deadline;
	
	// Weights to calculate HLFET levels
	private int weights_LO[];
	private int weights_HI[];
	
	// Scheduling tables, i: slot, j: task
	private String sched[][][];
	private String S_HLFET[][];
	private String S_HLFET_HI[][];
	
	// Starting times of HI tasks in HI mode
	private int Start_HI[];
	
	private boolean debug;

	/**
	 * Constructor of LS
	 * @param dln Deadline
	 * @param cores Number of cores
	 * @param d Dag
	 */
	public SingleDAG(McDAG d, int cores){
		this.setDeadline(d.getDeadline());
		this.setNbCores(cores);
		this.setMxcDag(d);
	}
		
	/**
	 * Initializes scheduling tables
	 */
	protected void initTables () {
		sched = new String[2][getDeadline()][getNbCores()];
		
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < getDeadline(); j++) {
				for (int k = 0; k < getNbCores(); k++) {
					sched[i][j][k] = "-";
				}
			}
		}
		
		if (debug) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] initTables(): Sched tables initialized!");
	}
	
	/**
	 * Calc weights for HLFET for both tables
	 */
	public void calcWeights(int mode) {	
		weights_LO = new int[mcDag.getVertices().size()];
		weights_HI = new int[mcDag.getVertices().size()];
		
		Iterator<Vertex> it_n = mcDag.getVertices().iterator();
		while(it_n.hasNext()){
			VertexScheduling n = (VertexScheduling)it_n.next();
			if(mode == 0) { // LO mode
				weights_LO[n.getId()] = calcHLFETLevel(n, mode);
				
			} else { // HI Mode
				weights_HI[n.getId()] = calcHLFETLevel(n, mode);
			}
		}
	}
	
	
	/**
	 * Calculates HLFET levels for each Node depending on the mode.
	 * Sets the HLFET level in the Node object.
	 * @param n Node of the graph
	 * @param mode Mode of the graph
	 * @return Level of the Node in the graph
	 */
	public int calcHLFETLevel(VertexScheduling n, int mode) {
		
		int max = 0;
		
		// Final case the node is a sink
		if (n.getSndEdges().size() == 0 && mode == VertexScheduling.LO){
			n.getHlfet()[0] = n.getWcets()[0];
			return n.getWcets()[0];
		} else if (n.isSinkinL(1) && mode == VertexScheduling.HI) {
			n.getHlfet()[1] = n.getWcets()[1];
			return n.getWcets()[1];
		}
		
		// General case
		int[] tmp_max = new int[n.getSndEdges().size()];
		
		Iterator<Edge> ie = n.getSndEdges().iterator();
		int i = 0;
		while (ie.hasNext()) {
			
			Edge e = ie.next();
			tmp_max[i] = calcHLFETLevel((VertexScheduling) e.getDest(), mode); 
			i++;
		}
		
		for(int j = 0; j < i; j++) {
			if (max < tmp_max[j])
				max = tmp_max[j];
		}
		
		if (mode == VertexScheduling.LO) { // LO mode
			n.getHlfet()[0] = max + n.getWcets()[0];
			return max + n.getWcets()[0];
		} else {
			n.getHlfet()[1] =max + n.getWcets()[1];
			return max + n.getWcets()[1];
		}
	}
	
	/**
	 * Allocation algorithm for the HI mode.
	 * Instantiates the scheduling table + gives start times
	 * for HI tasks in HI mode.
	 * @throws SchedulingException
	 */
	public void AllocHI() throws SchedulingException{
		this.calcWeights(VertexScheduling.HI);
			
		Start_HI = new int[mcDag.getVertices().size()];
		int[] t_hi = new int[mcDag.getVertices().size()];
		
		Iterator<Vertex> it_n = mcDag.getVertices().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<VertexScheduling> ready_hi = new LinkedList<VertexScheduling>();
		// List of recently finished tasks -> to activate new ones
		LinkedList<VertexScheduling> finished_hi = new LinkedList<VertexScheduling>();
		boolean task_finished = false;
		
		// Add HI nodes to the list
		while(it_n.hasNext()){
			VertexScheduling n = (VertexScheduling) it_n.next();
			if (n.getWcets()[1] != 0) {
				t_hi[n.getId()] = n.getWcets()[1];
				if (n.isSinkinL(1)) { // At the beginning only exit nodes are added
					ready_hi.add(n);
				}
			}
		}

		// Sort lists
		Collections.sort(ready_hi, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling n1, VertexScheduling n2) {
				if (n2.getHlfet()[1]- n1.getHlfet()[1] != 0)
					return n1.getHlfet()[1] - n2.getHlfet()[1];
				else
					return n1.getId() - n2.getId();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		ListIterator<VertexScheduling> li_it = ready_hi.listIterator();
		for(int t = deadline - 1; t >= 0 ; t--){
			
			// Check if there is enough slots to finish executing tasks
			if (! checkFreeSlot(t_hi, getMxcDag().getVertices().size(), (t+1) * nbCores)){
				SchedulingException se = new SchedulingException("Alloc HI : Not enough slot lefts");
				throw se;
			}
			
			for(int c = 0; c < nbCores; c++) {
				if (li_it.hasNext()){
					VertexScheduling n = li_it.next(); // Get head of the list
					sched[1][t][c] = n.getName(); // Give the slot to the task
					
					// Decrement slots left for the task
					t_hi[n.getId()] = t_hi[n.getId()] - 1;
					
					// Check if it's the first slot allocated
					if (t_hi[n.getId()] == 0){ // Task has began its execution
						Start_HI[n.getId()] = t;
						li_it.remove();
						finished_hi.add(n);
						task_finished = true;						
					}
				}
			}
			// Tasks finished their execution 
			if (task_finished) {
				// Check for new activations
				ListIterator<VertexScheduling> li_f = finished_hi.listIterator();
				while (li_f.hasNext()) {
					VertexScheduling n = li_f.next();
					checkActivationHI(ready_hi, li_it, n, t_hi);
					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_hi, new Comparator<VertexScheduling>() {
						@Override
						public int compare(VertexScheduling n1, VertexScheduling n2) {
							if (n2.getHlfet()[1]- n1.getHlfet()[1] < 0 ||
									n2.getHlfet()[1]- n1.getHlfet()[1] > 0)
								return n1.getHlfet()[1]- n2.getHlfet()[1];
							else
								return n1.getId() - n2.getId();
						}
					});
					
				}
				task_finished = false;
				finished_hi.clear();
			}
			li_it = ready_hi.listIterator(); // Restart the iterator for the next slot
			if (ready_hi.isEmpty())
				return;
		}
	}
	
	/**
	 * Allocation of the LO mode for the graph.
	 * Needs to be called after Alloc_HI.
	 * @throws SchedulingException
	 */
	public void AllocLO() throws SchedulingException{
			
		int[] t_lo = new int[mcDag.getVertices().size()];
		
		Iterator<Vertex> it_n = mcDag.getVertices().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<VertexScheduling> ready_lo = new LinkedList<VertexScheduling>();
		// List of recently finished tasks -> to activate new ones
		LinkedList<VertexScheduling> finished_lo = new LinkedList<VertexScheduling>();
		boolean task_finished = false;
		
		// Add LO nodes to the list
		while(it_n.hasNext()){
			VertexScheduling n = (VertexScheduling) it_n.next();
			t_lo[n.getId()] = n.getWcets()[0];
			if (n.getRcvEdges().size() == 0) // At the beginning only source nodes are added
				ready_lo.add(n);
		}

		// Sort lists
		Collections.sort(ready_lo, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling n1, VertexScheduling n2) {
				if (n2.getHlfet()[0] - n1.getHlfet()[0] !=0)
					return n2.getHlfet()[0] - n1.getHlfet()[0];
				else
					return n2.getId() - n1.getId();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		ListIterator<VertexScheduling> li_it = ready_lo.listIterator();
		for(int t = 0; t < deadline; t++){
			// For each slot check if it's an WC activation time
			if (! checkFreeSlot(t_lo, mcDag.getVertices().size(), (deadline - t) * nbCores)){
				SchedulingException se = new SchedulingException("Alloc LO : Not enough slot lefts");
				throw se;
			}
			
			checkStartHI(ready_lo, t, Start_HI, t_lo);
			
			
			for(int c = 0; c < nbCores; c++) {
				if (li_it.hasNext()){
					VertexScheduling n = li_it.next(); // Get head of the list
					
					sched[0][t][c] = n.getName(); // Give the slot to the task

					// Decrement slots left for the task
					t_lo[n.getId()] = t_lo[n.getId()] - 1;

					if (t_lo[n.getId()] == 0){ // Task has ended its execution
						li_it.remove();
						task_finished = true;
						finished_lo.add(n);
					}
				}
			}
			
			if (task_finished) {
				ListIterator<VertexScheduling> li_f = finished_lo.listIterator();
				while (li_f.hasNext()) {
					VertexScheduling n = li_f.next();
					// Check for new activations
					checkActivation(ready_lo, li_it, n, t_lo, 0);

					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_lo, new Comparator<VertexScheduling>() {
						@Override
						public int compare(VertexScheduling n1, VertexScheduling n2) {
							if (n2.getHlfet()[0] - n1.getHlfet()[0] !=0)
								return n2.getHlfet()[0] - n1.getHlfet()[0];
							else
								return n2.getId() - n1.getId();
						}
					});
				}
				task_finished = false;
				finished_lo.clear();
			}
			li_it = ready_lo.listIterator(); // Restart the iterator for the next slot
			if (ready_lo.isEmpty())
				return;
		}
	}
	
	
	/**
	 * Checks if a new HI task needs to be promoted. If it's the case then
	 * the ready list is reordered.
	 * @param ready_lo List of tasks that can be scheduled
	 * @param t Time unit to check
	 * @param start_hi Table of start times for HI tasks
	 * @param t_lo Table of execution times
	 * @return
	 */
	public boolean checkStartHI(LinkedList<VertexScheduling> ready_lo, int t, int[] start_hi, int[] t_lo){
		boolean ret = false;
		Iterator<Vertex> it_n = mcDag.getVertices().iterator();
		while (it_n.hasNext()){
			VertexScheduling n = (VertexScheduling) it_n.next();
			if (start_hi[n.getId()] == t && t_lo[n.getId()] != 0 && n.getWcets()[1] != 0){
				n.getHlfet()[0] = Integer.MAX_VALUE;
				Collections.sort(ready_lo, new Comparator<VertexScheduling>() {
					@Override
					public int compare(VertexScheduling n1, VertexScheduling n2) {
						if (n2.getHlfet()[0] - n1.getHlfet()[0] !=0)
							return n2.getHlfet()[0] - n1.getHlfet()[0];
						else
							return n2.getId() - n1.getId();
					}
				});

			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param li_r
	 * @param n
	 * @param t_hi
	 * @param mode
	 */
	public void checkActivation(LinkedList<VertexScheduling> l_r, ListIterator<VertexScheduling> li_r, VertexScheduling n, int[] t_hi, int mode){
		
		// Check all successors
		Iterator<Edge> it_e = n.getSndEdges().iterator();
		while (it_e.hasNext()){
			Edge e = it_e.next();
			VertexScheduling suc = (VertexScheduling) e.getDest();
			boolean ready = true;
			boolean add = true;
			
			if (mode == 1 && suc.getWcets()[1] == 0) { // Don't activate LO tasks in HI mode
				ready = false;
				break;
			}
			
			Iterator<Edge> it_e_rcv = suc.getRcvEdges().iterator();
			while (it_e_rcv.hasNext()){ // For each successor we check its dependencies
				
				Edge e2 = it_e_rcv.next();
				VertexScheduling pred = (VertexScheduling) e2.getSrc();
				if (t_hi[pred.getId()] != 0){
					ready = false;
					break;
				}
			}
			
			if (ready) {
				// Need to check if the task has already been added
				ListIterator<VertexScheduling> li = l_r.listIterator();
				while(li.hasNext()){
					if(li.next().getId() == suc.getId())
						add = false;
				}
				if (add)
					li_r.add(suc);
			}
		}
	}
	
	/**
	 * 
	 * @param li_r
	 * @param n
	 * @param t_hi
	 * @param mode
	 */
	public void checkActivationHI(LinkedList<VertexScheduling> l_r, ListIterator<VertexScheduling> li_r, VertexScheduling n, int[] t_hi){
		
		// Check all successors
		Iterator<Edge> it_e = n.getRcvEdges().iterator();
		while (it_e.hasNext()){
			Edge e = it_e.next();
			VertexScheduling pred = (VertexScheduling) e.getSrc();
			boolean ready = true;
			boolean add = true;
			
			if (pred.getWcets()[1] == 0) { // Don't activate LO tasks in HI mode
				ready = false;
				break;
			}
			
			Iterator<Edge> it_e_rcv = pred.getSndEdges().iterator();
			while (it_e_rcv.hasNext()){ // For each successor we check if it has been executed
				
				Edge e2 = it_e_rcv.next();
				VertexScheduling suc = (VertexScheduling) e2.getDest();
				if (t_hi[suc.getId()] != 0){
					ready = false;
					break;
				}
			}
			
			if (ready) {
				// Need to check if the task has already been added
				ListIterator<VertexScheduling> li = l_r.listIterator();
				while(li.hasNext()){
					if(li.next().getId() == pred.getId())
						add = false;
				}
				if (add)
					li_r.add(pred);
			}
		}
	}


	/**
	 * Does the whole allocaiton
	 * @throws SchedulingException 
	 */
	public void buildAllTables() throws SchedulingException{
		
		initTables();
		
		this.calcWeights(VertexScheduling.HI);
		if (isDebug()) printW(VertexScheduling.HI);
		this.AllocHI();
		if (isDebug()) printS_HI();
		
		this.calcWeights(VertexScheduling.LO);
		if (isDebug()) printW(VertexScheduling.LO);
		this.AllocLO();
		if (isDebug()) printS_LO();
	}

	
	/**
	 * Check if there is enough time slots for remaining tasks
	 * @param t 
	 * @param n
	 * @param l
	 * @return
	 */
	public boolean checkFreeSlot(int[] t, int n, int l){
		boolean r = true;
		int s = 0;
		
		for(int i = 0; i < n; i ++){
			s += t[i];
		}
		
		if (s > l)
			r = false;
		
		return r;
	}
	
	public boolean HLFETSchedulable() {

		this.calcWeights(VertexScheduling.LO);
		/* =============================================
		 *  Initialization of variables used by the method
		 ================================================*/
		S_HLFET = new String[deadline][nbCores];
		// Initialize with 0s
		for (int c = 0; c < nbCores; c++) {
			for(int t = 0; t < deadline; t++) {
				S_HLFET[t][c] = "-";
			}
		}
			
		int[] t_lo = new int[mcDag.getVertices().size()];
		
		Iterator<Vertex> it_n = mcDag.getVertices().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<VertexScheduling> ready_lo = new LinkedList<VertexScheduling>();
		// List of recently finished tasks -> to activate new ones
		LinkedList<VertexScheduling> finished_lo = new LinkedList<VertexScheduling>();
		boolean task_finished = false;
		
		// Add LO nodes to the list
		while(it_n.hasNext()){
			VertexScheduling n = (VertexScheduling) it_n.next();
			t_lo[n.getId()] = n.getWcets()[0];
			if (n.getRcvEdges().size() == 0) // At the beginning only source nodes are added
				ready_lo.add(n);
		}

		// Sort lists
		Collections.sort(ready_lo, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling n1, VertexScheduling n2) {
				if (n2.getHlfet()[0] - n1.getHlfet()[0] != 0)
					return n2.getHlfet()[0]- n1.getHlfet()[0];
				else
					return n2.getId() - n1.getId();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		ListIterator<VertexScheduling> li_it = ready_lo.listIterator();
		for(int t = 0; t < deadline; t++){
			// For each slot check if it's an WC activation time
			if (! checkFreeSlot(t_lo, mcDag.getVertices().size(), (deadline - t) * nbCores)){
				return false;
			}
			
			
			for(int c = 0; c < nbCores; c++) {
				if (li_it.hasNext()){
					VertexScheduling n = li_it.next(); // Get head of the list
					
					S_HLFET[t][c] = n.getName(); // Give the slot to the task

					// Decrement slots left for the task
					t_lo[n.getId()] = t_lo[n.getId()] - 1;

					if (t_lo[n.getId()] == 0){ // Task has ended its execution
						li_it.remove();
						task_finished = true;
						finished_lo.add(n);
					}
				}
			}
			
			if (task_finished) {
				ListIterator<VertexScheduling> li_f = finished_lo.listIterator();
				while (li_f.hasNext()) {
					VertexScheduling n = li_f.next();
					// Check for new activations
					checkActivation(ready_lo, li_it, n, t_lo, 0);

					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_lo, new Comparator<VertexScheduling>() {
						@Override
						public int compare(VertexScheduling n1, VertexScheduling n2) {
							if (n2.getHlfet()[0] - n1.getHlfet()[0] != 0)
								return n2.getHlfet()[0]- n1.getHlfet()[0];
							else
								return n2.getId() - n1.getId();
						}
					});
				}
				task_finished = false;
				finished_lo.clear();
			}
			li_it = ready_lo.listIterator(); // Restart the iterator for the next slot
		}
		return true;
	}
	
	public boolean HLFETSchedulableHI() {

		this.calcWeights(1);
		/* =============================================
		 *  Initialization of variables used by the method
		 ================================================*/
		S_HLFET_HI = new String[deadline][nbCores];
		// Initialize with 0s
		for (int c = 0; c < nbCores; c++) {
			for(int t = 0; t < deadline; t++) {
				S_HLFET_HI[t][c] = "-";
			}
		}
			
		int[] t_hi = new int[mcDag.getVertices().size()];
		
		Iterator<Vertex> it_n = mcDag.getVertices().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<VertexScheduling> ready_hi = new LinkedList<VertexScheduling>();
		// List of recently finished tasks -> to activate new ones
		LinkedList<VertexScheduling> finished_hi = new LinkedList<VertexScheduling>();
		boolean task_finished = false;
		
		// Add HI nodes to the list
		while(it_n.hasNext()){
			VertexScheduling n = (VertexScheduling) it_n.next();
			if (n.getWcets()[1] != 0) {
				t_hi[n.getId()] = n.getWcets()[1];
				if (n.getRcvEdges().size() == 0) // At the beginning only source nodes are added
					ready_hi.add(n);
			}
		}

		// Sort lists		
		Collections.sort(ready_hi, new Comparator<VertexScheduling>() {
			@Override
			public int compare(VertexScheduling n1, VertexScheduling n2) {
				if (n2.getHlfet()[1]- n1.getHlfet()[1] != 0)
					return n2.getHlfet()[1]- n1.getHlfet()[1];
				else
					return n2.getId() - n1.getId();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		ListIterator<VertexScheduling> li_it = ready_hi.listIterator();
		for(int t = 0 ; t < deadline ; t++){
			
			//Check if there is enough slots to finish executing tasks
			if (! checkFreeSlot(t_hi, mcDag.getVertices().size(), (deadline - t) * nbCores)){
				return false;
			}
			
			for(int c = 0; c < nbCores; c++) {
				if (li_it.hasNext()){
					VertexScheduling n = li_it.next(); // Get head of the list
					S_HLFET_HI[t][c] = n.getName(); // Give the slot to the task
					// Decrement slots left for the task
					t_hi[n.getId()] = t_hi[n.getId()] - 1;
				
					if (t_hi[n.getId()] == 0){ // Task has ended its execution
						li_it.remove();
						finished_hi.add(n);
						task_finished = true;						
					}
				}
			}
			// Tasks finished their execution 
			if (task_finished) {
				// Check for new activations
				ListIterator<VertexScheduling> li_f = finished_hi.listIterator();
				while (li_f.hasNext()) {
					VertexScheduling n = li_f.next();
					checkActivation(ready_hi, li_it, n, t_hi, 1);
					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_hi, new Comparator<VertexScheduling>() {
						@Override
						public int compare(VertexScheduling n1, VertexScheduling n2) {
							if (n2.getHlfet()[1]- n1.getHlfet()[1] != 0)
								return n2.getHlfet()[1]- n1.getHlfet()[1];
							else
								return n2.getId() - n1.getId();
						}
					});
					
				}
				task_finished = false;
				finished_hi.clear();
			}
			li_it = ready_hi.listIterator(); // Restart the iterator for the next slot
			if (ready_hi.isEmpty())
				return true;
		}
		return true;
	}
	
	/*
	 * Debugging functions
	 */
	
	/**
	 * Prints weights in the different modes
	 * @param mode
	 */
	public void printW(int mode) {
		for (int i = 0; i < getMxcDag().getVertices().size(); i++) {
			if (mode == VertexScheduling.HI ) {
				if (getMxcDag().getNodebyID(i).getWcets()[1] != 0)
					System.out.println("[DEBUG] Weight HI "+getMxcDag().getNodebyID(i).getName()+" = "+((VertexScheduling) getMxcDag().getNodebyID(i)).getHlfet()[1]);
			} else {
				System.out.println("[DEBUG] Weight LO "+getMxcDag().getNodebyID(i).getName()+" = "+weights_LO[i]);
			}
				
		}	
	}
	
	/**
	 * Prints the S_HI table & start times
	 */
	public void printS_HI(){
		for (int c = 0; c < nbCores; c++) {
			for(int t = 0; t < deadline; t++) {
				System.out.print(sched[1][t][c]+" | ");
			}
			System.out.print("\n");
		}
		System.out.print("\n");
			
	}
	
	/**
	 * Prints the S_LO table
	 */
	public void printS_LO(){
		for (int c = 0; c < nbCores; c++) {
			for(int t = 0; t < deadline; t++) {
				System.out.print(sched[0][t][c]+" | ");
			}
			System.out.print("\n");
		}
	}
	
	/**
	 * Prints the S_HLFET_HI LO table table
	 */
	public void printS_HLFETHI(){
		for (int c = 0; c < nbCores; c++) {
			for(int t = 0; t < deadline; t++) {
				System.out.print(S_HLFET_HI[t][c]+" | ");
			}
			System.out.print("\n");
		}
	}
	
	/* 
	 * Getters & Setters
	 */
	public void setMxcDag(McDAG d){
		this.mcDag = d;
	}
	public McDAG getMxcDag(){
		return this.mcDag;
	}
	
	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nb_cores) {
		this.nbCores = nb_cores;
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

	public int[] getStart_HI() {
		return Start_HI;
	}

	public void setStart_HI(int start_HI[]) {
		Start_HI = start_HI;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public String[][][] getSched() {
		return sched;
	}

	public void setSched(String sched[][][]) {
		this.sched = sched;
	}
}
