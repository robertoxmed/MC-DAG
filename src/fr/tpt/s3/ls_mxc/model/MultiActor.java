package fr.tpt.s3.ls_mxc.model;

public class MultiActor extends Actor{

	// Used for multi dag scheduling
	private int urgencyLO;
	private int urgencyHI;
	private int earDeadLO;
	private int earDeadHI;
	
	public MultiActor(int id, String name, int c_lo, int c_hi) {
		super(id, name, c_lo, c_hi);
		// TODO Auto-generated constructor stub
		urgencyLO = Integer.MAX_VALUE;
		urgencyHI = Integer.MAX_VALUE;
	}

	public int getUrgencyHI() {
		return urgencyHI;
	}

	public void setUrgencyHI(int urgencyHI) {
		this.urgencyHI = urgencyHI;
	}

	public int getUrgencyLO() {
		return urgencyLO;
	}

	public void setUrgencyLO(int urgencyLO) {
		this.urgencyLO = urgencyLO;
	}

	public int getEarDeadLO() {
		return earDeadLO;
	}

	public void setEarDeadLO(int earDeadLO) {
		this.earDeadLO = earDeadLO;
	}

	public int getEarDeadHI() {
		return earDeadHI;
	}

	public void setEarDeadHI(int earDeadHI) {
		this.earDeadHI = earDeadHI;
	}
	
}
