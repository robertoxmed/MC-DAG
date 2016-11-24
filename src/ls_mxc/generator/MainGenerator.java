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
		
		Option o_height = new Option("h", "height", true, "Height of the DAG");
		o_height.setRequired(true);
		options.addOption(o_height);
		
		Option o_width = new Option("w", "width", true, "Width of the DAG");
		o_width.setRequired(true);
		options.addOption(o_width);
		
		Option o_eprob = new Option("e", "eprobability", true, "Probability of edges");
		o_eprob.setRequired(true);
		options.addOption(o_eprob);
		
		Option o_hperc = new Option("hp", "hiperc", true, "Percentage of HI tasks");
		o_hperc.setRequired(true);
		options.addOption(o_hperc);
		
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

		int height = Integer.parseInt(cmd.getOptionValue("height"));
		int width = Integer.parseInt(cmd.getOptionValue("width"));
		int eprob = Integer.parseInt(cmd.getOptionValue("eprobability"));
		int hperc = Integer.parseInt(cmd.getOptionValue("hiperc"));
		String output = cmd.getOptionValue("output");
		
		/* ============================= Generator parameters ============================= */
		
		Generator g = new Generator(height, width, eprob, hperc);
		
		UtilizationGenerator ug = new UtilizationGenerator(2, 1, 20, 30, 1);
		
		g.generateGraph();
		
		ug.GenenrateGraphCp();
		
		// Generate the file used for the list scheduling
		try {
			ug.toFile(output);
		} catch (IOException e) {
			System.out.println("To file from generator " + e.getMessage());
			
			System.exit(1);
			return;
		}
		
		// Generate the file used for the CSP
		if (outputDZN != null) {
			try {
				g.toDZN(outputDZN);
			} catch (IOException e) {
				System.out.println("To DZN from generator " + e.getMessage());
			
				System.exit(1);
				return;
			}
		}
	}
}
