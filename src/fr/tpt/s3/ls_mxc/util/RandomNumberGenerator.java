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
package fr.tpt.s3.ls_mxc.util;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class RandomNumberGenerator {
	
	private MersenneTwister random;
	private Uniform uniform;
	
	public RandomNumberGenerator () {
		random = new MersenneTwister(new java.util.Date());
		uniform = new Uniform(random);
	}

	/**
	 * Returns a uniform integer between a lower and an upper bound
	 * @param lb
	 * @param ub
	 * @return
	 */
	public int randomUnifInt(int lb, int ub) {
		return uniform.nextIntFromTo(lb, ub);
	}
	
	/**
	 * Returns a uniform double between a lower and an upper bound
	 * @param lb
	 * @param ub
	 * @return
	 */
	public double randomUnifDouble (double lb, double ub) {
		return uniform.nextDoubleFromTo(lb, ub);
	}
}
