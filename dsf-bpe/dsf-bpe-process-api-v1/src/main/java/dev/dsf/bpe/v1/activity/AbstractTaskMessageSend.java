package dev.dsf.bpe.v1.activity;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.ProcessPluginDefinition;
import dev.dsf.bpe.v1.constants.BpmnExecutionVariables;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v1.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Targets;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.client.FhirWebserviceClient;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.StatusType;

/**
 * Base class for implementing BPMN message send tasks, intermediate message throw events and message end events using
 * FHIR Task resources. Requires three String fields to be injected via BPMN:
 * <ul>
 * <li><b>instantiatesCanonical</b> with the URL (including version) of the Activity to start or continue.
 * <li><b>messageName</b> with the with the BPMN message-name of the start event, intermediate message catch event or
 * message receive task.
 * <li><b>profile</b> with the URL (including version) of the profile (StructureDefinition) that the Task resource used
 * should conform to.
 * </ul>
 * <p>
 * Configure BPMN message send tasks, intermediate message throw events and message end event with an implementation of
 * type 'Java class' with the fully qualified class name of the class extending this abstract implementation.
 * <p>
 * Configure your service task implementation as a {@link Bean} in your spring {@link Configuration} class with scope
 * <code>"prototype"</code>.
 *
 * @see ProcessPluginDefinition#getSpringConfigurations()
 */
