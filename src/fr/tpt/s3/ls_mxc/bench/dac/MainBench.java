/*******************************************************************************
 * Copyright (c) 2017 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package fr.tpt.s3.ls_mxc.bench.dac;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This benchmarks compares us to the state of the art techniques
 * of multiDAG scheduling for MxC systems
 * @author roberto
 *
 */
public class MainBench {

	public static void main (String[] args) throws IOException, InterruptedException {
		
		// Command line options
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "MC-DAG XML models.");
		input.setRequired(true);
		input.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(input);
		
		Option output = new Option("o", "output", true, "File where results have to be written.");
		output.setRequired(true);
		options.addOption(output);
		
		Option jobs = new Option("j", "jobs", true, "Number of threads to be launched.");
		jobs.setRequired(false);
		options.addOption(jobs);
		
		Option debug = new Option("d", "debug", false, "Debug logs.");
		debug.setRequired(false);
		options.addOption(debug);
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e ) {
			System.err.println(e.getMessage());
			formatter.printHelp("Benchmarks MultiDAG", options);
			System.exit(1);
			return;
		}
		
		String inputFilePath[] = cmd.getOptionValues("input");
		String outputFilePath = cmd.getOptionValue("output");
		boolean boolDebug = cmd.hasOption("debug");
		int nbJobs = 1;
		int nbFiles = inputFilePath.length;
				
		if (cmd.hasOption("jobs"))
			nbJobs = Integer.parseInt(cmd.getOptionValue("jobs"));
		
		/*
		 * Write the fields at the beginning of the output file
		 */
		PrintWriter writer = new PrintWriter(outputFilePath, "UTF-8");
		writer.println("Thread; File; Federated; FSched (?) ; Laxity; LSched (?)");
		writer.close();
		
		/*
		 *  While files need to be allocated
		 *  run the tests in the pool of threads
		 */
		
		int i_files = 0;
		ExecutorService executor = Executors.newFixedThreadPool(nbJobs);
		
		while (i_files != nbFiles) {
			BenchThread bt = new BenchThread(inputFilePath[i_files], outputFilePath, boolDebug);
			
			executor.execute(bt);
			i_files++;
		}
		executor.shutdown();
		System.out.println("[BENCH Main] DONE");

	}
}
