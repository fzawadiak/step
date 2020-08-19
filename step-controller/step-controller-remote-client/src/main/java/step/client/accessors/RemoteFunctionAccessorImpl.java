package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

public class RemoteFunctionAccessorImpl extends AbstractRemoteCRUDAccessorImpl<Function> implements FunctionAccessor {

	public RemoteFunctionAccessorImpl() {
		super("/rest/functions/", Function.class);
	}
	
	public RemoteFunctionAccessorImpl(ControllerCredentials credentials) {
		super(credentials, "/rest/functions/", Function.class);
	}
}
