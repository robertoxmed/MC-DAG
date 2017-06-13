package fr.tpt.s3.ls_mxc.alloc;

/**
 * Custom exception for the Scheduler to throw
 * @author Roberto Medina
 *
 */
public class SchedulingException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public SchedulingException (String message) {
		super(message);
	}

}
