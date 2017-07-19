package fr.tpt.s3.ls_mxc.avail;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.tpt.s3.ls_mxc.alloc.LS;
import fr.tpt.s3.ls_mxc.alloc.SchedulingException;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.parser.MCParser;

public class MainAvailability {
		
	@SuppressWarnings("unused")
	public static void main (String[] args) throws IOException {
		
		/* ============================ Command line ================= */
		
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "input file path");
		input.setRequired(true);
		options.addOption(input);
		
		Option output = new Option("o", "output", true, "output file");
		output.setRequired(false);
		options.addOption(output);
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("PRISM model checker generator", options);
			
			System.exit(1);
			return;
		}
		
		
		String inputFilePath = cmd.getOptionValue("input");
		String outputFilePath = cmd.getOptionValue("output");
		
		/* =============== Read from file and try to solve ================ */
		
		DAG dag = new DAG();
		LS ls = new LS();
		Automata auto = new Automata(ls, dag);
		MCParser mcp = new MCParser(inputFilePath, outputFilePath, dag, ls);
		ls.setMxcDag(dag);
		mcp.readXML();
		
		try {
			ls.Alloc_All();
		} catch (SchedulingException e) {
			e.getMessage();
		}
		ls.printS_HI();
		System.out.println("Finished exec");

	}



}
