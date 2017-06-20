package fr.tpt.s3.ls_mxc.avail;


public class State {
	
	private String task;
	private int mode;
	private boolean produced;
	private Transition t;
	private int id;
	
	public String getTask() {
		return task;
	}
	public void setTask(String task) {
		this.task = task;
	}
	public int getMode() {
		return mode;
	}
	public void setMode(int mode) {
		this.mode = mode;
	}
	public boolean isProduced() {
		return produced;
	}
	public void setProduced(boolean produced) {
		this.produced = produced;
	}
	public Transition getT() {
		return t;
	}
	public void setT(Transition t) {
		this.t = t;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
}
