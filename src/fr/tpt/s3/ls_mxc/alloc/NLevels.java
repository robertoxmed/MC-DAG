package fr.tpt.s3.ls_mxc.alloc;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.ActorSched;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.util.MathMCDAG;

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
	// Level, DAG id, Actor id
	private int remainingTime[][][];
	
	// Current min activation time of HI tasks.
	private Hashtable<String, List<Integer>> currMinAct;
	
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
		
		// Init remaining scheduling time tables
		for (DAG d : getMcDags()) {
			for (int i = 0; i < getLevels(); i++) {
				remainingTime[i][d.getId()] = new int[d.getNodes().size()];
			}
		}
	}
	
	/**
	 * Initializes the remaining time to be allocated for each node in each level
	 */
	private void initRemainTime () {
		for (int i = 0; i < getLevels(); i++) {
			for (DAG d : getMcDags()) {
				for (Actor a : d.getNodes()) {
					remainingTime[i][d.getId()][a.getId()] = a.getCI(i);
				}	
			}
		}
		if (debug) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] initRemainTime(): Remaining time of actors initialized!");
	}
	
	/**
	 * Inits the scheduling tables and calculates the hyper-period
	 */
	private void initTables () {
		int[] input = new int[getMcDags().size()];
		int i = 0;
		
		for (DAG d : getMcDags()) {
			input[i] = d.getDeadline();
			i++;
		}
		
		sethPeriod(MathMCDAG.lcm(input));
		
		// Init scheduling tables
		for (i = 0; i < getLevels(); i++)
			sched = new String[getLevels()][gethPeriod()][getNbCores()];
		
		for (i = 0; i < getLevels(); i++) {
			for (int j = 0; j < gethPeriod(); j++) {
				for (int k = 0; k < getNbCores(); k++) {
					sched[i][j][k] = "-";
				}
			}
		}
		
		if (debug) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] initTables(): Sched tables initialized!");
	}
	
	/**
	 * Calculates the LFT of an actor in mode l which is a HI mode
	 * @param a
	 * @param l
	 */
	private void calcActorLFTrev (ActorSched a, int l, int dead) {
		int ret = Integer.MAX_VALUE;
		
		if (a.isSourceinL(l))
	}
	
	/**
	 * Calculates the LFT of an actor in the LO(west) mode
	 * @param a
	 * @param l
	 */
	private void calcActorLFT (ActorSched a, int l, int dead) {
		int ret = Integer.MAX_VALUE;
	}
	
	/**
	 * Builds the scheduling table of level l
	 * @param l
	 * @throws SchedulingException
	 */
	private void buildTable (int l) throws SchedulingException {
		
	}
	
	/**
	 * Builds all the scheduling tables for the system
	 */
	private void buildAllTables () {
		initRemainTime();
		initTables();
		
		for (int i = 0; i < getLevels(); i++) {
			try {
				buildTable(i);
			} catch (SchedulingException se) {
				System.err.println("[ERROR "+Thread.currentThread().getName()+"] Non schedulable example in mode "+i+".");
			}
		}
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

	public Hashtable<String, List<Integer>> getCurrMinAct() {
		return currMinAct;
	}

	public void setCurrMinAct(Hashtable<String, List<Integer>> currMinAct) {
		this.currMinAct = currMinAct;
	}
	
}
