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
		
		Option o_hi = new Option("mu", "max_utilization", true, "Max HI Utilization");
		o_hi.setRequired(true);
		options.addOption(o_hi);
		
		Option o_lo = new Option("lu", "low_utilization", true, "Max LO Utilization");
		o_lo.setRequired(true);
		options.addOption(o_lo);
				
		Option o_eprob = new Option("e", "eprobability", true, "Probability of edges");
		o_eprob.setRequired(true);
		options.addOption(o_eprob);
		
		Option o_levels = new Option("l", "levels", true, "Number of criticality levels");
		o_levels.setRequired(true);
		options.addOption(o_levels);
		
		Option o_para = new Option("p", "parallelism", true, "Max parallelism for the DAGs");
		o_para.setRequired(true);
		options.addOption(o_para);
		
		Option o_nbdags = new Option("nd", "num_dags", true, "Number of DAGs");
		o_nbdags.setRequired(true);
		options.addOption(o_nbdags);
		
		Option o_nbfiles = new Option("nf", "num_files", true, "Number of files");
		o_nbfiles.setRequired(true);
		options.addOption(o_nbfiles);
		
		Option o_out = new Option("o", "output", true, "Output file for the DAG");
		o_out.setRequired(true);
		options.addOption(o_out);
		
		Option graphOpt = new Option("g", "graphviz", false, "Generate a graphviz DOT file");
		graphOpt.setRequired(false);
		options.addOption(graphOpt);
		
		Option debugOpt = new Option("d", "debug", false, "Enabling debug");
		debugOpt.setRequired(false);
		options.addOption(debugOpt);
		
		Option jobsOpt = new Option("j", "jobs", true, "Number of jobs");
		jobsOpt.setRequired(false);
		options.addOption(jobsOpt);
		
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
		
		double maxU = Double.parseDouble(cmd.getOptionValue("max_utilization"));
		double minU = Double.parseDouble(cmd.getOptionValue("low_utilization"));
		int edgeProb = Integer.parseInt(cmd.getOptionValue("eprobability"));
		int levels = Integer.parseInt(cmd.getOptionValue("levels"));
		int nbDags = Integer.parseInt(cmd.getOptionValue("num_dags"));
		int nbFiles = Integer.parseInt(cmd.getOptionValue("num_files"));
		int para = Integer.parseInt(cmd.getOptionValue("parallelism"));
		boolean graph = cmd.hasOption("graphviz");	
		boolean debug = cmd.hasOption("debug");	
		String output = cmd.getOptionValue("output");
		int nbJobs = 1;
		if (cmd.hasOption("jobs"))
			nbJobs = Integer.parseInt(cmd.getOptionValue("jobs"));
		
		/* ============================= Generator parameters ============================= */
		
		if (nbFiles < 0 || nbDags < 0 || nbJobs < 0) {
			System.err.println("[ERROR] Generator: Number of files & DAGs need to be positive.");
			formatter.printHelp("DAG Generator", options);
			System.exit(1);
			return;
		}
		
		Thread threads[] = new Thread[nbJobs];
		
		int nbFilesCreated = 0;
		int count = 0;
		
		while (nbFilesCreated != nbFiles) {
			int launched = 0;
			
			for (int i = 0; i < nbJobs && count < nbFiles; i++) {
				String outFile = output.substring(0, output.lastIndexOf('.')).concat("-"+count+".xml");
				GeneratorThread gt = new GeneratorThread(minU, maxU, edgeProb, levels, para, nbDags, outFile, graph, debug);
				threads[i] = new Thread(gt);
				threads[i].setName("GeneratorThread-"+i);
				launched++;
				count++;
				threads[i].start();
			}
			
			for (int i = 0; i < launched; i++) {
				try {
					threads[i].join();
					nbFilesCreated++;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
