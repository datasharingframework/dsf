package dev.dsf.bpe.v2.activity;

import org.hl7.fhir.r4.model.Task;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ServiceTaskErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultServiceTaskErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public interface ServiceTask extends Activity
{
	/**
	 * @param api
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @throws ErrorBoundaryEvent
	 *             to trigger custom error handling flow in BPMN, when using {@link DefaultServiceTaskErrorHandler}
	 * @throws Exception
	 *             to fail the FHIR {@link Task} and stop the process instance, when using
	 *             {@link DefaultServiceTaskErrorHandler}
	 */
	void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception;

	@Override
	default ServiceTaskErrorHandler getErrorHandler()
	{
		return new DefaultServiceTaskErrorHandler();
	}
}
