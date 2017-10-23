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
package fr.tpt.s3.ls_mxc.appli;

import java.io.IOException;

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
		
		Option outSched = new Option("os", "out-scheduler", true, "File paths to write the scheduling tables");
		outSched.setRequired(false);
		outSched.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(outSched);
		
		Option outPrism = new Option("op", "out-prism", true, "File paths to write the PRISM model");
		outPrism.setRequired(false);
		outPrism.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(outPrism);
		
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
			formatter.printHelp("MC-DAG framework", options);
			
			System.exit(1);
			return;
		}
		
		String inputFilePath[] = cmd.getOptionValues("input");
		String outSchedFilePath[] = cmd.getOptionValues("out-scheduler");
		String outPrismFilePath[] = cmd.getOptionValues("out-prism");
		boolean debug = cmd.hasOption("debug");
		
		if ((outSchedFilePath != null && inputFilePath.length != outSchedFilePath.length)
				|| (outPrismFilePath != null && inputFilePath.length != outPrismFilePath.length )) {
			formatter.printHelp("MC-DAG framework", options);
			System.exit(1);
			return;
		}
		
		Thread threads[] = new Thread[inputFilePath.length];
		
		if (debug)
			System.out.println("[DEBUG] Launching "+inputFilePath.length+" thread(s.");
		
		/* Launch threads to solve allocation */
		for (int i = 0; i < inputFilePath.length; i++) {
			FrameworkThread ft = new FrameworkThread(inputFilePath[i], debug);
			
			if (outSchedFilePath != null)
				ft.setOutSchedFile(outSchedFilePath[i]);
			if (outPrismFilePath != null)
				ft.setOutPRISMFile(outPrismFilePath[i]);
			
			threads[i] = new Thread(ft);
			threads[i].start();
		}
		
		/* Join all launched threads */
		for (int i = 0; i < inputFilePath.length; i++)
			threads[i].join();
	}
}
