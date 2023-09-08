package dev.dsf.bpe.variables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.listener.ListenerVariables;
import dev.dsf.bpe.subscription.QuestionnaireResponseHandler;
import dev.dsf.bpe.v1.constants.BpmnExecutionVariables;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Targets;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.bpe.variables.FhirResourceValues.FhirResourceValue;
import dev.dsf.bpe.variables.FhirResourcesListValues.FhirResourcesListValue;
import dev.dsf.bpe.variables.TargetValues.TargetValue;
import dev.dsf.bpe.variables.TargetsValues.TargetsValue;

public class VariablesImpl implements Variables, ListenerVariables
{
	private static final Logger logger = LoggerFactory.getLogger(VariablesImpl.class);

	public static final String TASK_USERDATA_PARENT_ACTIVITY_INSTANCE_ID = VariablesImpl.class.getName()
			+ ".parentActivityInstanceId";
	private static final String TASKS = VariablesImpl.class.getName() + ".tasks";

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
			return new TargetsImpl(Collections.emptyList());

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
		{
			execution.setVariable(BpmnExecutionVariables.TARGETS, null);
			return;
		}

		if (!(targets instanceof TargetsImpl))
			throw new IllegalArgumentException(
					"Given targets implementing class " + targets.getClass().getName() + " not supported");

		TargetsValue variable = targets == null ? null : TargetsValues.create((TargetsImpl) targets);
		execution.setVariable(BpmnExecutionVariables.TARGETS, variable);
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
		return list == null ? null : list.getResourcesAndCast();
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
		List<Task> tasks = getTasks();
		return tasks == null || tasks.isEmpty() ? null : tasks.get(0);
	}

	@Override
	public Task getLatestTask()
	{
		List<Task> tasks = getCurrentTasks();
		return tasks == null || tasks.isEmpty() ? null : tasks.get(tasks.size() - 1);
	}

	@Override
	public List<Task> getTasks()
	{
		List<Task> tasks = getResourceList(TASKS);

		if (tasks == null)
			return Collections.emptyList();
		else
		{
			for (int i = 0; i < tasks.size(); i++)
			{
				Task t = tasks.get(i);
				logger.trace("Task [{}] id: {}, parentActivityInstanceId: {}", i, t.getIdElement().getIdPart(),
						t.getUserData(TASK_USERDATA_PARENT_ACTIVITY_INSTANCE_ID));
			}

			// wrapping again as unmodifiable list to make sure we stay independent from the rest of the code base
			return Collections.unmodifiableList(tasks);
		}
	}

	@Override
	public List<Task> getCurrentTasks()
	{
		logger.trace("parentActivityInstanceId: {}, parentId: {}", execution.getParentActivityInstanceId(),
				execution.getParentId());

		List<Task> currentTasks = getTasks().stream().filter(t ->
		{
			Object id = t.getUserData(TASK_USERDATA_PARENT_ACTIVITY_INSTANCE_ID);
			return Objects.equals(id, execution.getParentActivityInstanceId())
					|| (id == null && execution.getParentId() == null);
		}).toList();

		if (logger.isTraceEnabled())
			logger.trace("Current tasks: {}",
					currentTasks.stream()
							.map(t -> t.getIdElement().getValue() + " (parent_activity_instance_id = "
									+ t.getUserData(TASK_USERDATA_PARENT_ACTIVITY_INSTANCE_ID) + ")")
							.collect(Collectors.joining(", ", "[", "]")));

		return currentTasks;
	}

	@Override
	public void updateTask(Task task)
	{
		if (task != null)
		{
			List<Task> allTasks = getTasks();
			if (allTasks.contains(task))
				setResourceList(TASKS, allTasks);
			else
				logger.warn("Given task not part of all-tasks list, ignoring task");
		}
		else
			logger.warn("Given task is null");
	}

	@Override
	public void onStart(Task task)
	{
		if (task != null)
		{
			task.setUserData(TASK_USERDATA_PARENT_ACTIVITY_INSTANCE_ID, execution.getParentActivityInstanceId());
			setResourceList(TASKS, Collections.singletonList(task));
		}
		else
			logger.warn("Given task is null");
	}

	@Override
	public void onContinue(Task task)
	{
		if (task != null)
		{
			task.setUserData(TASK_USERDATA_PARENT_ACTIVITY_INSTANCE_ID, execution.getParentActivityInstanceId());

			List<Task> tasks = new ArrayList<>(getTasks());
			tasks.add(task);

			setResourceList(TASKS, tasks);
		}
		else
			logger.warn("Given task is null");
	}

	@Override
	public void onEnd()
	{
		List<Task> tasks = new ArrayList<>(getTasks());
		tasks.removeAll(getCurrentTasks());
		setResourceList(TASKS, tasks);
	}

	@Override
	public QuestionnaireResponse getLatestReceivedQuestionnaireResponse()
	{
		return (QuestionnaireResponse) getResource(QuestionnaireResponseHandler.QUESTIONNAIRE_RESPONSE_VARIABLE);
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
}
