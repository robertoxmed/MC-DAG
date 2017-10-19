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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.tpt.s3.ls_mxc.alloc.LS;
import fr.tpt.s3.ls_mxc.alloc.MultiDAG;
import fr.tpt.s3.ls_mxc.avail.Automata;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.parser.MCParser;

/**
 * Main class to create the MC-DAG Framework. All functionalities should be included
 * @author roberto
 *
 */
public class Main {

	public static void main (String[] args) throws IOException, InterruptedException {
		
		/* ============================ Command line ================= */
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "MC-DAG XML Model");
		input.setRequired(true);
		input.setArgs(Option.UNLIMITED_VALUES); // Sets maximum number of threads to be launched
		options.addOption(input);
		
		Option outSched = new Option("os", "out-scheduler", true, "File path to write the scheduling tables");
		outSched.setRequired(false);
		outSched.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(outSched);
		
		Option outPrism = new Option("op", "out-prism", true, "File path to write the PRISM model");
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
		
		/* =============== Read from files and try to solve ================ */
		for (int i = 0; i < inputFilePath.length; i++) {
			Set<DAG> dags = new HashSet<DAG>();
			MCParser mcp = new MCParser(inputFilePath[i], null, null, dags);
			
			if (outSchedFilePath != null)
				mcp.setOutSchedFile(outSchedFilePath[i]);
			if (outPrismFilePath != null )
				mcp.setOutputFile(outPrismFilePath[i]);
			
			mcp.readXML();
			LS ls = null;
			MultiDAG msched = null;
			Automata auto = null;
			
			if (dags.size() == 1) {
				DAG dag = dags.iterator().next();
				ls = new LS();
				ls.setMxcDag(dag);
				ls.setDeadline(dag.getDeadline());
				ls.setNbCores(mcp.getNbCores());
				ls.setDebug(debug);
				Thread t = new Thread (ls);
				
				t.start();
				
				if (outPrismFilePath != null) {
					t.join();
					if (debug) System.out.println("[DEBUG] UniDAG: Creating the automata object.");
					auto = new Automata(ls, dag);
				}

			} else if (dags.size() > 1) {
				msched = new MultiDAG(dags, mcp.getNbCores(), debug);
				
				System.out.println("MultiDAG: "+dags.size()+" DAGs are going to be scheduled in "+mcp.getNbCores()+" cores.");
				
				Thread t = new Thread(msched);
				t.start();
			}
			
			/* =============== Write results ================ */
			if (outSchedFilePath != null)
				mcp.writeSched();
			if (outPrismFilePath != null) {
				auto.createAutomata();
				mcp.setAuto(auto);
				mcp.writePRISM();
				System.out.println("PRISM file written.");
			}
		}
	}
}
