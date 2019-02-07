package step.resources;

import java.io.File;

import step.attachments.AttachmentManager;
import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin(prio=0)
public class ResourcePlugin extends AbstractPlugin {

	protected ResourceAccessor resourceAccessor;
	protected ResourceRevisionAccessor resourceRevisionAccessor;
	protected ResourceManager resourceManager;
	protected FileResolver fileResolver;
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		resourceAccessor = new ResourceAccessorImpl(context.getMongoClientSession());
		resourceRevisionAccessor = new ResourceRevisionAccessorImpl(context.getMongoClientSession());
		String resourceRootDir = context.getConfiguration().getProperty("resources.dir","resources");
		resourceManager = new ResourceManagerImpl(new File(resourceRootDir), resourceAccessor, resourceRevisionAccessor);
		context.put(ResourceAccessor.class, resourceAccessor);
		context.put(ResourceManager.class, resourceManager);
		context.getServiceRegistrationCallback().registerService(ResourceServices.class);
		
		fileResolver = new FileResolver(new AttachmentManager(context.getConfiguration()), resourceManager);
		context.put(FileResolver.class, fileResolver);
	}

	@Override
	public void executionStart(ExecutionContext context) {
		context.put(FileResolver.class, fileResolver);
	}

}
