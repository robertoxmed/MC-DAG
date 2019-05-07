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
package fr.tpt.s3.mcdag.bench;

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

import fr.tpt.s3.mcdag.bench.multidag.BenchThreadDualCriticality;
import fr.tpt.s3.mcdag.bench.nlevel.BenchThreadNLevels;

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
		
		Option output = new Option("o", "output", true, "Folder where results have to be written.");
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
		
		Option oLvls = new Option("l", "levels", true, "Levels tested for the system");
		oLvls.setRequired(true);
		options.addOption(oLvls);
		
		Option jobs = new Option("j", "jobs", true, "Number of threads to be launched.");
		jobs.setRequired(false);
		options.addOption(jobs);
		
		Option debug = new Option("d", "debug", false, "Debug logs.");
		debug.setRequired(false);
		options.addOption(debug);
		
		/*
		 * Parsing of the command line
		 */
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
		String outputFilePathTotal = cmd.getOptionValue("output-total");
		double utilization = Double.parseDouble(cmd.getOptionValue("utilization"));
		boolean boolDebug = cmd.hasOption("debug");
		int nbLvls = Integer.parseInt(cmd.getOptionValue("levels"));
		int nbJobs = 1;
		int nbFiles = inputFilePath.length;
				
		if (cmd.hasOption("jobs"))
			nbJobs = Integer.parseInt(cmd.getOptionValue("jobs"));
		
		int nbCores = Integer.parseInt(cmd.getOptionValue("cores"));
	
		/*
		 *  While files need to be allocated
		 *  run the tests in the pool of threads
		 */
		
		// For dual-criticality systems we call a specific thread
		if (nbLvls == 2) {			
			int i_files2 = 0;
			String outFile = outputFilePath.substring(0, outputFilePath.lastIndexOf('.')).concat("-schedulability.csv");
			PrintWriter writer = new PrintWriter(outFile, "UTF-8");
			writer.println("Thread; File; FSched (%); FPreempts; FAct; LSched (%); LPreempts; LAct; ESched (%); EPreempts; EAct; HSched(%); HPreempts; HAct; Utilization");
			writer.close();
						
			ExecutorService executor = Executors.newFixedThreadPool(nbJobs);
			while (i_files2 != nbFiles) {
				BenchThreadDualCriticality bt2 = new BenchThreadDualCriticality(inputFilePath[i_files2], outFile, nbCores, boolDebug);
				
				executor.execute(bt2);
				i_files2++;
			}
			
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			
			int fedTotal = 0;
			int laxTotal = 0;
			int edfTotal = 0;
			int hybridTotal = 0;
			int fedPreempts = 0;
			int laxPreempts = 0;
			int edfPreempts = 0;
			int hybridPreempts = 0;
			int fedActiv = 0;
			int laxActiv = 0;
			int edfActiv = 0;
			int hybridActiv = 0;
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
								fedPreempts += Integer.parseInt(val);
							} else if (j == 4) {
								fedActiv += Integer.parseInt(val);
							} else if (j == 5) {
								laxTotal += Integer.parseInt(val);
							} else if (j == 6) {
								laxPreempts += Integer.parseInt(val);
							} else if (j == 7) {
								laxActiv += Integer.parseInt(val);
							} else if (j == 8) {
								edfTotal += Integer.parseInt(val);
							} else if (j == 9) {
								edfPreempts += Integer.parseInt(val);
							} else if (j == 10) {
								edfActiv += Integer.parseInt(val);
							} else if (j == 11) {
								hybridTotal += Integer.parseInt(val);
							} else if (j == 12) {
								hybridPreempts += Integer.parseInt(val);
							} else if (j == 13) {
								hybridActiv += Integer.parseInt(val);
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
			double edfPerc = (double) edfTotal / nbFiles;
			double hybridPerc = (double) hybridTotal / nbFiles;
			
			double fedPercPreempts = (double) fedPreempts / fedActiv;
			double laxPercPreempts = (double) laxPreempts / laxActiv;
			double edfPercPreempts = (double) edfPreempts / edfActiv;
			double hybridPercPreempts = (double) hybridPreempts / hybridActiv;
			
			Writer wOutput = new BufferedWriter(new FileWriter(outputFilePathTotal, true));
			wOutput.write(Thread.currentThread().getName()+"; "+utilization+"; "+fedPerc+"; "+fedPreempts+"; "+fedActiv+"; "+fedPercPreempts+"; "
						  +laxPerc+"; "+laxPreempts+"; "+laxActiv+"; "+laxPercPreempts+"; "
						  +edfPerc+"; "+edfPreempts+"; "+edfActiv+"; "+edfPercPreempts+"; "
						  +hybridPerc+"; "+hybridPreempts+"; "+hybridActiv+"; "+hybridPercPreempts+"\n");
			wOutput.close();
			
		} else if (nbLvls > 2) {
			int i_files2 = 0;
			String outFile = outputFilePath.substring(0, outputFilePath.lastIndexOf('.')).concat("-schedulability.csv");
			PrintWriter writer = new PrintWriter(outFile, "UTF-8");
			writer.println("Thread; File; LLF (%); LLFPreempts; LLFAct; EDF (%); EDFPreempts; EDFAct; EZL(%); EZLPreempts; EZLAct; Utilization");
			writer.close();
			

			
			ExecutorService executor2 = Executors.newFixedThreadPool(nbJobs);
			while (i_files2 != nbFiles) {
				BenchThreadNLevels bt2 = new BenchThreadNLevels(inputFilePath[i_files2], outFile, nbCores, boolDebug);
				
				executor2.execute(bt2);
				i_files2++;
			}
			
			executor2.shutdown();
			executor2.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			
			int laxTotal = 0;
			int edfTotal = 0;
			int hybridTotal = 0;
			int laxPreempts = 0;
			int edfPreempts = 0;
			int hybridPreempts = 0;
			int laxActiv = 0;
			int edfActiv = 0;
			int hybridActiv = 0;
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
								laxTotal += Integer.parseInt(val);
							} else if (j == 3) {
								laxPreempts += Integer.parseInt(val);
							} else if (j == 4) {
								laxActiv += Integer.parseInt(val);
							} else if (j == 5) {
								edfTotal += Integer.parseInt(val);
							} else if (j == 6) {
								edfPreempts += Integer.parseInt(val);
							} else if (j == 7) {
								edfActiv += Integer.parseInt(val);
							} else if (j == 8) {
								hybridTotal += Integer.parseInt(val);
							} else if (j == 9) {
								hybridPreempts += Integer.parseInt(val);
							} else if (j == 10) {
								hybridActiv += Integer.parseInt(val);
							}
							j++;
						}
					}
				}
				i++;
			}
			
			// Write percentage
			double laxPerc = (double) laxTotal / nbFiles;
			double edfPerc = (double) edfTotal / nbFiles;
			double hybridPerc = (double) hybridTotal / nbFiles;
			
			double laxPercPreempts = (double) laxPreempts / laxActiv;
			double edfPercPreempts = (double) edfPreempts / edfActiv;
			double hybridPercPreempts = (double) hybridPreempts / hybridActiv;
			
			Writer wOutput = new BufferedWriter(new FileWriter(outputFilePathTotal, true));
			wOutput.write(utilization+","
						  +laxPerc+","+laxPreempts+","+laxActiv+","+laxPercPreempts+","
						  +edfPerc+","+edfPreempts+","+edfActiv+","+edfPercPreempts+","
						  +hybridPerc+","+hybridPreempts+","+hybridActiv+","+hybridPercPreempts+"\n");
			wOutput.close();
			
		} else {
			System.err.println("Wrong number of levels");
			System.exit(-1);
		}
		
		System.out.println("[BENCH Main] Done benchmarking U = "+utilization+" Levels "+nbLvls);
	}
}
