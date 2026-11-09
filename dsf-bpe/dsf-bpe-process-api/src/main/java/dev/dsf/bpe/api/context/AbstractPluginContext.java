/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.api.context;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.slf4j.MDC;

public abstract class AbstractPluginContext implements PluginContext
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
	private final ClassLoader pluginClassLoader;

	/**
	 * @param apiVersion
	 * @param name
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @param jar
	 *            not <code>null</code>
	 * @param pluginClassLoader
	 *            not <code>null</code>
	 */
	public AbstractPluginContext(int apiVersion, String name, String version, String jar, ClassLoader pluginClassLoader)
	{
		this.apiVersion = apiVersion;
		this.name = Objects.requireNonNull(name, "name");
		this.version = Objects.requireNonNull(version, "version");
		this.jar = Objects.requireNonNull(jar, "jar");
		this.pluginClassLoader = Objects.requireNonNull(pluginClassLoader, "pluginClassLoader");
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
			// business-key added to mdc by workflow engine
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
	public void executeWithProcessContext(DelegateTask delegateTask, Consumer<DelegateTask> executable)
	{
		withPluginClassLoader(withProcessMdc(delegateTask, executable));
	}

	@Override
	public void executeWithProcessContext(DelegateExecution delegateExecution,
			ConsumerWithException<DelegateExecution> executable) throws Exception
	{
		withPluginClassLoader(withProcessMdc(delegateExecution, executable));
	}

	@Override
	public void executeWithPluginContext(Runnable runnable)
	{
		withPluginClassLoader(withPluginMdc(runnable));
	}

	@Override
	public boolean executeWithPluginContext(Supplier<Boolean> supplier)
	{
		return withPluginClassLoader(withPluginMdc(supplier));
	}

	@FunctionalInterface
	private interface RunnableWithException
	{
		void run() throws Exception;
	}

	private Supplier<Void> withProcessMdc(DelegateTask delegateTask, Consumer<DelegateTask> executable)
	{
		return () ->
		{
			putPluginMdc();
			putProcessMdc(delegateTask.getExecution());

			try
			{
				executable.accept(delegateTask);
			}
			finally
			{
				removeProcessMdc();
				removePluginMdc();
			}

			return null;
		};
	}

	private RunnableWithException withProcessMdc(DelegateExecution delegateExecution,
			ConsumerWithException<DelegateExecution> executable) throws Exception
	{
		return () ->
		{
			putPluginMdc();
			putProcessMdc(delegateExecution);

			try
			{
				executable.accept(delegateExecution);
			}
			finally
			{
				removeProcessMdc();
				removePluginMdc();
			}
		};
	}

	private void withPluginClassLoader(RunnableWithException runnable) throws Exception
	{
		ClassLoader old = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(pluginClassLoader);

		try
		{
			runnable.run();
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(old);
		}
	}

	private Supplier<Void> withPluginMdc(Runnable runnable)
	{
		return () ->
		{
			putPluginMdc();

			try
			{
				runnable.run();

				return null;
			}
			finally
			{
				removePluginMdc();
			}
		};
	}

	private <T> Supplier<T> withPluginMdc(Supplier<T> supplier)
	{
		return () ->
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
		};
	}

	private <T> T withPluginClassLoader(Supplier<T> supplier)
	{
		ClassLoader old = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(pluginClassLoader);

		try
		{
			return supplier.get();
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(old);
		}
	}
}
