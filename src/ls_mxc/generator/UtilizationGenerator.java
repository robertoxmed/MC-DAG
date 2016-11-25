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
	private int edgeProb;
	private int uHIinLO;


	private int deadline;
	
	private int[][] adjMatrix;

	public UtilizationGenerator (int U_LO, int U_HI, int cp, int edgeProb, int UHIinLO) {
		this.setUserU_LO(U_LO);
		this.setUserU_HI(U_HI);
		this.setUserCp(cp);
		this.setEdgeProb(edgeProb);
		this.setuHIinLO(UHIinLO);
	}

	
	/**
	 * Generates a DAG
	 */
	public void GenenrateGraphCp() {
		// Variables
		int id = 0;
		setGenDAG(new DAG());
		Set<Node> nodes = new HashSet<Node>();
		boolean cpReached = false;
		int rank = 0;
		Random r = new Random();
		
		// Budgets deduced by utilization and CP
		int budgetHI = userCp * userU_HI;
		int budgetLO = userCp * userU_LO;
		int CHIBound = (int) Math.ceil(userCp / userU_HI);
		int CLOBound = (int) Math.ceil(userCp / userU_LO);
		
		if (userU_HI == 1)
			CHIBound = CLOBound;
		
		
		// Generate the CP in HI mode
		Node last = null;
		int toCP = userCp;
		while (!cpReached) {
			Node n = new Node(id, Integer.toString(id), 0, 0);
			n.setRank(rank);
			
			n.setC_HI(r.nextInt(CHIBound) + 2);
			
			
			// Add egde and update the CP (if not source)
			if (id != 0) {
				Edge e = new Edge(last, n, false);
				last.getSnd_edges().add(e);
				n.getRcv_edges().add(e);
			}
			
			nodes.add(n);
			n.CPfromNode(1);
			last = n;
			rank++;
			id++;
			
			if (toCP - n.getC_HI() > 0) {
				toCP = toCP  - n.getC_HI();
				budgetHI = budgetHI - n.getC_HI();
			} else {
				n.setC_HI(toCP);
				budgetHI = budgetHI - n.getC_HI();
				cpReached = true;
			}
			n.setC_LO(n.getC_HI());
		}
		
		// Generate the other HI nodes and the arcs
		rank = 0;		
		while (budgetHI > 0) {
			// Roll a number of nodes to add to the level
			int nodesPerRank = r.nextInt(5);
			for (int j=0; j < nodesPerRank && budgetHI > 0; j++) {
				Node n = new Node(id, Integer.toString(id), 0, 0);
			
				// Roll a C_HI and test if budget is left
				n.setC_HI(r.nextInt(CHIBound) + 2);
				if (budgetHI - n.getC_HI() > 0) {
					budgetHI = budgetHI - n.getC_HI();
				} else {
					n.setC_HI(budgetHI);
					budgetHI = 0;
					System.out.println("Budget for HI tasks reached!\n");
				}
				
				n.setRank(rank);
				if (rank != 0) {
					Iterator<Node> it_n = nodes.iterator();
					while (it_n.hasNext()) {
						Node src = it_n.next();
						// Test if the rank of the source is lower and if the CP
						// is not reached
						if (r.nextInt(100) <= edgeProb && n.getRank() > src.getRank()
								&& src.getCpFromNode_HI() + n.getC_HI() <= userCp) {
							Edge e = new Edge(src, n, false);
							src.getSnd_edges().add(e);
							n.getRcv_edges().add(e);
						}
					}
				}
				n.setC_LO(n.getC_HI());
				n.CPfromNode(1);
				nodes.add(n);
				id++;
			}

			rank++;
		}
		
		// Deflate HI execution times
		int wantedHIinLO = uHIinLO * userCp;
		int actualBudget = userU_HI * userCp;
		Iterator<Node> it_n;
		System.out.println("Trying to respect " + wantedHIinLO + " budget of HI tasks in LO mode from "+ actualBudget);
		while (wantedHIinLO <= actualBudget || allHIareMin(nodes)) {
			it_n = nodes.iterator();
			while (it_n.hasNext()) {
				Node n = it_n.next();
				
				n.setC_LO(r.nextInt(n.getC_LO()) + 1);				
				if (n.getC_LO() == 0)
					n.setC_LO(1);
				
				n.CPfromNode(0);
				actualBudget = actualBudget - n.getC_LO();
			}
		}
		
		System.out.println("Deflation completed! Actual budget " + actualBudget);
		
		// Add LO nodes
		actualBudget = 0;
		it_n = nodes.iterator();
		while (it_n.hasNext()) {
			actualBudget += it_n.next().getC_LO();
		}
		
		
		budgetLO = budgetLO - actualBudget;
		System.out.println("\nStarting LO node generation with budget " + budgetLO);
		
				
		rank = 0;
		while (budgetLO > 0) {
			// Roll a number of nodes to add to the level
			int nodesPerRank = r.nextInt(5);
			for (int j=0; j < nodesPerRank && budgetLO > 0; j++) {
				Node n = new Node(id, Integer.toString(id), 0, 0);
			
				// Roll a C_HI and test if budget is left
				n.setC_HI(0);
				n.setC_LO(r.nextInt(CLOBound));
				if (n.getC_LO() == 0)
					n.setC_LO(1); // Minimal execution time
				
				if (budgetLO - n.getC_LO() > 0) {
					budgetLO = budgetLO - n.getC_LO();
				} else {
					n.setC_LO(budgetLO);
					budgetLO = 0;
				}
				
				n.setRank(rank);
				if (rank != 0) {
					Iterator<Node> it = nodes.iterator();
					while (it.hasNext()) {
						Node src = it.next();
						// Test if the rank of the source is lower and if the CP
						// is not reached
						if (r.nextInt(100) <= edgeProb && n.getRank() > src.getRank()
								&& src.getCpFromNode_LO() + n.getC_LO() <= userCp &&
								allowedCommunitcation(src, n)) {
							Edge e = new Edge(src, n, false);
							src.getSnd_edges().add(e);
							n.getRcv_edges().add(e);
						}
					}
				}
				n.CPfromNode(0);
				nodes.add(n);
				id++;
			}

			rank++;
		}
		
		
		setNbNodes(id + 1);
		genDAG.setNodes(nodes);
		setDeadline(genDAG.calcCriticalPath());
		graphSanityCheck();
		calcMinCores();
		createAdjMatrix();
		
		System.out.println("Generation finished!");
	}
	
	
	/**
	 * Tests if all HI nodes are minimal execution <=> C LO = 1
	 * @param nodes
	 * @return
	 */
	public boolean allHIareMin (Set<Node> nodes) {
		Iterator<Node> it = nodes.iterator();
		while (it.hasNext()){
			if (it.next().getC_LO() != 1)
				return false;
		}
		return true;
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

	public int getuHIinLO() {
		return uHIinLO;
	}

	public void setuHIinLO(int uHIinLO) {
		this.uHIinLO = uHIinLO;
	}
}
