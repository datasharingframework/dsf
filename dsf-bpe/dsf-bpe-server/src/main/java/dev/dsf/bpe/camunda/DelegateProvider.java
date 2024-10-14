package dev.dsf.bpe.camunda;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.context.ApplicationContext;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;

public interface DelegateProvider extends ProcessPluginConsumer
{
	/**
	 * @param processIdAndVersion
	 *            not <code>null</code>
	 * @return returns the default class loader if no special class loader is registered for the given
	 *         <b>processIdAndVersion</b>
	 */
	ClassLoader getClassLoader(ProcessIdAndVersion processIdAndVersion);

	/**
	 * @param processIdAndVersion
	 *            not <code>null</code>
	 * @return returns the default application context if no special application context is registered for the given
	 *         <b>processIdAndVersion</b>
	 */
	ApplicationContext getApplicationContext(ProcessIdAndVersion processIdAndVersion);

	/**
	 * @param processPluginApiVersion
	 *            not <code>null</code>
	 * @return the default user task listener class for the given process plugin api version
	 */
	Class<? extends TaskListener> getDefaultUserTaskListenerClass(String processPluginApiVersion);
}
