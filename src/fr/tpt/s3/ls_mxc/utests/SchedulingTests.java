/*******************************************************************************
 * Copyright (c) 2017 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package fr.tpt.s3.ls_mxc.utests;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.tpt.s3.ls_mxc.parser.MCParser;

/**
 * Verifies that no regressions are introduced
 * on the existing scheduling heuristics
 * @author roberto
 *
 */
public class SchedulingTests {
	
	private Set<MCParser> mcps;
	
	public SchedulingTests() {
		mcps = new HashSet<MCParser>();
	}

	/**
	 * Tests scheduling for a single DAG in a monocore architecture
	 */
	@Test
	public void TestSched1 () {
		
	}
	
	/**
	 * Tests scheduling for multiple DAG, should obtain a schedulable example
	 */
	@Test
	public void TestSched2 () {
		
	}
	
	
	/**
	 * Runs all scheduling tests
	 */
	public void runAll () {
		
	}

	/*
	 * Getters and setters
	 */
	public Set<MCParser> getMcps() {
		return mcps;
	}

	public void setMcps(Set<MCParser> mcps) {
		this.mcps = mcps;
	}
}
