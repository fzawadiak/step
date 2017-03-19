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
package step.core.artefacts.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentManager;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.ArtefactFilter;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ArtefactCache;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.miscellaneous.ValidationException;

public abstract class ArtefactHandler<ARTEFACT extends AbstractArtefact, REPORT_NODE extends ReportNode> {
	
	protected static Logger logger = LoggerFactory.getLogger(ArtefactHandler.class);
	
	protected ExecutionContext context;
	
	protected ReportNodeAttachmentManager reportNodeAttachmentManager;
	
	public static String FILE_VARIABLE_PREFIX = "file:";
	public static String CONTINUE_EXECUTION = "tec.continueonerror";
	public static String CONTINUE_EXECUTION_ONCE = "tec.continueonerror.once";

	
	public ArtefactHandler() {
		super();
		
		context = ExecutionContext.getCurrentContext();
		
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(context.getGlobalContext().getAttachmentManager());
	}
	
	private enum Phase {
		
		SKELETON_CREATION,
		
		EXECUTION;
	}
		
	@SuppressWarnings("unchecked")
	private REPORT_NODE beforeDelegation(Phase executionPhase, ReportNode parentNode, ARTEFACT testArtefact,  Map<String, Object> newVariables) {
		REPORT_NODE node;
		
		if(executionPhase==Phase.EXECUTION && testArtefact.isCreateSkeleton()) {
			ReportNodeAccessor reportNodeAccessor = context.getGlobalContext().getReportAccessor();
			node = (REPORT_NODE) reportNodeAccessor.getReportNodeByParentIDAndArtefactID(parentNode.getId(), testArtefact.getId());
			if(node == null) {
				throw new RuntimeException("Unable to find report node during execution phase. "
						+ "The report node should have been created during skeleton creation phase as the artefact has createSkeleton flag enabled. AbstractArtefact="+testArtefact.toString()+ ". ParentNode:"+ parentNode.toString());
			}
		} else {
			node = createReportNode(parentNode, testArtefact);			
		}

		ExecutionContext.setCurrentReportNode(node);
		
		context.getReportNodeCache().put(node);
		
		if(newVariables!=null) {
			for(Entry<String, Object> var:newVariables.entrySet()) {
				context.getVariablesManager().putVariable(node, var.getKey(), var.getValue());
			}
		}
		
		handleAttachments(testArtefact, node);
		
		context.getVariablesManager().putVariable(parentNode, "currentArtefact", testArtefact);
		context.getVariablesManager().putVariable(parentNode, "currentReport", node);
		
		addCustomReportNodeAttributes(node);
		
		return node;
	}

	private void addCustomReportNodeAttributes(REPORT_NODE node) {
		for(Entry<String,String> entry:((Map<String,String>)ReportNodeAttributesManager.getCustomAttributes()).entrySet()) {
			node.addCustomAttribute(entry.getKey(), entry.getValue());
		}
	}
	
	private void afterDelegation(REPORT_NODE node, ReportNode parentNode, ARTEFACT testArtefact) {
		context.getReportNodeCache().remove(node);
		ExecutionContext.setCurrentReportNode(context.getReportNodeCache().get(node.getParentID().toString()));
		
		context.getVariablesManager().releaseVariables(node.getId().toString());
		
		context.getVariablesManager().putVariable(parentNode, "report", node);
		
		context.getGlobalContext().getEventManager().notifyReportNodeDestroyed(node);
	}
	
	public void createReportSkeleton(ReportNode parentNode, ARTEFACT testArtefact,  Map<String, Object> newVariables) {
		REPORT_NODE node = beforeDelegation(Phase.SKELETON_CREATION, parentNode, testArtefact, newVariables);
		
		try {
			context.getGlobalContext().getDynamicBeanResolver().evaluate(testArtefact, getBindings());
			
			ArtefactFilter filter = ExecutionContext.getCurrentContext().getExecutionParameters().getArtefactFilter();
			if(filter!=null&&!filter.isSelected(testArtefact)) {
				node.setStatus(ReportNodeStatus.SKIPPED);
			} else {
				createReportSkeleton_(node, testArtefact);
			}
		} catch (Exception e) {
			getListOfArtefactsNotInitialized().add(testArtefact.getId().toString());
			failWithException(node, e, false);
		}
		
		if(testArtefact.isCreateSkeleton()) {
			ReportNodeAccessor reportNodeAccessor = context.getGlobalContext().getReportAccessor();
			reportNodeAccessor.save(node);
		}
		
		context.getGlobalContext().getPluginManager().getProxy().afterReportNodeSkeletonCreation(node);
		
		afterDelegation(node, parentNode, testArtefact);
	}

	private Map<String, Object> getBindings() {
		Map<String, Object> bindings = new HashMap<>();
		bindings.putAll(context.getVariablesManager().getAllVariables());
		bindings.put("attachmentManager", context.getGlobalContext().getAttachmentManager());
		return bindings;
	}
	
	protected abstract void createReportSkeleton_(REPORT_NODE parentNode, ARTEFACT testArtefact);

	@SuppressWarnings("unchecked")
	private HashSet<String> getListOfArtefactsNotInitialized() {
		Object o = context.get("SKELETON_NOT_INIT");
		HashSet<String> result;
		if(o==null) {
			result = new HashSet<String>();
			context.put("SKELETON_NOT_INIT", result);
		} else {
			result = (HashSet<String>) o;
		}
		return result;
	}
	
