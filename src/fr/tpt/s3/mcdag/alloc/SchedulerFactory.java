package fr.tpt.s3.mcdag.alloc;

public abstract class SchedulerFactory {
	
	public abstract void initTables();
	
	public abstract void buildAllTables() throws SchedulingException;
}
