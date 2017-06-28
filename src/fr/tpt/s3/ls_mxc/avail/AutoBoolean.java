package fr.tpt.s3.ls_mxc.avail;

public class AutoBoolean {
	private Boolean b;
	private String Task;
	
	public AutoBoolean (String task) {
		this.setTask(task);
	}

	public Boolean getB() {
		return b;
	}

	public void setB(Boolean b) {
		this.b = b;
	}

	public String getTask() {
		return Task;
	}

	public void setTask(String task) {
		Task = task;
	}
}
