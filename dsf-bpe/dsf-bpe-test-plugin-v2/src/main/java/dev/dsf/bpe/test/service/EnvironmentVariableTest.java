package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class EnvironmentVariableTest extends AbstractTest implements ServiceTask, InitializingBean
{
	private final String envVariableMandatory;
	private final String envVariableOptional;
	private final String envVariableProxyUrl;

	public EnvironmentVariableTest(String envVariableMandatory, String envVariableOptional, String envVariableProxyUrl)
	{
		this.envVariableMandatory = envVariableMandatory;
		this.envVariableOptional = envVariableOptional;
		this.envVariableProxyUrl = envVariableProxyUrl;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(envVariableMandatory, "envVariableMandatory");
		Objects.requireNonNull(envVariableOptional, "envVariableOptional");
		Objects.requireNonNull(envVariableProxyUrl, "envVariableProxyUrl");
	}

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void verifyEnvVariableMandatory() throws Exception
	{
		expectNotNull(envVariableMandatory);
		expectSame("test-value", envVariableMandatory);
	}

	@PluginTest
	public void verifyenvVariableOptional() throws Exception
	{
		expectNotNull(envVariableOptional);
		expectSame("default-value", envVariableOptional);
	}

	@PluginTest
	public void verifyEnvVariableProxyUrl() throws Exception
	{
		expectNotNull(envVariableProxyUrl);
		expectSame("http://proxy:8080", envVariableProxyUrl);
	}
}
