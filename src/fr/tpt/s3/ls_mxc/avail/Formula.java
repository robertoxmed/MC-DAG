package fr.tpt.s3.ls_mxc.avail;

import java.util.List;

public class Formula {
	
	private String name;
	private List<AutoBoolean> lab;
	
	public Formula (String name, List<AutoBoolean> lab) {
		setName(name);
		setLab(lab);
	}

	public List<AutoBoolean> getLab() {
		return lab;
	}

	public void setLab(List<AutoBoolean> lba) {
		this.lab = lba;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
