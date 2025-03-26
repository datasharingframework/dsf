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
import dev.dsf.bpe.v2.error.MessageEndEventErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class DefaultMessageEndEventErrorHandler extends AbstractMessageActivityErrorHandler
		implements MessageEndEventErrorHandler
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultMessageEndEventErrorHandler.class);

	/**
	 * Logs the given {@link Exception}, updates all received {@link Task} resource with {@link TaskStatus#INPROGRESS}
	 * to {@link TaskStatus#FAILED} and adds a {@link TaskOutputComponent} of type {@link BpmnMessage#error()}.
	 * <p>
	 * Exceptions thrown while updating the {@link Task} resources to status failed are logged.
	 * <p>
	 * Returns <code>null</code> resulting in a continuing process instance.
	 */
	@Override
	public Exception handleException(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			Exception exception)
	{
		logger.debug("Error while executing Task message send {}", getClass().getName(), exception);
		logger.error("Process {} has fatal error in step {} for task {}, reason: {} - {}",
				variables.getProcessDefinitionId(), variables.getActivityInstanceId(),
				api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()),
				exception.getClass().getName(), exception.getMessage());

		String errorMessage = createErrorMessage(api, variables, exception, sendTaskValues, this::getExceptionMessage);

		List<Task> tasks = getTasks(api, variables, exception);
		updateFailedIfInprogress(api, variables, errorMessage, tasks);

		// End event: No need to delete process instance
		return null;
	}
}
