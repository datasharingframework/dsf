package dev.dsf.bpe.test.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;

public class ContinueSendTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		String organizationIdentifierValue = api.getOrganizationProvider().getLocalOrganizationIdentifierValue().get();
		String endpointIdentifierValue = api.getEndpointProvider().getLocalEndpointIdentifierValue().get();
		String endpointAddress = api.getEndpointProvider().getLocalEndpointAddress();

		Target target = variables.createTarget(organizationIdentifierValue, endpointIdentifierValue, endpointAddress);
		variables.setTarget(target);
	}
}
