package step.plugins.functions.types;

import java.io.File;
import java.util.Map;

import step.functions.type.AbstractFunctionType;
import step.functions.type.SetupFunctionException;

public class SeleniumFunctionType extends AbstractFunctionType<SeleniumFunction> {

	ScriptFunctionTypeHelper helper;
	
	@Override
	public void init() {
		super.init();
		helper = new ScriptFunctionTypeHelper(getContext());
	}
	
	@Override
	public String getHandlerChain(SeleniumFunction function) {
		return "class:step.handlers.scripthandler.ScriptHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(SeleniumFunction function) {
		return helper.getHandlerProperties(function);
	}

	@Override
	public void setupFunction(SeleniumFunction function) throws SetupFunctionException {
		File scriptFile = helper.setupScriptFile(function);
		helper.createScriptFromTemplate(scriptFile, "kw_selenium.js");
	}
	
	@Override
	public SeleniumFunction newFunction() {
		SeleniumFunction function = new SeleniumFunction();
		function.getScriptLanguage().setValue("javascript");
		return function;
	}
}
