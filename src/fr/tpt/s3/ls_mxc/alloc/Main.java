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
package fr.tpt.s3.ls_mxc.alloc;

import java.io.IOException;

import org.apache.commons.cli.*;

/**
 * Main class to build the Jar for the allocation problem
 */
public class Main {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
				
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
			formatter.printHelp("Allocator", options);
			
			System.exit(1);
			return;
		}
		
		
		String inputFilePath = cmd.getOptionValue("input");
		String outputFilePath = cmd.getOptionValue("output");
		
		/* =============== Read from file and try to solve ================ */
		
		FileUtilities fu = new FileUtilities();
		
		LS ls = new LS();
		
		fu.ReadAndInit(inputFilePath, ls);
		
		try {
			ls.Alloc_All();
		} catch (SchedulingException se) {
			System.out.println(se.getMessage());
			
			System.exit(1);
			return;
		}
		
		try {
			ls.CheckBaruah();
		} catch (SchedulingException se) {
			System.out.println(se.getMessage());
			
			System.exit(20);
			return;
		}
		
		// User specified a file to write to
		if (outputFilePath != null) {
			try {
				fu.writeToFile(outputFilePath, ls);
			} catch (IOException ie) {
				System.out.println("Write to file " + ie.getMessage());
				System.exit(3);
				return;
			}
		}
		System.exit(0);
	}
}
