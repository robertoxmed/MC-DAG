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
		
		Option jobs = new Option("j", "jobs", true, "Number of threads to be launched.");
		jobs.setRequired(false);
		options.addOption(jobs);
		
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
		int nbJobs = 1;
		int nbFiles = inputFilePath.length;
		int count = 0;
				
		if (cmd.hasOption("jobs"))
			nbJobs = Integer.parseInt(cmd.getOptionValue("jobs"));
		Thread threads[] = new Thread[nbJobs];
		
		/*
		 *  While files need to be allocated
		 *  run the tests in the pool of threads
		 */
		while (nbFiles != 0) {
			int launched = 0;
			
			for (int i = 0; i < nbJobs && count < nbFiles; i++) {
				BenchThread bt = new BenchThread(inputFilePath[count]);
				threads[i] = new Thread(bt);
				threads[i].setName("BenchThread-"+i);
				launched++;
				count++;
				threads[i].start();
			}
			
			for (int i = 0; i < launched; i++) {
				try {
					threads[i].join();
					nbFiles--;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		

	}
}
