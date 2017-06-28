package fr.tpt.s3.ls_mxc.avail;

public class State {
	
	private String task;
	private int mode;
	private Transition t;
	private int id;
	private int c_t;
	
	public State (int id, String t, int m) {
		setId(id);
		setTask(t);
		setMode(m);
		t = null;
	}
	
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

	public int getC_t() {
		return c_t;
	}

	public void setC_t(int c_t) {
		this.c_t = c_t;
	}
}
