/*******************************************************************************
 * Copyright (c) 2017, 2018 Roberto Medina
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
package fr.tpt.s3.mcdag.bench.multidag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Scanner;
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
 * These benchmarks compares us to the state of the art techniques
 * of multiDAG scheduling for MxC systems
 * @author roberto
 *
 */
public class MainBench {

	public static void main (String[] args) throws IOException, InterruptedException {
		
		// Command line options
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "MC-DAG XML models");
		input.setRequired(true);
		input.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(input);
		
		Option output = new Option("o", "output", true, "File where results have to be written.");
		output.setRequired(true);
		options.addOption(output);
		
		Option uUti = new Option("u", "utilization", true, "Utilization.");
		uUti.setRequired(true);
		options.addOption(uUti);
		
		Option output2 = new Option("ot", "output-total", true, "File where total results are being written");
		output2.setRequired(true);
		options.addOption(output2);
		
		Option oCores = new Option("c", "cores", true, "Cores given to the test");
		oCores.setRequired(true);
		options.addOption(oCores);
		
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
		String outputFilePath2 = cmd.getOptionValue("output-total");
		double utilization = Double.parseDouble(cmd.getOptionValue("utilization"));

		boolean boolDebug = cmd.hasOption("debug");
		int nbJobs = 1;
		int nbFiles = inputFilePath.length;
				
		if (cmd.hasOption("jobs"))
			nbJobs = Integer.parseInt(cmd.getOptionValue("jobs"));
		
		int nbCores = Integer.parseInt(cmd.getOptionValue("cores"));
	
		/*
		 *  While files need to be allocated
		 *  run the tests in the pool of threads
		 */
	
		int i_files2 = 0;
		String outFile = outputFilePath.substring(0, outputFilePath.lastIndexOf('.')).concat("-schedulability.csv");
		PrintWriter writer = new PrintWriter(outFile, "UTF-8");
		writer.println("Thread; File; FSched (?); LSched (?); Utilization");
		writer.close();
		
		ExecutorService executor2 = Executors.newFixedThreadPool(nbJobs);
		while (i_files2 != nbFiles) {
			BenchThread bt2 = new BenchThread(inputFilePath[i_files2], outFile, nbCores, boolDebug);
			
			executor2.execute(bt2);
			i_files2++;
		}
		
		executor2.shutdown();
		executor2.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		
		int fedTotal = 0;
		int laxTotal = 0;
		// Read lines in file and do average
		int i = 0;
		File f = new File(outFile);
		@SuppressWarnings("resource")
		Scanner line = new Scanner(f);
		while (line.hasNextLine()) {
			String s = line.nextLine();
			if (i > 0) { // To skip the first line
				try (Scanner inLine = new Scanner(s).useDelimiter("; ")) {
					int j = 0;
					
					while (inLine.hasNext()) {
						String val = inLine.next();
						if (j == 2) {
							fedTotal += Integer.parseInt(val);
						} else if (j == 3) {
							laxTotal += Integer.parseInt(val);
						}
						j++;
					}
				}
			}
			i++;
		}
		
		// Write percentage
		double fedPerc = (double) fedTotal / nbFiles;
		double laxPerc = (double) laxTotal / nbFiles;

		Writer wOutput = new BufferedWriter(new FileWriter(outputFilePath2, true));
		wOutput.write(Thread.currentThread().getName()+"; "+utilization+"; "+fedPerc+"; "+laxPerc+"\n");
		wOutput.close();
		
		System.out.println("[BENCH Main] Done scheduling U = "+utilization+".");
	}
}
