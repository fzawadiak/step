/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.grid.agent;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import step.grid.bootstrap.ResourceExtractor;

public class AgentRunner {
	
	public static void main(String[] args) throws Exception {
		File gridJar = ResourceExtractor.extractResource(AgentRunner.class.getClassLoader(), "step-grid-agent.jar");
		URLClassLoader cl = new URLClassLoader(new URL[]{gridJar.toURI().toURL()}, AgentRunner.class.getClassLoader());
		Thread.currentThread().setContextClassLoader(cl);
		cl.loadClass("step.grid.agent.Agent").getMethod("newInstanceFromArgs",args.getClass()).invoke(null, (Object)args);
	}
}
