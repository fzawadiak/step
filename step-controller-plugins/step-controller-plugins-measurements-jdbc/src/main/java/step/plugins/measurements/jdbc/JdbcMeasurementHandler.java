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
package step.plugins.measurements.jdbc;

import java.sql.*;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.plugins.measurements.GaugeCollectorRegistry;
import step.plugins.measurements.MeasurementPlugin;
import step.plugins.measurements.Measurement;
import step.plugins.measurements.MeasurementHandler;

public class JdbcMeasurementHandler implements MeasurementHandler {

	private static final Logger logger = LoggerFactory.getLogger(JdbcMeasurementHandler.class);

	private static String SQLinsertWJson = "INSERT INTO measurements("+MeasurementPlugin.BEGIN+","+ MeasurementPlugin.ATTRIBUTE_EXECUTION_ID +"," +
			MeasurementPlugin.STATUS +"," + MeasurementPlugin.PLAN_ID + ","+ MeasurementPlugin.TASK_ID+ "," + MeasurementPlugin.NAME + "," +
			MeasurementPlugin.TYPE + "," + MeasurementPlugin.VALUE + ",info) VALUES(?,?,?,?,?,?,?,?,?)";

	private static String SQLinsertPostgres = "INSERT INTO measurements("+MeasurementPlugin.BEGIN+","+ MeasurementPlugin.ATTRIBUTE_EXECUTION_ID +"," +
			MeasurementPlugin.STATUS +"," + MeasurementPlugin.PLAN_ID + ","+ MeasurementPlugin.TASK_ID+ "," + MeasurementPlugin.NAME + "," +
			MeasurementPlugin.TYPE + "," + MeasurementPlugin.VALUE + ",info) VALUES(?,?,?,?,?,?,?,?,?::jsonb)";

	private static String SQLinsert = "INSERT INTO measurements("+MeasurementPlugin.BEGIN+","+ MeasurementPlugin.ATTRIBUTE_EXECUTION_ID +"," +
			MeasurementPlugin.STATUS +"," + MeasurementPlugin.PLAN_ID + ","+ MeasurementPlugin.TASK_ID+ "," + MeasurementPlugin.NAME + "," +
			MeasurementPlugin.TYPE + "," + MeasurementPlugin.VALUE + ") VALUES(?,?,?,?,?,?,?,?)";

	private boolean useCustomJsonColumn;

	public JdbcMeasurementHandler(boolean useCustomJsonColumn) {
		super();
		this.useCustomJsonColumn = useCustomJsonColumn;
		GaugeCollectorRegistry.getInstance().registerHandler(this);
	}

	public void processMeasurements(List<Measurement> measurements) {
		try (Connection jdbcCon = DriverManager.getConnection(JdbcMeasurementControllerPlugin.ConnectionPoolName)) {
			boolean isPostGres = jdbcCon.getMetaData().getDatabaseProductName().contains("PostgreSQL");
			String insertStatement = SQLinsert;
			if (useCustomJsonColumn) {
				insertStatement = (isPostGres) ? SQLinsertPostgres : SQLinsertWJson;
			}
			try (PreparedStatement preparedStatement = jdbcCon.prepareStatement(insertStatement, Statement.RETURN_GENERATED_KEYS)) {
				jdbcCon.setAutoCommit(false);

				//Prepare statements
				for (Measurement measurement : measurements){
					try {
						addPrpStmtToBatch(preparedStatement,useCustomJsonColumn, measurement.getBegin(), measurement.getExecId(),
								measurement.getStatus(), measurement.getPlanId(), measurement.getTaskId(),
								measurement.getName(), measurement.getType(), measurement.getValue(), measurement.getCustomFields());
					} catch (SQLException e) {
						logger.error("Error while setting values of prepared statement", e);
					} catch (JsonProcessingException e) {
						logger.error("Error while transforming map to json payload", e);
					}
				}
				//Execute in batch
				int[] numUpdates = preparedStatement.executeBatch();
				for (int i = 0; i < numUpdates.length; i++) {
					if (numUpdates[i] < 0)
						logger.error("Batch updates failed for statement #" + i + ", " + preparedStatement.toString());
				}
				jdbcCon.commit();
			} catch (SQLException e) {
				logger.error("Error while persisting measurements to the database", e );
			}

		} catch (SQLException e) {
			logger.error("Error while persisting measurements to the database", e );
		}

	}

	public void processGauges(List<Measurement> measurements) {
		processMeasurements(measurements);

	}

	private void addPrpStmtToBatch(PreparedStatement preparedStatement, boolean isPostGres, long executionTime,
								   String executionID, String status, String planId, String taskId, String name,
								   String type, long duration, Map<String, Object> info) throws SQLException, JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		preparedStatement.clearParameters();
		//begin_t,info,eId,status,planId,taskId,name,type,value
		preparedStatement.setTimestamp(1,new java.sql.Timestamp(executionTime));
		preparedStatement.setString(2, executionID);
		preparedStatement.setString(3, status);
		preparedStatement.setString(4, planId);
		preparedStatement.setString(5, taskId);
		preparedStatement.setString(6, name);
		preparedStatement.setString(7, type);
		preparedStatement.setLong(8, duration);
		if (isPostGres) {
			preparedStatement.setObject(9, objectMapper.writeValueAsString(info));
		}
		//execute statements in batch with autocommit = false
		preparedStatement.addBatch();
	}

	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext){}
	public void afterExecutionEnd(ExecutionContext context) {}


}
