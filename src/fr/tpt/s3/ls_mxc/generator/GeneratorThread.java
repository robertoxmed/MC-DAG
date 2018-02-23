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
package fr.tpt.s3.ls_mxc.generator;

import java.io.IOException;
import fr.tpt.s3.ls_mxc.parser.MCParser;

public class GeneratorThread implements Runnable{

	private NLevelsGenerator ug;
	private MCParser mcp;
	private boolean graphBool;
	private boolean debug;
	
	public GeneratorThread (double minU, double maxU, double eProb, int levels, int pDegree,
			int nbDags, String outFile,	boolean graphBool, boolean debug) {
		ug = new NLevelsGenerator(minU, maxU, eProb, levels, pDegree, nbDags, debug);
		mcp = new MCParser(outFile, ug);
		setDebug(debug);
		setGraphBool(graphBool);
	}
	
	@Override
	public void run() {
		for (int i = 0; i < ug.getNbDAGs(); i++) {
			if (isDebug()) System.out.println("[DEBUG "+Thread.currentThread().getName()+"] Generating DAG #"+i+" of "+ug.getNbDAGs());
			ug.GenerateGraph();
		}

		// Write the file
		try {
			mcp.setNbLevels(ug.getNbLevels());
			mcp.writeGennedDAG();
			if (isGraphBool()) {
				System.out.println("Dot : "+mcp.getOutGenFile().concat(".dot"));
				mcp.setOutDotFile(mcp.getOutGenFile().concat(".dot"));
				
				mcp.writeDot();
			}
			System.out.println(Thread.currentThread().getName()+"> Written generated DAG(s)!");
		} catch (IOException e) {
			System.err.println("[ERROR] Failed to write the XML file in the generator " + e.getMessage());
			System.exit(1);
			return;
		}
	}

	/*
	 * Getters & setters
	 */
	public NLevelsGenerator getUg() {
		return ug;
	}

	public void setUg(NLevelsGenerator ug) {
		this.ug = ug;
	}
	
	public MCParser getMcp() {
		return mcp;
	}

	public void setMcp(MCParser mcp) {
		this.mcp = mcp;
	}
	
	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isGraphBool() {
		return graphBool;
	}

	public void setGraphBool(boolean graphBool) {
		this.graphBool = graphBool;
	}
}
