package dev.dsf.bpe.v2.error;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.variables.Variables;

public interface ErrorBoundaryEventErrorHandler extends ErrorHandler
{
	/**
	 * @param api
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @param event
	 *            not <code>null</code>
	 * @return <code>null</code> to stop event propagation
	 */
	ErrorBoundaryEvent handleErrorBoundaryEvent(ProcessPluginApi api, Variables variables, ErrorBoundaryEvent event);
}
