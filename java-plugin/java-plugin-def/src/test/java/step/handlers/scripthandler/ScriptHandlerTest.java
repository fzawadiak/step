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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import step.commons.helpers.FileHelper;
import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;
import step.functions.Output;
import step.functions.runner.FunctionRunner;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.GeneralScriptFunctionType;

public class ScriptHandlerTest {

	@Test 
	public void test1() {
		GeneralScriptFunction f = buildTestFunction("javascript","test1.js");
		Output output = run(f, "{\"key1\":\"val1\"}");
		Assert.assertEquals("val1",output.getResult().getString("key1"));
	}
	private Output run(GeneralScriptFunction f, String inputJson) {
		return FunctionRunner.getContext(new GeneralScriptFunctionType()).run(f, inputJson, new HashMap<>());
	}

	@Test 
	public void testGroovy1() {
		GeneralScriptFunction f = buildTestFunction("groovy","testGroovy1.groovy");
		Output output = run(f, "{\"key1\":\"val1\"}");
		Assert.assertEquals("val1",output.getResult().getString("key1"));
	}

	@Test 
	public void testGroovy() {
		GeneralScriptFunction f = buildTestFunction("groovy","testGroovyUTF8.groovy");
		Output output = run(f, "{\"key1\":\"val1\"}");
		Assert.assertEquals("kéÿ1",output.getResult().getString("key1"));
	}
	
//	@Test 
//	public void testPython1() {
//		GeneralScriptFunction f = buildTestFunction("python","testPython.py");
//		Output output = run(f, "{\"key1\":\"val1\"}");
//		Assert.assertEquals("val1",output.getResult().getString("key1"));
//	}
	
// TODO implement error handler
//	@Test 
//	public void testErrorHandler() {
//		GeneralScriptFunction f = buildTestFunction("javascript","errorHandler.js");
//		f.setErrorHandler ...
//		try {
//			Output out = run(f, "{\"key1\":\"val1\"}");
//			Assert.assertFalse(true);
//		} catch(Exception e) {
//			Assert.assertEquals("executed", System.getProperties().get("errorHandler"));
//		}
//	}
	
	@Test 
	public void testParallel() throws InterruptedException, ExecutionException, TimeoutException {
		int nIt = 100;
		int nThreads = 10;
		ExecutorService e = Executors.newFixedThreadPool(nThreads);
		
		List<Future<Boolean>> results = new ArrayList<>();
		
		for(int i=0;i<nIt;i++) {
			results.add(e.submit(new Callable<Boolean>() {
				
				@Override
				public Boolean call() throws Exception {
					GeneralScriptFunction f = buildTestFunction("javascript","test1.js");
					Output output = run(f, "{\"key1\":\"val1\"}");
					Assert.assertEquals("val1",output.getResult().getString("key1"));
					return true;
				}
			}));
		}
		
		for (Future<Boolean> future : results) {
			future.get(1, TimeUnit.MINUTES);
		}
	}
	
	private GeneralScriptFunction buildTestFunction(String scriptLanguage, String scriptFile) {
		GeneralScriptFunction f = new GeneralScriptFunction();
		f.setScriptLanguage(new DynamicValue<String>(scriptLanguage));
		f.setLibrariesFile(new DynamicValue<>());
		f.setId(new ObjectId());
		Map<String, String> attributes = new HashMap<>();
		attributes.put(Function.NAME, "medor");
		f.setAttributes(attributes);

		f.setScriptFile(new DynamicValue<String>(getScriptDir() + "/" + scriptFile));
		return f;
	}
	
	private String getScriptDir() {
		return FileHelper.getClassLoaderResource(this.getClass(),"scripts").getAbsolutePath();
	}
}