package fr.tpt.s3.ls_mxc.generator;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class RandomNumberGenerator {
	
	private MersenneTwister random;
	private Uniform uniform;
	
	public RandomNumberGenerator () {
		random = new MersenneTwister();
		uniform = new Uniform(random);
	}

	public int randomUnifInt(int f, int t) {
		return uniform.nextIntFromTo(f, t);
	}
}
