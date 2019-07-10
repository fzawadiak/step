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
package step.core.scheduler;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import step.core.GlobalContext;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.execution.ExecutionRunnable;
import step.core.execution.ExecutionRunnableFactory;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;

public class ExecutionJobFactory implements JobFactory {

	private GlobalContext context;
	
	private ExecutionRunnableFactory executionRunnableFactory;
	
	private ControllerSettingAccessor controllerSettingAccessor;

	public ExecutionJobFactory(GlobalContext context, ExecutionRunnableFactory executionRunnableFactory) {
		super();
		this.context = context;
		this.executionRunnableFactory = executionRunnableFactory;
		
		controllerSettingAccessor = new ControllerSettingAccessor(context.getMongoClientSession());
	}

	@Override
	public Job newJob(TriggerFiredBundle arg0, Scheduler arg1) throws SchedulerException {
		JobDataMap data = arg0.getJobDetail().getJobDataMap();
		ExecutionRunnable task;
		Execution execution;
		if(data.containsKey(Executor.EXECUTION_ID)) {
			String executionID = data.getString(Executor.EXECUTION_ID);
			execution = context.getExecutionAccessor().get(executionID);
		} else {
			String executionTaskID = data.getString(Executor.EXECUTION_TASK_ID);
			ExecutionParameters executionParams = (ExecutionParameters) data.get(Executor.EXECUTION_PARAMETERS);
			
			ControllerSetting schedulerUsernameSetting = controllerSettingAccessor.getSettingByKey("scheduler_execution_username");
			if(schedulerUsernameSetting != null) {
				String schedulerUsername = schedulerUsernameSetting.getValue();
				if(schedulerUsername != null && schedulerUsername.trim().length()>0) {
					executionParams.setUserID(schedulerUsername);
				}
			}
			
			execution = executionRunnableFactory.createExecution(executionParams, executionTaskID);			
		}
		task = executionRunnableFactory.newExecutionRunnable(execution);
		
		ExecutionJob job = new ExecutionJob(task);
		return job;
	}
}