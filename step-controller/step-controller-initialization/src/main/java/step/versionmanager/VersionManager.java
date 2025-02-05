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
package step.versionmanager;

import java.util.Date;

import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;

public class VersionManager {
	
	private static final Logger logger = LoggerFactory.getLogger(VersionManager.class);
	
	private final GlobalContext context;
	
	private final MongoCollection controllerLogs;
	
	private ControllerLog latestControllerLog = null;
	
	public VersionManager(GlobalContext context) {
		super();
		this.context = context;
		
		controllerLogs = context.getMongoClientSession().getJongoCollection("controllerlogs");
	}

	public void readLatestControllerLog() {
		MongoCursor<ControllerLog> cursor = controllerLogs.find().sort("{start:-1}").as(ControllerLog.class);
		
		if(cursor.count()>0) {
			latestControllerLog = controllerLogs.find().sort("{start:-1}").as(ControllerLog.class).next();
			logger.info("Last start of the controller: "+ latestControllerLog.toString());
		} else {
			logger.info("No start log found. Starting the controller for the first time against this DB...");
		}
	}
	
	public ControllerLog getLatestControllerLog() {
		return latestControllerLog;
	}

	public void setLatestControllerLog(ControllerLog latestControllerLog) {
		this.latestControllerLog = latestControllerLog;
	}

	public void insertControllerLog() {
		ControllerLog logEntry = new ControllerLog();
		logEntry.setStart(new Date());
		logEntry.setVersion(context.getCurrentVersion());
		controllerLogs.insert(logEntry);
	}
}
