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

import fr.tpt.s3.ls_mxc.alloc.LS;
import fr.tpt.s3.ls_mxc.alloc.SchedulingException;
import fr.tpt.s3.ls_mxc.avail.Automata;
import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.parser.MCParser;

/**
 * Main class to create the MC-DAG Framework. All functionalities should be included
 * @author roberto
 *
 */
public class Main {
	

	public static void main (String[] args) throws IOException {
		
		/* ============================ Command line ================= */
		
		Options options = new Options();
		
		Option input = new Option("i", "input", true, "MC-DAG XML Model");
		input.setRequired(true);
		options.addOption(input);
		
		Option outSched = new Option("os", "out-scheduler", true, "File path to write the scheduling tables");
		outSched.setRequired(false);
		options.addOption(outSched);
		
		Option outPrism = new Option("op", "out-prism", true, "File path to write the PRISM model");
		outPrism.setRequired(false);
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
		
		String inputFilePath = cmd.getOptionValue("input");
		String outSchedFilePath = cmd.getOptionValue("out-scheduler");
		String outPrismFilePath = cmd.getOptionValue("out-prism");
		boolean debug = cmd.hasOption("debug");
		
		/* =============== Read from file and try to solve ================ */
		
		DAG dag = new DAG();
		LS ls = new LS();
		Automata auto = new Automata(ls, dag);
		MCParser mcp = new MCParser(inputFilePath, outSchedFilePath, outPrismFilePath, dag, ls);
		mcp.readXML();
		ls.setDeadline(dag.getDeadline());
		
		try {
			ls.AllocAll();
			if (debug) {
				System.out.println("========== Weights ===============");
				ls.printW(Actor.LO);
				ls.printW(Actor.HI);
				System.out.println("========== Scheduling tables ===============");
				ls.printS_HI();
				ls.printS_LO();
			}
		} catch (SchedulingException e) {
			e.getMessage();
		}
		
		auto.createAutomata();
		mcp.setAuto(auto);
		mcp.writePRISM();

	}
}
