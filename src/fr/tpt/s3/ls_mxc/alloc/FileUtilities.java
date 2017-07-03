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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Edge;
import fr.tpt.s3.ls_mxc.model.Node;

/**
 * Utility class to read and write from/to files
 * @author Roberto Medina
 *
 */
public class FileUtilities {
	
	public FileUtilities (){}

	public void ReadAndInit(String file, LS ls) {
		
		String line;
		int nb_nodes = 0;
		
		try {
			// Open file
			FileInputStream fr = new FileInputStream(file);
			
			// Initiate the buffer reader
			BufferedReader br = new BufferedReader(new InputStreamReader(fr));
			
			line = br.readLine();
			// Read comments and empty lines
			while ((line.length() == 0) || (line.charAt(0) == '#')) 
				line = br.readLine();
			
			// First line is the nb of nodes
			line = line.trim();
			nb_nodes = Integer.parseInt(line);
			
			line = br.readLine();
			// Read comments and empty lines
			while ((line.length() == 0) || (line.charAt(0) == '#')) 
				line = br.readLine();
			
			// Second line is the number of cores
			line = line.trim();
			ls.setNb_cores(Integer.parseInt(line));
			
			line = br.readLine();
			// Read comments and empty lines
			while ((line.length() == 0) || (line.charAt(0) == '#')) 
				line = br.readLine();
			
			// Third line is the deadline
			line = line.trim();
			ls.setDeadline(Integer.parseInt(line));
			
			line = br.readLine();
			// Read comments and empty lines
			while ((line.length() == 0) || (line.charAt(0) == '#')) 
				line = br.readLine();
			
			// Instantiate the DAG
			DAG d = new DAG();
			
			// C LOs are passed afterwards
			
			for(int i = 0; i < nb_nodes; i++){
				line = line.trim();
				Node n = new Node(i, Integer.toString(i), 0, 0);
				n.setC_LO(Integer.parseInt(line));
				
				d.getNodes().add(n);
				line = br.readLine();
			}
			
			// Read comments and empty lines
			while ((line.length() == 0) || (line.charAt(0) == '#')) 
				line = br.readLine();
			
			// C HIs are passed afterwards
			for (int i = 0; i < nb_nodes; i++){
				line = line.trim();
				
				Node n = d.getNodebyID(i);
				n.setC_HI(Integer.parseInt(line));
				line = br.readLine();
			}
			
			// Read comments and empty lines
			while ((line.length() == 0) || (line.charAt(0) == '#')) 
				line = br.readLine();

			// Edges are passed afterwards
			for (int i = 0; i < nb_nodes; i++){
				Node n = d.getNodebyID(i);
				String[] dep = line.split(",");
				
				for (int j = 0; j < dep.length; j++){
					if (dep[j].contains("1")){
						Node src = d.getNodebyID(j);
						@SuppressWarnings("unused")
						Edge e = new Edge(src, n, false);
					}
				}
				line = br.readLine();
			}
			
			// Set the constructed DAG
			Iterator<Node> it_n = d.getNodes().iterator();
			while(it_n.hasNext()){
				Node n = it_n.next();
				n.checkifSink();
				n.checkifSource();
				n.checkifSinkinHI();
			}
			
			ls.setMxcDag(d);
			br.close();
			fr.close();
		} catch(IOException e) {
			System.out.println("Unable to open file "+file+" exception "+e.getMessage()); 
		}
		
	}
	
	/**
	 * Write the allocation problem to an output filename
	 * This method is only called if the allocation is satisfied
	 * @param filename
	 * @throws IOException 
	 */
	public void writeToFile(String filename, LS ls) throws IOException{
		
		BufferedWriter out = null;
		try {
			File f = new File(filename);
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			
			String[][] S_HI_out = ls.getS_HI();
			String[][] S_LO_out = ls.getS_LO();
			
			/* Write the S HI table*/
			
			out.write("============= S HI =================\n\n");
			
			for (int i = 0; i < ls.getNb_cores(); i++) {
				out.write("|");
				for (int j = 0; j < ls.getDeadline(); j++) {
					out.write(S_HI_out[j][i] + " | ");
				}
				out.write("\n");
			}
			
			/* Write the S LO table*/
			
			out.write("\n\n============= S LO =================\n\n");
			
			for (int i = 0; i < ls.getNb_cores(); i++) {
				out.write("|");
				for (int j = 0; j < ls.getDeadline(); j++) {
					out.write(S_LO_out[j][i] + " | ");
				}
				out.write("\n");
			}
			
		}catch (IOException e ){
			System.out.print("writeToFile Exception " + e.getMessage());
		}finally{
			if(out != null)
				out.close();
		}
	}
	
}
