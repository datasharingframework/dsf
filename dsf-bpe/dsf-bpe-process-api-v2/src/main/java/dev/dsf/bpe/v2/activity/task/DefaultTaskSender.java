package dev.dsf.bpe.v2.activity.task;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;

public class DefaultTaskSender implements TaskSender
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskSender.class);

	protected static record TaskAndConfig(Task task, String instantiatesCanonical, String organizationIdentifierValue,
			String endpointIdentifierValue, String endpointUrl, String businessKey, String correlationKey,
			String messageName)
	{
	}

	protected final ProcessPluginApi api;
	protected final Variables variables;
	protected final SendTaskValues sendTaskValues;
	protected final BusinessKeyStrategy businessKeyStrategy;
	protected final Function<Target, List<ParameterComponent>> additionalInputParameters;

	public DefaultTaskSender(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			BusinessKeyStrategy businessKeyStrategy)
	{
		this(api, variables, sendTaskValues, businessKeyStrategy, _ -> List.of());
	}

	public DefaultTaskSender(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			BusinessKeyStrategy businessKeyStrategy,
			Function<Target, List<ParameterComponent>> additionalInputParameters)
	{
		this.api = Objects.requireNonNull(api, "api");
		this.variables = Objects.requireNonNull(variables, "variables");
		this.sendTaskValues = Objects.requireNonNull(sendTaskValues, "sendTaskValues");
		this.businessKeyStrategy = Objects.requireNonNull(businessKeyStrategy, "businessKeyStrategy");
		this.additionalInputParameters = Objects.requireNonNull(additionalInputParameters, "additionalInputParameters");
	}

	@Override
	public void send()
	{
		TaskAndConfig tc = createTaskAndConfig(businessKeyStrategy);

		if (tc.correlationKey() != null)
			logger.info(
					"Sending task {} [recipient: {}, endpoint: {}, businessKey: {}, correlationKey: {}, message: {}] ...",
					tc.instantiatesCanonical(), tc.organizationIdentifierValue(), tc.endpointIdentifierValue(),
					tc.businessKey(), tc.correlationKey(), tc.messageName());
		else
			logger.info("Sending task {} [recipient: {}, endpoint: {}, businessKey: {}, message: {}] ...",
					tc.instantiatesCanonical(), tc.organizationIdentifierValue(), tc.endpointIdentifierValue(),
					tc.businessKey(), tc.messageName());

		IdType created = doSend(tc.task(), tc.endpointUrl());

		logger.info("Task {} sent [task: {}]", tc.instantiatesCanonical(), created.toVersionless().getValue());
	}

	protected IdType doSend(Task task, String targetEndpointUrl)
	{
		return api.getDsfClientProvider().getDsfClient(targetEndpointUrl).withMinimalReturn().create(task);
	}

	protected TaskAndConfig createTaskAndConfig(BusinessKeyStrategy businessKeyStrategy)
	{
		Target target = getTarget();

		String profile = getProfile(target);
		Reference requester = getRequester(target);
		Reference recipient = getRecipient(target);
		String instantiatesCanonical = getInstantiatesCanonical(target);
		String messageName = getMessageName(target);
		String businessKey = businessKeyStrategy.get(variables, target);
		String correlationKey = getCorrelationKey(target);
		List<ParameterComponent> additionalInputParameters = this.additionalInputParameters.apply(target);
		String organizationIdentifierValue = getOrganizationIdentifierValue(target);
		String endpointIdentifierValue = getEndpointIdentifierValue(target);
		String endpointUrl = getEndpointUrl(target);

		Task task = new Task();
		task.setMeta(new Meta().addProfile(profile));
		task.setStatus(TaskStatus.REQUESTED);
		task.setIntent(TaskIntent.ORDER);
		task.setAuthoredOn(new Date());
		task.setRequester(requester);
		task.getRestriction().addRecipient(recipient);
		task.setInstantiatesCanonical(instantiatesCanonical);

		List<ParameterComponent> input = task.getInput();

		input.add(new ParameterComponent(new CodeableConcept(BpmnMessage.messageName()), new StringType(messageName)));
		input.add(new ParameterComponent(new CodeableConcept(BpmnMessage.businessKey()), new StringType(businessKey)));

		if (correlationKey != null)
			input.add(new ParameterComponent(new CodeableConcept(BpmnMessage.correlationKey()),
					new StringType(correlationKey)));

		if (additionalInputParameters != null)
			additionalInputParameters.forEach(input::add);

		return new TaskAndConfig(task, instantiatesCanonical, organizationIdentifierValue, endpointIdentifierValue,
				endpointUrl, businessKey, correlationKey, messageName);
	}

	/**
	 * @return not <code>null</code>
	 */
	protected Target getTarget()
	{
		return variables.getTarget();
	}

	/**
	 * @param target
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	protected String getProfile(Target target)
	{
		return sendTaskValues.profile();
	}

	/**
	 * @param target
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	protected Reference getRequester(Target target)
	{
		return new Reference().setType(ResourceType.Organization.name())
				.setIdentifier(api.getOrganizationProvider().getLocalOrganizationIdentifier()
						.orElseThrow(() -> new IllegalStateException("Local organization identifier unknown")));
	}

	/**
	 * @param target
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	protected Reference getRecipient(Target target)
	{
		return new Reference().setType(ResourceType.Organization.name())
				.setIdentifier(OrganizationIdentifier.withValue(target.getOrganizationIdentifierValue()));
	}

	/**
	 * @param target
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	protected String getInstantiatesCanonical(Target target)
	{
		return sendTaskValues.instantiatesCanonical();
	}

	/**
	 * @param target
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	protected String getMessageName(Target target)
	{
		return sendTaskValues.messageName();
	}

	/**
	 * @param target
	 *            not <code>null</code>
	 * @return may be <code>null</code>
	 */
	protected String getCorrelationKey(Target target)
	{
		return target.getCorrelationKey();
	}

	/**
	 * @param target
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	protected String getOrganizationIdentifierValue(Target target)
	{
		return target.getOrganizationIdentifierValue();
	}

	/**
	 * @param target
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	protected String getEndpointIdentifierValue(Target target)
	{
		return target.getEndpointIdentifierValue();
	}

	/**
	 * @param target
	 *            not <code>null</code>
	 * @return not <code>null</code>
	 */
	protected String getEndpointUrl(Target target)
	{
		return target.getEndpointUrl();
	}
}
