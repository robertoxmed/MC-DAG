/*******************************************************************************
 * Copyright (c) 2017, 2018 Roberto Medina
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
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import fr.tpt.s3.ls_mxc.avail.AutoBoolean;
import fr.tpt.s3.ls_mxc.avail.Automata;
import fr.tpt.s3.ls_mxc.avail.FTM;
import fr.tpt.s3.ls_mxc.avail.Formula;
import fr.tpt.s3.ls_mxc.avail.State;
import fr.tpt.s3.ls_mxc.avail.Transition;
import fr.tpt.s3.ls_mxc.generator.NLevelsGenerator;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.model.Actor;
import fr.tpt.s3.ls_mxc.model.ActorAvail;
import fr.tpt.s3.ls_mxc.model.ActorSched;

public class MCParser {

	private String inputFile;
	private String outSchedFile;
	private String outPrismFile;
	private String outGenFile;
	private String outDotFile;
	private boolean bOutPrism;

	// Only references do not have to be instantiated
	private Set<DAG> dags;
	private Automata auto;
	private NLevelsGenerator ug;
	
	// Writing scheduling tables
	private String[][][] sched;
	private int hPeriod;
	private int nbCores;
	private int nbLevels;
	
	public MCParser (String iFile, String oSFile,Set<DAG> dags, boolean bop) {
		setInputFile(iFile);
		setOutSchedFile(oSFile);
		setDags(dags);
		bOutPrism = bop;
		this.setNbLevels(2);
	}
	
	public MCParser (String oGFile, NLevelsGenerator ug) {
		setOutGenFile(oGFile);
		setUg(ug);
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
			int count = 0;
			
			for (int d = 0; d < eList.getLength(); d++) {
				Element eDag = (Element) eList.item(d);
				DAG dag	= new DAG();
				dag.setId(count);
				dag.setDeadline(Integer.parseInt(eDag.getAttribute("deadline")));
				
				// Instantiate the DAG
				int nb_actors = 0;
				
				// List of actors in the DAG
				NodeList nList = eDag.getElementsByTagName("actor");
				for (int i = 0; i < nList.getLength(); i++) {
					Node n = nList.item(i);
					if (n.getNodeType() == Node.ELEMENT_NODE) {
						Element e = (Element) n;
						Actor a;
						if (outPrismFile == null) {
							a = new ActorSched(nb_actors++, e.getAttribute("name"),
											Integer.parseInt(e.getElementsByTagName("clo").item(0).getTextContent()),
											Integer.parseInt(e.getElementsByTagName("chi").item(0).getTextContent()));
						} else {
							a = new ActorAvail(nb_actors++, e.getAttribute("name"),
									Integer.parseInt(e.getElementsByTagName("clo").item(0).getTextContent()),
									Integer.parseInt(e.getElementsByTagName("chi").item(0).getTextContent()));
						}
						if (isbOutPrism())
							((ActorSched) a).setfProb(Double.parseDouble(e.getElementsByTagName("fprob").item(0).getTextContent()));
						((ActorSched) a).setGraphDead(dag.getDeadline());
						dag.getNodes().add(a);
					}
				}
				
				// List of fault tolerance mechanisms
				NodeList ftList = eDag.getElementsByTagName("ftm");
				for (int i = 0; i < ftList.getLength(); i++) {
					Node n = ftList.item(i);
					if (n.getNodeType() == Node.ELEMENT_NODE) {
						Element e = (Element) n;
						if (e.getAttribute("type").contains("voter")) {
							ActorAvail a = new ActorAvail(nb_actors++, e.getAttribute("name"),
												Integer.parseInt(e.getElementsByTagName("clo").item(0).getTextContent()),
												Integer.parseInt(e.getElementsByTagName("chi").item(0).getTextContent()));
							a.setfMechanism(true);
							a.setfMechType(ActorAvail.VOTER);
							a.setVotTask(e.getElementsByTagName("vtask").item(0).getTextContent());
							((ActorAvail) dag.getNodebyName(e.getElementsByTagName("vtask").item(0).getTextContent())).setVoted(true);
							a.setNbReplicas(Integer.parseInt(e.getElementsByTagName("replicas").item(0).getTextContent()));
							dag.getNodes().add(a);
						} else if (e.getAttribute("type").contains("mkfirm")) {
							ActorAvail a = (ActorAvail) dag.getNodebyName(e.getAttribute("name"));
							a.setfMechanism(true);
							a.setfMechType(ActorAvail.MKFIRM);
							a.setM(Integer.parseInt(e.getElementsByTagName("m").item(0).getTextContent()));
							a.setK(Integer.parseInt(e.getElementsByTagName("k").item(0).getTextContent()));
							a.setVoted(true);
						} else {
							System.out.println("Uknown FTM");
						}
					}
				}
				
				// List of connections
				NodeList ports = eDag.getElementsByTagName("ports");
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
				dag.sanityChecks();
				dags.add(dag);
				count++;
			}
			NodeList cList = doc.getElementsByTagName("cores");
			Element c = (Element) cList.item(0);
			setNbCores(Integer.parseInt(c.getAttribute("number")));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads the XML file for a N level MC System
	 */
	public void readXMLNlevels () {
		try {
			File iFile = new File(inputFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		
			// 	Root element
			Document doc = dBuilder.parse(iFile);
			doc.getDocumentElement().normalize();
			
			// Extract number of cores
			NodeList cList = doc.getElementsByTagName("cores");
			Element c = (Element) cList.item(0);
			setNbCores(Integer.parseInt(c.getAttribute("number")));
			
			// Extract number of levels
			NodeList lList = doc.getElementsByTagName("levels");
			Element l = (Element) lList.item(0);
			setNbLevels(Integer.parseInt(l.getAttribute("number")));
			
			// Extract DAGs that constitute the system
			NodeList eList = doc.getElementsByTagName("mcdag");
			int count = 0;
			
			for (int d = 0; d < eList.getLength(); d++) {
				Element eDag = (Element) eList.item(d);
				DAG dag	= new DAG();
				dag.setId(count);
				dag.setDeadline(Integer.parseInt(eDag.getAttribute("deadline")));
				dag.setLevels(getNbLevels());
				// Instantiate the DAG
				int nb_actors = 0;
				
				// List of actors in the DAG
				NodeList nList = eDag.getElementsByTagName("actor");
				for (int i = 0; i < nList.getLength(); i++) {
					Node n = nList.item(i);
					if (n.getNodeType() == Node.ELEMENT_NODE) {
						Element e = (Element) n;
						Actor a;
						int[] wcets = new int[getNbLevels()];
						
						// Initialize all the WCET of a node
						NodeList wList = e.getElementsByTagName("wcet");
						for (int j = 0; j < getNbLevels(); j++) {
							Node w = wList.item(j);
							if (w.getNodeType() == Node.ELEMENT_NODE) {
								Element we = (Element) w;
								
								wcets[Integer.parseInt(we.getAttribute("number"))] = Integer.parseInt(we.getTextContent());
							}
						}
						
						a = new ActorSched(nb_actors++, e.getAttribute("name"),getNbLevels());
						((ActorSched) a).setGraphID(count);
						a.setcIs(wcets);
						((ActorSched) a).setGraphDead(dag.getDeadline());
						dag.getNodes().add(a);
					}
				}
				
				// List of connections
				NodeList ports = eDag.getElementsByTagName("ports");
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
				// dag.sanityChecks();
				dags.add(dag);
				count++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the scheduling tables
	 */
	public void writeSched () throws IOException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.newDocument();
			
			// Root element
			Element rootElement = doc.createElement("sched");
			doc.appendChild(rootElement);
			
			
			for (int i = 0; i < this.getNbLevels(); i++) {
				Element table = doc.createElement("Mode-"+i);
				rootElement.appendChild(table);
				for (int c = 0; c < this.getNbCores(); c++) {
					Element core = doc.createElement("core");
					Attr attrCoreNb = doc.createAttribute("number");
					attrCoreNb.setNodeValue(String.valueOf(i));
					core.setAttributeNode(attrCoreNb);
					for (int s = 0; s < this.gethPeriod(); s++) {
						Element slot = doc.createElement("slot");
						Attr slotNb = doc.createAttribute("slot");
						slotNb.setNodeValue(String.valueOf(s));
						slot.setAttributeNodeNS(slotNb);
						slot.appendChild(doc.createTextNode(this.getSched()[i][s][c]));
						core.appendChild(slot);
					}
					table.appendChild(core);
				}
			}
			
			// Write the content
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer trans = tFactory.newTransformer();
			DOMSource dSource = new DOMSource(doc);
			StreamResult sResult = new StreamResult(new File(outSchedFile));
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			trans.transform(dSource, sResult);
		} catch (Exception ie) {
			ie.printStackTrace();
		}
	}
	
	/**
	 * Writes the properties to check by PRISM
	 * @throws IOException
	 */
	private void writePCTL () throws IOException {
		BufferedWriter out = null;
		try {
			String fileName = getOutPrismFile();
			int pos = fileName.lastIndexOf(".");
			if (pos > 0) {
			    fileName = fileName.substring(0, pos);
			}
			File f = new File(fileName+".pctl");
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			
			// Write total cycles
			out.write("// Total Cycles\n");
			out.write("R{\"total_cycles\"}=? [ C <= D ]\n\n");
			
			// Write properties for all LO outputs
			for (DAG d : dags) {
				for (Actor aout : d.getLoOuts()) {
					out.write("(R{\""+aout.getName()+"_cycles\"}=? [ C <= D ])/(R{\"total_cycles\"}=? [ C <= D ])\n\n");
				}
			}
			
			out.write("E[F \"deadlock\"]\n");
			
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} finally {
			if (out != null)
				out.close();
		}		
	}
	
	/**
	 * Writes a model for the PRISM model checker
	 */
	public void writePRISM () throws IOException {
		BufferedWriter out = null;
		try {
			DAG dag = dags.iterator().next();
			
			File f = new File(getOutPrismFile());
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			
			out.write("dtmc\n\n");
			out.write("const int D;\n\n");
			
			// Write FTMs
			int countFtm = 0;
			Iterator<FTM> iftm = auto.getFtms().iterator();
			while (iftm.hasNext() ) {
				FTM ftm = iftm.next();
				if (ftm.getType() == ActorAvail.VOTER) {
					out.write("module "+ftm.getName()+"\n");
					out.write("\tv"+countFtm+": [0..20] init 0;\n");
					Iterator<Transition> it = ftm.getTransitions().iterator();
					int i = 0;
					while (it.hasNext()) {
						Transition t = it.next();
						out.write("\t["+t.getDestOk().getTask()+"_ok] v"+countFtm+" = "+t.getSrc().getId()+" -> (v"+countFtm+"' = "+t.getDestOk().getId()+");\n");
						out.write("\t["+t.getDestOk().getTask()+"_fail] v"+countFtm+" = "+t.getSrc().getId()+" -> (v"+countFtm+"' = "+t.getDestFail().getId()+");\n");
						out.write("\n");
					}
				
					it = ftm.getFinTrans().iterator();
					while (it.hasNext()) {
						Transition t = it.next();
						out.write("\t["+t.getName()+"] v"+countFtm+" = "+t.getSrc().getId()+" -> (v"+countFtm+"' = "+t.getDestOk().getId()+");\n");
					}
					
					// Write reinitialization of Voter due to HI transitions -> avoids deadlock
					Transition t = auto.getH_transitions().get(auto.getH_transitions().size() - 1);
					for (i = 0; i < ftm.getStates().size(); i++) {	
						out.write("\t["+t.getSrc().getTask()+"_hi] v"+countFtm+" = "+i+" -> (v"+countFtm+"' = 0);\n");
					}
					
					out.write("endmodule\n");
					out.write("\n");
				
					// Create replicas if it is a voter
					for (i = 0; i < ftm.getNbVot(); i++) {
						out.write("module "+ftm.getVotTask().getName()+i+"\n");
						out.write("\tr_"+countFtm+"_"+i+": [0..2] init 0;\n");
						out.write("\t["+ftm.getVotTask().getName()+"0_run] r_"+countFtm+"_"+i+" = 0 ->  1 - "+ftm.getVotTask().getfProb()+" : (r_"+countFtm+"_"+i+"' = 1) + "+ftm.getVotTask().getfProb()+" : (r_"+countFtm+"_"+i+"' = 2);\n");
						out.write("\t["+ftm.getVotTask().getName()+i+"_ok] r_"+countFtm+"_"+i+" = 1 -> (r_"+countFtm+"_"+i+"' = 0);\n");
						out.write("\t["+ftm.getVotTask().getName()+i+"_fail] r_"+countFtm+"_"+i+" = 2 -> (r_"+countFtm+"_"+i+"' = 0);\n");
						out.write("\t["+t.getSrc().getTask()+"_hi] r_"+countFtm+"_"+i+" = 0 -> (r_"+countFtm+"_"+i+"' = 0);\n");
						out.write("\t["+t.getSrc().getTask()+"_hi] r_"+countFtm+"_"+i+" = 1 -> (r_"+countFtm+"_"+i+"' = 0);\n");
						out.write("\t["+t.getSrc().getTask()+"_hi] r_"+countFtm+"_"+i+" = 2 -> (r_"+countFtm+"_"+i+"' = 0);\n");


						out.write("endmodule\n");
						out.write("\n");
					}
				} else if (ftm.getType() != ActorAvail.MKFIRM) {
					System.out.print("Uknown Voting mechanism");
				}
				countFtm++;
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
			for (Actor a : dag.getNodes()) {
				if (a.getCI(1) == 0) // It is a LO task
					out.write("\t"+a.getName()+"bool: bool init false;\n");
			}
			
			out.write("\n");
			
			// Create MK firms if they exist
			iftm = auto.getFtms().iterator();
			while (iftm.hasNext()) {
				FTM ftm = iftm.next();
				if (ftm.getType() == ActorAvail.MKFIRM) {
					for (int i = 0; i < ftm.getK(); i ++) {
						out.write("\t"+ftm.getName()+"_v"+i+": [0..1] init 1;\n");
					}
				}
			}
			
			out.write("\n");
			
			// Create the LO scheduling zone
			Iterator<State> is = null;
			Iterator<Transition> it = auto.getL_transitions().iterator();
			while (it.hasNext()) {
				Transition t = it.next();
				if (t.getSrc().getMode() == ActorSched.HI) {
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
									&& !s.getTask().contains("Init") && !s.isExit() && !s.isSynched()) // It is a LO task
								out.write(" & ("+s.getTask()+"bool' = false)");
						}
						out.write(";\n");
					} else if (t.getSrc().isVoted()){
						out.write("\t["+t.getSrc().getTask()+"0_run] s = " + t.getSrc().getId()
								+" -> 1 - "+t.getP()+": (s' = " + t.getDestOk().getId() +") & ");
						FTM ftm = auto.getFTMbyName(t.getSrc().getTask());
						for (int i = ftm.getK() - 1 ; i > 0; i--) {
							out.write("("+ftm.getName()+"_v"+i+"' = "+ftm.getName()+"_v"+(i-1)+") &");
						}
						out.write(" ("+ftm.getName()+"_v0' = 1) + "+t.getP()+": (s' = " + t.getDestOk().getId() +") & ");
						for (int i = ftm.getK() - 1 ; i > 0; i--) {
							out.write("("+ftm.getName()+"_v"+i+"' = "+ftm.getName()+"_v"+(i-1)+") &");
						}
						out.write("("+ftm.getName()+"_v0' =0);\n");
					} else if (t.getSrc().isSynched()) {
						// OK transition
						out.write("\t["+t.getSrc().getTask()+"_ok] s = " + t.getSrc().getId()+" & ");
						FTM ftm = auto.getFTMbyName(t.getSrc().getTask());
						for (int i = 0; i < ftm.getK(); i++) {
							if (i < ftm.getK() - 1)
								out.write(ftm.getName()+"_v"+i+" + ");
							else
								out.write(ftm.getName()+"_v"+i+" >= "+ftm.getM());
						}
						out.write(" -> (s' = " + t.getDestOk().getId() +") & ("+t.getSrc().getTask()+"bool' = true);\n" );
						
						// Fail transition
						out.write("\t["+t.getSrc().getTask()+"_fail] s = " + t.getSrc().getId()+" & ");
						for (int i = 0; i < ftm.getK(); i++) {
							if (i < ftm.getK() - 1)
								out.write(ftm.getName()+"_v"+i+" + ");
							else
								out.write(ftm.getName()+"_v"+i+" < "+ftm.getM());
						}
						out.write(" -> (s' = " + t.getDestOk().getId() +");\n");
					} else if (t.getSrc().isExit()){
						out.write("\t["+t.getSrc().getTask()+"_ok] s = " + t.getSrc().getId()+ " & "
								+t.getSrc().getTask()+" -> (s' = " + t.getDestOk().getId() +");\n" );
						out.write("\t["+t.getSrc().getTask()+"_fail] s = " + t.getSrc().getId()+ " & "
								+t.getSrc().getTask()+" = false -> (s' = " + t.getDestOk().getId() +");\n" );
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
				out.write("\t["+n.getName()+"_ok] true : 1;\n");
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
			
			writePCTL();
		} catch (IOException ie){
			System.out.println(ie.getMessage());
		} finally {
			if (out != null)
				out.close();
		}
	}
	
	/**
	 * Writes the generated DAG to a file
	 * @throws IOException
	 */
	public void writeGennedDAG () throws IOException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.newDocument();
			
			// Root element (MC System)
			Element rootElement = doc.createElement("mcsystem");
			doc.appendChild(rootElement);
			
			for (DAG d : ug.getGennedDAGs()) {
				// MC DAG
				Element mcdag = doc.createElement("mcdag");
				Attr dagName = doc.createAttribute("name");
				dagName.setValue("genned-"+d.getId()+"-ed-"+ug.getEdgeProb()+"-"+d.getId());
				Attr dagDead = doc.createAttribute("deadline");
				dagDead.setValue(String.valueOf(d.getDeadline()));
				mcdag.setAttributeNodeNS(dagName);
				mcdag.setAttributeNode(dagDead);
				rootElement.appendChild(mcdag);
				// Actors
				for (Actor a : d.getNodes()) {
					Element actor = doc.createElement("actor");
					Attr actorNb = doc.createAttribute("name");
					actorNb.setNodeValue("D"+d.getId()+"N"+a.getId());
					actor.setAttributeNode(actorNb);
					// Add Cis
					for (int i = 0; i < nbLevels; i++) {
						Element ci = doc.createElement("wcet");
						ci.setAttribute("number", Integer.toString(i));
						ci.appendChild(doc.createTextNode(String.valueOf(a.getCI(i))));
						actor.appendChild(ci);
					}
					Element fprob = doc.createElement("fprob");
					fprob.appendChild(doc.createTextNode("0.0"));
					actor.appendChild(fprob);
					mcdag.appendChild(actor);
				}
				
				// Ports
				Element edges = doc.createElement("ports");
				int counter = 0;
				for (Actor a : d.getNodes()) {
					if (a.getSndEdges().size() != 0)  {
						for (Edge e : a.getSndEdges()) {
							Element edge = doc.createElement("port");
							Attr portName = doc.createAttribute("name");
							portName.setValue("p"+counter);
							counter++;
							Attr portSrc = doc.createAttribute("srcActor");
							portSrc.setValue("D"+d.getId()+"N"+e.getSrc().getName());
							Attr portDst = doc.createAttribute("dstActor");
							portDst.setValue("D"+d.getId()+"N"+e.getDest().getName());
							edge.setAttributeNode(portName);
							edge.setAttributeNode(portSrc);
							edge.setAttributeNode(portDst);
							edges.appendChild(edge);
						}
					}
				}
				mcdag.appendChild(edges);
			}
			
			// Number of cores of the architecture
			Element cores = doc.createElement("cores");
			Attr nbCores = doc.createAttribute("number");
			nbCores.setValue(String.valueOf(1));
			cores.setAttributeNode(nbCores);
			rootElement.appendChild(cores);
			
			// Number of levels of the system
			Element levels = doc.createElement("levels");
			levels.setAttribute("number", String.valueOf(nbLevels));
			rootElement.appendChild(levels);
			
			// Write the content
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer trans = tFactory.newTransformer();
			DOMSource dSource = new DOMSource(doc);
			StreamResult sResult = new StreamResult(new File(outGenFile));
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			trans.transform(dSource, sResult);
		} catch (Exception ie) {
			ie.printStackTrace();
		}
	}
	
	/**
	 * Writtes the dot file for the generated graphs.
	 * @throws IOException
	 */
	public void writeDot () throws IOException {
		BufferedWriter out = null;
		try {
			File f = new File(outDotFile);
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			
			out.write("digraph test{\n");
			
			
			for (DAG d : ug.getGennedDAGs()) {
				for (Actor a : d.getNodes()) {
					out.write("\"D"+d.getId()+"N"+a.getName()+"\" [label=\"D"+d.getId()+"N"+a.getName()+"\\n");
					for (int i = ug.getNbLevels() - 1; i >= 0; i--) {
						if (i != 0)
							out.write(a.getCI(i)+"/");
						else
							out.write(a.getCI(i)+"\"]\n");
						
					}
				}
			}
			out.write("\n");
			
			// Create the edges between the nodes
			for (DAG d : ug.getGennedDAGs()) {
				for (Actor a : d.getNodes()) {
					for (Edge e : a.getSndEdges())
						out.write("\tD"+d.getId()+"N"+e.getSrc().getName()+" -> D"+d.getId()+"N"+e.getDest().getName()+";\n");
				}
				out.write("\n");
			}
			// Options and EoF
			out.write("overlap=false\n"
					+ "fontsize = 12\n"
					+ "}");
			
		} catch (IOException ie) {
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

	public Automata getAuto() {
		return auto;
	}

	public void setAuto(Automata auto) {
		this.auto = auto;
	}

	public String getOutSchedFile() {
		return outSchedFile;
	}

	public void setOutSchedFile(String outSchedFile) {
		this.outSchedFile = outSchedFile;
	}

	public String getOutGenFile() {
		return outGenFile;
	}

	public void setOutGenFile(String outGenFile) {
		this.outGenFile = outGenFile;
	}

	public NLevelsGenerator getUg() {
		return ug;
	}

	public void setUg(NLevelsGenerator ug) {
		this.ug = ug;
	}

	public int getNbCores() {
		return nbCores;
	}

	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}

	public Set<DAG> getDags() {
		return dags;
	}

	public void setDags(Set<DAG> dags) {
		this.dags = dags;
	}

	public String getOutDotFile() {
		return outDotFile;
	}

	public void setOutDotFile(String outDotFile) {
		this.outDotFile = outDotFile;
	}

	public String getOutPrismFile() {
		return outPrismFile;
	}

	public void setOutPrismFile(String outPrismFile) {
		this.outPrismFile = outPrismFile;
	}

	public int gethPeriod() {
		return hPeriod;
	}

	public void sethPeriod(int hPeriod) {
		this.hPeriod = hPeriod;
	}

	public int getNbLevels() {
		return nbLevels;
	}

	public void setNbLevels(int nbLevels) {
		this.nbLevels = nbLevels;
	}
	
	public boolean isbOutPrism() {
		return bOutPrism;
	}

	public void setbOutPrism(boolean bOutPrism) {
		this.bOutPrism = bOutPrism;
	}

	public String[][][] getSched() {
		return sched;
	}

	public void setSched(String[][][] sched) {
		this.sched = sched;
	}
}
