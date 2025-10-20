package dev.dsf.bpe.api.logging;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.slf4j.MDC;

public abstract class AbstractPluginMdc implements PluginMdc
{
	public static final String DSF_PLUGIN_API = "dsf.plugin.api";
	public static final String DSF_PLUGIN_JAR = "dsf.plugin.jar";
	public static final String DSF_PLUGIN_NAME = "dsf.plugin.name";
	public static final String DSF_PLUGIN_VERSION = "dsf.plugin.version";

	public static final String DSF_PROCESS = "dsf.process";
	public static final String DSF_PROCESS_TASK_START = "dsf.process.task.start";
	public static final String DSF_PROCESS_REQUESTER_START = "dsf.process.requester.start";

	public static final String DSF_PROCESS_CORRELATION_KEY = "dsf.process.correlationKey";
	public static final String DSF_PROCESS_TASK_LATEST = "dsf.process.task.latest";
	public static final String DSF_PROCESS_REQUESTER_LATEST = "dsf.process.requester.latest";

	private final int apiVersion;
	private final String jar;
	private final String name;
	private final String version;

	/**
	 * @param apiVersion
	 * @param name
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @param jar
	 *            not <code>null</code>
	 */
	public AbstractPluginMdc(int apiVersion, String name, String version, String jar)
	{
		this.apiVersion = apiVersion;
		this.name = Objects.requireNonNull(name, "name");
		this.version = Objects.requireNonNull(version, "version");
		this.jar = Objects.requireNonNull(jar, "jar");
	}

	private void putPluginMdc()
	{
		MDC.put(DSF_PLUGIN_API, String.valueOf(apiVersion));
		MDC.put(DSF_PLUGIN_JAR, jar);
		MDC.put(DSF_PLUGIN_NAME, name);
		MDC.put(DSF_PLUGIN_VERSION, version);
	}

	private void putProcessMdc(DelegateExecution delegateExecution)
	{
		ProcessValues processValues = getProcessValues(delegateExecution);
		if (processValues != null)
		{
			// business-key added to mdc by camunda
			MDC.put(DSF_PROCESS, processValues.processUrl());
			MDC.put(DSF_PROCESS_TASK_START, processValues.startTaskUrl());
			MDC.put(DSF_PROCESS_REQUESTER_START, processValues.startRequesterIdentifierValue());

			if (processValues.correlationKey() != null)
				MDC.put(DSF_PROCESS_CORRELATION_KEY, processValues.correlationKey());
			if (processValues.latestTaskUrl() != null)
				MDC.put(DSF_PROCESS_TASK_LATEST, processValues.latestTaskUrl());
			if (processValues.latestRequesterIdentifierValue() != null)
				MDC.put(DSF_PROCESS_REQUESTER_LATEST, processValues.latestRequesterIdentifierValue());
		}
	}

	private void removePluginMdc()
	{
		MDC.remove(DSF_PLUGIN_API);
		MDC.remove(DSF_PLUGIN_JAR);
		MDC.remove(DSF_PLUGIN_NAME);
		MDC.remove(DSF_PLUGIN_VERSION);
	}

	private void removeProcessMdc()
	{
		MDC.remove(DSF_PROCESS);
		MDC.remove(DSF_PROCESS_TASK_START);
		MDC.remove(DSF_PROCESS_REQUESTER_START);

		MDC.remove(DSF_PROCESS_CORRELATION_KEY);
		MDC.remove(DSF_PROCESS_TASK_LATEST);
		MDC.remove(DSF_PROCESS_REQUESTER_LATEST);
	}

	public static final record ProcessValues(String processUrl, String startTaskUrl,
			String startRequesterIdentifierValue, String correlationKey, String latestTaskUrl,
			String latestRequesterIdentifierValue)
	{
	}

	protected abstract ProcessValues getProcessValues(DelegateExecution delegateExecution);

	@Override
	public void executeWithProcessMdc(DelegateTask delegateTask, Consumer<DelegateTask> executable)
	{
		putPluginMdc();
		putProcessMdc(delegateTask.getExecution());

		try
		{
			executable.accept(delegateTask);
		}
		finally
		{
			removePluginMdc();
			removeProcessMdc();
		}
	}

	@Override
	public void executeWithProcessMdc(DelegateExecution delegateExecution,
			ConsumerWithException<DelegateExecution> executable) throws Exception
	{
		putPluginMdc();
		putProcessMdc(delegateExecution);

		try
		{
			executable.accept(delegateExecution);
		}
		finally
		{
			removePluginMdc();
			removeProcessMdc();
		}
	}

	@Override
	public void executeWithPluginMdc(Runnable runnable)
	{
		putPluginMdc();

		try
		{
			runnable.run();
		}
		finally
		{
			removePluginMdc();
		}
	}

	@Override
	public boolean executeWithPluginMdc(Supplier<Boolean> supplier)
	{
		putPluginMdc();

		try
		{
			return supplier.get();
		}
		finally
		{
			removePluginMdc();
		}
	}
}
