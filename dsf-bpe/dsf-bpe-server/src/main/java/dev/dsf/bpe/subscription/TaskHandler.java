package dev.dsf.bpe.subscription;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.api.Constants;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.client.FhirWebserviceClient;
import dev.dsf.bpe.plugin.ProcessPluginManager;

public class TaskHandler extends AbstractResourceHandler implements ResourceHandler<Task>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(TaskHandler.class);

	private static final String INSTANTIATES_CANONICAL_PATTERN_STRING = "(?<processUrl>http[s]{0,1}://(?<domain>(?:(?:[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])\\.)+(?:[a-zA-Z0-9]{1,63}))"
			+ "/bpe/Process/(?<processName>[a-zA-Z0-9-]+))\\|(?<processVersion>\\d+\\.\\d+)$";
	private static final Pattern INSTANTIATES_CANONICAL_PATTERN = Pattern
			.compile(INSTANTIATES_CANONICAL_PATTERN_STRING);

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

	private final RuntimeService runtimeService;
	private final FhirWebserviceClient webserviceClient;

	public TaskHandler(RepositoryService repositoryService, ProcessPluginManager processPluginManager,
			FhirContext fhirContext, RuntimeService runtimeService, FhirWebserviceClient webserviceClient)
	{
		super(repositoryService, processPluginManager, fhirContext);

		this.runtimeService = runtimeService;
		this.webserviceClient = webserviceClient;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(runtimeService, "runtimeService");
		Objects.requireNonNull(webserviceClient, "webserviceClient");
	}

	@Override
	public void onResource(Task task)
	{
		Objects.requireNonNull(task, "task");
		Objects.requireNonNull(task.getInstantiatesCanonical(), "task.instantiatesCanonical");

		if (!TaskStatus.REQUESTED.equals(task.getStatus()))
			throw new IllegalArgumentException("Task.status != " + TaskStatus.REQUESTED.toCode());

		Matcher matcher = INSTANTIATES_CANONICAL_PATTERN.matcher(task.getInstantiatesCanonical());
		if (!matcher.matches())
			throw new IllegalStateException("InstantiatesCanonical of Task with id " + task.getIdElement().getIdPart()
					+ " does not match " + INSTANTIATES_CANONICAL_PATTERN_STRING);

		String processDomain = matcher.group("domain").replace(".", "");
		String processDefinitionKey = matcher.group("processName");
		String processVersion = matcher.group("processVersion");

		ProcessDefinition processDefinition = getProcessDefinition(processDomain, processDefinitionKey, processVersion);

		if (processDefinition == null)
			throw new ProcessNotFoundException(processDomain, processDefinitionKey, processVersion, null);

		Optional<ProcessPlugin> processPlugin = getProcessPlugin(processDefinition);

		if (processPlugin.isEmpty())
			throw new ProcessNotFoundException(processDomain, processDefinitionKey, processVersion, null);

		String messageName = getFirstBpmnMessageInputParameter(task, Constants.BPMN_MESSAGE_MESSAGE_NAME);
		String businessKey = getFirstBpmnMessageInputParameter(task, Constants.BPMN_MESSAGE_BUSINESS_KEY);
		String correlationKey = getFirstBpmnMessageInputParameter(task, Constants.BPMN_MESSAGE_CORRELATION_KEY);

		if (businessKey == null)
		{
			businessKey = UUID.randomUUID().toString();
			logger.debug("Adding business-key {} to Task with id {}", businessKey, task.getId());
			task.addInput().setType(new CodeableConcept().addCoding(
					new Coding().setSystem(Constants.BPMN_MESSAGE_URL).setCode(Constants.BPMN_MESSAGE_BUSINESS_KEY)))
					.setValue(new StringType(businessKey));
		}
		task.setStatus(Task.TaskStatus.INPROGRESS);
		task = webserviceClient.update(task);

		PrimitiveValue<?> fhirTaskVariable = processPlugin.get()
				.createFhirTaskVariable(newJsonParser().encodeResourceToString(task));
		Map<String, Object> variables = Map.of(Constants.TASK_VARIABLE, fhirTaskVariable);

		try
		{
			onMessage(businessKey, correlationKey, processDomain, processDefinitionKey, processVersion, messageName,
					processDefinition.getId(), variables);
		}
		catch (MismatchingMessageCorrelationException e)
		{
			logger.debug("Unable to handle Task with id {}", task.getId(), e);
			logger.warn("Unable to handle Task with id {}: {} - {}", task.getId(), e.getClass().getName(),
					e.getMessage());

			updateTaskFailed(task, "Unable to correlate Task");
		}
		catch (ProcessNotFoundException e)
		{
			logger.debug("Unable to handle Task with id {}", task.getId(), e);
			logger.warn("Unable to handle Task with id {}: {} - {}", task.getId(), e.getClass().getName(),
					e.getMessage());

			updateTaskFailed(task, e.getShortMessage());
		}
		catch (Exception e)
		{
			logger.debug("Unable to handle Task with id {}", task.getId(), e);
			logger.error("Unable to handle Task with id {}: {} - {}", task.getId(), e.getClass().getName(),
					e.getMessage());

			updateTaskFailed(task, e);
		}
	}

	private void updateTaskFailed(Task task, Exception e)
	{
		updateTaskFailed(task, e.getClass().getName() + ": " + e.getMessage());
	}

	private void updateTaskFailed(Task task, String message)
	{
		task.addOutput()
				.setType(new CodeableConcept().addCoding(
						new Coding().setSystem(Constants.BPMN_MESSAGE_URL).setCode(Constants.BPMN_MESSAGE_ERROR)))
				.setValue(new StringType(message));
		task.setStatus(Task.TaskStatus.FAILED);

		try
		{
			webserviceClient.update(task);
		}
		catch (Exception e)
		{
			logger.debug("Unable to update Task with id {} (status failed)", task.getId(), e);
			logger.error("Unable to update Task with id {} (status failed): {} - {}", task.getId(),
					e.getClass().getName(), e.getMessage());
		}
	}

	private String getFirstBpmnMessageInputParameter(Task task, String code)
	{
		if (task == null || code == null)
			return null;

		return task.getInput().stream().filter(ParameterComponent::hasType)
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> co != null && Objects.equals(Constants.BPMN_MESSAGE_URL, co.getSystem())
								&& Objects.equals(code, co.getCode())))
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
	 * @param processDefinitionId
	 *            not <code>null</code>
	 * @param variables
	 *            may be <code>null</code>
	 */
	protected void onMessage(String businessKey, String correlationKey, String processDomain,
			String processDefinitionKey, String processVersion, String messageName, String processDefinitionId,
			Map<String, Object> variables)
	{
		// businessKey may be null
		// correlationKey may be null
		Objects.requireNonNull(processDomain, "processDomain");
		Objects.requireNonNull(processDefinitionKey, "processDefinitionKey");
		Objects.requireNonNull(processVersion, "processVersion");
		Objects.requireNonNull(messageName, "messageName");
		Objects.requireNonNull(processDefinitionId, "processDefinitionId");

		if (variables == null)
			variables = Collections.emptyMap();

		if (businessKey == null)
		{
			runtimeService.startProcessInstanceByMessageAndProcessDefinitionId(messageName, processDefinitionId,
					UUID.randomUUID().toString(), variables);
		}
		else
		{
			List<ProcessInstance> instances = getProcessInstanceQuery(processDefinitionId, businessKey).list();
			List<ProcessInstance> instancesWithAlternativeBusinessKey = getAlternativeProcessInstanceQuery(
					processDefinitionId, businessKey).list();

			if (instances.size() + instancesWithAlternativeBusinessKey.size() > 1)
				logger.warn("instance-ids {}",
						Stream.concat(instances.stream(), instancesWithAlternativeBusinessKey.stream())
								.map(ProcessInstance::getId).collect(Collectors.joining(", ", "[", "]")));

			if (instances.size() + instancesWithAlternativeBusinessKey.size() <= 0)
			{
				BpmnModelInstance model = repositoryService.getBpmnModelInstance(processDefinitionId);
				Collection<StartEvent> startEvents = model == null ? Collections.emptySet()
						: model.getModelElementsByType(StartEvent.class);
				Stream<String> startEventMesssageNames = startEvents.stream().flatMap(e ->
				{
					Collection<MessageEventDefinition> m = e.getChildElementsByType(MessageEventDefinition.class);
					return m == null ? Stream.empty() : m.stream();
				}).map(d -> d.getMessage().getName());

				if (startEventMesssageNames.anyMatch(m -> m.equals(messageName)))
				{
					runtimeService.createMessageCorrelation(messageName).processDefinitionId(processDefinitionId)
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
							.processInstanceVariableEquals(Constants.ALTERNATIVE_BUSINESS_KEY, businessKey);

				if (correlationKey != null)
					correlation = correlation.localVariableEquals(Constants.CORRELATION_KEY, correlationKey);

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
					.stream().max(Comparator.comparing(ProcessDefinition::getVersion)).orElse(null);
		else
			return repositoryService.createProcessDefinitionQuery().active()
					.processDefinitionKey(processDomain + "_" + processDefinitionKey).latestVersion().singleResult();
	}

	private ProcessInstanceQuery getProcessInstanceQuery(String processDefinitionId, String businessKey)
	{
		return runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinitionId)
				.processInstanceBusinessKey(businessKey);
	}

	private ProcessInstanceQuery getAlternativeProcessInstanceQuery(String processDefinitionId, String businessKey)
	{
		return runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinitionId)
				.variableValueEquals(Constants.ALTERNATIVE_BUSINESS_KEY, businessKey);
	}
}
