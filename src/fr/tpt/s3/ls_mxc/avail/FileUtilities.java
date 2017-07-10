package fr.tpt.s3.ls_mxc.avail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Node;

public class FileUtilities {

	public FileUtilities () {}
	
	
	public void writeVoters (BufferedWriter out, Voter vot) throws IOException {
		
		out.write("module voter\n");
		out.write("\tv: [0..20] init 0;\n");
		Iterator<Transition> it = vot.getTransitions().iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			out.write("\t["+t.getDestOk().getTask()+"_ok] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestOk().getId()+");\n");
			out.write("\t["+t.getDestOk().getTask()+"_fail] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestFail().getId()+");\n");
			out.write("\n");
		}
		
		it = vot.getF_trans().iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			out.write("\t["+t.getName()+"] v = "+t.getSrc().getId()+" -> (v' = "+t.getDestOk().getId()+");\n");
		}
		out.write("endmodule\n");
		out.write("\n");
	}
	
	/**
	 *  This procedures prints the automata
	 * @throws IOException 
	 */
	public void writeAutomata (BufferedWriter out, Automata a, DAG d) throws IOException {
		
		out.write("module proc\n");
		out.write("\ts : [0..50] init 0;\n");
		
		// Create all necessary booleans
		Iterator<State> is = a.getLo_sched().iterator();
		while (is.hasNext()) {
			State s = is.next();
			if (s.getMode() == 0 && !s.getTask().contains("Final")) // It is a LO task
				out.write("\t"+s.getTask()+"bool: bool init false;\n");
		}
		
		System.out.println("");
		
		// Create the LO scheduling zone
		Iterator<Transition> it = a.getL_transitions().iterator();
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
				out.write("\t["+t.getSrc().getTask()+"_lo] s = " + t.getSrc().getId()
						+ " -> 1 - "+ t.getP() +" : (s' = " + t.getDestOk().getId() +") & ("+t.getSrc().getTask()+"bool' = true) + "
						+ t.getP() + ": (s' =" + t.getDestFail().getId() + ");\n" );
			}
		}
		
		// Create the 2^n transitions for the end of LO
		Iterator<Transition> itf = a.getF_transitions().iterator();
		int curr = 0;
		while (itf.hasNext()) {
			Transition t = itf.next();
			out.write("\t["+t.getSrc().getTask()+curr+"] s = " + t.getSrc().getId());
			Iterator<AutoBoolean> ib = t.getbSet().iterator();
			while(ib.hasNext()) {
				AutoBoolean ab = ib.next();
				out.write(" & " + ab.getTask()+"bool = true");
			}
			Iterator<AutoBoolean> iff = t.getfSet().iterator();
			while(iff.hasNext()) {
				AutoBoolean ab = iff.next();
				out.write(" & " + ab.getTask()+"bool = false");
			}
			out.write(" -> (s' = "+t.getDestOk().getId()+");\n");
			curr++;
		}
		
		// Create the HI scheduling zone
		// Need to iterate through transitions
		out.write("\n");
		it = a.getH_transitions().iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			out.write("\t["+t.getSrc().getTask()+"_hi] s = " + t.getSrc().getId() + " -> (s' =" + t.getDestOk().getId() +");\n");
		}
		
		out.write("end module;\n");
				
		// Create the rewards
		out.write("\n");
		Iterator<Node> in = d.getLO_outs().iterator();
		while (in.hasNext()) {
			Node n = in.next();
			out.write("rewards \""+n.getName()+"_cycles\"\n");
			it = a.getF_transitions().iterator();
			int c = 0;
			while (it.hasNext()) {
				Transition t = it.next();
				Iterator<AutoBoolean> iab = t.getbSet().iterator();

				while (iab.hasNext()) {
					if (iab.next().getTask().contentEquals(n.getName()))
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
		it = a.getF_transitions().iterator();
		int c = 0;
		while (it.hasNext()) {
			Transition t = it.next();
			out.write("\t["+t.getSrc().getTask()+c+"] true : 1;\n");
			c++;
		}
		c = 0;
		out.write("endrewards\n");
		out.write("\n");
	}
	
	
	public void writeModelToFile(String filename, List<Voter> voters, DAG d, Automata aut) throws IOException {
		
		BufferedWriter out = null;
		try {
			File f = new File(filename);
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			
			out.write("dtmc\n\n");
			
			Iterator<Voter> iv = voters.iterator();
			while (iv.hasNext()) {
				writeVoters(out, iv.next());
			}
			
			writeAutomata(out, aut, d);
			
		} catch (IOException e) {
			System.out.println("writeModelToFile Exception "+e.getMessage());
		} finally {
			if (out != null)
				out.close();
		}
	}
}
