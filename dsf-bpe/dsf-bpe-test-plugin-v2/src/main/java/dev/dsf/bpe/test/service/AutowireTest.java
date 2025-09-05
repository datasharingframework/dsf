package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.test.TestProcessPluginDefinition;
import dev.dsf.bpe.test.autowire.DemoService;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class AutowireTest extends AbstractTest implements ServiceTask
{
	private final DemoService demoService;
	private final TestProcessPluginDefinition pluginDefinition;

	public AutowireTest(DemoService demoService, TestProcessPluginDefinition pluginDefinition)
	{
		this.demoService = demoService;
		this.pluginDefinition = pluginDefinition;
	}

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void demoServiceNotNull() throws Exception
	{
		expectNotNull(demoService);
	}

	@PluginTest
	public void testProcessPluginDefinitionNotNull() throws Exception
	{
		expectNotNull(pluginDefinition);
	}
}
