package dev.dsf.bpe.v2.error;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.variables.Variables;

public interface ExceptionErrorHandler extends ErrorHandler
{
	/**
	 * @param api
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @param exception
	 *            not <code>null</code>
	 * @return <code>null</code> to prevent the process from being stopped
	 */
	Exception handleException(ProcessPluginApi api, Variables variables, Exception exception);
}
