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
package dev.dsf.bpe.engine;

import static org.hl7.fhir.instance.model.api.IBaseBundle.LINK_NEXT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import dev.dsf.bpe.api.Constants;
import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.client.dsf.WebserviceClient;

public class DelegateProviderImpl implements DelegateProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(DelegateProviderImpl.class);

	private final ClassLoader defaultClassLoader;
	private final ApplicationContext defaultApplicationContext;
	private final WebserviceClient webserviceClient;

	private final Map<ProcessIdAndVersion, ProcessPlugin> processPluginsByProcessIdAndVersion = new HashMap<>();

	public DelegateProviderImpl(ClassLoader mainClassLoader, ApplicationContext mainApplicationContext,
			WebserviceClient webserviceClient)
	{
		this.defaultClassLoader = mainClassLoader;
		this.defaultApplicationContext = mainApplicationContext;
		this.webserviceClient = webserviceClient;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(defaultClassLoader, "defaultClassLoader");
		Objects.requireNonNull(defaultApplicationContext, "defaultApplicationContext");
		Objects.requireNonNull(webserviceClient, "webserviceClient");
	}

	@Override
	public void setProcessPlugins(List<ProcessPlugin> plugins,
			Map<ProcessIdAndVersion, ProcessPlugin> processPluginsByProcessIdAndVersion)
	{
		this.processPluginsByProcessIdAndVersion.putAll(processPluginsByProcessIdAndVersion);
	}

	/**
	 * @param processIdAndVersion
	 * @return {@link Optional#empty()} if plugin not available
	 */
	private Optional<ProcessPlugin> getPlugin(ProcessIdAndVersion processIdAndVersion)
	{
		return Optional.ofNullable(processPluginsByProcessIdAndVersion.get(processIdAndVersion));
	}

	@Override
	public Class<?> getDefaultUserTaskListenerClass(ProcessIdAndVersion processIdAndVersion)
	{
		return getPlugin(processIdAndVersion).map(ProcessPlugin::getDefaultUserTaskListenerClass)
				.orElseThrow(handlePluginNotFound(processIdAndVersion));
	}

	@Override
	public boolean isDefaultUserTaskListenerOrSuperClassOf(ProcessIdAndVersion processIdAndVersion, String className)
	{
		return getPlugin(processIdAndVersion).map(p -> p.isDefaultUserTaskListenerOrSuperClassOf(className))
				.orElseThrow(handlePluginNotFound(processIdAndVersion));
	}

	private Supplier<ProcessEngineException> handlePluginNotFound(ProcessIdAndVersion processIdAndVersion)
	{
		return () ->
		{
			logger.warn("Plugin for process {} not found", processIdAndVersion);
			return new ProcessEngineException("Plugin for process " + processIdAndVersion + " not found");
		};
	}

	@Override
	public JavaDelegate getMessageSendTask(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getJavaDelegate(processIdAndVersion,
				plugin -> plugin.getMessageSendTask(className, fieldDeclarations, variableScope));
	}

	@Override
	public JavaDelegate getServiceTask(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getJavaDelegate(processIdAndVersion,
				plugin -> plugin.getServiceTask(className, fieldDeclarations, variableScope));
	}

	@Override
	public JavaDelegate getMessageEndEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getJavaDelegate(processIdAndVersion,
				plugin -> plugin.getMessageEndEvent(className, fieldDeclarations, variableScope));
	}

	@Override
	public JavaDelegate getMessageIntermediateThrowEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getJavaDelegate(processIdAndVersion,
				plugin -> plugin.getMessageIntermediateThrowEvent(className, fieldDeclarations, variableScope));
	}

	private JavaDelegate getJavaDelegate(ProcessIdAndVersion processIdAndVersion,
			Function<ProcessPlugin, JavaDelegate> getDelegate)
	{
		return getPlugin(processIdAndVersion).map(plugin ->
		{
			JavaDelegate delegate = getDelegate.apply(plugin);
			return (JavaDelegate) e -> plugin.getPluginMdc().executeWithProcessMdc(e, delegate::execute);

		}).orElseGet(() -> e -> stopProcess(processIdAndVersion, e));
	}

	@Override
	public ExecutionListener getExecutionListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getPlugin(processIdAndVersion).map(plugin ->
		{
			ExecutionListener delegate = plugin.getExecutionListener(className, fieldDeclarations, variableScope);
			return (ExecutionListener) e -> plugin.getPluginMdc().executeWithProcessMdc(e, delegate::notify);

		}).orElseGet(() -> e -> stopProcess(processIdAndVersion, e));
	}

	@Override
	public TaskListener getTaskListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		return getPlugin(processIdAndVersion).<TaskListener> map(plugin ->
		{
			TaskListener delegate = plugin.getTaskListener(className, fieldDeclarations, variableScope);
			return (TaskListener) e -> plugin.getPluginMdc().executeWithProcessMdc(e, delegate::notify);

		}).orElseGet(() -> e -> stopProcess(processIdAndVersion, e.getExecution()));
	}

	private void stopProcess(ProcessIdAndVersion processIdAndVersion, DelegateExecution execution)
	{
		logger.warn(
				"Plugin for process {} not found, unable to continue execution of instance with business-key {}, updating tasks to status 'failed'",
				processIdAndVersion, execution.getBusinessKey());

		try
		{
			searchInProgessTasks(execution.getBusinessKey()).forEach(task ->
			{
				try
				{
					task.setStatus(TaskStatus.FAILED);
					task.addOutput()
							.setValue(new StringType("Plugin for process not found, unable to continue execution"))
							.getType().getCodingFirstRep().setSystem(Constants.BPMN_MESSAGE_URL)
							.setCode(Constants.BPMN_MESSAGE_ERROR);

					webserviceClient.update(task);
				}
				catch (Exception e)
				{
					logger.debug("Unable to update Task with id {} (status failed)", task.getIdElement().getIdPart(),
							e);
					logger.error("Unable to update Task with id {} (status failed): {} - {}",
							task.getIdElement().getIdPart(), e.getClass().getName(), e.getMessage());
				}
			});
		}
		catch (Exception e)
		{
			logger.debug("Unable to update Tasks to status 'failed' for process {} instance with business-key {}",
					processIdAndVersion, execution.getBusinessKey(), e);
			logger.error(
					"Unable to update Tasks to status 'failed' for process {} instance with business-key {}: {} - {}",
					processIdAndVersion, execution.getBusinessKey(), e.getClass().getName(), e.getMessage());
		}

		execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(),
				"Plugin for process " + processIdAndVersion + " not found");
	}

	protected final Stream<Task> searchInProgessTasks(String businessKey)
	{
		List<Stream<BundleEntryComponent>> resources = new ArrayList<>();

		boolean hasMore = true;
		int page = 1;
		while (hasMore)
		{
			Bundle resultBundle = searchInProgessTasks(page++);

			resources.add(resultBundle.getEntry().stream().filter(BundleEntryComponent::hasSearch)
					.filter(BundleEntryComponent::hasResource));

			hasMore = resultBundle.getLink(LINK_NEXT) != null;
		}

		return resources.stream().flatMap(Function.identity())
				.filter(e -> SearchEntryMode.MATCH.equals(e.getSearch().getMode()))
				.filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.filter(r -> r instanceof Task).map(r -> (Task) r).filter(hasBusinessKey(businessKey));
	}

	private Bundle searchInProgessTasks(int page)
	{
		// TODO add business-key custom search parameter to FHIR server

		Map<String, List<String>> parameters = Map.of("status", List.of("in-progress"), "_page",
				List.of(String.valueOf(page)), "_sort", List.of("_id"));

		return webserviceClient.searchWithStrictHandling(Task.class, parameters);
	}

	private Predicate<Task> hasBusinessKey(String businessKey)
	{
		return t -> businessKey != null && t.getInput().stream().filter(ParameterComponent::hasType)
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> co != null && Constants.BPMN_MESSAGE_URL.equals(co.getSystem())
								&& Constants.BPMN_MESSAGE_BUSINESS_KEY.equals(co.getCode())))
				.filter(ParameterComponent::hasValue).map(ParameterComponent::getValue)
				.filter(v -> v instanceof StringType).map(v -> (StringType) v).map(StringType::getValue)
				.anyMatch(businessKey::equals);
	}
}
