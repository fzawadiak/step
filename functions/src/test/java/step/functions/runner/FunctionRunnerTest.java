package step.functions.runner;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import step.functions.Function;
import step.functions.Output;

public class FunctionRunnerTest {

	@Test
	public void test() {
		TestFunction f = new TestFunction();
		f.setId(new ObjectId());
		Map<String, String> attributes = new HashMap<>();
		attributes.put(Function.NAME, "moustache");
		f.setAttributes(attributes);
		Output o = FunctionRunner.getContext(new TestFunctionType()).run(f, "{}", new HashMap<>());
		Assert.assertEquals("tache", o.getResult().getString("mous"));
	}
}