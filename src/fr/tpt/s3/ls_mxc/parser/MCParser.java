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
package fr.tpt.s3.ls_mxc.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import fr.tpt.s3.ls_mxc.alloc.LS;
import fr.tpt.s3.ls_mxc.avail.AutoBoolean;
import fr.tpt.s3.ls_mxc.avail.Automata;
import fr.tpt.s3.ls_mxc.avail.FTM;
import fr.tpt.s3.ls_mxc.avail.Formula;
import fr.tpt.s3.ls_mxc.avail.State;
import fr.tpt.s3.ls_mxc.avail.Transition;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.model.Actor;

public class MCParser {

	private String inputFile;
	private String outputFile;
	// Only references do not have to be instantiated
	private DAG dag;
	private LS ls;
	private Automata auto;
	
	public MCParser (String iFile, String oFile, DAG dag, LS ls) {
		setInputFile(iFile);
		setOutputFile(oFile);
		setDag(dag);
		setLs(ls);
		ls.setMxcDag(dag);
	}
	
	/**
	 * Reads the XML file and creates actors and edges
	 */
	public void readXML () {
		try {
			File iFile = new File(inputFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			
			// Root element
			Document doc = dBuilder.parse(iFile);
			doc.getDocumentElement().normalize();
			
			NodeList eList = doc.getElementsByTagName("mcdag");
			Element eDag = (Element) eList.item(0);
			dag.setDeadline(Integer.parseInt(eDag.getAttribute("deadline")));
			
			// Instantiate the DAG
			int nb_actors = 0;
			
			// List of actors in the DAG
			NodeList nList = doc.getElementsByTagName("actor");
			for (int i = 0; i < nList.getLength(); i++) {
				Node n = nList.item(i);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) n;
					Actor a = new Actor(nb_actors++, e.getAttribute("name"),
										Integer.parseInt(e.getElementsByTagName("clo").item(0).getTextContent()),
										Integer.parseInt(e.getElementsByTagName("chi").item(0).getTextContent()));
					a.setfProb(Double.parseDouble(e.getElementsByTagName("fprob").item(0).getTextContent()));
					dag.getNodes().add(a);
				}
			}
			
			// List of fault tolerance mechanisms
			NodeList ftList = doc.getElementsByTagName("ftm");
			for (int i = 0; i < ftList.getLength(); i++) {
				Node n = ftList.item(i);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) n;
					Actor a = new Actor(nb_actors++, e.getAttribute("name"),
										Integer.parseInt(e.getElementsByTagName("clo").item(0).getTextContent()),
										Integer.parseInt(e.getElementsByTagName("chi").item(0).getTextContent()));
					a.setfMechanism(true);
					a.setVotTask(e.getElementsByTagName("vtask").item(0).getTextContent());
					dag.getNodebyName(e.getElementsByTagName("vtask").item(0).getTextContent()).setVoted(true);
					a.setNbReplicas(Integer.parseInt(e.getElementsByTagName("replicas").item(0).getTextContent()));
					dag.getNodes().add(a);
				}
			}
			
			// List of connections
			NodeList ports = doc.getElementsByTagName("ports");
			NodeList pList = ports.item(0).getChildNodes();
			for (int i = 0; i < pList.getLength(); i++) {
				Node n = pList.item(i);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) n;
					// Creating the edge adds it to the corresponding nodes
					@SuppressWarnings("unused")
					Edge ed = new Edge(dag.getNodebyName(e.getAttribute("srcActor")),
									   dag.getNodebyName(e.getAttribute("dstActor")));
				}
			}
			
			NodeList cList = doc.getElementsByTagName("cores");
			Element c = (Element) cList.item(0);
			ls.setNbCores(Integer.parseInt(c.getAttribute("number")));
			dag.sanityChecks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the scheduling tables
	 */
	public void writeSched () throws IOException {
		
	}
	
	/**
	 * Writes a model for the PRISM model checker
	 */
	public void writePRISM () throws IOException {
		BufferedWriter out = null;
		try {
			File f = new File(getOutputFile());
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			
			out.write("dtmc\n\n");
			out.write("const int D;\n\n");
			
			// Write FTMs
			Iterator<FTM> iftm = auto.getFtms().iterator();
			while (iftm.hasNext() ) {
				FTM ftm = iftm.next();
				ftm.createVoter();
				out.write("module "+ftm.getName()+"\n");
				out.write("\tv: [0..20] init 0;\n");
				Iterator<Transition> it = ftm.getTransitions().iterator();
				int i = 0;
				while (it.hasNext()) {
					Transition t = it.next();
					out.write("\t["+t.getDestOk().getTask()+"_ok] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestOk().getId()+");\n");
					out.write("\t["+t.getDestOk().getTask()+"_fail] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestFail().getId()+");\n");
					out.write("\n");
				}
				
				it = ftm.getF_trans().iterator();
				while (it.hasNext()) {
					Transition t = it.next();
					out.write("\t["+t.getName()+"] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestOk().getId()+");\n");
				}
				out.write("endmodule\n");
				out.write("\n");
				
				// Create replicas if it is a voter
				for (i = 0; i < ftm.getNbVot(); i++) {
					out.write("module "+ftm.getVotTask().getName()+i+"\n");
					out.write("\tv"+i+": [0..2] init 0;\n");
					out.write("\t["+ftm.getVotTask().getName()+i+"_run] v"+i+" = 0 ->  1 - "+ftm.getVotTask().getfProb()+" : (v"+i+"' = 1) + "+" \n");
					out.write("\t["+ftm.getVotTask().getName()+i+"_ok] v"+i+" = 1 -> (v"+i+"' = 0);\n");
					out.write("\t["+ftm.getVotTask().getName()+i+"_fail] v"+i+" = 2 -> (v"+i+"' = 0);\n");

					out.write("endmodule\n");
					out.write("\n");
				}
			}
			
			
			// Write formulas
			Iterator<Formula> iab = auto.getL_outs_b().iterator();
			while (iab.hasNext()) {
				Formula form = iab.next();
				out.write("formula "+form.getName()+" = ");
				Iterator<AutoBoolean> ia = form.getLab().iterator();
				while (ia.hasNext()) {
					out.write(ia.next().getTask()+"bool");
					if (ia.hasNext())
						out.write(" & ");
				}
				out.write(";\n");
			}
			out.write("\n");

			
			// Write Processor Module
			out.write("module proc\n");
			out.write("\ts : [0.."+auto.getNbStates()+"] init "+auto.getLo_sched().get(0).getId()+";\n");
			
			// Create all necessary booleans
			Iterator<State> is = auto.getLo_sched().iterator();
			while (is.hasNext()) {
				State s = is.next();
				if (s.getMode() == 0 && !s.getTask().contains("Final") && !s.getTask().contains("Init")) // It is a LO task
					out.write("\t"+s.getTask()+"bool: bool init false;\n");
			}
			
			System.out.println("");
			
			// Create the LO scheduling zone
			Iterator<Transition> it = auto.getL_transitions().iterator();
			while (it.hasNext()) {
				Transition t = it.next();
				if (t.getSrc().getMode() == 1) {
					if (! t.getSrc().isfMechanism())
						out.write("\t["+t.getSrc().getTask()+"_lo] s = " + t.getSrc().getId()
								+ " -> 1 - "+ t.getP() +" : (s' = " + t.getDestOk().getId() + ") +"
								+ t.getP() + ": (s' =" + t.getDestFail().getId() +");\n");
					else {
						out.write("\t["+t.getSrc().getTask()+"_ok] s = " + t.getSrc().getId()
								+ " -> (s' = " + t.getDestOk().getId() + ");\n");
						out.write("\t["+t.getSrc().getTask()+"_fail] s = " + t.getSrc().getId()
								+ " -> (s' = " + t.getDestFail().getId() + ");\n");
					}
				} else { // If it's a LO task we need to update the boolean
					if (t.getSrc().getId() == 0) { // Initial state resets booleans
						out.write("\t["+t.getSrc().getTask()+"_lo] s = " + t.getSrc().getId()
								+ " -> (s' = " + t.getDestOk().getId()+")");
						is = auto.getLo_sched().iterator();
						while (is.hasNext()) {
							State s = is.next();
							if (s.getMode() == 0 && !s.getTask().contains("Final")
									&& !s.getTask().contains("Init")) // It is a LO task
								out.write(" & ("+s.getTask()+"bool' = false)");
						}
						out.write(";\n");
					} else { 
						out.write("\t["+t.getSrc().getTask()+"_lo] s = " + t.getSrc().getId()
								+ " -> 1 - "+ t.getP() +" : (s' = " + t.getDestOk().getId() +") & ("+t.getSrc().getTask()+"bool' = true) + "
								+ t.getP() + ": (s' =" + t.getDestFail().getId() + ");\n" );
					}
				}
			}
			
			// Create the 2^n transitions for the end of LO
			Iterator<Transition> itf = auto.getF_transitions().iterator();
			int curr = 0;
			while (itf.hasNext()) {
				Transition t = itf.next();
				out.write("\t["+t.getSrc().getTask()+curr+"] s = " + t.getSrc().getId());
				Iterator<Formula> ib = t.getbSet().iterator();
				while(ib.hasNext()) {
					Formula ab = ib.next();
					out.write(" & " + ab.getName()+" = true");
				}
				Iterator<Formula> iff = t.getfSet().iterator();
				while(iff.hasNext()) {
					Formula ab = iff.next();
					out.write(" & " + ab.getName()+" = false");
				}
				out.write(" -> (s' = "+t.getDestOk().getId()+");\n");
				curr++;
			}
			
			// Create the HI scheduling zone
			// Need to iterate through transitions
			out.write("\n");
			it = auto.getH_transitions().iterator();
			while (it.hasNext()) {
				Transition t = it.next();
				out.write("\t["+t.getSrc().getTask()+"_hi] s = " + t.getSrc().getId() + " -> (s' =" + t.getDestOk().getId() +");\n");
			}
			
			out.write("endmodule\n");
					
			// Create the rewards
			out.write("\n");
			Iterator<Actor> in = dag.getLoOuts().iterator();
			while (in.hasNext()) {
				Actor n = in.next();
				out.write("rewards \""+n.getName()+"_cycles\"\n");
				it = auto.getF_transitions().iterator();
				int c = 0;
				while (it.hasNext()) {
					Transition t = it.next();
					Iterator<Formula> iab2 = t.getbSet().iterator();

					while (iab2.hasNext()) {
						if (iab2.next().getName().contentEquals(n.getName()))
							out.write("\t["+t.getSrc().getTask()+c+"] true : 1;\n");
					}
					c++;
				}
				c = 0;
				out.write("endrewards\n");
				out.write("\n");
			}
					
			// Total cycles reward
			out.write("rewards \"total_cycles\"\n");
			it = auto.getF_transitions().iterator();
			int c = 0;
			while (it.hasNext()) {
				Transition t = it.next();
				out.write("\t["+t.getSrc().getTask()+c+"] true : 1;\n");
				c++;
			}
			out.write("\t["+auto.getH_transitions().get(auto.getH_transitions().size() - 1).getSrc().getTask()+"_hi] true : 1;\n");
			out.write("endrewards\n");
			out.write("\n");		
		} catch (IOException ie){
			System.out.println(ie.getMessage());
		} finally {
			if (out != null)
				out.close();
		}
	}
	
	/* Getters and setters */
	
	public String getInputFile() {
		return inputFile;
	}


	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}


	public String getOutputFile() {
		return outputFile;
	}


	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}


	public DAG getDag() {
		return dag;
	}


	public void setDag(DAG dag) {
		this.dag = dag;
	}


	public LS getLs() {
		return ls;
	}


	public void setLs(LS ls) {
		this.ls = ls;
	}


	public Automata getAuto() {
		return auto;
	}


	public void setAuto(Automata auto) {
		this.auto = auto;
	}

}
