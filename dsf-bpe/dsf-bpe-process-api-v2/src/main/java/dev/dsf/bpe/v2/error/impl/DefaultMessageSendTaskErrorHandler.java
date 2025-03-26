package dev.dsf.bpe.v2.error.impl;

import java.util.List;

import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v2.error.MessageSendTaskErrorHandler;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Targets;
import dev.dsf.bpe.v2.variables.Variables;

public class DefaultMessageSendTaskErrorHandler extends AbstractMessageActivityErrorHandler
		implements MessageSendTaskErrorHandler
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultMessageSendTaskErrorHandler.class);

	/**
	 * <b>Single instance message send task</b> ({@link Targets} variables not set or empty):
	 * <p>
	 * Logs the given {@link Exception}, updates all received {@link Task} resource with {@link TaskStatus#INPROGRESS}
	 * to {@link TaskStatus#FAILED} and adds a {@link TaskOutputComponent} of type {@link BpmnMessage#error()}.
	 * <p>
	 * Exceptions thrown while updating the {@link Task} resources to status failed are logged. Returns the given
	 * {@link Exception} resulting in a deleted i.e. stopped process instance.
	 * <p>
	 * <b>Multi instance message send task</b> ({@link Targets} variables set and not empty):
	 * <p>
	 * Removes the current {@link Target} from the {@link Targets} variable and logs the error. Returns
	 * <code>null</code> resulting in a continuing process instance.
	 */
	@Override
	public Exception handleException(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			Exception exception)
	{
		Targets targets = variables.getTargets();

		// if we are a multi instance message send task, remove target
		if (targets != null && !targets.isEmpty())
			return handleMultiInstance(api, variables, sendTaskValues, exception, targets);

		// if we are a single instance message send task
		else
			return handleSingleInstance(api, variables, sendTaskValues, exception);
	}

	protected Exception handleMultiInstance(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			Exception exception, Targets targets)
	{
		Target target = variables.getTarget();
		targets = targets.removeByEndpointIdentifierValue(target);
		variables.setTargets(targets);

		String errorMessage = createErrorMessage(api, variables, exception, sendTaskValues, super::getExceptionMessage);
		List<Task> tasks = getTasks(api, variables, exception);

		updateFailedIfInprogress(api, variables, errorMessage, tasks);

		logger.debug("Target organization {}, endpoint {} with error {} removed from target list",
				target.getOrganizationIdentifierValue(), target.getEndpointIdentifierValue(), exception.getMessage());

		return null;
	}

	protected Exception handleSingleInstance(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			Exception exception)
	{
		logger.debug("Error while executing Task message send {}", getClass().getName(), exception);
		logger.error("Process {} has fatal error in step {} for task {}, last reason: {} - {}",
				variables.getProcessDefinitionId(), variables.getActivityInstanceId(),
				api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()),
				exception.getClass().getName(), exception.getMessage());

		String errorMessage = createErrorMessage(api, variables, exception, sendTaskValues, super::getExceptionMessage);
		List<Task> tasks = getTasks(api, variables, exception);

		updateFailedIfInprogress(api, variables, errorMessage, tasks);

		return exception;
	}
}
