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
package step.plugins.datatable;

import step.core.GlobalContext;
import step.core.accessors.Collection;
import step.core.accessors.CollectionRegistry;
import step.core.deployment.ObjectHookPlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin(dependencies= {ObjectHookPlugin.class})
public class TablePlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) {
		context.getServiceRegistrationCallback().registerService(TableService.class);
		
		context.get(CollectionRegistry.class).register("users", new Collection(context.getMongoClientSession().getMongoDatabase(), "users", false));
	}

}