public abstract class AbstractTaskMessageSend implements JavaDelegate, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractTaskMessageSend.class);

	protected final ProcessPluginApi api;

	// set via field injection
	private FixedValue instantiatesCanonical;
	private FixedValue messageName;
	private FixedValue profile;

	/**
	 * @param api
	 *            not <code>null</code>
	 */
	public AbstractTaskMessageSend(ProcessPluginApi api)
	{
		this.api = api;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(api, "api");
	}

	/**
	 * @param instantiatesCanonical
	 *            not <code>null</code>
	 * @deprecated only for process engine field injection
	 */
	@Deprecated
	public final void setInstantiatesCanonical(FixedValue instantiatesCanonical)
	{
		this.instantiatesCanonical = instantiatesCanonical;
	}

	/**
	 * Retrieves the instantiatesCanonical value used for Task resources send by this class via the injected field
	 * <b>instantiatesCanonical</b>.
	 * <p>
	 * <i>Override this method to use a different mechanism for retrieving the value for instantiatesCanonical. For
	 * example via a process variable. Note: A non empty value e.g 'disable' still needs to be injected in the BPMN file
	 * in order to comply with the validation performed during plugin loading.</i>
	 *
	 * @param execution
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @return instantiatesCanonical value used for Task resources send by this class
	 */
	protected String getInstantiatesCanonical(DelegateExecution execution, Variables variables)
	{
		return instantiatesCanonical == null ? null : instantiatesCanonical.getExpressionText();
	}

	/**
	 * @param messageName
	 *            not <code>null</code>
	 * @deprecated only for process engine field injection
	 */
	@Deprecated
	public final void setMessageName(FixedValue messageName)
	{
		this.messageName = messageName;
	}

	/**
	 * Retrieves the messageName value used for Task resources send by this class via the injected field
	 * <b>messageName</b>.
	 * <p>
	 * <i>Override this method to use a different mechanism for retrieving the value for messageName. For example via a
	 * process variable. Note: A non empty value e.g 'disable' still needs to be injected in the BPMN file in order to
	 * comply with the validation performed during plugin loading.</i>
	 *
	 * @param execution
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @return messageName value used for Task resources send by this class
	 */
	protected String getMessageName(DelegateExecution execution, Variables variables)
	{
		return messageName == null ? null : messageName.getExpressionText();
	}

	/**
	 * @param profile
	 *            not <code>null</code>
	 * @deprecated only for process engine field injection
	 */
	@Deprecated
	public final void setProfile(FixedValue profile)
	{
		this.profile = profile;
	}

	/**
	 * Retrieves the profile value used for Task resources send by this class via the injected field <b>profile</b>.
	 * <p>
	 * <i>Override this method to use a different mechanism for retrieving the value for profile. For example via a
	 * process variable. Note: A non empty value e.g 'disable' still needs to be injected in the BPMN file in order to
	 * comply with the validation performed during plugin loading.</i>
	 *
	 * @param execution
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @return profile value used for Task resources send by this class
	 */
	protected String getProfile(DelegateExecution execution, Variables variables)
	{
		return profile == null ? null : profile.getExpressionText();
	}

	@Override
	public final void execute(DelegateExecution execution) throws Exception
	{
		doExecute(execution, api.getVariables(execution));
	}

	protected void doExecute(DelegateExecution execution, Variables variables) throws Exception
	{
		final String instantiatesCanonical = getInstantiatesCanonical(execution, variables);
		final String messageName = getMessageName(execution, variables);
		final String profile = getProfile(execution, variables);
		final String businessKey = execution.getBusinessKey();
		final Target target = variables.getTarget();

		try
		{
			sendTask(execution, variables, target, instantiatesCanonical, messageName, businessKey, profile,
					getAdditionalInputParameters(execution, variables));
		}
		catch (Exception e)
		{
			String exceptionMessage = e.getMessage();
			if (e instanceof WebApplicationException && (e.getMessage() == null || e.getMessage().isBlank()))
			{
				StatusType statusInfo = ((WebApplicationException) e).getResponse().getStatusInfo();
				exceptionMessage = statusInfo.getStatusCode() + " " + statusInfo.getReasonPhrase();
			}

			String errorMessage = "Task " + instantiatesCanonical + " send failed [recipient: "
					+ target.getOrganizationIdentifierValue() + ", endpoint: " + target.getEndpointIdentifierValue()
					+ ", businessKey: " + businessKey
					+ (target.getCorrelationKey() == null ? "" : ", correlationKey: " + target.getCorrelationKey())
					+ ", message: " + messageName + ", error: " + e.getClass().getName() + " - " + exceptionMessage
					+ "]";
			logger.warn(errorMessage);
			logger.debug("Error while sending Task", e);

			if (execution.getBpmnModelElementInstance() instanceof IntermediateThrowEvent)
				handleIntermediateThrowEventError(execution, variables, e, errorMessage);
			else if (execution.getBpmnModelElementInstance() instanceof EndEvent)
				handleEndEventError(execution, variables, e, errorMessage);
			else if (execution.getBpmnModelElementInstance() instanceof SendTask)
				handleSendTaskError(execution, variables, e, errorMessage);
			else
				logger.warn("Error handling for {} not implemented",
						execution.getBpmnModelElementInstance().getClass().getName());
		}
	}

	protected void handleIntermediateThrowEventError(DelegateExecution execution, Variables variables,
			Exception exception, String errorMessage)
	{
		logger.debug("Error while executing Task message send " + getClass().getName(), exception);
		logger.error("Process {} has fatal error in step {} for task {}, reason: {} - {}",
				execution.getProcessDefinitionId(), execution.getActivityInstanceId(),
				api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()),
				exception.getClass().getName(), exception.getMessage());

		updateFailedIfInprogress(variables.getTasks(), errorMessage);

		execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(),
				exception.getMessage());
	}

	protected void handleEndEventError(DelegateExecution execution, Variables variables, Exception exception,
			String errorMessage)
	{
		logger.debug("Error while executing Task message send " + getClass().getName(), exception);
		logger.error("Process {} has fatal error in step {} for task {}, reason: {} - {}",
				execution.getProcessDefinitionId(), execution.getActivityInstanceId(),
				api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()),
				exception.getClass().getName(), exception.getMessage());

		updateFailedIfInprogress(variables.getTasks(), errorMessage);

		// End event: No need to delete process instance
	}

	protected void handleSendTaskError(DelegateExecution execution, Variables variables, Exception exception,
			String errorMessage)
	{
		Targets targets = variables.getTargets();

		// if we are a multi instance message send task, remove target
		if (targets != null && !targets.isEmpty())
		{
			Target target = variables.getTarget();
			targets = targets.removeByEndpointIdentifierValue(target);
			variables.setTargets(targets);

			addErrorIfInprogress(variables.getTasks(), errorMessage);

			logger.debug("Target organization {}, endpoint {} with error {} removed from target list",
					target.getOrganizationIdentifierValue(), target.getEndpointIdentifierValue(),
					exception.getMessage());
		}

		// if we are not a multi instance message send task or all sends have failed (targets emtpy)
		else
		{
			logger.debug("Error while executing Task message send " + getClass().getName(), exception);
			logger.error("Process {} has fatal error in step {} for task {}, last reason: {} - ",
					execution.getProcessDefinitionId(), execution.getActivityInstanceId(),
					api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()),
					exception.getClass().getName(), exception.getMessage());

			updateFailedIfInprogress(variables.getTasks(), errorMessage);

			execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(),
					exception.getMessage());
		}
	}

	private void addErrorIfInprogress(List<Task> tasks, String errorMessage)
	{
		for (int i = tasks.size() - 1; i >= 0; i--)
		{
			Task task = tasks.get(i);

			if (TaskStatus.INPROGRESS.equals(task.getStatus()))
			{
				addErrorMessage(task, errorMessage);
			}
			else
			{
				logger.debug("Not adding error to Task {} with status: {}",
						api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task), task.getStatus());
			}
		}
	}

	private void updateFailedIfInprogress(List<Task> tasks, String errorMessage)
	{
		for (int i = tasks.size() - 1; i >= 0; i--)
		{
			Task task = tasks.get(i);

			if (TaskStatus.INPROGRESS.equals(task.getStatus()))
			{
				task.setStatus(Task.TaskStatus.FAILED);
				addErrorMessage(task, errorMessage);
				updateAndHandleException(task);
			}
			else
			{
				logger.debug("Not updating Task {} with status: {}",
						api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task), task.getStatus());
			}
		}
	}

	protected void addErrorMessage(Task task, String errorMessage)
	{
		task.addOutput(new TaskOutputComponent(new CodeableConcept(BpmnMessage.error()), new StringType(errorMessage)));
	}

	private void updateAndHandleException(Task task)
	{
		try
		{
			logger.debug("Updating Task {}, new status: {}", api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task),
					task.getStatus().toCode());

			api.getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn().update(task);
		}
		catch (Exception e)
		{
			logger.error("Unable to update Task " + api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task), e);
		}
	}

	/**
	 * <i>Override this method to add additional input parameters to the task resource being send.</i>
	 *
	 * @param execution
	 *            the delegate execution of this process instance
	 * @return {@link Stream} of {@link ParameterComponent}s to be added as input parameters
	 */
	protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution, Variables variables)
	{
		return Stream.empty();
	}

	/**
	 * Generates an alternative business-key and stores it as a process variable with name
	 * {@link BpmnExecutionVariables#ALTERNATIVE_BUSINESS_KEY}
	 * <p>
	 * <i>Use this method in combination with overriding
	 * {@link #sendTask(DelegateExecution, Variables, Target, String, String, String, String, Stream)} to use an
	 * alternative business-key with the communication target.</i>
	 *
	 * <pre>
	 * &#64;Override
	 * protected void sendTasksendTask(DelegateExecution execution, Variables variables, Target target,
	 * 		String instantiatesCanonical, String messageName, String businessKey, String profile,
	 * 		Stream&lt;ParameterComponent&gt; additionalInputParameters)
	 * {
	 * 	String alternativeBusinesKey = createAndSaveAlternativeBusinessKey();
	 * 	super.sendTask(execution, target, instantiatesUri, messageName, alternativeBusinesKey, profile,
	 * 			additionalInputParameters);
	 * }
	 * </pre>
	 *
	 * <i>Return tasks from the target using the alternative business-key will correlate with this process instance.</i>
	 * <p>
	 *
	 *
	 * @param execution
	 *            not <code>null</code>
	 * @return the alternative business-key stored as variable {@link BpmnExecutionVariables#ALTERNATIVE_BUSINESS_KEY}
	 * @see Variables#setAlternativeBusinessKey(String)
	 */
	protected final String createAndSaveAlternativeBusinessKey(DelegateExecution execution, Variables variables)
	{
		String alternativeBusinessKey = UUID.randomUUID().toString();
		variables.setAlternativeBusinessKey(alternativeBusinessKey);
		return alternativeBusinessKey;
	}

	/**
	 * @param execution
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @param target
	 *            not <code>null</code>
	 * @param instantiatesCanonical
	 *            not <code>null</code>, not empty
	 * @param messageName
	 *            not <code>null</code>, not empty
	 * @param businessKey
	 *            not <code>null</code>, not empty
	 * @param profile
	 *            not <code>null</code>, not empty
	 * @param additionalInputParameters
	 *            may be <code>null</code>
	 */
	protected void sendTask(DelegateExecution execution, Variables variables, Target target,
			String instantiatesCanonical, String messageName, String businessKey, String profile,
			Stream<ParameterComponent> additionalInputParameters)
	{
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(instantiatesCanonical, "instantiatesCanonical");
		if (instantiatesCanonical.isEmpty())
			throw new IllegalArgumentException("instantiatesCanonical empty");
		Objects.requireNonNull(messageName, "messageName");
		if (messageName.isEmpty())
			throw new IllegalArgumentException("messageName empty");
		Objects.requireNonNull(businessKey, "businessKey");
		if (businessKey.isEmpty())
			throw new IllegalArgumentException("profile empty");
		Objects.requireNonNull(profile, "profile");
		if (profile.isEmpty())
			throw new IllegalArgumentException("profile empty");

		Task task = new Task();
		task.setMeta(new Meta().addProfile(profile));
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.setRequester(getRequester());
		task.getRestriction().addRecipient(getRecipient(target));
		task.setInstantiatesCanonical(instantiatesCanonical);

		ParameterComponent messageNameInput = new ParameterComponent(new CodeableConcept(BpmnMessage.messageName()),
				new StringType(messageName));
		task.getInput().add(messageNameInput);

		ParameterComponent businessKeyInput = new ParameterComponent(new CodeableConcept(BpmnMessage.businessKey()),
				new StringType(businessKey));
		task.getInput().add(businessKeyInput);

		String correlationKey = target.getCorrelationKey();
		if (correlationKey != null)
		{
			ParameterComponent correlationKeyInput = new ParameterComponent(
					new CodeableConcept(BpmnMessage.correlationKey()), new StringType(correlationKey));
			task.getInput().add(correlationKeyInput);
		}

		if (additionalInputParameters != null)
			additionalInputParameters.forEach(task.getInput()::add);

		FhirWebserviceClient client = api.getFhirWebserviceClientProvider()
				.getWebserviceClient(target.getEndpointUrl());

		if (correlationKey != null)
			logger.info(
					"Sending task {} [recipient: {}, endpoint: {}, businessKey: {}, correlationKey: {}, message: {}] ...",
					task.getInstantiatesCanonical(), target.getOrganizationIdentifierValue(),
					target.getEndpointIdentifierValue(), businessKey, correlationKey, messageName);
		else
			logger.info("Sending task {} [recipient: {}, endpoint: {}, businessKey: {}, message: {}] ...",
					task.getInstantiatesCanonical(), target.getOrganizationIdentifierValue(),
					target.getEndpointIdentifierValue(), businessKey, messageName);

		logger.trace("Task resource to send: {}", api.getFhirContext().newJsonParser().encodeResourceToString(task));

		IdType created = doSend(client, task);

		logger.info("Task {} send [task: {}]", task.getInstantiatesCanonical(), created.toVersionless().getValue());
	}

	/**
	 * <i>Override this method to modify the remote task create behavior, e.g. to implement retries</i>
	 *
	 * <pre>
	 * <code>
	 * &#64;Override
	 * protected void doSend(FhirWebserviceClient client, Task task)
	 * {
	 *     client.withMinimalReturn().withRetry(2).create(task);
	 * }
	 * </code>
	 * </pre>
	 *
	 * @param client
	 *            not <code>null</code>
	 * @param task
	 *            not <code>null</code>
	 * @return id of created task
	 */
	protected IdType doSend(FhirWebserviceClient client, Task task)
	{
		return client.withMinimalReturn().create(task);
	}

	protected Reference getRecipient(Target target)
	{
		return new Reference().setType(ResourceType.Organization.name())
				.setIdentifier(OrganizationIdentifier.withValue(target.getOrganizationIdentifierValue()));
	}

	protected Reference getRequester()
	{
		return new Reference().setType(ResourceType.Organization.name())
				.setIdentifier(api.getOrganizationProvider().getLocalOrganizationIdentifier()
						.orElseThrow(() -> new IllegalStateException("Local organization identifier unknown")));
	}
}
