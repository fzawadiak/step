package step.core.execution;

import ch.exense.commons.app.Configuration;
import step.core.AbstractContext;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.PlanAccessor;
import step.engine.execution.ExecutionManager;
import step.engine.execution.MockedExecutionManagerImpl;
import step.expressions.ExpressionHandler;

public abstract class AbstractExecutionEngineContext extends AbstractContext {

	private Configuration configuration;
	private ExpressionHandler expressionHandler;
	private DynamicBeanResolver dynamicBeanResolver;
	private PlanAccessor planAccessor;
	private ReportNodeAccessor reportNodeAccessor;
	private ExecutionManager executionManager;
	
	public AbstractExecutionEngineContext() {
		super();
	}

	public AbstractExecutionEngineContext(AbstractExecutionEngineContext parentContext) {
		super(parentContext);
		if(parentContext != null) {
			setAttributesFromParentContext(parentContext);
		} else {
			setDefaultAttributes();
		}
	}

	protected void setDefaultAttributes() {
		configuration = new Configuration();
		expressionHandler = new ExpressionHandler();
		dynamicBeanResolver = new DynamicBeanResolver(new DynamicValueResolver(expressionHandler));
		planAccessor = new InMemoryPlanAccessor();
		reportNodeAccessor = new InMemoryReportNodeAccessor();
		executionManager = new MockedExecutionManagerImpl();
	}
	
	protected void setAttributesFromParentContext(AbstractExecutionEngineContext parentContext) {
		configuration = parentContext.getConfiguration();
		expressionHandler = parentContext.getExpressionHandler();
		dynamicBeanResolver = parentContext.getDynamicBeanResolver();
		planAccessor = parentContext.getPlanAccessor();
		reportNodeAccessor = parentContext.getReportNodeAccessor();
		executionManager = parentContext.getExecutionManager();
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public ExpressionHandler getExpressionHandler() {
		return expressionHandler;
	}
	
	public void setExpressionHandler(ExpressionHandler expressionHandler) {
		this.expressionHandler = expressionHandler;
	}
	
	public DynamicBeanResolver getDynamicBeanResolver() {
		return dynamicBeanResolver;
	}
	
	public void setDynamicBeanResolver(DynamicBeanResolver dynamicBeanResolver) {
		this.dynamicBeanResolver = dynamicBeanResolver;
	}
	
	public PlanAccessor getPlanAccessor() {
		return planAccessor;
	}
	
	public void setPlanAccessor(PlanAccessor planAccessor) {
		this.planAccessor = planAccessor;
	}
	
	public ReportNodeAccessor getReportAccessor() {
		return getReportNodeAccessor();
	}
	
	public ReportNodeAccessor getReportNodeAccessor() {
		return reportNodeAccessor;
	}
	
	public void setReportNodeAccessor(ReportNodeAccessor reportNodeAccessor) {
		this.reportNodeAccessor = reportNodeAccessor;
	}

	public ExecutionManager getExecutionManager() {
		return executionManager;
	}

	public void setExecutionManager(ExecutionManager executionManager) {
		this.executionManager = executionManager;
	}
}
