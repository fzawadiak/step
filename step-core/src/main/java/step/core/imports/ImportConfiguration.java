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
package step.core.imports;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import step.core.Version;
import step.core.objectenricher.ObjectEnricher;
import step.resources.LocalResourceManagerImpl;

public class ImportConfiguration {
	private File file;
	private ObjectEnricher objectEnricher;
	private List<String> entitiesFilter;
	private boolean overwrite;
	private Map<String,String> metadata;
	private Version version;
	private LocalResourceManagerImpl localResourceMgr;
	private Set<String> messages = new HashSet<>();

	public ImportConfiguration(File file, ObjectEnricher objectEnricher, List<String> entitiesFilter,
							   boolean overwrite) {
		super();
		this.file = file;
		this.objectEnricher = objectEnricher;
		this.entitiesFilter = entitiesFilter;
		this.overwrite = overwrite;
	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public ObjectEnricher getObjectEnricher() {
		return objectEnricher;
	}
	public void setObjectEnricher(ObjectEnricher objectEnricher) {
		this.objectEnricher = objectEnricher;
	}
	public List<String> getEntitiesFilter() {
		return entitiesFilter;
	}
	public void setEntitiesFilter(List<String> entitiesFilter) {
		this.entitiesFilter = entitiesFilter;
	}
	public boolean isOverwrite() {
		return overwrite;
	}
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	public Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	public Version getVersion() {
		return version;
	}
	public void setVersion(Version version) {
		this.version = version;
	}
	public LocalResourceManagerImpl getLocalResourceMgr() {
		return localResourceMgr;
	}
	public void setLocalResourceMgr(LocalResourceManagerImpl localResourceMgr) {
		this.localResourceMgr = localResourceMgr;
	}
	public Set<String> getMessages() {
		return messages;
	}

	public void addMessages(Set<String> newMessages) {
		messages.addAll(newMessages);
	}

	public void addMessage(String s) {
		messages.add(s);
	}
}
