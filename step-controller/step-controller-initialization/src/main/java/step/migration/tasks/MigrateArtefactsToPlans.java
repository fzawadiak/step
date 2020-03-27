package step.migration.tasks;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.jongo.Mapper;
import org.jongo.marshall.Unmarshaller;
import org.jongo.marshall.jackson.JacksonMapper;

import com.mongodb.BasicDBObject;

import ch.exense.commons.app.ArgumentParser;
import ch.exense.commons.app.Configuration;
import step.artefacts.CallPlan;
import step.core.GlobalContext;
import step.core.Version;
import step.core.accessors.AccessorLayerJacksonMapperProvider;
import step.core.accessors.MongoClientSession;
import step.core.accessors.PlanAccessorImpl;
import step.core.artefacts.AbstractArtefact;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessorImpl;
import step.core.plans.Plan;
import step.core.scheduler.ExecutionTaskAccessorImpl;
import step.core.scheduler.ExecutiontTaskParameters;
import step.functions.accessor.FunctionAccessorImpl;
import step.functions.accessor.FunctionCRUDAccessor;
import step.migration.MigrationTask;
import step.plugins.functions.types.CompositeFunction;

/**
 * This task migrates the collection 'artefacts' to the collection 'plans' which has been introduced in 3.13 
 *
 */
public class MigrateArtefactsToPlans extends MigrationTask {

	private static final String CHILDREN_ID_FIELD = "childrenIDs";
	private com.mongodb.client.MongoCollection<Document> artefactCollection;
	private com.mongodb.client.MongoCollection<Document> functionCollection;
	private com.mongodb.client.MongoCollection<Document> executionCollection;
	private com.mongodb.client.MongoCollection<Document> tasksCollection;
	private PlanAccessorImpl planAccessor;
	private ExecutionAccessorImpl executionAccessor;
	private ExecutionTaskAccessorImpl executionTaskAccessor;
	private FunctionCRUDAccessor functionAccessor;
	private Mapper dbLayerObjectMapper;
	private Map<ObjectId, ObjectId> artefactIdToPlanId;
	private Unmarshaller unmarshaller;

	public MigrateArtefactsToPlans() {
		super(new Version(3,13,0));
	}

	@Override
	protected void setContext(GlobalContext context) {
		super.setContext(context);
		init(context.getMongoClientSession());
		context.put(MigrateArtefactsToPlans.class, this);
	}

	protected void init(MongoClientSession mongoClientSession) {
		artefactCollection = mongoClientSession.getMongoDatabase().getCollection("artefacts");
		executionCollection = mongoClientSession.getMongoDatabase().getCollection("executions");
		functionCollection = mongoClientSession.getMongoDatabase().getCollection("functions");
		tasksCollection = mongoClientSession.getMongoDatabase().getCollection("tasks");
		
		JacksonMapper.Builder builder2 = new JacksonMapper.Builder();
		AccessorLayerJacksonMapperProvider.getModules().forEach(m->builder2.registerModule(m));
		dbLayerObjectMapper = builder2.build();
		unmarshaller = dbLayerObjectMapper.getUnmarshaller();
		
		planAccessor = new PlanAccessorImpl(mongoClientSession);
		executionAccessor = new ExecutionAccessorImpl(mongoClientSession);
		executionTaskAccessor = new ExecutionTaskAccessorImpl(mongoClientSession);
		functionAccessor = new FunctionAccessorImpl(mongoClientSession);

		artefactIdToPlanId = new HashMap<>();
	}

	@Override
	public void runUpgradeScript() {
		int count = generatePlanIds();
		logger.info("Found "+count+" root artefacts to be migrated. Starting migration...");
		
		migrateArtefactsToPlans();
		migrateCompositeFunctionsFunctions();
		migrateExecutions();
		migrateSchedulerTasks();
	}
	
	private int generatePlanIds() {
		logger.info("Searching for root artefacts to be migrated...");
		AtomicInteger count = new AtomicInteger();
		Document filterRootArtefacts = new Document("root", true);
		artefactCollection.find(filterRootArtefacts, BasicDBObject.class).iterator().forEachRemaining(t -> {
			ObjectId objectId = t.getObjectId("_id");
			artefactIdToPlanId.put(objectId, new ObjectId());
			count.incrementAndGet();
		});
		return count.get();
	}
	
