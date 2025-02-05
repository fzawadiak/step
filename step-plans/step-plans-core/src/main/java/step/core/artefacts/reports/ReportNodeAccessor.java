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
package step.core.artefacts.reports;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import step.commons.datatable.DataTable;
import step.core.accessors.CRUDAccessor;

public interface ReportNodeAccessor extends CRUDAccessor<ReportNode>, ReportTreeAccessor {

	void createIndexesIfNeeded(Long ttl);

	List<ReportNode> getReportNodePath(ObjectId id);

	Iterator<ReportNode> getChildren(ObjectId parentID);

	Iterator<ReportNode> getChildren(ObjectId parentID, int skip, int limit);

	Iterator<ReportNode> getReportNodesByExecutionID(String executionID);

	long countReportNodesByExecutionID(String executionID);

	Iterator<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_);

	Iterator<ReportNode> getLeafReportNodesByExecutionID(String executionID);

	Iterator<ReportNode> getReportNodesByExecutionIDAndCustomAttribute(String executionID,
			List<Map<String, String>> customAttributes);

	ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID);

	Iterator<ReportNode> getReportNodesByExecutionIDAndArtefactID(String executionID, String artefactID);

	Iterator<ReportNode> getFailedLeafReportNodesByExecutionID(String executionID);

	// TODO check if still working
	DataTable getTimeBasedReport(String executionID, int resolution);

	ReportNode getRootReportNode(String executionID);

	Map<ReportNodeStatus, Integer> getLeafReportNodesStatusDistribution(String executionID, String reportNodeClass);

	Iterator<ReportNode> getChildren(String parentID);

	void removeNodesByExecutionID(String executionID);
}
