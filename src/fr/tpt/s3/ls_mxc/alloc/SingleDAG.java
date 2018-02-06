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
package fr.tpt.s3.ls_mxc.alloc;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.ActorSched;

/**
 * List scheduling algorithm + construction of tables
 * @author Roberto Medina
 *
 */
public class SingleDAG{
	
	// DAG to be scheduled
	private DAG mcDag;
	
	// Architecture, only nb cores atm
	private int nbCores;
	private int deadline;
	
	// Weights to calculate HLFET levels
	private int weights_LO[];
	private int weights_HI[];
	private int weights_B[];
	
	// Scheduling tables, i: slot, j: task
	private String sched[][][];
	private String S_B[][];
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
	public SingleDAG(int dln, int cores, DAG d){
		this.setDeadline(dln);
		this.setNbCores(cores);
		this.setMxcDag(d);
	}
	
	public SingleDAG() {}
	
	/**
	 * Initializes scheduling tables
	 */
	private void initTables () {
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
		weights_LO = new int[mcDag.getNodes().size()];
		weights_HI = new int[mcDag.getNodes().size()];
		
		Iterator<Actor> it_n = mcDag.getNodes().iterator();
		while(it_n.hasNext()){
			ActorSched n = (ActorSched)it_n.next();
			if(mode == 0) { // LO mode
				weights_LO[n.getId()] = calcHLFETLevel(n, mode);
				
			} else { // HI Mode
				weights_HI[n.getId()] = calcHLFETLevel(n, mode);
			}
		}
	}
	
