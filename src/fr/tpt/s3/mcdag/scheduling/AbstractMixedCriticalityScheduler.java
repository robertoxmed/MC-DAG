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
package fr.tpt.s3.mcdag.scheduling;

import java.util.ArrayList;

import fr.tpt.s3.mcdag.model.Vertex;
import fr.tpt.s3.mcdag.model.VertexScheduling;
import fr.tpt.s3.mcdag.model.McDAG;
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
	protected boolean predVisitedInLevel (VertexScheduling a, int level) {
		for (Edge e : a.getRcvEdges()) {
			if (e.getSrc().getWcet(level) != 0 && !((VertexScheduling) e.getSrc()).getVisitedL()[level])
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
	protected boolean succVisitedInLevel (VertexScheduling a, int level) {
		for (Edge e : a.getSndEdges()) {
			if (e.getDest().getWcet(level) != 0 && !((VertexScheduling) e.getDest()).getVisitedL()[level])
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
	protected void calcDeadlineReverse (VertexScheduling a, int level, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSourceinL(level)) {
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getRcvEdges()) {
				test = ((VertexScheduling) e.getSrc()).getDeadlines()[level] - e.getSrc().getWcet(level);
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
	protected void calcDeadline (VertexScheduling a, int level, int deadline) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSinkinL(level)) {
			ret = deadline;
		} else {
			int test = Integer.MAX_VALUE;
			
			for (Edge e : a.getSndEdges()) {
				test = ((VertexScheduling) e.getDest()).getDeadlines()[level] - e.getDest().getWcet(level);
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
	protected void calcDeadlines (McDAG d, int levels) {
		
		// Start by calculating deadlines in HI modes
		for (int i = 1; i < levels; i++) {
			ArrayList<VertexScheduling> toVisit = new ArrayList<VertexScheduling>();
			
			// Calculate sources in i mode
			for (Vertex a : d.getVertices()) {
				if (a.isSourceinL(i))
					toVisit.add((VertexScheduling) a);
			}
			
			// Visit all nodes iteratively
			while (!toVisit.isEmpty()) {
				VertexScheduling a = toVisit.get(0);
				
				calcDeadlineReverse(a, i, d.getDeadline());
				a.getVisitedL()[i] = true;
				
				for (Edge e: a.getSndEdges()) {
					if (e.getDest().getWcet(i) != 0 && !((VertexScheduling) e.getDest()).getVisitedL()[i]
							&& predVisitedInLevel((VertexScheduling) e.getDest(), i)
							&& !toVisit.contains((VertexScheduling) e.getDest())) {
						toVisit.add((VertexScheduling) e.getDest());
					}
				}
				toVisit.remove(0);
			}
		}
		
		// Calculate deadlines in LO mode
		ArrayList<VertexScheduling> toVisit = new ArrayList<VertexScheduling>();
		// Calculate sources in i mode
		for (Vertex a : d.getVertices()) {
			if (a.isSinkinL(0))
				toVisit.add((VertexScheduling) a);
		}
					
		// Visit all nodes iteratively
		while (!toVisit.isEmpty()) {
			VertexScheduling a = toVisit.get(0);
						
			calcDeadline(a, 0, d.getDeadline());
			a.getVisitedL()[0] = true;
						
			for (Edge e: a.getRcvEdges()) {
				if (!((VertexScheduling) e.getSrc()).getVisitedL()[0]
						&& succVisitedInLevel((VertexScheduling) e.getSrc(), 0)
						&& !toVisit.contains((VertexScheduling) e.getSrc())) {
					toVisit.add((VertexScheduling) e.getSrc());
				}
			}
			toVisit.remove(0);
		}
	}
}
