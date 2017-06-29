package fr.tpt.s3.ls_mxc.avail;

import java.util.LinkedList;
import java.util.List;

public class Transition {
	// Failure probability
	private State src;
	private State destOk;
	private State destFail;
	private double p;
	private List<AutoBoolean> bSet;
	
	/**
	 * Constructor the Transitions object
	 */
	public Transition (State src, State destOk, State destFail) {
		this.setSrc(src);
		this.setDestOk(destOk);
		this.setDestFail(destFail);
		bSet = new LinkedList<AutoBoolean>();
	}
	

	public State getSrc() {
		return src;
	}
	public void setSrc(State src) {
		this.src = src;
	}
	public State getDestOk() {
		return destOk;
	}
	public void setDestOk(State dest) {
		this.destOk = dest;
	}
	public State getDestFail() {
		return destFail;
	}
	public void setDestFail(State destFail) {
		this.destFail = destFail;
	}
	public double getP() {
		return p;
	}
	public void setP(double p) {
		this.p = p;
	}
	public List<AutoBoolean> getbSet() {
		return bSet;
	}
	public void setbSet(List<AutoBoolean> bSet) {
		this.bSet = bSet;
	}

}
