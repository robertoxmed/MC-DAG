package ls_mxc.generator;

import java.util.HashSet;
import java.util.Set;
import ls_mxc.alloc.Node;

public class Generator {

	/**
	 * Variables
	 */
	private int nbNodes;
	private int nbCores;
	
	private float edgeProb;
	private Set<Node> Nodes;
	private float hiPerc;
	
	public Generator(int nodes, int cores, int eprob, int hperc) {
		this.setNbNodes(nodes);
		this.setNbCores(cores);
		this.setEdgeProb(eprob);
		this.setHiPerc(hperc);
	}
	
	/**
	 * Graph generation should avoid 2 things:
	 *  - Making cycles with the edges
	 *  - Creating an edge from a LO task to a HI task
	 * 
	 */
	public void GenerateNodes(){
		
		Set<Node> nodes = new HashSet<Node>();
		
		// How "tall" the DAG should be
		int ranks =  
		
		for (int = 0; i < ranks; i++) {
			
			// Generate a new node with a higher rank than the one previously made
			Node n = new Node(); 
			
			// Iterate through lower ranks and add an edge from low rank to higher one
			Iterator
			
			
		}
	}
	
	
	public void toFile(String filename){
		
	}
	
	/**
	 * Getters and setters
	 */
	public int getNbNodes() {
		return nbNodes;
	}
	public void setNbNodes(int nbNodes) {
		this.nbNodes = nbNodes;
	}
	public int getNbCores() {
		return nbCores;
	}
	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}
	public float getEdgeProb() {
		return edgeProb;
	}
	public void setEdgeProb(float edgeProb) {
		this.edgeProb = edgeProb;
	}
	public Set<Node> getNodes() {
		return Nodes;
	}
	public void setNodes(Set<Node> nodes) {
		Nodes = nodes;
	}
	public float getHiPerc() {
		return hiPerc;
	}
	public void setHiPerc(float hiPerc) {
		this.hiPerc = hiPerc;
	}
	
	
	
}
