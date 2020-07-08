package step.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.imports.GenericDBImporter;
import step.core.imports.ImportConfiguration;


public class ResourceImpoter extends GenericDBImporter<Resource, ResourceAccessor> {

	public ResourceImpoter(GlobalContext context) {
		super(context);
	}

	@Override
	public Resource importOne(ImportConfiguration importConfig, JsonParser jParser, ObjectMapper mapper,
			Map<String, String> references) throws JsonParseException, JsonMappingException, IOException {
		ResourceManager resourceManager = context.get(ResourceManager.class);
		Resource resource = mapper.readValue(jParser, entity.getEntityClass());
		importConfig.getObjectEnricher().accept(resource);
		resource = importConfig.getLocalResourceMgr().saveResource(resource);
		String origResourceId = resource.getId().toHexString();
		ResourceRevision revision = importConfig.getLocalResourceMgr().getResourceRevisionByResourceId(origResourceId);
		String origRevisionId = revision.getId().toHexString();
		File resourceFile = importConfig.getLocalResourceMgr().getResourceFile(resource.getId().toHexString()).getResourceFile();
		try (FileInputStream fileInputStream = new FileInputStream(resourceFile)){
			if (! importConfig.isOverwrite()) {
				//copy resource to target resource manager with new Id and new revision id,			
				try {
					Resource newResouce = resourceManager.createResource(resource.getResourceType(), fileInputStream,
							resourceFile.getName(), false, importConfig.getObjectEnricher());
					references.put(origResourceId, newResouce.getId().toHexString());
					references.put(origRevisionId, newResouce.getCurrentRevisionId().toHexString());	
				} catch (SimilarResourceExistingException e) {
					throw new RuntimeException("Could not import resource",e);
				}
			} else {
				//for resource do not completely overwrite, but create a new revision
				//note: currently there is anyway no direct ref to revisions (the latest is used)
				 resource = resourceManager.updateResourceContent(resource, fileInputStream, resourceFile.getName(), revision);
			}
		}

		return resource;
	}
}
