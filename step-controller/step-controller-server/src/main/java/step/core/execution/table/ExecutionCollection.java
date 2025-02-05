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
package step.core.execution.table;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.conversions.Bson;

import step.core.GlobalContext;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.DateRangeCriterium;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.ExecutionStatus;

public class ExecutionCollection extends Collection<ExecutionWrapper> {
	
	private final ExecutionSummaryProvider executionSummaryFormatter;
	private final RootReportNodeProvider rootReportNodeFormatter;

	public ExecutionCollection(GlobalContext context) {
		super(context.getMongoClientSession().getMongoDatabase(), "executions", ExecutionWrapper.class, true);
		RootReportNodeProvider rootReportNodeFormatter = new RootReportNodeProvider(context);
		ExecutionSummaryProvider executionSummaryFormatter = new ExecutionSummaryProvider(context);
		this.executionSummaryFormatter = executionSummaryFormatter;
		this.rootReportNodeFormatter = rootReportNodeFormatter;
	}

	@Override
	public ExecutionWrapper enrichEntity(ExecutionWrapper execution) {
		execution.setRootReportNode(rootReportNodeFormatter.getRootReportNode(execution));
		Object executionSummary = executionSummaryFormatter.format(execution);
		execution.setExecutionSummary(executionSummary);
		return execution;
	}

	@Override
	public List<String> distinct(String key) {
		if(key.equals("result")) {
			return Arrays.asList(ReportNodeStatus.values()).stream().map(Object::toString).collect(Collectors.toList());
		} else if(key.equals("status")) {
			return Arrays.asList(ExecutionStatus.values()).stream().map(Object::toString).collect(Collectors.toList());
		} else {
			return super.distinct(key);
		}
	}

	@Override
	public Bson getQueryFragmentForColumnSearch(String columnName, String searchValue) {
		if(columnName.equals("startTime") || columnName.equals("endTime")) {
			Bson queryFragment = new DateRangeCriterium("dd.MM.yyyy").createQuery(columnName, searchValue);
			return queryFragment;
		} else {
			return super.getQueryFragmentForColumnSearch(columnName, searchValue);
		}
	}

	@Override
	public Class<?> getEntityClass() {
		return ExecutionWrapper.class;
	}
}
