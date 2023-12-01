package dev.dsf.bpe.subscription;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.v1.constants.BpmnExecutionVariables;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.variables.FhirResourceValues;
import dev.dsf.fhir.client.FhirWebserviceClient;

public class TaskHandler implements ResourceHandler<Task>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(TaskHandler.class);

	private static final class ProcessNotFoundException extends ProcessEngineException
	{
		private static final long serialVersionUID = 1L;

		private final String startMessageName;

		ProcessNotFoundException(String processDomain, String processDefinitionKey, String processVersion,
				String startMessageName)
		{
			super(toMessage(processDomain, processDefinitionKey, processVersion, startMessageName));
			this.startMessageName = startMessageName;
		}

		private static String toMessage(String processDomain, String processDefinitionKey, String processVersion,
				String startMessageName)
		{
			Objects.requireNonNull(processDomain, "processDomain");
			Objects.requireNonNull(processDefinitionKey, "processDefinitionKey");

			if (processVersion != null && !processVersion.isBlank())
			{
				if (startMessageName != null && !startMessageName.isBlank())
					return "Process with id '" + processDomain + "_" + processDefinitionKey + "', version '"
							+ processVersion + "' and start message-name '" + startMessageName + "' not found";
				else
					return "Process with id: '" + processDomain + "_" + processDefinitionKey + "' and version '"
							+ processVersion + "' not found";
			}
			else
			{
				if (startMessageName != null && !startMessageName.isBlank())
					return "Process with id: '" + processDomain + "_" + processDefinitionKey
							+ "' and start message-name: '" + startMessageName + "' not found";
				else
					return "Process with id: '" + processDomain + "_" + processDefinitionKey + "' not found";
			}
		}

		String getShortMessage()
		{
			if (startMessageName != null && !startMessageName.isBlank())
				return "Process with start message-name '" + startMessageName + "' not found";
			else
				return "Process not found";
		}
	}

	public static final String TASK_VARIABLE = TaskHandler.class.getName() + ".task";

	private static final String INSTANTIATES_CANONICAL_PATTERN_STRING = "(?<processUrl>http://(?<processDomain>(?:(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*(?:[A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9]))/bpe/Process/(?<processDefinitionKey>[-\\w]+))\\|(?<processVersion>\\d+\\.\\d+)";
	private static final Pattern INSTANTIATES_CANONICAL_PATTERN = Pattern
			.compile(INSTANTIATES_CANONICAL_PATTERN_STRING);

	private final RuntimeService runtimeService;
	private final RepositoryService repositoryService;
	private final FhirWebserviceClient webserviceClient;

	public TaskHandler(RuntimeService runtimeService, RepositoryService repositoryService,
			FhirWebserviceClient webserviceClient)
	{
		this.runtimeService = runtimeService;
		this.repositoryService = repositoryService;
		this.webserviceClient = webserviceClient;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(runtimeService, "runtimeService");
		Objects.requireNonNull(repositoryService, "repositoryService");
		Objects.requireNonNull(webserviceClient, "webserviceClient");
	}

	public void onResource(Task task)
	{
		Objects.requireNonNull(task, "task");
		Objects.requireNonNull(task.getInstantiatesCanonical(), "task.instantiatesCanonical");

		Matcher matcher = INSTANTIATES_CANONICAL_PATTERN.matcher(task.getInstantiatesCanonical());
		if (!matcher.matches())
			throw new IllegalStateException("InstantiatesCanonical of Task with id " + task.getIdElement().getIdPart()
					+ " does not match " + INSTANTIATES_CANONICAL_PATTERN_STRING);

		String processDomain = matcher.group("processDomain").replace(".", "");
		String processDefinitionKey = matcher.group("processDefinitionKey");
		String processVersion = matcher.group("processVersion");

		String messageName = getFirstInputParameter(task, BpmnMessage.messageName());
		String businessKey = getFirstInputParameter(task, BpmnMessage.businessKey());
		String correlationKey = getFirstInputParameter(task, BpmnMessage.correlationKey());

		if (businessKey == null)
		{
			businessKey = UUID.randomUUID().toString();
			logger.debug("Adding business-key {} to Task with id {}", businessKey, task.getId());
			task.addInput().setType(new CodeableConcept(BpmnMessage.businessKey()))
					.setValue(new StringType(businessKey));
		}
		task.setStatus(Task.TaskStatus.INPROGRESS);
		task = webserviceClient.update(task);

		Map<String, Object> variables = Map.of(TASK_VARIABLE, FhirResourceValues.create(task));

		try
		{
			onMessage(businessKey, correlationKey, processDomain, processDefinitionKey, processVersion, messageName,
					variables);
		}
		catch (MismatchingMessageCorrelationException e)
		{
			logger.warn("Unable to handle Task with id {}: {} - {}", task.getId(), e.getClass().getName(),
					e.getMessage());
			logger.debug("Unable to handle Task with id {}", task.getId(), e);
			updateTaskFailed(task, "Unable to correlate Task");
		}
		catch (ProcessNotFoundException e)
		{
			logger.warn("Unable to handle Task with id {}: {} - {}", task.getId(), e.getClass().getName(),
					e.getMessage());
			logger.debug("Unable to handle Task with id {}", task.getId(), e);
			updateTaskFailed(task, e.getShortMessage());
		}
		catch (Exception e)
		{
			logger.error("Unable to handle Task with id {}: {} - {}", task.getId(), e.getClass().getName(),
					e.getMessage());
			logger.debug("Unable to handle Task with id {}", task.getId(), e);
			updateTaskFailed(task, e);
		}
	}

	private void updateTaskFailed(Task task, Exception e)
	{
		updateTaskFailed(task, e.getClass().getName() + ": " + e.getMessage());
	}

	private void updateTaskFailed(Task task, String message)
	{
		task.addOutput().setType(new CodeableConcept(BpmnMessage.error())).setValue(new StringType(message));
		task.setStatus(Task.TaskStatus.FAILED);

		try
		{
			webserviceClient.update(task);
		}
		catch (Exception ex)
		{
			logger.error("Unable to update Task with id {} (status failed)", task.getId());
		}
	}

	private String getFirstInputParameter(Task task, Coding code)
	{
		if (task == null || code == null)
			return null;

		return task.getInput().stream().filter(ParameterComponent::hasType)
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> co != null && Objects.equals(code.getSystem(), co.getSystem())
								&& Objects.equals(code.getCode(), co.getCode())))
				.filter(ParameterComponent::hasValue).map(ParameterComponent::getValue)
				.filter(v -> v instanceof StringType).map(v -> (StringType) v).map(StringType::getValue).findFirst()
				.orElse(null);
	}

	/**
	 * @param businessKey
	 *            may be <code>null</code>
	 * @param correlationKey
	 *            may be <code>null</code>
	 * @param processDomain
	 *            not <code>null</code>
	 * @param processDefinitionKey
	 *            not <code>null</code>
	 * @param processVersion
	 *            not <code>null</code>
	 * @param messageName
	 *            not <code>null</code>
	 * @param variables
	 *            may be <code>null</code>
	 */
	protected void onMessage(String businessKey, String correlationKey, String processDomain,
			String processDefinitionKey, String processVersion, String messageName, Map<String, Object> variables)
	{
		// businessKey may be null
		// correlationKey may be null
		Objects.requireNonNull(processDomain, "processDomain");
		Objects.requireNonNull(processDefinitionKey, "processDefinitionKey");
		Objects.requireNonNull(processVersion, "processVersion");
		Objects.requireNonNull(messageName, "messageName");

		if (variables == null)
			variables = Collections.emptyMap();

		ProcessDefinition processDefinition = getProcessDefinition(processDomain, processDefinitionKey, processVersion);

		if (processDefinition == null)
			throw new ProcessNotFoundException(processDomain, processDefinitionKey, processVersion, null);

		if (businessKey == null)
		{
			runtimeService.startProcessInstanceByMessageAndProcessDefinitionId(messageName, processDefinition.getId(),
					UUID.randomUUID().toString(), variables);
		}
		else
		{
			List<ProcessInstance> instances = getProcessInstanceQuery(processDefinition, businessKey).list();
			List<ProcessInstance> instancesWithAlternativeBusinessKey = getAlternativeProcessInstanceQuery(
					processDefinition, businessKey).list();

			if (instances.size() + instancesWithAlternativeBusinessKey.size() > 1)
				logger.warn("instance-ids {}",
						Stream.concat(instances.stream(), instancesWithAlternativeBusinessKey.stream())
								.map(ProcessInstance::getId).collect(Collectors.joining(", ", "[", "]")));

			if (instances.size() + instancesWithAlternativeBusinessKey.size() <= 0)
			{
				BpmnModelInstance model = repositoryService.getBpmnModelInstance(processDefinition.getId());
				Collection<StartEvent> startEvents = model == null ? Collections.emptySet()
						: model.getModelElementsByType(StartEvent.class);
				Stream<String> startEventMesssageNames = startEvents.stream().flatMap(e ->
				{
					Collection<MessageEventDefinition> m = e.getChildElementsByType(MessageEventDefinition.class);
					return m == null ? Stream.empty() : m.stream();
				}).map(d -> d.getMessage().getName());

				if (startEventMesssageNames.anyMatch(m -> m.equals(messageName)))
				{
					runtimeService.createMessageCorrelation(messageName).processDefinitionId(processDefinition.getId())
							.processInstanceBusinessKey(businessKey).setVariables(variables).correlateStartMessage();
				}
				else
					throw new ProcessNotFoundException(processDomain, processDefinitionKey, processVersion,
							messageName);
			}
			else
			{
				MessageCorrelationBuilder correlation;

				if (instances.size() > 0)
					correlation = runtimeService.createMessageCorrelation(messageName).setVariables(variables)
							.processInstanceBusinessKey(businessKey);
				else
					correlation = runtimeService.createMessageCorrelation(messageName).setVariables(variables)
							.processInstanceVariableEquals(BpmnExecutionVariables.ALTERNATIVE_BUSINESS_KEY,
									businessKey);

				if (correlationKey != null)
					correlation = correlation.localVariableEquals(BpmnExecutionVariables.CORRELATION_KEY,
							correlationKey);

				// throws MismatchingMessageCorrelationException - if none or more than one execution or process
				// definition is matched by the correlation
				correlation.correlate();
			}
		}
	}

	private ProcessDefinition getProcessDefinition(String processDomain, String processDefinitionKey,
			String processVersion)
	{
		if (processVersion != null && !processVersion.isBlank())
			return repositoryService.createProcessDefinitionQuery().active()
					.processDefinitionKey(processDomain + "_" + processDefinitionKey).versionTag(processVersion).list()
					.stream().sorted(Comparator.comparing(ProcessDefinition::getVersion).reversed()).findFirst()
					.orElse(null);
		else
			return repositoryService.createProcessDefinitionQuery().active()
					.processDefinitionKey(processDomain + "_" + processDefinitionKey).latestVersion().singleResult();
	}

	private ProcessInstanceQuery getProcessInstanceQuery(ProcessDefinition processDefinition, String businessKey)
	{
		return runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId())
				.processInstanceBusinessKey(businessKey);
	}

	private ProcessInstanceQuery getAlternativeProcessInstanceQuery(ProcessDefinition processDefinition,
			String businessKey)
	{
		return runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId())
				.variableValueEquals(BpmnExecutionVariables.ALTERNATIVE_BUSINESS_KEY, businessKey);
	}
}
