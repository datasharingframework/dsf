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
package dev.dsf.bpe.v1.variables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.Constants;
import dev.dsf.bpe.v1.constants.BpmnExecutionVariables;
import dev.dsf.bpe.v1.listener.ListenerVariables;
import dev.dsf.bpe.v1.variables.FhirResourceValues.FhirResourceValue;
import dev.dsf.bpe.v1.variables.FhirResourcesListValues.FhirResourcesListValue;
import dev.dsf.bpe.v1.variables.TargetValues.TargetValue;

public class VariablesImpl implements Variables, ListenerVariables
{
	private static final Logger logger = LoggerFactory.getLogger(VariablesImpl.class);

	private static final String TASKS_PREFIX = VariablesImpl.class.getName() + ".tasks.";
	private static final String START_TASK = VariablesImpl.class.getName() + ".startTask";

	private static final class DistinctTask
	{
		final Task task;

		DistinctTask(Task task)
		{
			this.task = task;
		}

		Task getTask()
		{
			return task;
		}

		@Override
		public boolean equals(Object otherO)
		{
			if (otherO instanceof DistinctTask other)
				return Objects.equals(other.task.getIdElement().getIdPart(), task.getIdElement().getIdPart());
			else
				return false;
		}

		@Override
		public int hashCode()
		{
			return task.getIdElement().getIdPart().hashCode();
		}
	}

	private final DelegateExecution execution;

	public VariablesImpl(DelegateExecution execution)
	{
		this.execution = Objects.requireNonNull(execution, "execution");
	}

	@Override
	public void setAlternativeBusinessKey(String alternativeBusinessKey)
	{
		execution.setVariable(BpmnExecutionVariables.ALTERNATIVE_BUSINESS_KEY, alternativeBusinessKey);
	}

	@Override
	public Target createTarget(String organizationIdentifierValue, String endpointIdentifierValue,
			String endpointAddress, String correlationKey)
	{
		Objects.requireNonNull(organizationIdentifierValue, "organizationIdentifierValue");
		Objects.requireNonNull(endpointIdentifierValue, "endpointIdentifierValue");
		Objects.requireNonNull(endpointAddress, "endpointAddress");

		return new TargetImpl(organizationIdentifierValue, endpointIdentifierValue, endpointAddress, correlationKey);
	}

	@Override
	public void setTarget(Target target) throws IllegalArgumentException
	{
		if (target == null)
		{
			execution.setVariable(BpmnExecutionVariables.TARGET, null);
			return;
		}

		if (!(target instanceof TargetImpl))
			throw new IllegalArgumentException(
					"Given target implementing class " + target.getClass().getName() + " not supported");

		TargetValue variable = TargetValues.create((TargetImpl) target);
		execution.setVariable(BpmnExecutionVariables.TARGET, variable);
	}

