package dev.dsf.bpe.test.service;

import java.util.List;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class QuestionnaireTestSetIdentifies implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		variables.setStringList("identifierValues", List.of("dic-user@test.org", "foo@invalid", "bar@invalid"));
	}
}
