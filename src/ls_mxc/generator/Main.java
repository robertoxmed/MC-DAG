package ls_mxc.generator;

import java.io.IOException;

public class Main {

	public static void main (String args[]){
		Generator g = new Generator(5, 5, 5, 2, 60, 50, 10);
		
		g.GenerateGraph();
		
		try {
			g.toFile("/home/roberto/workspace/LS_mxc/src/ls_mxc/tests/ex1.test");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
