package fr.tpt.s3.ls_mxc.util;

public class AlignScheduler {
	
	/**
	 * Static method to align slots on the scheduling table
	 * @param sched
	 * @param level
	 * @param hPeriod
	 * @param cores
	 */
	public static void align (String sched[][][], int level, int hPeriod, int cores) {
		for (int s = 1; s < hPeriod; s++) {
			String ordered[] = new String[cores];
			
			// Init array
			for (int c = 0; c < cores; c++)
				ordered[c] = "-";
			
			for (int c = 0; c < cores; c++) {
				String task = sched[level][s][c];
				
				// If a task is being executed in the core at that time slot
				if (!task.contentEquals("-")) {
					boolean skip = false;
					// Check if it was scheduled before
					for (int c2 = 0; c2 < cores; c2++) {
						if (sched[level][s - 1][c2].contains(task)) {
							// Swap
							if (!ordered[c2].contentEquals("-")) {
								String tmp = ordered[c2];
								ordered[c2] = task;
								ordered[c] = tmp;
							} else {
								ordered[c2] = task;
							}
							skip = true;
							break;
						}
					}
					if (!skip)
						ordered[c] = task;
				}
			}
			
			// Copy the new order
			for (int c = 0; c < cores; c++)
				sched[level][s][c] = ordered[c];
		}
	}
}
