package fr.tpt.s3.ls_mxc.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.ls_mxc.alloc.LS;

/**
 * Class to model the DAG in MxC
 * @author Roberto Medina
 *
 */
public class DAG {
	
	private Set<Node> Nodes;
	private Set<Node> Nodes_HI;
	private int critPath;
	
	public DAG() {
		Nodes = new HashSet<Node>();
		Nodes_HI = new HashSet<Node>();
	}
	
	/**
	 * Utility methods
	 */
	public List<Node> topoSort() {
		List<Node> sort_nodes = new LinkedList<Node>();
		// Set of nodes with no incoming edges
		Set<Node> not_visited = this.getNodes();
		
		
		// Initialize the topo sort with source nodes
		Iterator<Node> it_n = Nodes.iterator();
		while (it_n.hasNext()) {
			Node n = it_n.next();
			if (n.isSource())
				not_visited.add(n);
			
			// Initialize edges to not visited
			Iterator<Edge> it_s = n.getSnd_edges().iterator();
			Iterator<Edge> it_r = n.getSnd_edges().iterator();
			while (it_s.hasNext()){
				it_s.next().setVisited(false);
			}
			while (it_r.hasNext()){
				it_r.next().setVisited(false);
			}
		}
		
		Iterator<Node> it_n2 = not_visited.iterator();
		while (it_n2.hasNext()) {
			// Add the node to the list -> all its parents are in the list
			Node n = it_n2.next();
			sort_nodes.add(n);
			not_visited.remove(n);
			
			// Check destination nodes and see if their parents were visited
			Iterator<Edge> it_e = n.getSnd_edges().iterator();
			while (it_e.hasNext()) {
				Edge e = it_e.next();
				e.setVisited(true);
				Node m = e.getDest();
				
				Iterator<Edge> it_e2 = m.getRcv_edges().iterator();
				boolean add_m = true;
				while(it_e2.hasNext()) {
					if(!it_e2.next().isVisited()) {
						add_m = false;
						break;
					}
				}
				if (add_m)
					not_visited.add(m);
			}
		}
		return sort_nodes;
	}
	
	/**
	 * Method to get the critical Path.
	 * Needs to be called after getting HLFET levels.
	 */
	public int calcCriticalPath() {
		int cp = 0;
		
		LS ls = new LS();
		ls.setMxcDag(this);
		ls.calcWeights(0);
		ls.calcWeights(1);
		
		for(int i = 0; i < this.getNodes().size(); i++) {
			if (cp < this.getNodebyID(i).getWeight_LO())
				cp = this.getNodebyID(i).getWeight_LO();
			if (cp < this.getNodebyID(i).getWeight_HI())
				cp = this.getNodebyID(i).getWeight_HI();
		}
		this.setCritPath(cp);
		return cp;
	}
	
	
	/**
	 * Getters & Setters
	 * 
	 */
	public Set<Node> getNodes() {
		return Nodes;
	}
	public void setNodes(Set<Node> nodes) {
		Nodes = nodes;
	}
	
	public Node getNodebyID(int id){
		Iterator<Node> it = Nodes.iterator();
		while(it.hasNext()){
			Node n = it.next();
			if (n.getId() == id)
				return n; 
		}
		return null;
	}

	public Set<Node> getNodes_HI() {
		return Nodes_HI;
	}

	public void setNodes_HI(Set<Node> nodes_HI) {
		Nodes_HI = nodes_HI;
	}
	
	public Node getNodeHIbyID(int id){
		Iterator<Node> it = Nodes_HI.iterator();
		while(it.hasNext()){
			Node n = it.next();
			if (n.getId() == id)
				return n; 
		}
		return null;
	}

	public int getCritPath() {
		return critPath;
	}

	public void setCritPath(int critPath) {
		this.critPath = critPath;
	}

	
}