	private void migrateArtefactsToPlans() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();

		Document filterRootArtefacts = new Document("root", true);
		artefactCollection.find(filterRootArtefacts, BasicDBObject.class).iterator().forEachRemaining(t -> {
			migrateArtefactToPlan(successCount, errorCount, t);
		});
		
		logger.info("Migrated "+successCount.get()+" artefacts successfully.");
		if(errorCount.get()>0) {
			logger.error(errorCount.get() + " artefacts couldn't be migrated. See error logs for details");
		}
		
		successCount.set(0);
		errorCount.set(0);
		
	}

	protected Plan migrateArtefactToPlan(BasicDBObject t) {
		return migrateArtefactToPlan(null, null, t);
	}
	
	protected Plan migrateArtefactToPlan(AtomicInteger successCount, AtomicInteger errorCount, BasicDBObject t) {
		Map<String, String> attributes = new HashMap<>();
		try {
			BasicDBObject document = (BasicDBObject)t.get("attributes");
			if(document != null) {
				document.keySet().forEach(key->{
					attributes.put(key, document.getString(key));
				});
			}
			
			AbstractArtefact artefact = unmarshallArtefact(t);
			
			Plan plan = new Plan();
			
			plan.setId(artefactIdToPlanId.get(artefact.getId()));
			plan.setAttributes(attributes);
			plan.setRoot(artefact);
			plan.setVisible(true);
			
			logger.info("Migrated plan "+attributes);
			
			plan = planAccessor.save(plan);
			if(successCount != null) {
				successCount.incrementAndGet();
			}
			return plan;
		} catch(Exception e) {
			logger.error("Error while migrating plan "+attributes, e);
			if(errorCount != null) {
				errorCount.incrementAndGet();
			}
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private AbstractArtefact unmarshallArtefact(BasicDBObject t) {
		List<ObjectId> childrendIDs = null;
		if(t.containsField(CHILDREN_ID_FIELD)) {
			childrendIDs = (List<ObjectId>) t.get(CHILDREN_ID_FIELD);
		}
		t.remove(CHILDREN_ID_FIELD);
		
		AbstractArtefact artefact = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(t), AbstractArtefact.class);
		
		if(artefact instanceof CallPlan) {
			String artefactId = t.getString("artefactId");
			if(artefactId!=null) {
				ObjectId referencedPlanId = artefactIdToPlanId.get(new ObjectId(artefactId));
				if(referencedPlanId != null) {
					((CallPlan) artefact).setPlanId(referencedPlanId.toString());
				} else {
					logger.warn("The artefact "+artefactId+" referenced by the artefact (call plan) "+t.getObjectId("_id").toString()+" doesn't exist");
				}
			} else {
				// Call by attributes => nothing to do as we're assigning the attributes of the root artefact to the plan
			}
		}
		
		if(childrendIDs!=null) {
			childrendIDs.forEach(childID->{
				BasicDBObject child = artefactCollection.find(new Document("_id", childID), BasicDBObject.class).first();
				AbstractArtefact artefactChild = unmarshallArtefact(child);
				artefact.getChildren().add(artefactChild);
			});
		}
		
		return artefact;
	}
	
	private void migrateCompositeFunctionsFunctions() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		
		Document filterCompositeFunction = new Document("type", CompositeFunction.class.getName());
		functionCollection.find(filterCompositeFunction, BasicDBObject.class).iterator().forEachRemaining(t -> {
			try {
				if(t.containsField("artefactId")) {
					String id = t.getString("_id");
					String artefactId = t.getString("artefactId");
					
					BasicDBObject rootArtefact = artefactCollection.find(new Document("_id", new ObjectId(artefactId)), BasicDBObject.class).first();
					if(rootArtefact != null) {
						Plan plan = migrateArtefactToPlan(rootArtefact);
						if(plan != null) {
							ObjectId planId = plan.getId();
							t.put("planId", planId);
							t.remove("artefactId");
							CompositeFunction compositeFunction = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(t), CompositeFunction.class);
							functionAccessor.save(compositeFunction);
							successCount.incrementAndGet();
						} else {
							errorCount.incrementAndGet();
							logger.error("Error while migrating plan for composite function " + id + " with artefactId "+artefactId);
						}
					} else {
						errorCount.incrementAndGet();
						logger.error("Unable to find root artefact for composite function " + id + " with artefactId "+artefactId);
					}
				}
			} catch (Exception e) {
				errorCount.incrementAndGet();
				logger.error("Unexpected error while migrating composite function " + t, e);
			}
		});
		
		logger.info("Migrated "+successCount.get()+" composite functions successfully.");
		if(errorCount.get()>0) {
			logger.error("Got "+errorCount+" errors while migrating composite functions. See previous error logs for details.");
		}
	}

	private void migrateExecutions() {
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger errorCount = new AtomicInteger();
		
		logger.info("Searching for executions be migrated...");
		executionCollection.find(BasicDBObject.class).iterator().forEachRemaining(t -> {
			try {
				BasicDBObject object = (BasicDBObject) t.get("executionParameters");
				ExecutionParametersMigrationResult executionParameterMigrationResult = migrateExecutionParameter(object);
				if(executionParameterMigrationResult.executionParametersUpdated) {
					// ... and save the result while ensuring integrity by unmarshalling as POJO
					Execution execution = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(t), Execution.class);
					execution.setPlanId(executionParameterMigrationResult.planId);
					executionAccessor.save(execution);
					successCount.incrementAndGet();
				}
			} catch (Exception e) {
				errorCount.incrementAndGet();
				logger.error("Error while migrating execution " + t, e);
			}
		});
		logger.info("Migrated "+successCount.get()+" executions successfully.");
		if(errorCount.get()>0) {
			logger.error("Got "+errorCount+" errors while migrating executions. See previous error logs for details.");
		}
	}
	
	protected static class ExecutionParametersMigrationResult {
		boolean executionParametersUpdated;
		String planId;
	}
	
	protected ExecutionParametersMigrationResult migrateExecutionParameter(BasicDBObject object) {
		ExecutionParametersMigrationResult result = new ExecutionParametersMigrationResult();
		if(object != null) {
			BasicDBObject artefact = (BasicDBObject) object.get("artefact");
			if(artefact != null) {
				// Rename the field "repositoryParameters.artefactid" to "repositoryParameters.planid"
				String planIdString = migrateRepositoryObjectReference(artefact);
				result.planId = planIdString;
				
				// Rename the field "artefact" to "repositoryObject"
				object.put("repositoryObject", artefact);
				object.remove("artefact");
				result.executionParametersUpdated = true;
			}
		}
		return result;
	}

	protected String migrateRepositoryObjectReference(BasicDBObject artefact) {
		String result = null;
		ObjectId planId;
		BasicDBObject repositoryParameters = (BasicDBObject) artefact.get("repositoryParameters");
		if(repositoryParameters != null) {
			String artefactId = repositoryParameters.getString("artefactid");
			planId = artefactIdToPlanId.get(new ObjectId(artefactId));
			if(planId != null) {
				String planIdString = planId.toString();
				repositoryParameters.put("planid", planIdString);
				result = planIdString;
			}
			repositoryParameters.remove("artefactid");
		}
		return result;
	}
	
	private void migrateSchedulerTasks() {
		tasksCollection.find(BasicDBObject.class).iterator().forEachRemaining(t -> {
			BasicDBObject executionsParameters = (BasicDBObject) t.get("executionsParameters");
			ExecutionParametersMigrationResult executionParameterMigrationResult = migrateExecutionParameter(executionsParameters);
			if(executionParameterMigrationResult.executionParametersUpdated) {
				ExecutiontTaskParameters executionTaskParameters = unmarshaller.unmarshall(org.jongo.bson.Bson.createDocument(t), ExecutiontTaskParameters.class);
				executionTaskAccessor.save(executionTaskParameters);
			}
		});
	}
	
	public static void main(String[] args) throws IOException {
		ArgumentParser arguments = new ArgumentParser(args);
		Configuration configuration;
		if(arguments.hasOption("config")) {
			configuration = new Configuration(new File(arguments.getOption("config")));
		} else {
			configuration = new Configuration();
			configuration.putProperty("db.host", "localhost");
		}
		MongoClientSession mongoClientSession = new MongoClientSession(configuration);
		MigrateArtefactsToPlans task = new MigrateArtefactsToPlans();
		task.init(mongoClientSession);
		task.runUpgradeScript();
	}
	
	@Override
	public void runDowngradeScript() {
	}
}
