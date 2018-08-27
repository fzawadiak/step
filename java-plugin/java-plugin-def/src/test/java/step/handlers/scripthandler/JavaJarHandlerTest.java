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
package step.handlers.scripthandler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;
import step.functions.Output;
import step.functions.runner.FunctionRunner;
import step.grid.bootstrap.ResourceExtractor;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.GeneralScriptFunctionType;

public class JavaJarHandlerTest {

	@Test 
	public void testJarWithoutKeywords() {
		GeneralScriptFunction f = buildTestFunction("dummy","java-plugin-handler.jar");
		Output output = run(f, "{\"key1\":\"val1\"}");
		Assert.assertEquals("Unexpected error while calling function: java.lang.Exception Unable to find method annoted by 'step.handlers.javahandler.Keyword' with name=='dummy'",output.getError());
	}
	
	@Test 
	public void testJarWithMatchingKeyword() {
		GeneralScriptFunction f = buildTestFunction("MyKeywordNotExisting","java-plugin-handler-test.jar");
		Output output = run(f, "{}");
		Assert.assertEquals("Unexpected error while calling function: java.lang.Exception Unable to find method annoted by 'step.handlers.javahandler.Keyword' with name=='MyKeywordNotExisting'",output.getError());
	}
	
	@Test 
	public void testJarWithKeyword() {
		GeneralScriptFunction f = buildTestFunction("MyKeyword1","java-plugin-handler-test.jar");
		Output output = run(f, "{\"key1\":\"val1\"}");
		Assert.assertEquals("MyValue",output.getResult().getString("MyKey"));
	}
	
	private Output run(GeneralScriptFunction f, String inputJson) {
		return FunctionRunner.getContext(new GeneralScriptFunctionType()).run(f, inputJson, new HashMap<>());
	}
	
	private GeneralScriptFunction buildTestFunction(String kwName, String scriptFile) {
		GeneralScriptFunction f = new GeneralScriptFunction();
		f.setScriptLanguage(new DynamicValue<String>("java"));
		f.setLibrariesFile(new DynamicValue<>());
		f.setId(new ObjectId());
		Map<String, String> attributes = new HashMap<>();
		attributes.put(Function.NAME, kwName);
		f.setAttributes(attributes);
		File file = ResourceExtractor.extractResource(getClass().getClassLoader(), scriptFile);
		f.setScriptFile(new DynamicValue<String>(file.getPath()));
		return f;
	}
}