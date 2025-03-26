package dev.dsf.bpe.test.message;

import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.test.listener.StartSendTaskTestListener;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.MessageSendTask;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.variables.Variables;

public class SendTaskTest extends AbstractTest implements MessageSendTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables, SendTaskValues sendTask) throws Exception
	{
		executeTests(api, variables, sendTask);
	}

	@PluginTest
	public void checkSendTaskInstantiatesCanonical(SendTaskValues sendTask) throws Exception
	{
		expectSame("instantiatesCanonicalValue", sendTask.instantiatesCanonical());
	}

	@PluginTest
	public void checkSendTaskMessageName(SendTaskValues sendTask) throws Exception
	{
		expectSame("messageNameValue", sendTask.messageName());
	}

	@PluginTest
	public void checkSendTaskProfile(SendTaskValues sendTask) throws Exception
	{
		expectSame(StartSendTaskTestListener.TEST_VARIABLE_VALUE, sendTask.profile());
	}
}
