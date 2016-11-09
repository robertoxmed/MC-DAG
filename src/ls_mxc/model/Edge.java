package ls_mxc.model;

public class Edge {
	
	// One edge has a src and a destination
	private Node src;
	private Node dest;
	
	private boolean visited;
	
	// To include cost in further implementations
	// private int cost;
	
	/**
	 * Constructor for the Edge
	 */
	public Edge(Node s, Node d, boolean v){
		this.setSrc(s);
		this.setDest(d);
		this.setVisited(v);
	}
	
	/**
	 * Getters & Setters	
	 * 
	 */
	public Node getSrc() {
		return src;
	}
	public void setSrc(Node src) {
		this.src = src;
	}
	public Node getDest() {
		return dest;
	}
	public void setDest(Node dest) {
		this.dest = dest;
	}
	public boolean isVisited() {
		return visited;
	}
	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	

}
