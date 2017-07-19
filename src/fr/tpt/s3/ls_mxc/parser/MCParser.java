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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import fr.tpt.s3.ls_mxc.alloc.LS;
import fr.tpt.s3.ls_mxc.avail.Automata;
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
	public void writePRISM () {
	
		
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
