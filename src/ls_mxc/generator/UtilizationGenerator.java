package ls_mxc.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import ls_mxc.model.DAG;
import ls_mxc.model.Edge;
import ls_mxc.model.Node;

public class UtilizationGenerator {
	
	private int userU_LO;
	private int userU_HI;
	private int userCp;
	private int nbNodes;
	private int nbCores;
	private DAG genDAG;
	private int hiPerc;
	private int edgeProb;


	private int deadline;
	
	private int[][] adjMatrix;

	public UtilizationGenerator (int U_LO, int U_HI, int cp, int hiPerc) {
		this.setUserU_LO(U_LO);
		this.setUserU_HI(U_HI);
		this.setUserCp(cp);
		this.setHiPerc(hiPerc);
	}

	/**
	 * Generates the DAG with U at least equals to U LO and U HI
	 * given by the user. The Critical path is also respected
	 */
	public void GenerateGraph() {
		int nodesPerRank; // Number of nodes at each rank
		int id = 0; // Counter of the ids for the nodes
		Set<Node> nodes = new HashSet<Node>();
		Set<Node> newNodes = new HashSet<Node>();
		setGenDAG(new DAG());
		boolean cpReached = false;
		
		Random r = new Random();
		
		// Execution time that needs to be allocated for each mode
		int budgetLO = getUserCp() * getUserU_LO();
		int budgetHI = getUserCp() * getUserU_HI();
		// Maximum execution time 
		int cLOBound = (int) Math.ceil(getUserCp() / getUserU_HI());
		int currentCPLO = 0;
		int currentCPHI = 0;
		int maxCP = 0;
		
		int i = 0;
		while (!cpReached) {
			nodesPerRank = r.nextInt(10) + 1;
			System.out.println("Height " + i + " with " + nodesPerRank + " nodes." );

			
			for (int j = 0; j < nodesPerRank; j++) {
				Node n = new Node(id, Integer.toString(id), 0, 0);
				n.setRank(i);
				
				n.setC_LO(r.nextInt(20) + 1);
				budgetLO = budgetLO - n.getC_LO(); // Decrement LO budget
				
				if ((r.nextInt(100) % 100 <= hiPerc) || (id == 0))  {
					n.setC_HI(r.nextInt(n.getC_LO()) + n.getC_LO()); 
					budgetHI = budgetHI - n.getC_HI(); // Decrement HI budget
				} else {
					n.setC_HI(0);
				}
				
				newNodes.add(n);
				id++;
			}
			
			// Loop to add edges
			Iterator<Node> it_n = nodes.iterator();
			while (it_n.hasNext()) {
				Iterator<Node> it_n2 = newNodes.iterator();
				Node src = it_n.next();
				while (it_n2.hasNext()) {
					Node dest = it_n2.next();
					
					// Probability of adding an edge between 2 nodes
					// And communciation between them is allowed
					if (r.nextInt(100) <= edgeProb && allowedCommunitcation(src, dest)) {

						int tmpLO = src.CPfromNode(0);
						int tmpHI = src.CPfromNode(1);
						
						if (currentCPLO +  tmpLO >= userCp ||
								currentCPHI + tmpHI >= userCp) {
							// Check if the Critical Path is respected/reached
							cpReached = true;
							System.out.println("CP Reached");
						}
						
						if (currentCPLO < tmpLO)
							currentCPLO = tmpLO;
						if (currentCPHI < tmpHI)
							currentCPHI = tmpHI;
							
						maxCP = (currentCPHI <= currentCPLO) ? currentCPHI : currentCPLO;
						
						Edge e = new Edge(src, dest, false);
						src.getSnd_edges().add(e);
						dest.getRcv_edges().add(e);
					}
				}
			}
			
			Iterator<Node> it_n2 = newNodes.iterator();
			while (it_n2.hasNext()) {
				Node n = it_n2.next();
				nodes.add(n);
				it_n2.remove();
			}
			i++;
		}
		
		if (budgetHI > 0)
			inflateCHIs(nodes, budgetHI);
		else if (budgetLO > 0)
			inflateCLOs(nodes, budgetLO);
		
		setNbNodes(id + 1);
		genDAG.setNodes(nodes);
		setDeadline(genDAG.calcCriticalPath());
		calcMinCores();
		graphSanityCheck();
		createAdjMatrix();
	}
	
	/**
	 * Calculate the minimum number of cores for the Graph.
	 */
	public void calcMinCores() {
		int sumClo = 0;
		int sumChi = 0;
		int max;
		
		Iterator<Node> it_n = genDAG.getNodes().iterator();
		
		while (it_n.hasNext()) {
			Node n = it_n.next();
			sumChi += n.getC_HI();
			sumClo += n.getC_LO();
		}
		
		if (sumChi >= sumClo)
			max = sumChi;
		else
			max = sumClo;
		
		this.setNbCores((int)Math.ceil(max/this.getDeadline()) + 1);
	}
	
	public boolean allowedCommunitcation (Node src, Node dest) {
		if ((src.getC_HI() > 0 && dest.getC_HI() >= 0) ||
		(src.getC_HI() == 0 && dest.getC_HI() == 0))
			return true;
		
		return false;	
	}
	
