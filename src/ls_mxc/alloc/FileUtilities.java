package ls_mxc.alloc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import ls_mxc.model.DAG;
import ls_mxc.model.Edge;
import ls_mxc.model.Node;

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
			for (int i = 0; i < nb_nodes - 1; i++){
				Node n = d.getNodebyID(i);
				String[] dep = line.split(",");
				
				for (int j = 0; j < dep.length; j++){
					if (dep[j].contains("1")){
						Node src = d.getNodebyID(j);
						Edge e = new Edge(src, n, false);
						src.getSnd_edges().add(e);
						n.getRcv_edges().add(e);
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