	@Override
	public Target getTarget()
	{
		return (TargetImpl) execution.getVariable(BpmnExecutionVariables.TARGET);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Targets createTargets(List<? extends Target> targets)
	{
		if (targets == null)
			return new TargetsImpl(List.of());

		Optional<? extends Target> firstNonMatch = targets.stream().filter(t -> !(t instanceof TargetImpl)).findFirst();
		if (firstNonMatch.isPresent())
			throw new IllegalArgumentException("Target implementing class " + firstNonMatch.get().getClass().getName()
					+ " (in given List) not supported");

		return new TargetsImpl((List<? extends TargetImpl>) targets);
	}

	@Override
	public void setTargets(Targets targets) throws IllegalArgumentException
	{
		if (targets == null)
			execution.setVariable(BpmnExecutionVariables.TARGETS, null);

		else if (targets instanceof TargetsImpl t)
			execution.setVariable(BpmnExecutionVariables.TARGETS, TargetsValues.create(t));

		else
			throw new IllegalArgumentException(
					"Given targets implementing class " + targets.getClass().getName() + " not supported");
	}

	@Override
	public Targets getTargets()
	{
		return (Targets) execution.getVariable(BpmnExecutionVariables.TARGETS);
	}

	@Override
	public void setResourceList(String variableName, List<? extends Resource> resources)
	{
		FhirResourcesListValue variable = resources == null ? null : FhirResourcesListValues.create(resources);
		execution.setVariable(variableName, variable);
	}

	@Override
	public <R extends Resource> List<R> getResourceList(String variableName)
	{
		FhirResourcesList list = (FhirResourcesList) execution.getVariable(variableName);
		return list != null ? list.getResourcesAndCast() : null;
	}

	private <R extends Resource> List<R> getResourceListOrDefault(String variableName, List<R> defaultList)
	{
		List<R> list = getResourceList(variableName);
		return list != null ? list : defaultList;
	}

	@Override
	public void setResource(String variableName, Resource resource)
	{
		FhirResourceValue variable = resource == null ? null : FhirResourceValues.create(resource);
		execution.setVariable(variableName, variable);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Resource> R getResource(String variableName)
	{
		Resource resource = (Resource) execution.getVariable(variableName);
		return (R) resource;
	}

	@Override
	public Task getStartTask()
	{
		logger.trace("getStartTask - parentActivityInstanceId: {}, parentId: {}",
				execution.getParentActivityInstanceId(), execution.getParentId());

		return getResource(START_TASK);
	}

	@Override
	public Task getLatestTask()
	{
		logger.trace("getLatestTask - parentActivityInstanceId: {}, parentId: {}",
				execution.getParentActivityInstanceId(), execution.getParentId());

		List<Task> tasks = getCurrentTasks();
		return tasks == null || tasks.isEmpty() ? null : tasks.get(tasks.size() - 1);
	}

	@Override
	public List<Task> getTasks()
	{
		logger.trace("getTasks - parentActivityInstanceId: {}, parentId: {}", execution.getParentActivityInstanceId(),
				execution.getParentId());

		List<Task> tasks = Stream
				.concat(Stream.of(getStartTask()),
						execution.getVariables().keySet().stream().filter(k -> k.startsWith(TASKS_PREFIX))
								.map(this::getResourceList).flatMap(List::stream).filter(r -> r instanceof Task)
								.map(r -> (Task) r))
				.filter(t -> t != null).map(DistinctTask::new).distinct().map(DistinctTask::getTask).toList();

		return Collections.unmodifiableList(tasks);
	}

	@Override
	public List<Task> getCurrentTasks()
	{
		logger.trace("getCurrentTasks - parentActivityInstanceId: {}, parentId: {}",
				execution.getParentActivityInstanceId(), execution.getParentId());

		Stream<Task> start = execution.getParentId() == null ? Stream.of(getStartTask()) : Stream.empty();
		Stream<Task> current = getResourceListOrDefault(TASKS_PREFIX + execution.getParentActivityInstanceId(),
				List.<Task> of()).stream();

		return Collections.unmodifiableList(Stream.concat(start, current).toList());
	}

	@Override
	public void updateTask(Task task)
	{
		logger.trace("updateTask - Task.id: {}", task == null ? "null" : task.getIdElement().getIdPart());

		if (task != null)
		{
			if (getStartTask() != null
					&& Objects.equals(getStartTask().getIdElement().getIdPart(), task.getIdElement().getIdPart()))
				setResource(START_TASK, task);
			else
			{
				String instanceId = execution.getParentActivityInstanceId();
				List<Task> tasks = getResourceListOrDefault(TASKS_PREFIX + instanceId, List.of());

				if (tasks.stream().anyMatch(t -> t.getIdElement().getIdPart().equals(task.getIdElement().getIdPart())))
					setResourceList(TASKS_PREFIX + instanceId, tasks);
				else
					logger.warn("Given task {} not part of tasks list '{}', ignoring task",
							task.getIdElement().getIdPart(), instanceId);
			}
		}
		else
			logger.warn("Given task is null");
	}

	@Override
	public QuestionnaireResponse getLatestReceivedQuestionnaireResponse()
	{
		return (QuestionnaireResponse) getResource(Constants.QUESTIONNAIRE_RESPONSE_VARIABLE);
	}

	@Override
	public void setVariable(String variableName, TypedValue value)
	{
		Objects.requireNonNull(variableName, "variableName");

		execution.setVariable(variableName, value);
	}

	@Override
	public Object getVariable(String variableName)
	{
		Objects.requireNonNull(variableName, "variableName");

		return execution.getVariable(variableName);
	}

	@Override
	public void onStart(Task task)
	{
		logger.trace("onStart - Task.id: {}", task == null ? "null" : task.getIdElement().getIdPart());

		if (task != null)
			setResource(START_TASK, task);
		else
			logger.warn("Given task is null");
	}

	@Override
	public void onContinue(Task task)
	{
		logger.trace("onContinue - Task.id: {}", task == null ? "null" : task.getIdElement().getIdPart());

		if (task != null)
		{
			String instanceId = execution.getParentActivityInstanceId();

			List<Task> tasks = new ArrayList<>(getResourceListOrDefault(TASKS_PREFIX + instanceId, List.of()));
			tasks.add(task);

			setResourceList(TASKS_PREFIX + instanceId, tasks);
		}
		else
			logger.warn("Given task is null");
	}

	@Override
	public void onEnd()
	{
		logger.trace("onEnd");

		String instanceId = execution.getParentActivityInstanceId();
		List<Task> tasks = new ArrayList<>(getResourceListOrDefault(TASKS_PREFIX + instanceId, List.of()));
		tasks.removeAll(getCurrentTasks());
		setResourceList(TASKS_PREFIX + instanceId, tasks);
	}
}
