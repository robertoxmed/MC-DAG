package fr.tpt.s3.ls_mxc.model;

public class ActorAvail extends Actor {

	public static final short VOTER = 2;
	public static final short MKFIRM = 3;
	
	// Used for DAG availability analysis
	private double fProb;
	private String votTask;
	private boolean fMechanism;
	private short fMechType;
	private boolean isVoted;
	private int nbReplicas;
	private int M;
	private int K;
	
	public double getfProb() {
		return fProb;
	}
	public void setfProb(double fProb) {
		this.fProb = fProb;
	}
	public String getVotTask() {
		return votTask;
	}
	public void setVotTask(String votTask) {
		this.votTask = votTask;
	}
	public boolean isfMechanism() {
		return fMechanism;
	}
	public void setfMechanism(boolean fMechanism) {
		this.fMechanism = fMechanism;
	}
	public short getfMechType() {
		return fMechType;
	}
	public void setfMechType(short fMechType) {
		this.fMechType = fMechType;
	}
	public boolean isVoted() {
		return isVoted;
	}
	public void setVoted(boolean isVoted) {
		this.isVoted = isVoted;
	}
	public int getNbReplicas() {
		return nbReplicas;
	}
	public void setNbReplicas(int nbReplicas) {
		this.nbReplicas = nbReplicas;
	}
	public int getM() {
		return M;
	}
	public void setM(int m) {
		M = m;
	}
	public int getK() {
		return K;
	}
	public void setK(int k) {
		K = k;
	}
	
}
