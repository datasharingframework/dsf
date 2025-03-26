package dev.dsf.bpe.v2.activity;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ServiceTaskErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultServiceTaskErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public interface ServiceTask extends Activity
{
	void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception;

	@Override
	default ServiceTaskErrorHandler getErrorHandler()
	{
		return new DefaultServiceTaskErrorHandler();
	}
}
