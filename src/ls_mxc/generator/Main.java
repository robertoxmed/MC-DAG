package ls_mxc.generator;

import java.io.IOException;

import org.apache.commons.cli.*;
/**
 * 
 * @author Roberto Medina
 *
 */
public class Main {

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
		
		Option o_cores = new Option("c", "cores", true, "Cores for the scheduling");
		o_cores.setRequired(true);
		options.addOption(o_cores);
		
		Option o_eprob = new Option("e", "eprobability", true, "Probability of edges");
		o_eprob.setRequired(true);
		options.addOption(o_eprob);
		
		Option o_hperc = new Option("hp", "hiperc", true, "Percentage of HI tasks");
		o_hperc.setRequired(true);
		options.addOption(o_hperc);
		
		Option o_dead = new Option("d", "deadline", true, "Deadline for the DAG");
		o_dead.setRequired(true);
		options.addOption(o_dead);
		
		Option o_out = new Option("o", "output", true, "Output file for the DAG");
		o_out.setRequired(true);
		options.addOption(o_out);
		
		
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
		
		
		int height = Integer.parseInt(cmd.getOptionValue("height"));
		int width = Integer.parseInt(cmd.getOptionValue("width"));
		int cores = Integer.parseInt(cmd.getOptionValue("cores"));
		int eprob = Integer.parseInt(cmd.getOptionValue("eprobability"));
		int hperc = Integer.parseInt(cmd.getOptionValue("hiperc"));
		int dead = Integer.parseInt(cmd.getOptionValue("deadline"));
		String output = cmd.getOptionValue("output");
		
		/* ============================= Generator parameters ============================= */
		
		Generator g = new Generator(height, width, cores, eprob, hperc, dead);
				
		g.generateGraph();
		
		// Generate the file used for the list scheduling
		try {
			g.toFile(output);
		} catch (IOException e) {
			System.out.println("To file from generator " + e.getMessage());
			
			System.exit(1);
			return;
		}
		
		// Generate the file used for the CSP
		try {
			g.toDZN("/home/roberto/workspace/LS_mxc/src/ls_mxc/tests/ex1.dzn");
		} catch (IOException e) {
			System.out.println("To DZN from generator " + e.getMessage());
			
			System.exit(1);
			return;
		}
	}
}
