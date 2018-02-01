/*******************************************************************************
 * Copyright (c) 2017, 2018 Roberto Medina
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
package fr.tpt.s3.ls_mxc.alloc;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Main class to create the MC-DAG Framework. All functionalities should be included
 * @author roberto
 *
 */
public class Main {

	public static void main (String[] args) throws IOException, InterruptedException {
		
		/* Command line options */
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "MC-DAG XML Models");
		input.setRequired(true);
		input.setArgs(Option.UNLIMITED_VALUES); // Sets maximum number of threads to be launched
		options.addOption(input);
		
		Option outSched = new Option("os", "out-scheduler", false, "Write the scheduling tables into a file.");
		outSched.setRequired(false);
		options.addOption(outSched);
		
		Option outPrism = new Option("op", "out-prism", false, "Write PRISM model into a file.");
		outPrism.setRequired(false);
		options.addOption(outPrism);
		
		Option jobs = new Option("j", "jobs", true, "Number of threads to be launched.");
		jobs.setRequired(false);
		options.addOption(jobs);
		
		Option nlevels = new Option("n", "n-levels", false, "Model has N levels");
		nlevels.setRequired(false);
		options.addOption(nlevels);
		
		Option debugOpt = new Option("d", "debug", false, "Enabling debug.");
		debugOpt.setRequired(false);
		options.addOption(debugOpt);
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			formatter.printHelp("MC-DAG framework", options);
			
			System.exit(1);
			return;
		}
		
		String inputFilePath[] = cmd.getOptionValues("input");
		boolean bOutSched = cmd.hasOption("out-scheduler");
		boolean bOutPrism = cmd.hasOption("out-prism");
		boolean debug = cmd.hasOption("debug");
		boolean levels = cmd.hasOption("n-levels");
		int nbFiles = inputFilePath.length;
		
		int nbJobs = 1;
		if (cmd.hasOption("jobs"))
			nbJobs = Integer.parseInt(cmd.getOptionValue("jobs"));
		
		if (debug)
			System.out.println("[DEBUG] Launching "+inputFilePath.length+" thread(s).");
		
		int i_files = 0;
		ExecutorService executor = Executors.newFixedThreadPool(nbJobs);
		
		/* Launch threads to solve allocation */
		while (i_files != nbFiles) {
			AllocationThread ft = new AllocationThread(inputFilePath[i_files], bOutSched, bOutPrism, debug);
			
			ft.setLevels(levels);
			executor.execute(ft);
			i_files++;
		}
		
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		System.out.println("[FRAMEWORK Main] DONE");
	}
}
