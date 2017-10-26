/*******************************************************************************
 * Copyright (c) 2017 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package fr.tpt.s3.ls_mxc.generator;

import org.apache.commons.cli.*;

/**
 * Main for the Graph generator interface
 * @author Roberto Medina
 *
 */
public class MainGenerator {

	/**
	 * Main method for the generator: it launches a given number of threads with the parameters
	 * given
	 * @param args
	 */
	public static void main (String[] args) {
		
		/* ============================ Command line ================= */
		Options options = new Options();
		
		Option o_hi = new Option("h", "hi_utilization", true, "Max HI Utilization");
		o_hi.setRequired(true);
		options.addOption(o_hi);
		
		Option o_lo = new Option("l", "lo_utilization", true, "Max LO Utilization");
		o_lo.setRequired(true);
		options.addOption(o_lo);
		
		Option o_hi_lo = new Option("hl", "hi_lo_utilization", true, "Max HI Utilization in LO mode");
		o_hi_lo.setRequired(true);
		options.addOption(o_hi_lo);
		
		Option o_eprob = new Option("e", "eprobability", true, "Probability of edges");
		o_eprob.setRequired(true);
		options.addOption(o_eprob);
		
		Option o_para = new Option("p", "parallelism", true, "Max parallelism for the DAGs");
		o_para.setRequired(true);
		options.addOption(o_para);
		
		Option o_cp = new Option("cp", "critical_path", true, "Max critical path of the DAGs");
		o_cp.setRequired(true);
		options.addOption(o_cp);
		
		Option o_nbdags = new Option("nd", "num_dags", true, "Number of DAGs");
		o_nbdags.setRequired(true);
		options.addOption(o_nbdags);
		
		Option o_nbfiles = new Option("nf", "num_files", true, "Number of files");
		o_nbfiles.setRequired(true);
		options.addOption(o_nbfiles);
		
		Option o_cores = new Option("c", "cores", true, "Number of cores");
		o_cores.setRequired(true);
		options.addOption(o_cores);
		
		Option o_out = new Option("o", "output", true, "Output file for the DAG");
		o_out.setRequired(true);
		options.addOption(o_out);
		
		Option graphOpt = new Option("g", "graphviz", false, "Generate a graphviz DOT file");
		graphOpt.setRequired(false);
		options.addOption(graphOpt);
		
		Option debugOpt = new Option("d", "debug", false, "Enabling debug");
		debugOpt.setRequired(false);
		options.addOption(debugOpt);
		
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
		
		double userHI = Double.parseDouble(cmd.getOptionValue("hi_utilization"));
		double userLO = Double.parseDouble(cmd.getOptionValue("lo_utilization"));
		double UserHIinLO = Double.parseDouble(cmd.getOptionValue("hi_lo_utilization"));
		int edgeProb = Integer.parseInt(cmd.getOptionValue("eprobability"));
		int cp = Integer.parseInt(cmd.getOptionValue("critical_path"));
		int nbDags = Integer.parseInt(cmd.getOptionValue("num_dags"));
		int nbFiles = Integer.parseInt(cmd.getOptionValue("num_files"));
		int para = Integer.parseInt(cmd.getOptionValue("parallelism"));
		int cores = Integer.parseInt(cmd.getOptionValue("cores"));
		boolean graph = cmd.hasOption("graphviz");	
		boolean debug = cmd.hasOption("debug");	
		String output = cmd.getOptionValue("output");
		
		/* ============================= Generator parameters ============================= */
		
		if (nbFiles < 0 || nbDags < 0) {
			System.err.println("[ERROR] Generator: Number of files & DAGs need to be positive.");
			formatter.printHelp("DAG Generator", options);
			System.exit(1);
			return;
		}
		
		Thread threads[] = new Thread[nbFiles];
		
		for (int i = 0; i < nbFiles; i++) {
			String outFile = output.substring(0, output.lastIndexOf('.')).concat("-"+i+".xml");
			GeneratorThread gt = new GeneratorThread(userLO, userHI, cp, edgeProb, UserHIinLO, para, cores, nbDags, outFile, graph, debug);
			threads[i] = new Thread(gt);
			threads[i].start();
		}
		
		for (int i = 0; i < nbFiles; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
