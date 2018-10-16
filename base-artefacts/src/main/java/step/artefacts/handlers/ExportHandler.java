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
package step.artefacts.handlers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.io.Files;

import step.artefacts.Export;
import step.attachments.AttachmentMeta;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class ExportHandler extends ArtefactHandler<Export, ReportNode> {
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Export testArtefact) {
		export(parentNode, testArtefact);
	}

	@Override
	protected void execute_(ReportNode node, Export testArtefact) {
		node.setStatus(ReportNodeStatus.PASSED);
		export(node, testArtefact);
	}

	private void export(ReportNode node, Export testArtefact) {
		String filename = testArtefact.getFile().get();
		if(filename != null) {
			File file = new File(filename);
			if(testArtefact.getValue()!=null) {
				Object value = testArtefact.getValue().get();
				if(value instanceof List<?>) {
					if(file.isDirectory()) {
						List<?> list = (List<?>) value;
						for (Object object : list) {
							if(object instanceof AttachmentMeta) {
								AttachmentMeta attachmentMeta = (AttachmentMeta) object;
								File fileToCopy = context.getAttachmentManager().getFileById(attachmentMeta.getId().toString());
								File target = new File(file+"/"+fileToCopy.getName());
								try {
									Files.copy(fileToCopy, target);
								} catch (IOException e) {
									throw new RuntimeException("Error while copying file "+fileToCopy.getName()+" to "+target.getAbsolutePath());
								}
							}
						}						
					}
				} else {
					
				}
			}
		} else {
			
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Export testArtefact) {
		return new ReportNode();
	}
}