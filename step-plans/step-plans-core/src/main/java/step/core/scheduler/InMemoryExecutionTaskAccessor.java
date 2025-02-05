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
package step.core.scheduler;

import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Collectors;

import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryExecutionTaskAccessor extends InMemoryCRUDAccessor<ExecutiontTaskParameters> implements ExecutionTaskAccessor {

	@Override
	public Iterator<ExecutiontTaskParameters> getActiveExecutionTasks() {
		return map.values().stream().filter(e->e.active).sorted(new Comparator<ExecutiontTaskParameters>() {

			@Override
			public int compare(ExecutiontTaskParameters o1, ExecutiontTaskParameters o2) {
				return o1.getId().compareTo(o2.getId());
			}
		}).collect(Collectors.toList()).iterator();
	}

}
