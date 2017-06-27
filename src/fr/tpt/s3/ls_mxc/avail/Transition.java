package fr.tpt.s3.ls_mxc.avail;

public class Transition {
	// Failure probability
	private State src;
	private State destOk;
	private State destFail;
	private double p;
	
	/**
	 * Constructor the Transitions object
	 */
	public Transition (State src, State destOk, State destFail) {
		this.setSrc(src);
		this.setDestOk(destOk);
		this.setDestFail(destFail);
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
	
	
}
