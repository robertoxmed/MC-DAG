package ls_mxc.generator;

import java.io.IOException;

import org.apache.commons.cli.*;

/**
 * Main for the Graph generator interface
 * @author Roberto Medina
 *
 */
public class MainGenerator {

	/**
	 * 
	 * @param args
	 */
	public static void main (String[] args) {
		
		/* ============================ Command line ================= */

		Options options = new Options();
		
		Option o_hi = new Option("h", "hi_utilization", true, "HI Utilization");
		o_hi.setRequired(true);
		options.addOption(o_hi);
		
		Option o_lo = new Option("l", "lo_utilization", true, "LO Utilization");
		o_lo.setRequired(true);
		options.addOption(o_lo);
		
		Option o_hi_lo = new Option("hl", "hi_lo_utilization", true, "HI Utilization in LO mode");
		o_hi_lo.setRequired(true);
		options.addOption(o_hi_lo);
		
		Option o_eprob = new Option("e", "eprobability", true, "Probability of edges");
		o_eprob.setRequired(true);
		options.addOption(o_eprob);
		
		Option o_para = new Option("p", "parallelism", true, "Max parallelism for the DAG");
		o_para.setRequired(true);
		options.addOption(o_para);
		
		Option o_cp = new Option("cp", "critical_path", true, "Critical Path of the DAG");
		o_cp.setRequired(true);
		options.addOption(o_cp);
		
		Option o_cores = new Option("c", "cores", true, "Number of cores");
		o_cores.setRequired(true);
		options.addOption(o_cores);
		
		Option o_out = new Option("o", "output", true, "Output file for the DAG");
		o_out.setRequired(true);
		options.addOption(o_out);
		
		Option o_dznout = new Option("d", "dzn_output", true, "Output file for the DAG in DZN format");
		o_dznout.setRequired(false);
		options.addOption(o_dznout);
		
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("DAG Generator", options);
			
			System.exit(1);
			return;
		}
		
		String outputDZN = cmd.getOptionValue("dzn_output");

		double userHI = Double.parseDouble(cmd.getOptionValue("hi_utilization"));
		double userLO = Double.parseDouble(cmd.getOptionValue("lo_utilization"));
		double UserHIinLO = Double.parseDouble(cmd.getOptionValue("hi_lo_utilization"));
		int edgeProb = Integer.parseInt(cmd.getOptionValue("eprobability"));
		int cp = Integer.parseInt(cmd.getOptionValue("critical_path"));
		int para = Integer.parseInt(cmd.getOptionValue("parallelism"));
		int cores = Integer.parseInt(cmd.getOptionValue("cores"));
		
		String output = cmd.getOptionValue("output");
		
		/* ============================= Generator parameters ============================= */
		
		
		UtilizationGenerator ug = new UtilizationGenerator(userLO, userHI, cp, edgeProb, UserHIinLO, para, cores);
		
		ug.GenenrateGraphCp();
		
		// Generate the file used for the list scheduling
		try {
			ug.toFile(output);
			System.out.println("Written to file!\n");
		} catch (IOException e) {
			System.out.println("To file from generator " + e.getMessage());
			
			System.exit(1);
			return;
		}
		
		// Generate the file used for the CSP
		if (outputDZN != null) {
			try {
				ug.toDZN(outputDZN);
			} catch (IOException e) {
				System.out.println("To DZN from generator " + e.getMessage());
			
				System.exit(1);
				return;
			}
		}
	}
}
