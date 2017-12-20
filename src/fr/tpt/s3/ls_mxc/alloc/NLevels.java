package fr.tpt.s3.ls_mxc.alloc;

import java.util.Set;

import fr.tpt.s3.ls_mxc.model.DAG;

public class NLevels {
	
	// Set of DAGs to be scheduled
	private Set<DAG> mcDags;
	
	// Architecture + hyperperiod + nb of levels
	private int nbCores;
	private int hPeriod;
	private int levels;
	
	// Scheduling tables
	private String sched[][][];
	
	// Remaining time to be allocated for each node
	private int remainingTime[][][];
	
	// Debugging boolean
	private boolean debug;
	
	/**
	 * Constructor
	 * @param dags
	 * @param cores
	 * @param levels
	 * @param debug
	 */
	public NLevels (Set<DAG> dags, int cores, int levels, boolean debug) {
		setMcDags(dags);
		setNbCores(cores);
		setLevels(levels);
		setDebug(debug);
		remainingTime = new int[getLevels()][getMcDags().size()][];
		
		for (DAG d : getMcDags()) {
			for (int i = 0; i < getLevels(); i++) {
				remainingTime[i][d.getId()] = new int[d.getNodes().size()];
			}
		}
	}
	
	/**
	 * Inits the remaining time to be allocated for each node in each level
	 */
	private void initRemainTime () {
		for (DAG d : getMcDags()) {
			for (int i = 0; i < getLevels(); i++) {
				sched = new String[getLevels()][gethPeriod()][getNbCores()];
			}
		}
	}
	
	
	private void buildTable (int level) throws SchedulingException {
		
	}
	
	
	private void buildAllTables () {
		
	}
	
	/*
	 * Getters & Setters
	 */
	
	public Set<DAG> getMcDags() {
		return mcDags;
	}

	public void setMcDags(Set<DAG> mcDags) {
		this.mcDags = mcDags;
	}

	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}

	public int gethPeriod() {
		return hPeriod;
	}

	public void sethPeriod(int hPeriod) {
		this.hPeriod = hPeriod;
	}

	public int getLevels() {
		return levels;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}

	public String[][][] getSched() {
		return sched;
	}

	public void setSched(String[][][] sched) {
		this.sched = sched;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int[][][] getRemainingTime() {
		return remainingTime;
	}

	public void setRemainingTime(int remainingTime[][][]) {
		this.remainingTime = remainingTime;
	}
	
	
	
}
