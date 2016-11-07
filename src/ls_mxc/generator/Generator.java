package ls_mxc.generator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import ls_mxc.alloc.Edge;
import ls_mxc.alloc.Node;

public class Generator {

	/**
	 * Variables
	 */
	private int nbNodes;
	private int nbCores;
	private int MIN_RANKS;
	private int MAX_RANKS;
	private int MIN_PER_RANK;
	private int MAX_PER_RANK;
	
	private int deadline;
	
	private float edgeProb;
	private Set<Node> Nodes;
	private float hiPerc;
	
	private int[][] adjMatrix;
	
	public Generator(int rank, int cores, int eprob, int hperc, int dead) {
		this.setNbCores(cores);
		this.setEdgeProb(eprob);
		this.setHiPerc(hperc);
		this.setDeadline(dead);
		
		MIN_RANKS = 1;
		MAX_RANKS = rank;
		MIN_PER_RANK = 1;
		MAX_PER_RANK = rank;
	}
	
	/**
	 * Graph generation should avoid 2 things:
	 *  - Making cycles with the edges
	 *  - Creating an edge from a LO task to a HI task
	 * 
	 */
	public void GenerateGraph(){
		
		Set<Node> nodes = new HashSet<Node>();
		Set<Node> new_nodes = new HashSet<Node>();
		Random r = new Random();
		int id = 0;
		
		// How "tall" the DAG should be
		int ranks = MIN_RANKS + (r.nextInt() % (MAX_RANKS - MIN_RANKS + 1));
		
		// For each level of the DAG
		for (int i = 0; i < ranks; i++) {
			
			// Generate a new node with a higher rank than the one previously made
			// And its parameters
			int nb_nodes_rank = MIN_PER_RANK+ (r.nextInt() % (MAX_PER_RANK - MIN_PER_RANK +1));
			
			for(int j = 0; j < nb_nodes_rank; j++) {
			
				Node n = new Node(id, Character.toString((char) ((char)'A'+j+(i*10))), 0, 0);
				n.setRank(i);
				n.setC_LO(r.nextInt(5));
				
				if ((r.nextInt() % 100) < hiPerc)
					n.setC_HI((int) (n.getC_LO() * 1.5));
				else
					n.setC_HI(0);
				
				new_nodes.add(n);
				id++;
			}
		
			
			// Iterate through lower ranks and add an edge only from low rank to higher one
			Iterator<Node> it_n = nodes.iterator();
			while (it_n.hasNext()){
				Iterator<Node> it_n2 = new_nodes.iterator();
				while (it_n2.hasNext()){
					Node src = it_n.next();
					Node dest = it_n2.next();
					
					// Probably of adding an edge between the 2 nodes
					if ((r.nextInt()%100) < edgeProb){
						Edge e = new Edge(src, dest, false);
						src.getSnd_edges().add(e);
						dest.getRcv_edges().add(e);
					}
				}
			}
			
			// Add the "new" nodes to the set
			Iterator<Node> it_n2 = new_nodes.iterator();
			while (it_n2.hasNext()){
				Node n = it_n2.next();
				nodes.add(n);
				it_n2.remove();
			}
			
		}
		setNbNodes(id);
	}
	
	public void createAdjMatrix(){
		adjMatrix = new int[nbNodes][];
		for (int i = 0; i < nbNodes; i++)
			adjMatrix[i] = new int[nbNodes];
		
		Iterator<Node> it_n = Nodes.iterator();
		while (it_n.hasNext()){
			Node n = it_n.next();
			
			Iterator<Edge> it_e = n.getRcv_edges().iterator();
			while (it_e.hasNext()){
				Edge e = it_e.next();
				adjMatrix[e.getDest().getId()][e.getSrc().getId()] = 1;
			}
		}
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
	
	
	public void toFile(String filename) throws IOException{
		BufferedWriter out = null;
		try {
			FileWriter fstream = new FileWriter(filename);
			out = new BufferedWriter(fstream);
			
			// Write number of nodes
			out.write("#NbNodes\n");
			out.write(Integer.toString(this.getNbNodes()) + "\n\n");
			
			// Write number of cores
			out.write("#NbCores\n");
			out.write(Integer.toString(this.getNbCores()) + "\n\n");
			
			// Write number of cores
			out.write("#Deadline\n");
			out.write(Integer.toString(this.getDeadline()) + "\n\n");
			
			//Write C LOs
			out.write("#C_LO\n");
			for (int i = 0; i < nbNodes; i++) {
				Node n = getNodebyID(i);
				out.write(Integer.toString(n.getC_LO()) + "\n");
			}
			out.write("\n");
			
			//Write C HIs
			out.write("#C_HI\n");
			for (int i = 0; i < nbNodes; i++) {
				Node n = getNodebyID(i);
				out.write(Integer.toString(n.getC_HI()) + "\n");
			}
			out.write("\n");
			
			//Write precedence matrix
			out.write("#Pred\n");
			for (int i = 0; i < nbNodes; i++) {
				for (int j = 0; j < nbNodes; j++){
					out.write(Integer.toString(adjMatrix[i][j]));
					if (j < nbNodes - 1)
						out.write(",");
				}
				out.write("\n");
			}
			out.write("\n");
			
		}catch (IOException e ){
			System.out.print("Exception " + e.getMessage());
		}finally{
			if(out != null)
				out.close();
		}
		
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

	public int[][] getAdjMatrix() {
		return adjMatrix;
	}

	public void setAdjMatrix(int[][] adjMatrix) {
		this.adjMatrix = adjMatrix;
	}

	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}
	
	
	
}
