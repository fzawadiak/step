/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.artefacts.handlers;

import static step.planbuilder.BaseArtefacts.sequence;
import static step.planbuilder.BaseArtefacts.sleep;

import java.io.StringWriter;

import org.junit.Test;

import junit.framework.Assert;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;

public class SleepHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test() throws Exception {
		int sleepTime = 100;
		Plan plan = PlanBuilder.create().startBlock(sequence()).add(sleep(sleepTime)).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		long t1 = System.currentTimeMillis();
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		Assert.assertTrue(System.currentTimeMillis()-t1>sleepTime);
		Assert.assertTrue(writer.toString().startsWith("Sequence:"+ReportNodeStatus.PASSED));
	}
}

