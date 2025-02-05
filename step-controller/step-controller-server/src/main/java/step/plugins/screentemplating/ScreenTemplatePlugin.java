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
package step.plugins.screentemplating;

import java.util.Arrays;

import step.core.GlobalContext;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.CollectionRegistry;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class ScreenTemplatePlugin extends AbstractControllerPlugin {

	protected ScreenTemplateManager screenTemplateManager;
	protected ScreenInputAccessor screenInputAccessor;
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		screenInputAccessor = new ScreenInputAccessorImpl(context.getMongoClientSession());
		screenTemplateManager = new ScreenTemplateManager(screenInputAccessor, context.getConfiguration());
		
		initializeScreenInputsIfNecessary();
		
		context.put(ScreenInputAccessor.class, screenInputAccessor);
		context.put(ScreenTemplateManager.class, screenTemplateManager);
		context.getServiceRegistrationCallback().registerService(ScreenTemplateService.class);
		
		context.get(CollectionRegistry.class).register("screenInputs", new Collection<ScreenInput>(context.getMongoClientSession().getMongoDatabase(), "screenInputs", ScreenInput.class, true));
	}

	private void initializeScreenInputsIfNecessary() {
		if(screenInputAccessor.getScreenInputsByScreenId("functionTable").isEmpty()) {
			screenInputAccessor.save(new ScreenInput("functionTable", new Input(InputType.TEXT, "attributes.name", "Name", null)));
		}
		if(screenInputAccessor.getScreenInputsByScreenId("executionTable").isEmpty()) {
			screenInputAccessor.save(new ScreenInput("executionTable", new Input(InputType.TEXT, "executionParameters.customParameters.env", "Environment", null)));
		}
		if(screenInputAccessor.getScreenInputsByScreenId("executionParameters").isEmpty()) {
			screenInputAccessor.save(new ScreenInput("executionParameters", new Input(InputType.DROPDOWN, "env", "Environment", Arrays.asList(new Option[] {new Option("TEST"),new Option("PROD")}))));
		}
	}
}