	/**
	 * Calc weights for HLFET for Baruah
	 */
	public void calcWeightsB() {
		
		weights_B = new int[mcDag.getNodes().size()];
		
		Iterator<Actor> it_n = mcDag.getNodes().iterator();
		while(it_n.hasNext()){
			ActorSched n = (ActorSched) it_n.next();
			if (n.getcIs()[1] !=  0) {
				weights_B[n.getId()] = calcHLFETLevel(n, 0) + 200; // Add constant
				n.setWeight_B(n.getWeightLO() + mcDag.getCritPath() * 2);
			} else {
				weights_B[n.getId()] = calcHLFETLevel(n, 0);
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
	public int calcHLFETLevel(ActorSched n, int mode) {
		
		int max = 0;
		
		// Final case the node is a sink
		if (n.isSink() && mode == ActorSched.LO){
			n.setWeightLO(n.getcIs()[0]);
			return n.getcIs()[0];
		} else if (n.isSinkHI() && mode == ActorSched.HI) {
			n.setWeightHI(n.getcIs()[1]);
			return n.getcIs()[1];
		}
		
		// General case
		int[] tmp_max = new int[n.getSndEdges().size()];
		
		Iterator<Edge> ie = n.getSndEdges().iterator();
		int i = 0;
		while (ie.hasNext()) {
			
			Edge e = ie.next();
			tmp_max[i] = calcHLFETLevel((ActorSched) e.getDest(), mode); 
			i++;
		}
		
		for(int j = 0; j < i; j++) {
			if (max < tmp_max[j])
				max = tmp_max[j];
		}
		
		if (mode == ActorSched.LO) { // LO mode
			n.setWeightLO(max + n.getcIs()[0]);
			return max + n.getcIs()[0];
		} else {
			n.setWeightHI(max + n.getcIs()[1]);
			return max + n.getcIs()[1];
		}
	}
	
	/**
	 * Allocation algorithm for the HI mode.
	 * Instantiates the scheduling table + gives start times
	 * for HI tasks in HI mode.
	 * @throws SchedulingException
	 */
	public void AllocHI() throws SchedulingException{
		this.calcWeights(ActorSched.HI);
			
		Start_HI = new int[mcDag.getNodes().size()];
		int[] t_hi = new int[mcDag.getNodes().size()];
		
		Iterator<Actor> it_n = mcDag.getNodes().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<ActorSched> ready_hi = new LinkedList<ActorSched>();
		// List of recently finished tasks -> to activate new ones
		LinkedList<ActorSched> finished_hi = new LinkedList<ActorSched>();
		boolean task_finished = false;
		
		// Add HI nodes to the list
		while(it_n.hasNext()){
			ActorSched n = (ActorSched) it_n.next();
			if (n.getcIs()[1] != 0) {
				t_hi[n.getId()] = n.getcIs()[1];
				if (n.isSinkHI()) { // At the beginning only exit nodes are added
					ready_hi.add(n);
				}
			}
		}

		// Sort lists
		Collections.sort(ready_hi, new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched n1, ActorSched n2) {
				if (n2.getWeightHI()- n1.getWeightHI() != 0)
					return n1.getWeightHI()- n2.getWeightHI();
				else
					return n1.getId() - n2.getId();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		ListIterator<ActorSched> li_it = ready_hi.listIterator();
		for(int t = deadline - 1; t >= 0 ; t--){
			
			// Check if there is enough slots to finish executing tasks
			if (! checkFreeSlot(t_hi, getMxcDag().getNodes().size(), (t+1) * nbCores)){
				SchedulingException se = new SchedulingException("Alloc HI : Not enough slot lefts");
				throw se;
			}
			
			for(int c = 0; c < nbCores; c++) {
				if (li_it.hasNext()){
					ActorSched n = li_it.next(); // Get head of the list
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
				ListIterator<ActorSched> li_f = finished_hi.listIterator();
				while (li_f.hasNext()) {
					ActorSched n = li_f.next();
					checkActivationHI(ready_hi, li_it, n, t_hi);
					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_hi, new Comparator<ActorSched>() {
						@Override
						public int compare(ActorSched n1, ActorSched n2) {
							if (n2.getWeightHI()- n1.getWeightHI() < 0 ||
									n2.getWeightHI()- n1.getWeightHI() > 0)
								return n1.getWeightHI()- n2.getWeightHI();
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
			
		int[] t_lo = new int[mcDag.getNodes().size()];
		
		Iterator<Actor> it_n = mcDag.getNodes().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<ActorSched> ready_lo = new LinkedList<ActorSched>();
		// List of recently finished tasks -> to activate new ones
		LinkedList<ActorSched> finished_lo = new LinkedList<ActorSched>();
		boolean task_finished = false;
		
		// Add LO nodes to the list
		while(it_n.hasNext()){
			ActorSched n = (ActorSched) it_n.next();
			t_lo[n.getId()] = n.getcIs()[0];
			if (n.isSource()) // At the beginning only source nodes are added
				ready_lo.add(n);
		}

		// Sort lists
		
		Collections.sort(ready_lo, new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched n1, ActorSched n2) {
				if (n2.getWeightLO() - n1.getWeightLO() !=0)
					return n2.getWeightLO() - n1.getWeightLO();
				else
					return n2.getId() - n1.getId();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		ListIterator<ActorSched> li_it = ready_lo.listIterator();
		for(int t = 0; t < deadline; t++){
			// For each slot check if it's an WC activation time
			if (! checkFreeSlot(t_lo, mcDag.getNodes().size(), (deadline - t) * nbCores)){
				SchedulingException se = new SchedulingException("Alloc LO : Not enough slot lefts");
				throw se;
			}
			
			checkStartHI(ready_lo, t, Start_HI, t_lo);
			
			
			for(int c = 0; c < nbCores; c++) {
				if (li_it.hasNext()){
					ActorSched n = li_it.next(); // Get head of the list
					
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
				ListIterator<ActorSched> li_f = finished_lo.listIterator();
				while (li_f.hasNext()) {
					ActorSched n = li_f.next();
					// Check for new activations
					checkActivation(ready_lo, li_it, n, t_lo, 0);

					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_lo, new Comparator<ActorSched>() {
						@Override
						public int compare(ActorSched n1, ActorSched n2) {
							if (n2.getWeightLO() - n1.getWeightLO() !=0)
								return n2.getWeightLO() - n1.getWeightLO();
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
	 * Allocation of the LO mode for the graph.
	 * Needs to be called after Alloc_HI.
	 * @throws SchedulingException
	 */
	public void Alloc_B() throws SchedulingException{
		/* =============================================
		 *  Initialization of variables used by the method
		 ================================================*/
		S_B = new String[deadline][nbCores];
		// Initialize with 0s
		for (int c = 0; c < nbCores; c++) {
			for(int t = 0; t < deadline; t++) {
				S_B[t][c] = "-";
			}
		}
			
		int[] t_lo = new int[mcDag.getNodes().size()];
		
		Iterator<Actor> it_n = mcDag.getNodes().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<ActorSched> ready_lo = new LinkedList<ActorSched>();
		// List of recently finished tasks -> to activate new ones
		LinkedList<ActorSched> finished_lo = new LinkedList<ActorSched>();
		boolean task_finished = false;
		
		// Add LO nodes to the list
		while(it_n.hasNext()){
			ActorSched n = (ActorSched) it_n.next();
			t_lo[n.getId()] = n.getcIs()[0];
			if (n.isSource()) // At the beginning only source nodes are added
				ready_lo.add(n);
		}

		// Sort lists
		Collections.sort(ready_lo, new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched n1, ActorSched n2) {
				if (n2.getWeight_B() - n1.getWeight_B() !=0)
					return n2.getWeight_B() - n1.getWeight_B();
				else
					return n2.getId() - n1.getId();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		ListIterator<ActorSched> li_it = ready_lo.listIterator();
		for(int t = 0; t < deadline; t++){
			// For each slot check if it's an WC activation time
			if (! checkFreeSlot(t_lo, mcDag.getNodes().size(), (deadline - t) * nbCores)){
				SchedulingException se = new SchedulingException("Alloc B : Not enough slot lefts");
				throw se;
			}			
			
			for(int c = 0; c < nbCores; c++) {
				if (li_it.hasNext()){
					ActorSched n = li_it.next(); // Get head of the list
					
					S_B[t][c] = n.getName(); // Give the slot to the task

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
				ListIterator<ActorSched> li_f = finished_lo.listIterator();
				while (li_f.hasNext()) {
					ActorSched n = li_f.next();
					// Check for new activations
					checkActivation(ready_lo, li_it, n, t_lo, 0);

					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_lo, new Comparator<ActorSched>() {
						@Override
						public int compare(ActorSched n1, ActorSched n2) {
							if (n2.getWeight_B() - n1.getWeight_B() !=0)
								return n2.getWeight_B() - n1.getWeight_B();
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
	public boolean checkStartHI(LinkedList<ActorSched> ready_lo, int t, int[] start_hi, int[] t_lo){
		boolean ret = false;
		Iterator<Actor> it_n = mcDag.getNodes().iterator();
		while (it_n.hasNext()){
			ActorSched n = (ActorSched) it_n.next();
			if (start_hi[n.getId()] == t && t_lo[n.getId()] != 0 && n.getcIs()[1] != 0){
				n.setWeightLO(Integer.MAX_VALUE);
				Collections.sort(ready_lo, new Comparator<ActorSched>() {
					@Override
					public int compare(ActorSched n1, ActorSched n2) {
						if (n2.getWeightLO() - n1.getWeightLO() !=0)
							return n2.getWeightLO() - n1.getWeightLO();
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
	public void checkActivation(LinkedList<ActorSched> l_r, ListIterator<ActorSched> li_r, ActorSched n, int[] t_hi, int mode){
		
		// Check all successors
		Iterator<Edge> it_e = n.getSndEdges().iterator();
		while (it_e.hasNext()){
			Edge e = it_e.next();
			ActorSched suc = (ActorSched) e.getDest();
			boolean ready = true;
			boolean add = true;
			
			if (mode == 1 && suc.getcIs()[1] == 0) { // Don't activate LO tasks in HI mode
				ready = false;
				break;
			}
			
			Iterator<Edge> it_e_rcv = suc.getRcvEdges().iterator();
			while (it_e_rcv.hasNext()){ // For each successor we check its dependencies
				
				Edge e2 = it_e_rcv.next();
				ActorSched pred = (ActorSched) e2.getSrc();
				if (t_hi[pred.getId()] != 0){
					ready = false;
					break;
				}
			}
			
			if (ready) {
				// Need to check if the task has already been added
				ListIterator<ActorSched> li = l_r.listIterator();
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
	public void checkActivationHI(LinkedList<ActorSched> l_r, ListIterator<ActorSched> li_r, ActorSched n, int[] t_hi){
		
		// Check all successors
		Iterator<Edge> it_e = n.getRcvEdges().iterator();
		while (it_e.hasNext()){
			Edge e = it_e.next();
			ActorSched pred = (ActorSched) e.getSrc();
			boolean ready = true;
			boolean add = true;
			
			if (pred.getcIs()[1] == 0) { // Don't activate LO tasks in HI mode
				ready = false;
				break;
			}
			
			Iterator<Edge> it_e_rcv = pred.getSndEdges().iterator();
			while (it_e_rcv.hasNext()){ // For each successor we check if it has been executed
				
				Edge e2 = it_e_rcv.next();
				ActorSched suc = (ActorSched) e2.getDest();
				if (t_hi[suc.getId()] != 0){
					ready = false;
					break;
				}
			}
			
			if (ready) {
				// Need to check if the task has already been added
				ListIterator<ActorSched> li = l_r.listIterator();
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
	public boolean AllocAll() throws SchedulingException{
		boolean ret = true;
		
		initTables();
		
		this.calcWeights(ActorSched.HI);
		if (isDebug()) printW(ActorSched.HI);
		this.AllocHI();
		if (isDebug()) printS_HI();
		
		this.calcWeights(ActorSched.LO);
		if (isDebug()) printW(ActorSched.LO);
		this.AllocLO();
		if (isDebug()) printS_LO();
		
		return ret;
	}
	
	public boolean CheckBaruah() throws SchedulingException{
		// Check if schedulable by Baruah
		boolean ret = true;
		
		this.calcWeights(ActorSched.HI);
		this.calcWeightsB();
		
		this.AllocHI();
		
		this.Alloc_B();
		
		return ret;
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

		this.calcWeights(ActorSched.LO);
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
			
		int[] t_lo = new int[mcDag.getNodes().size()];
		
		Iterator<Actor> it_n = mcDag.getNodes().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<ActorSched> ready_lo = new LinkedList<ActorSched>();
		// List of recently finished tasks -> to activate new ones
		LinkedList<ActorSched> finished_lo = new LinkedList<ActorSched>();
		boolean task_finished = false;
		
		// Add LO nodes to the list
		while(it_n.hasNext()){
			ActorSched n = (ActorSched) it_n.next();
			t_lo[n.getId()] = n.getcIs()[0];
			if (n.isSource()) // At the beginning only source nodes are added
				ready_lo.add(n);
		}

		// Sort lists
		Collections.sort(ready_lo, new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched n1, ActorSched n2) {
				if (n2.getWeightLO() - n1.getWeightLO() != 0)
					return n2.getWeightLO()- n1.getWeightLO();
				else
					return n2.getId() - n1.getId();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		ListIterator<ActorSched> li_it = ready_lo.listIterator();
		for(int t = 0; t < deadline; t++){
			// For each slot check if it's an WC activation time
			if (! checkFreeSlot(t_lo, mcDag.getNodes().size(), (deadline - t) * nbCores)){
				return false;
			}
			
			
			for(int c = 0; c < nbCores; c++) {
				if (li_it.hasNext()){
					ActorSched n = li_it.next(); // Get head of the list
					
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
				ListIterator<ActorSched> li_f = finished_lo.listIterator();
				while (li_f.hasNext()) {
					ActorSched n = li_f.next();
					// Check for new activations
					checkActivation(ready_lo, li_it, n, t_lo, 0);

					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_lo, new Comparator<ActorSched>() {
						@Override
						public int compare(ActorSched n1, ActorSched n2) {
							if (n2.getWeightLO() - n1.getWeightLO() != 0)
								return n2.getWeightLO()- n1.getWeightLO();
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
			
		int[] t_hi = new int[mcDag.getNodes().size()];
		
		Iterator<Actor> it_n = mcDag.getNodes().iterator(); 
		// Ready list of tasks that have their dependencies met
		LinkedList<ActorSched> ready_hi = new LinkedList<ActorSched>();
		// List of recently finished tasks -> to activate new ones
		LinkedList<ActorSched> finished_hi = new LinkedList<ActorSched>();
		boolean task_finished = false;
		
		// Add HI nodes to the list
		while(it_n.hasNext()){
			ActorSched n = (ActorSched) it_n.next();
			if (n.getcIs()[1] != 0) {
				t_hi[n.getId()] = n.getcIs()[1];
				if (n.isSource()) // At the beginning only source nodes are added
					ready_hi.add(n);
			}
		}

		// Sort lists		
		Collections.sort(ready_hi, new Comparator<ActorSched>() {
			@Override
			public int compare(ActorSched n1, ActorSched n2) {
				if (n2.getWeightHI()- n1.getWeightHI() != 0)
					return n2.getWeightHI()- n1.getWeightHI();
				else
					return n2.getId() - n1.getId();
			}
		});
		
		/* =============================================
		 *  Actual allocation
		 * =============================================*/
		
		// Iterate through slots
		ListIterator<ActorSched> li_it = ready_hi.listIterator();
		for(int t = 0 ; t < deadline ; t++){
			
			//Check if there is enough slots to finish executing tasks
			if (! checkFreeSlot(t_hi, mcDag.getNodes().size(), (deadline - t) * nbCores)){
				return false;
			}
			
			for(int c = 0; c < nbCores; c++) {
				if (li_it.hasNext()){
					ActorSched n = li_it.next(); // Get head of the list
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
				ListIterator<ActorSched> li_f = finished_hi.listIterator();
				while (li_f.hasNext()) {
					ActorSched n = li_f.next();
					checkActivation(ready_hi, li_it, n, t_hi, 1);
					// Heavier tasks can be activated -> needs a new sort
					Collections.sort(ready_hi, new Comparator<ActorSched>() {
						@Override
						public int compare(ActorSched n1, ActorSched n2) {
							if (n2.getWeightHI()- n1.getWeightHI() != 0)
								return n2.getWeightHI()- n1.getWeightHI();
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
		for (int i = 0; i < getMxcDag().getNodes().size(); i++) {
			if (mode == ActorSched.HI ) {
				if (getMxcDag().getNodebyID(i).getcIs()[1] != 0)
					System.out.println("[DEBUG] Weight HI "+getMxcDag().getNodebyID(i).getName()+" = "+((ActorSched) getMxcDag().getNodebyID(i)).getWeightHI());
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
	public void setMxcDag(DAG d){
		this.mcDag = d;
	}
	public DAG getMxcDag(){
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
