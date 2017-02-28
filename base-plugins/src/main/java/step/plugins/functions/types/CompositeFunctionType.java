package step.plugins.functions.types;

import java.util.HashMap;
import java.util.Map;

import step.artefacts.Sequence;
import step.core.artefacts.AbstractArtefact;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.type.AbstractFunctionType;
import step.functions.type.SetupFunctionException;

public class CompositeFunctionType extends AbstractFunctionType<CompositeFunction> {


	@Override
	public void init() {
		super.init();
		
		context.get(FunctionEditorRegistry.class).register(CompositeFunction.class, new FunctionEditor<CompositeFunction>() {
			@Override
			public String getEditorPath(CompositeFunction function) {
				return "/root/artefacteditor/"+function.getArtefactId();
			}
		});
	}
	
	@Override
	public String getHandlerChain(CompositeFunction function) {
		return "class:step.core.tokenhandlers.ArtefactMessageHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(CompositeFunction function) {
		Map<String, String> props = new HashMap<>();
		props.put("artefactid", function.getArtefactId());
		return props;
	}

	@Override
	public void setupFunction(CompositeFunction function) throws SetupFunctionException {
		super.setupFunction(function);
  		Sequence sequence = new Sequence();
  		context.getArtefactAccessor().save(sequence);
  		
  		function.setArtefactId(sequence.getId().toString());		
	}

	@Override
	public CompositeFunction copyFunction(CompositeFunction function) {
		CompositeFunction copy = super.copyFunction(function);

		String artefactId = function.getArtefactId();
		AbstractArtefact artefactCopy = context.getArtefactManager().copyArtefact(artefactId);
		
		copy.setArtefactId(artefactCopy.getId().toString());
		return copy;
	}

	@Override
	public CompositeFunction newFunction() {
		return new CompositeFunction();
	}
}