	public void inflateCHIs(Set<Node> nodes, int budgetHI) {
		Iterator<Node> it_n;
		while (budgetHI > 0) {
			it_n = nodes.iterator();
			while (it_n.hasNext() && budgetHI > 0) {
				Node n = it_n.next();
				if (n.getC_HI() > 0) {
					n.setC_HI(n.getC_HI()+1);
					budgetHI--;
				}
			}
		}
	}
	
	public void inflateCLOs(Set<Node> nodes, int budgetLO) {
		Iterator<Node> it_n;
		while (budgetLO > 0) {
			it_n = nodes.iterator();
			while (it_n.hasNext() && budgetLO > 0) {
				Node n = it_n.next();
			
				n.setC_LO(n.getC_LO()+1);
				budgetLO--;
			}
		}
	}
	
	/**
	 * Sanity check for the graph:
	 * 	- Each node has to have at least one edge
	 */
	public void graphSanityCheck() {
		boolean added = false;
		Iterator<Node> it_n = genDAG.getNodes().iterator();
		
		while (it_n.hasNext()) {
			Node n = it_n.next();
			
			// It is an independent node with no edges
			if (n.getRcv_edges().size() == 0 && n.getSnd_edges().size() == 0) {
				Iterator<Node> it_n2 = genDAG.getNodes().iterator();
				while (it_n2.hasNext() && added == false) {
					Node n2 = it_n2.next(); 
					if ((n.getRank() < n2.getRank()) &&
							((n.getC_HI() > 0 && n2.getC_HI() > 0) ||
							 (n.getC_HI() == 0 && n2.getC_HI() == 0) ||
							 (n.getC_HI() > 0 && n2.getC_HI() == 0))){
						Edge e = new Edge(n, n2, false);
						n.getSnd_edges().add(e);
						n2.getRcv_edges().add(e);
						added = true; 
					} else if (n.getRank() > n2.getRank() &&
							((n.getC_HI() > 0 && n2.getC_HI() > 0) ||
							 (n.getC_HI() == 0 && n2.getC_HI() == 0) ||
							 (n.getC_HI() == 0 && n2.getC_HI() > 0))) {
						Edge e = new Edge(n2, n, false);
						n.getRcv_edges().add(e);
						n2.getSnd_edges().add(e);
						added = true;
					}
				}
				added = false;
			}
		}
	}
	
	
	/**
	 * Creates the matrix to be written in the files
	 */
	public void createAdjMatrix(){
		adjMatrix = new int[nbNodes][];
		for (int i = 0; i < nbNodes; i++)
			adjMatrix[i] = new int[nbNodes];
		
		Iterator<Node> it_n = genDAG.getNodes().iterator();
		while (it_n.hasNext()){
			Node n = it_n.next();
			
			Iterator<Edge> it_e = n.getRcv_edges().iterator();
			while (it_e.hasNext()){
				Edge e = it_e.next();
				adjMatrix[e.getDest().getId()][e.getSrc().getId()] = 1;
			}
		}
	}
	
	/**
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void toFile(String filename) throws IOException {
		BufferedWriter out = null;
		try {
			File f = new File(filename);
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
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
			for (int i = 0; i < nbNodes - 1; i++) {
				Node n = genDAG.getNodebyID(i);
				out.write(Integer.toString(n.getC_LO()) + "\n");
			}
			out.write("\n");
			
			//Write C HIs
			out.write("#C_HI\n");
			for (int i = 0; i < nbNodes - 1; i++) {
				Node n = genDAG.getNodebyID(i);
				out.write(Integer.toString(n.getC_HI()) + "\n");
			}
			out.write("\n");
			
			//Write precedence matrix
			out.write("#Pred\n");
			for (int i = 0; i < nbNodes - 1; i++) {
				for (int j = 0; j < nbNodes - 1; j++){
					out.write(Integer.toString(adjMatrix[i][j]));
					if (j < nbNodes - 2)
						out.write(",");
				}
				out.write("\n");
			}
			out.write("\n");
			
		}catch (IOException e){
			System.out.print("To File : " + e.getMessage());
		}finally{
			if(out != null)
				out.close();
		}
	}
	

	/**
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void toDZN(String filename) throws IOException {
		
	}

	/**
	 * Getters and setters
	 */
	public int getUserU_LO() {
		return userU_LO;
	}

	public void setUserU_LO(int userU_LO) {
		this.userU_LO = userU_LO;
	}

	public int getUserCp() {
		return userCp;
	}

	public void setUserCp(int userCp) {
		this.userCp = userCp;
	}

	public int getUserU_HI() {
		return userU_HI;
	}

	public void setUserU_HI(int userU_HI) {
		this.userU_HI = userU_HI;
	}

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

	public DAG getGenDAG() {
		return genDAG;
	}

	public void setGenDAG(DAG genDAG) {
		this.genDAG = genDAG;
	}
	public int getHiPerc() {
		return hiPerc;
	}

	public void setHiPerc(int hiPerc) {
		this.hiPerc = hiPerc;
	}

	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}
	public int getEdgeProb() {
		return edgeProb;
	}

	public void setEdgeProb(int edgeProb) {
		this.edgeProb = edgeProb;
	}
}
