package step.functions.execution;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.handler.FunctionInputOutputObjectMapperFactory;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.client.GridClient;
import step.grid.client.GridClientImpl.AgentCallTimeoutException;
import step.grid.client.GridClientImpl.AgentCommunicationException;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.AgentError;
import step.grid.io.AgentErrorCode;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionExecutionServiceImplTest {

	
	@Test
	public void testHappyPath() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, null, null);
		
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		token.setCurrentOwner(new TokenWrapperOwner() {});
		Input<JsonObject> i = getDummyInput();
		Assert.assertFalse(beforeFunctionCallHasBeenCall.get());
		Output<JsonObject> output = f.callFunction(token, getFunction(), i, JsonObject.class);
		Assert.assertNotNull(output);
		Assert.assertTrue(beforeFunctionCallHasBeenCall.get());
		
		Assert.assertNull(output.getError());
		Assert.assertNotNull(token.getCurrentOwner());
		f.returnTokenHandle(token);
	}
	
	@Test
	public void testReserveError() {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, new AgentCommunicationException("Reserve error"), null);
		
		FunctionExecutionServiceException e = null;
		try {
			f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		} catch (FunctionExecutionServiceException e1) {
			e = e1;
		}
		Assert.assertNotNull(e);
		Assert.assertEquals("Communication error between the controller and the agent while reserving the agent token", e.getMessage());
	}
	
	@Test
	public void testReserveTimeoutError() {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, new AgentCallTimeoutException(functionCallTimeout, "Reserve error", null), null);
		
		FunctionExecutionServiceException e = null;
		try {
			f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		} catch (FunctionExecutionServiceException e1) {
			e = e1;
		}
		Assert.assertNotNull(e);
		Assert.assertEquals("Timeout after "+functionCallTimeout+"ms while reserving the agent token. You can increase the call timeout by setting 'grid.client.token.reserve.timeout.ms' in step.properties", e.getMessage());
	}
	
	@Test
	public void testReleaseError() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, null, new AgentCommunicationException("Release error"));
		
		FunctionExecutionServiceException e = null;
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		try {
			f.returnTokenHandle(token);
		} catch (FunctionExecutionServiceException e1) {
			e = e1;
		}
		Assert.assertNotNull(e);
		Assert.assertEquals("Communication error between the controller and the agent while releasing the agent token", e.getMessage());
	}
	
	@Test
	public void testReleaseTimeoutError() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, null, new AgentCallTimeoutException(functionCallTimeout, "Release error", null));
		
		FunctionExecutionServiceException e = null;
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		try {
			f.returnTokenHandle(token);
		} catch (FunctionExecutionServiceException e1) {
			e = e1;
		}
		Assert.assertNotNull(e);
		Assert.assertEquals("Timeout after "+functionCallTimeout+"ms while releasing the agent token. You can increase the call timeout by setting 'grid.client.token.release.timeout.ms' in step.properties", e.getMessage());
	}
	
	@Test
	public void testCallAgentCommunicationException() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, new AgentCommunicationException("Call error"), null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Communication error between the controller and the agent while calling the agent", output.getError().getMsg());
	}
	
	@Test
	public void testCallTimeout() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, new AgentCallTimeoutException(functionCallTimeout, "Call timeout", null), null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Timeout after "+functionCallTimeout+ "ms while calling the agent. You can increase the call timeout in the configuration screen of the keyword", output.getError().getMsg());
		
	}
	
	@Test
	public void testCallException() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, new Exception("Call exception", null), null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Unexpected error while calling keyword: java.lang.Exception Call exception", output.getError().getMsg());
	}
	
	@Test
	public void testMeasures() throws FunctionExecutionServiceException {
		OutputBuilder outputBuilder = new OutputBuilder();
		
		outputBuilder.startMeasure("Measure1");
		outputBuilder.stopMeasure();
		
		Output<JsonObject> expectedOutput = outputBuilder.build();
		
		FunctionExecutionService f = getFunctionExecutionService(expectedOutput, null, null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals(1, output.getMeasures().size());
	}
	
	@Test
	public void testError() throws FunctionExecutionServiceException {
		OutputBuilder outputBuilder = new OutputBuilder();
		Output<JsonObject> expectedOutput = outputBuilder.setError("My error").build();
		
		FunctionExecutionService f = getFunctionExecutionService(expectedOutput, null, null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("My error", output.getError().getMsg());
	}
	
	@Test
	public void testCallAgentError() throws FunctionExecutionServiceException {
		OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
		OutputMessage outputMessage = outputMessageBuilder.build();
		outputMessage.setAgentError(new AgentError(AgentErrorCode.UNEXPECTED));
		
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(outputMessage, null, null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Unexepected error while executing the keyword on the agent", output.getError().getMsg());
	}

	protected Output<JsonObject> callFunctionWithDummyInput(FunctionExecutionService f)
			throws FunctionExecutionServiceException {
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		token.setCurrentOwner(new TokenWrapperOwner() {});
		Input<JsonObject> i = getDummyInput();
		Output<JsonObject> output = f.callFunction(token, getFunction(), i, JsonObject.class);
		return output;
	}

	protected Input<JsonObject> getDummyInput() {
		Input<JsonObject> i = new Input<JsonObject>();
		HashMap<String, String> inputProperties = new HashMap<>();
		inputProperties.put("inputProperty1", "inputProperty1");
		i.setProperties(inputProperties);
		i.setPayload(Json.createObjectBuilder().build());
		return i;
	}

	protected FunctionExecutionService getFunctionExecutionServiceForGridClientTest(OutputMessage outputMessage, Exception callException, AgentCommunicationException reserveTokenException, AgentCommunicationException returnTokenException) {
		GridClient gridClient = getGridClient(outputMessage, callException, reserveTokenException, returnTokenException);
		FunctionTypeRegistry functionTypeRegistry = getFunctionTypeRegistry();
		DynamicBeanResolver dynamicBeanResolver = getDynamicBeanResolver();
		FunctionExecutionService f = new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, dynamicBeanResolver);
		return f;
	}
	
	protected FunctionExecutionService getFunctionExecutionService(Output<JsonObject> output, Exception callException, AgentCommunicationException reserveTokenException, AgentCommunicationException returnTokenException) {
		OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
		
		ObjectMapper mapper = FunctionInputOutputObjectMapperFactory.createObjectMapper();
		outputMessageBuilder.setPayload(mapper.valueToTree(output));
		
		return getFunctionExecutionServiceForGridClientTest(outputMessageBuilder.build(), callException, reserveTokenException, returnTokenException);
	}

	protected DynamicBeanResolver getDynamicBeanResolver() {
		return new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler()));
	}

	AtomicBoolean beforeFunctionCallHasBeenCall = new AtomicBoolean(false);
	
	protected FunctionTypeRegistry getFunctionTypeRegistry() {
		return new FunctionTypeRegistry() {
			
			@Override
			public void registerFunctionType(AbstractFunctionType<? extends Function> functionType) {
			}
			
			@Override
			public AbstractFunctionType<Function> getFunctionTypeByFunction(Function function) {
				return dummyFunctionType();
			}
			
			@Override
			public AbstractFunctionType<Function> getFunctionType(String functionType) {
				return dummyFunctionType();
			}

			protected AbstractFunctionType<Function> dummyFunctionType() {
				return new AbstractFunctionType<Function>() {
					
					@Override
					public void beforeFunctionCall(Function function, Input<?> input, Map<String, String> properties) {
						super.beforeFunctionCall(function, input, properties);
						beforeFunctionCallHasBeenCall.set(true);
					}

					@Override
					public Function newFunction() {
						return null;
					}
					
					@Override
					public Map<String, String> getHandlerProperties(Function function) {
						Map<String, String> handlerProperties = new HashMap<>();
						handlerProperties.put("handlerProperty1", "handlerProperty1");
						return handlerProperties;
					}
					
					@Override
					public String getHandlerChain(Function function) {
						return null;
					}
				};
			}
		};
	}

	int functionCallTimeout = 985;

	private Function getFunction() {
		Function function = new Function();
		function.setCallTimeout(new DynamicValue<Integer>(functionCallTimeout));
		function.setAttributes(new HashMap<>());
		return function;
	}

	protected GridClient getGridClient(OutputMessage outputMessage, Exception callException, AgentCommunicationException reserveTokenException, AgentCommunicationException returnTokenException) {
		return new GridClient() {
			
			@Override
			public String registerFile(File file) {
				return "DummyId";
			}
			
			@Override
			public File getRegisteredFile(String fileHandle) {
				return new File("testFile");
			}
			
			@Override
			public void returnTokenHandle(TokenWrapper tokenWrapper) throws AgentCommunicationException {
				if(returnTokenException != null) {
					throw returnTokenException;
				}
			}
			
			@Override
			public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
					boolean createSession) throws AgentCommunicationException {
				if(reserveTokenException != null) {
					throw reserveTokenException;
				} else {
					return new TokenWrapper();
				}
			}
			
			@Override
			public TokenWrapper getLocalTokenHandle() {
				return new TokenWrapper();
			}
			
			@Override
			public OutputMessage call(TokenWrapper tokenWrapper, JsonNode argument, String handler,
					FileVersionId handlerPackage, Map<String, String> properties, int callTimeout) throws Exception {
				assert callTimeout == functionCallTimeout;
				assert !properties.containsKey("inputProperty1");
				assert !properties.containsKey("handlerProperty1");
				
				ObjectMapper mapper = FunctionInputOutputObjectMapperFactory.createObjectMapper();
				Input<?> input = mapper.treeToValue(argument, Input.class);
				assert input.getFunctionCallTimeout() == functionCallTimeout-100l;
				assert input.getProperties().containsKey("inputProperty1");
				assert input.getProperties().containsKey("handlerProperty1");
				
				if(callException !=null) {
					throw callException;
				} else if(outputMessage !=null) {
					return outputMessage;					
				} else {
					return new OutputMessageBuilder().build();
				}
			}

			@Override
			public void close() {
			}
		};
	}

}
