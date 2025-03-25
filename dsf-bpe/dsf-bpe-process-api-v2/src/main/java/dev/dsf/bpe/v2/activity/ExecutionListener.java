package dev.dsf.bpe.v2.activity;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ExecutionListenerErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultExecutionListenerErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public interface ExecutionListener extends Activity
{
	void notify(ProcessPluginApi api, Variables variables) throws Exception;
	
	default ExecutionListenerErrorHandler getErrorHandler()
	{
		return new DefaultExecutionListenerErrorHandler();
	}
}