	public ReportNode execute(REPORT_NODE parentNode, ARTEFACT testArtefact, Map<String, Object> newVariables) {
		if(getListOfArtefactsNotInitialized().contains(testArtefact.getId().toString())) {
			createReportSkeleton(parentNode, testArtefact, newVariables);
		}
		
		REPORT_NODE node = beforeDelegation(Phase.EXECUTION, parentNode, testArtefact, newVariables);
		ReportNodeAccessor reportNodeAccessor = context.getGlobalContext().getReportAccessor();
		
		long t1 = System.currentTimeMillis();
		try {
			context.getGlobalContext().getDynamicBeanResolver().evaluate(testArtefact, getBindings());
			node.setArtefactInstance(testArtefact);
			
			ArtefactFilter filter = ExecutionContext.getCurrentContext().getExecutionParameters().getArtefactFilter();
			if(filter!=null&&!filter.isSelected(testArtefact)) {
				node.setStatus(ReportNodeStatus.SKIPPED);
			} else {
				boolean persistBefore = context.getVariablesManager().getVariableAsBoolean("tec.execution.reportnodes.persistbefore",true);
				if(persistBefore) {
					reportNodeAccessor.save(node);					
				}
				
				execute_(node, testArtefact);
			}
		} catch (Exception e) {
			failWithException(node, e);
		}
		long duration = System.currentTimeMillis() - t1;
		
		node.setDuration((int)duration);
		node.setExecutionTime(System.currentTimeMillis());
		
		boolean persistAfter = context.getVariablesManager().getVariableAsBoolean("tec.execution.reportnodes.persistafter",true);
		if(persistAfter) {
			reportNodeAccessor.save(node);					
		}
		
		context.getGlobalContext().getPluginManager().getProxy().afterReportNodeExecution(node);
		
		afterDelegation(node, parentNode, testArtefact);
		
		return node;
	}
	
	protected abstract void execute_(REPORT_NODE node, ARTEFACT testArtefact);

	@SuppressWarnings("unchecked")
	public static void delegateCreateReportSkeleton(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables) {
		ArtefactHandler<AbstractArtefact, ReportNode> testArtefactHandler = ArtefactHandlerRegistry.getInstance()
				.getArtefactHandler((Class<AbstractArtefact>) artefact.getClass());
		testArtefactHandler.createReportSkeleton(parentNode, artefact, newVariables);
	}
	
	public static void delegateCreateReportSkeleton(AbstractArtefact artefact, ReportNode parentNode) {
		delegateCreateReportSkeleton(artefact, parentNode, null);
	}
	
	public static ReportNode delegateExecute(AbstractArtefact artefact, ReportNode parentNode) {
		return delegateExecute(artefact, parentNode, null);
	}
	
	@SuppressWarnings("unchecked")
	public static ReportNode delegateExecute(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables) {
		ArtefactHandler<AbstractArtefact, ReportNode> testArtefactHandler = ArtefactHandlerRegistry.getInstance()
				.getArtefactHandler((Class<AbstractArtefact>) artefact.getClass());
		return testArtefactHandler.execute(parentNode, artefact, newVariables);
	}
	
	private REPORT_NODE createReportNode(ReportNode parentNode, ARTEFACT testArtefact) {
		REPORT_NODE node = createReportNode_(parentNode, testArtefact);
		node._id = new ObjectId();
		node.setName(testArtefact.getReportNodeName());
		node.setParentID(parentNode.getId());
		node.setArtefactID(testArtefact.getId());
		node.setExecutionID(context.getExecutionId().toString());
		node.setStatus(ReportNodeStatus.NORUN);
		return node;
	}
	
	public abstract REPORT_NODE createReportNode_(ReportNode parentNode, ARTEFACT testArtefact);	
	
	public static List<AbstractArtefact> getChildren(AbstractArtefact artefact) { 
		ArtefactCache artefactCache = ExecutionContext.getCurrentContext().getArtefactCache();
		ArtefactAccessor accessor = ExecutionContext.getCurrentContext().getGlobalContext().getArtefactAccessor();
		List<AbstractArtefact> result = new ArrayList<>();
		if(artefact.getChildrenIDs()!=null) {
			for(ObjectId childrenID:artefact.getChildrenIDs()) {
				AbstractArtefact child = artefactCache.get(childrenID.toString());
				if(child==null) {
					child = accessor.get(childrenID);
					artefactCache.put(child);
				}
				result.add(child);
			}
		}
		return result;
	}
	
	private void handleAttachments(AbstractArtefact artefact, ReportNode report) {
		List<ObjectId> attachments = artefact.getAttachments();
		if(attachments!=null) {
			for(ObjectId attachmentId:attachments) {
				AttachmentManager attachmentManager = context.getGlobalContext().getAttachmentManager();
				File file = attachmentManager.getFileById(attachmentId);
				context.getVariablesManager().putVariable(report, FILE_VARIABLE_PREFIX+file.getName(), file);
			}
		}
	}
	
	protected static Integer asInteger(String string, Integer defaultValue) {
		return string!=null&&string.length()>0?Integer.decode(string):defaultValue;
	}
	
	protected static Integer asInteger(String string) {
		return asInteger(string, null);
	}
	
	protected static Boolean asBoolean(String string, Boolean defaultValue) {
		return string!=null&&string.length()>0?Boolean.valueOf(string):defaultValue;
	}
	
	protected void fail(ReportNode node, String error) {
		node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
		node.setError(error, 0, true);
	}
	
	protected void failWithException(ReportNode result, Throwable e) {
		failWithException(result, e, true);
	}
	
	protected void failWithException(ReportNode result, Throwable e, boolean generateAttachment) {
		failWithException(result, null, e, generateAttachment);

	}
	
	protected void failWithException(ReportNode result, String errorMsg, Throwable e, boolean generateAttachment) {
		if(generateAttachment && !(e instanceof ValidationException)) {			
			reportNodeAttachmentManager.attach(e, result);
		}
		result.setError(errorMsg!=null?errorMsg+":"+e.getMessage():e.getMessage(), 0, true);	
		result.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
	}
}
