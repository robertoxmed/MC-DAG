/*******************************************************************************
 * Copyright (c) 2018 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package fr.tpt.s3.mcdag.alloc;

import java.util.ArrayList;

import fr.tpt.s3.mcdag.model.Actor;
import fr.tpt.s3.mcdag.model.ActorSched;
import fr.tpt.s3.mcdag.model.DAG;
import fr.tpt.s3.mcdag.model.Edge;

/**
 * Abstract class to implement MC-DAG schedulers
 * @author roberto
 *
 */
public abstract class AbstractMixedCriticalityScheduler {
	
	protected abstract void initTables();
	
	public abstract void buildAllTables() throws SchedulingException;
	
	/**
	 * Internal function that checks if all the predecessors of an actor are visited
	 * @param a
	 * @param level
	 * @return
	 */
	protected boolean predVisitedInLevel (ActorSched a, int level) {
		for (Edge e : a.getRcvEdges()) {
			if (e.getSrc().getWcet(level) != 0 && !((ActorSched) e.getSrc()).getVisitedL()[level])
				return false;
		}
		return true;
	}
	
	/**
	 * Internal function that checks if all the sucessors of an actor are visited
	 * @param a
	 * @param level
	 * @return
	 */
	protected boolean succVisitedInLevel (ActorSched a, int level) {
		for (Edge e : a.getSndEdges()) {
			if (e.getDest().getWcet(level) != 0 && !((ActorSched) e.getDest()).getVisitedL()[level])
				return false;
		}
		return true;
	}
	
	/**
	 * Calculates deadline of an actor by considering the dual of the graph
	 * @param a
	 * @param level
	 * @param deadline
	 */
	protected void calcDeadlineReverse (ActorSched a, int level, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSourceinL(level)) {
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getRcvEdges()) {
				test = ((ActorSched) e.getSrc()).getDeadlines()[level] - e.getSrc().getWcet(level);
				if (test < ret)
					ret = test;
			}
		}
		a.setDeadlineInL(ret, level);
	}
	
	/**
	 * Calculates the deadline of an actor, successors should be have their value assigned
	 * first
	 * @param a
	 * @param level
	 * @param deadline
	 */
	protected void calcDeadline (ActorSched a, int level, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSinkinL(level)) {
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getSndEdges()) {
				test = ((ActorSched) e.getDest()).getDeadlines()[level] - e.getDest().getWcet(level);
				if (test < ret)
					ret = test;
			}
		}
		a.setDeadlineInL(ret, level);
	}
	
	/**
	 * Calculate deadlines for a DAG in all its mode
	 * @param d The MC-DAG
	 */
	protected void calcDeadlines (DAG d, int levels) {
		
		// Start by calculating deadlines in HI modes
		for (int i = 1; i < levels; i++) {
			ArrayList<ActorSched> toVisit = new ArrayList<ActorSched>();
			
			// Calculate sources in i mode
			for (Actor a : d.getNodes()) {
				if (a.isSourceinL(i))
					toVisit.add((ActorSched) a);
			}
			
			// Visit all nodes iteratively
			while (!toVisit.isEmpty()) {
				ActorSched a = toVisit.get(0);
				
				calcDeadlineReverse(a, i, d.getDeadline());
				a.getVisitedL()[i] = true;
				
				for (Edge e: a.getSndEdges()) {
					if (e.getDest().getWcet(i) != 0 && !((ActorSched) e.getDest()).getVisitedL()[i]
							&& predVisitedInLevel((ActorSched) e.getDest(), i)
							&& !toVisit.contains((ActorSched) e.getDest())) {
						toVisit.add((ActorSched) e.getDest());
					}
				}
				toVisit.remove(0);
			}
		}
		
		// Calculate deadlines in LO mode
		ArrayList<ActorSched> toVisit = new ArrayList<ActorSched>();
		// Calculate sources in i mode
		for (Actor a : d.getNodes()) {
			if (a.isSinkinL(0))
				toVisit.add((ActorSched) a);
		}
					
		// Visit all nodes iteratively
		while (!toVisit.isEmpty()) {
			ActorSched a = toVisit.get(0);
						
			calcDeadline(a, 0, d.getDeadline());
			a.getVisitedL()[0] = true;
						
			for (Edge e: a.getRcvEdges()) {
				if (!((ActorSched) e.getSrc()).getVisitedL()[0]
						&& succVisitedInLevel((ActorSched) e.getSrc(), 0)
						&& !toVisit.contains((ActorSched) e.getSrc())) {
					toVisit.add((ActorSched) e.getSrc());
				}
			}
			toVisit.remove(0);
		}
	}
}
