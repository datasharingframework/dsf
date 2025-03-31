package dev.dsf.bpe.v2.variables;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.Constants;
import dev.dsf.bpe.v2.constants.BpmnExecutionVariables;
import dev.dsf.bpe.v2.listener.ListenerVariables;
import dev.dsf.bpe.v2.variables.FhirResourceValues.FhirResourceValue;
import dev.dsf.bpe.v2.variables.FhirResourcesListValues.FhirResourcesListValue;
import dev.dsf.bpe.v2.variables.TargetValues.TargetValue;

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
	public String getBusinessKey()
	{
		return execution.getBusinessKey();
	}

	@Override
	public String getCurrentActivityId()
	{
		return execution.getCurrentActivityId();
	}

	@Override
	public String getProcessDefinitionId()
	{
		return execution.getProcessDefinitionId();
	}

	@Override
	public String getActivityInstanceId()
	{
		return execution.getActivityInstanceId();
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
	public void setFhirResourceList(String variableName, List<? extends Resource> resources)
	{
		FhirResourcesListValue variable = resources == null ? null : FhirResourcesListValues.create(resources);
		execution.setVariable(variableName, variable);
	}

	@Override
	public <R extends Resource> List<R> getFhirResourceList(String variableName)
	{
		FhirResourcesList list = (FhirResourcesList) execution.getVariable(variableName);
		return list != null ? list.getResourcesAndCast() : null;
	}

	private <R extends Resource> List<R> getFhirResourceListOrDefault(String variableName, List<R> defaultList)
	{
		List<R> list = getFhirResourceList(variableName);
		return list != null ? list : defaultList;
	}

	@Override
	public void setFhirResource(String variableName, Resource resource)
	{
		FhirResourceValue variable = resource == null ? null : FhirResourceValues.create(resource);
		execution.setVariable(variableName, variable);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Resource> R getFhirResource(String variableName)
	{
		Resource resource = (Resource) execution.getVariable(variableName);
		return (R) resource;
	}

	@Override
	public void setFhirResourceListLocal(String variableName, List<? extends Resource> resources)
	{
		FhirResourcesListValue variable = resources == null ? null : FhirResourcesListValues.create(resources);
		execution.setVariableLocal(variableName, variable);
	}

	@Override
	public <R extends Resource> List<R> getFhirResourceListLocal(String variableName)
	{
		FhirResourcesList list = (FhirResourcesList) execution.getVariableLocal(variableName);
		return list != null ? list.getResourcesAndCast() : null;
	}

	@Override
	public void setFhirResourceLocal(String variableName, Resource resource)
	{
		FhirResourceValue variable = resource == null ? null : FhirResourceValues.create(resource);
		execution.setVariableLocal(variableName, variable);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Resource> R getFhirResourceLocal(String variableName)
	{
		Resource resource = (Resource) execution.getVariableLocal(variableName);
		return (R) resource;
	}

	@Override
	public Task getStartTask()
	{
		logger.trace("getStartTask - parentActivityInstanceId: {}, parentId: {}",
				execution.getParentActivityInstanceId(), execution.getParentId());

		return getFhirResource(START_TASK);
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
								.map(this::getFhirResourceList).flatMap(List::stream).filter(r -> r instanceof Task)
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
		Stream<Task> current = getFhirResourceListOrDefault(TASKS_PREFIX + execution.getParentActivityInstanceId(),
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
				setFhirResource(START_TASK, task);
			else
			{
				String instanceId = execution.getParentActivityInstanceId();
				List<Task> tasks = getFhirResourceListOrDefault(TASKS_PREFIX + instanceId, List.of());

				if (tasks.stream().anyMatch(t -> t.getIdElement().getIdPart().equals(task.getIdElement().getIdPart())))
					setFhirResourceList(TASKS_PREFIX + instanceId, tasks);
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
		return (QuestionnaireResponse) getFhirResource(Constants.QUESTIONNAIRE_RESPONSE_VARIABLE);
	}

	@Override
	public void setInteger(String variableName, Integer value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.integerValue(value));
	}

	@Override
	public void setString(String variableName, String value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.stringValue(value));
	}

	@Override
	public void setByteArray(String variableName, byte[] value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.byteArrayValue(value));
	}

	@Override
	public void setDate(String variableName, Date value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.dateValue(value));
	}

	@Override
	public void setLong(String variableName, Long value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.longValue(value));
	}

	@Override
	public void setShort(String variableName, Short value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.shortValue(value));
	}

	@Override
	public void setDouble(String variableName, Double value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.doubleValue(value));
	}

	@Override
	public void setNumber(String variableName, Number value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.numberValue(value));
	}

	@Override
	public void setFile(String variableName, File value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.fileValue(value));
	}

	@Override
	public void setBoolean(String variableName, Boolean value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.booleanValue(value));
	}

	@Override
	public void setJsonVariable(String variableName, Object value)
	{
		Objects.requireNonNull(variableName, "variableName");

		execution.setVariable(variableName, JsonVariableValues.create(new JsonVariable(value)));
	}

	private void setVariable(String variableName, TypedValue value)
	{
		Objects.requireNonNull(variableName, "variableName");

		execution.setVariable(variableName, value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getVariable(String variableName)
	{
		Objects.requireNonNull(variableName, "variableName");

		Object variable = execution.getVariable(variableName);

		if (variable instanceof JsonVariable jsonVariable)
			return (T) jsonVariable.getValue();
		else
			return (T) variable;
	}

	@Override
	public void setIntegerLocal(String variableName, Integer value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.integerValue(value));
	}

	@Override
	public void setStringLocal(String variableName, String value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.stringValue(value));
	}

	@Override
	public void setByteArrayLocal(String variableName, byte[] value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.byteArrayValue(value));
	}

	@Override
	public void setDateLocal(String variableName, Date value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.dateValue(value));
	}

	@Override
	public void setLongLocal(String variableName, Long value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.longValue(value));
	}

	@Override
	public void setShortLocal(String variableName, Short value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.shortValue(value));
	}

	@Override
	public void setDoubleLocal(String variableName, Double value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.doubleValue(value));
	}

	@Override
	public void setNumberLocal(String variableName, Number value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.numberValue(value));
	}

	@Override
	public void setFileLocal(String variableName, File value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.fileValue(value));
	}

	@Override
	public void setBooleanLocal(String variableName, Boolean value)
	{
		setVariableLocal(variableName, org.camunda.bpm.engine.variable.Variables.booleanValue(value));
	}

	private void setVariableLocal(String variableName, TypedValue value)
	{
		Objects.requireNonNull(variableName, "variableName");

		execution.setVariableLocal(variableName, value);
	}

	@Override
	public void setJsonVariableLocal(String variableName, Object value)
	{
		Objects.requireNonNull(variableName, "variableName");

		execution.setVariableLocal(variableName, JsonVariableValues.create(new JsonVariable(value)));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getVariableLocal(String variableName)
	{
		Objects.requireNonNull(variableName, "variableName");

		Object variable = execution.getVariable(variableName);

		if (variable instanceof JsonVariable jsonVariable)
			return (T) jsonVariable.getValue();
		else
			return (T) variable;
	}

	@Override
	public void onStart(Task task)
	{
		logger.trace("onStart - Task.id: {}", task == null ? "null" : task.getIdElement().getIdPart());

		if (task != null)
			setFhirResource(START_TASK, task);
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

			List<Task> tasks = new ArrayList<>(getFhirResourceListOrDefault(TASKS_PREFIX + instanceId, List.of()));
			tasks.add(task);

			setFhirResourceList(TASKS_PREFIX + instanceId, tasks);
		}
		else
			logger.warn("Given task is null");
	}

	@Override
	public void onEnd()
	{
		logger.trace("onEnd");

		String instanceId = execution.getParentActivityInstanceId();
		List<Task> tasks = new ArrayList<>(getFhirResourceListOrDefault(TASKS_PREFIX + instanceId, List.of()));
		tasks.removeAll(getCurrentTasks());
		setFhirResourceList(TASKS_PREFIX + instanceId, tasks);
	}
}
