package dev.dsf.bpe.v2.activity;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.CreateQuestionnaireResponseValues;
import dev.dsf.bpe.v2.error.UserTaskListenerErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultUserTaskListenerErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public interface UserTaskListener extends Activity
{
	void notify(ProcessPluginApi api, Variables variables,
			CreateQuestionnaireResponseValues createQuestionnaireResponse) throws Exception;

	default UserTaskListenerErrorHandler getErrorHandler()
	{
		return new DefaultUserTaskListenerErrorHandler();
	}
}
