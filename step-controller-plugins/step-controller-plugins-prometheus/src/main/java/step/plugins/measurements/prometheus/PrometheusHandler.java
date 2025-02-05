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
package step.plugins.measurements.prometheus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.plugins.measurements.*;


public class PrometheusHandler implements MeasurementHandler {

	private static final Logger logger = LoggerFactory.getLogger(PrometheusHandler.class);

	private Map<String, Set<String[]>> labelsByExec;

	private final Histogram measurementHisto;

	public PrometheusHandler(Histogram measurementHistogram) {
		super();
		labelsByExec = new ConcurrentHashMap();
		this.measurementHisto = measurementHistogram;
		GaugeCollectorRegistry.getInstance().registerHandler(this);
	}

	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
		if (!labelsByExec.containsKey(executionContext.getExecutionId())) {
			labelsByExec.put(executionContext.getExecutionId(), new HashSet<>());
		}
	}

	public void processMeasurements(List<Measurement> measurements) {
		for (Measurement measurement : measurements) {
			String taskId = measurement.getTaskId();
			String[] labels = {measurement.getExecId(), measurement.getName(), measurement.getType(),
					measurement.getStatus(), measurement.getPlanId(), (taskId!=null) ? taskId :""};
			labelsByExec.get(measurement.getExecId()).add(labels);
			try {
				measurementHisto.labels(labels).observe(measurement.getValue() / 1000.0);
			}catch (Exception e){
				logger.error("Unable to update prometheus measurement histogram for labels: " + labels,e);
			}
		}
	}

	public void afterExecutionEnd(ExecutionContext context) {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		Runnable task = new Runnable() {
			public void run() {
				for (String[] labels : labelsByExec.remove(context.getExecutionId())) {
					measurementHisto.remove(labels);
				}
			}
		};
		int delay = 70;
		scheduler.schedule(task, delay, TimeUnit.SECONDS);
		scheduler.shutdown();
	}

	public void processGauges(List<Measurement> measurements) {
		//nothing to be done for prometheus, gauge are only defined in abstract measurement plugin and
		// exposed by the prometheus controller plugin
	}
}
