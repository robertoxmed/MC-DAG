package fr.tpt.s3.mcdag.alloc;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.mcdag.model.ActorSched;
import fr.tpt.s3.mcdag.model.DAG;

/**
 * Class implementing Federated MC-DAG scheduling
 * @author roberto
 *
 */
public class Federated {
	
	// Set of DAGs to be scheduled 
	private Set<DAG> mcDags;
	
	// Architecture
	private int nbCores;
	
	// Lists for scheduling
	private List<ActorSched> loList;
	private List<ActorSched> hiList;
	
	private Comparator<ActorSched> loComp;
	private Comparator<ActorSched> hiComp;
	
	private boolean debug;
	
	/**
	 * Constructor
	 * @param system
	 * @param architecture
	 */
	public Federated (Set<DAG> system, int architecture) {
		setMcDags(system);
		setNbCores(architecture);
		setDebug(debug);
		setLoComp(new Comparator<ActorSched>() {
			@Override
			public int compare (ActorSched o1, ActorSched o2) {
				if (o1.getWeightB() - o2.getWeightB() != 0)
					return o1.getWeightB() - o2.getWeightB();
				else
					return o1.getId() - o2.getId();
			}
		});
	}
	
	private void initRemainingTimes (DAG d) {
		
	}
	
	private void initTables (int sched[][][]) {
		
	}
	
	private void calcHLFETs (DAG d, int level) {
		
	}
	
	private void buildHITable (DAG d, int sched[][][]) throws SchedulingException {
		
	}
	
	private void buildLOTable (DAG d, int sched[][][]) throws SchedulingException {
		
	}
	
	public void buildTables () throws SchedulingException {
		
		Set<DAG> heavyDAGs = new HashSet<DAG>();
		Set<DAG> lightDAGs = new HashSet<DAG>();
		
		// Separate heavy and light DAGs
		for (DAG d : getMcDags()) {
			if (d.getUmax() <= 1)
				lightDAGs.add(d);
			else
				heavyDAGs.add(d);
		}
		
		// Check for scheduling of light DAGs

		
		// Check for scheduling of heavy DAGs
		for (DAG d : heavyDAGs) {
			
			// Init remaining times, tables, priority ordering
			int sched[][][] = new int[2][d.getDeadline()][d.getMinCores()];
			
			initRemainingTimes(d);
			initTables(sched);
			
			calcHLFETs(d, 0);
			calcHLFETs(d, 1);
			
			buildHITable(d, sched);
			
			buildLOTable(d, sched);
		}
		

		
	}

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

	public List<ActorSched> getLoList() {
		return loList;
	}

	public void setLoList(List<ActorSched> loList) {
		this.loList = loList;
	}

	public List<ActorSched> getHiList() {
		return hiList;
	}

	public void setHiList(List<ActorSched> hiList) {
		this.hiList = hiList;
	}

	public Comparator<ActorSched> getLoComp() {
		return loComp;
	}

	public void setLoComp(Comparator<ActorSched> loComp) {
		this.loComp = loComp;
	}

	public Comparator<ActorSched> getHiComp() {
		return hiComp;
	}

	public void setHiComp(Comparator<ActorSched> hiComp) {
		this.hiComp = hiComp;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

}
