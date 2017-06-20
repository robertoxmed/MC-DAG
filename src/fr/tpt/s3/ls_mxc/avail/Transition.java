package fr.tpt.s3.ls_mxc.avail;

public class Transition {
	// Failure probability
	private Double p;
	private State src;
	private State destOk;
	private State destFail;
	
	public Double getP() {
		return p;
	}
	public void setP(Double p) {
		this.p = p;
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
	
	
}
