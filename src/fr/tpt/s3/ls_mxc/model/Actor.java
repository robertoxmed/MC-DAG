package fr.tpt.s3.ls_mxc.model;

import java.util.Set;

public abstract class Actor {
	
	public static final short LO = 0;
	public static final short HI = 1;

	private int id;
	private String name;
	
	private int[] cIs;
	
	private Set<Edge> rcvEdges;
	private Set<Edge> sndEdges;
	
	private boolean sink;
	private boolean source;
	private boolean sinkHI;
	private boolean sourceHI;
	
	/**
	 * Returns the jth Ci(J)
	 * @param j
	 * @return
	 */
	public int getCI (int j) {
		return this.cIs[j];
	}
	
	
	/**
	 * Tests if the node is an exit node
	 * @return
	 */
	public boolean isExitNode() {
		if (this.getSndEdges().size() == 0)
			return true;
		else
			return false;
	}
	
	
	
	/*
	 * Getters and setters
	 *
	 */
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int[] getcIs() {
		return cIs;
	}
	
	public void setcIs(int[] cIs) {
		this.cIs = cIs;
	}
	
	public Set<Edge> getRcvEdges() {
		return rcvEdges;
	}
	
	public void setRcvEdges(Set<Edge> rcvEdges) {
		this.rcvEdges = rcvEdges;
	}
	
	public Set<Edge> getSndEdges() {
		return sndEdges;
	}
	
	public void setSndEdges(Set<Edge> sndEdges) {
		this.sndEdges = sndEdges;
	}
	
	public boolean isSink() {
		return sink;
	}
	
	public void setSink(boolean sink) {
		this.sink = sink;
	}
	
	public boolean isSource() {
		return source;
	}
	
	public void setSource(boolean source) {
		this.source = source;
	}
	
	public boolean isSinkHI() {
		return sinkHI;
	}
	
	public void setSinkHI(boolean sinkHI) {
		this.sinkHI = sinkHI;
	}
	
	public boolean isSourceHI() {
		return sourceHI;
	}
	
	public void setSourceHI(boolean sourceHI) {
		this.sourceHI = sourceHI;
	}
}
