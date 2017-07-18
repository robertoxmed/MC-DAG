package fr.tpt.s3.ls_mxc.parser;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.cli.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import fr.tpt.s3.ls_mxc.alloc.LS;
import fr.tpt.s3.ls_mxc.avail.Automata;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.model.Actor;

public class Parser {

	private String inputFile;
	private String outputFile;
	private DAG dag;
	private LS ls;
	private Automata auto;
	
	public Parser () { }
	
	public void readXML () {
		try {
			File iFile = new File(inputFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(iFile);
			doc.getDocumentElement().normalize();
			
			// Instantiate the DAG
			int nb_actors = 0;
			dag = new DAG();
			
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
					Edge ed = new Edge(dag.getNodebyName(e.getElementsByTagName("src").item(0).getTextContent()),
									   dag.getNodebyName(e.getElementsByTagName("src").item(0).getTextContent()));
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	public void main (String[] args) {
		
		/* Parse all options */
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
			formatter.printHelp("MC-DAG", options);
			
			System.exit(1);
			return;
		}
		
		this.setInputFile(cmd.getOptionValue("input"));
		this.setOutputFile(cmd.getOptionValue("output"));
		
		/* Parse the input file */
		readXML();
		ls = new LS();
		ls.setMxcDag(dag);
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
