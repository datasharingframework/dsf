package dev.dsf.bpe.v1.activity;

import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v1.variables.Variables;

public abstract class AbstractServiceDelegate implements JavaDelegate, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractServiceDelegate.class);

	protected final ProcessPluginApi api;

	/**
	 * @param api
	 *            not <code>null</code>
	 */
	public AbstractServiceDelegate(ProcessPluginApi api)
	{
		this.api = api;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(api, "api");
	}

	@Override
	public final void execute(DelegateExecution execution) throws Exception
	{
		final Variables variables = api.getVariables(execution);

		try
		{
			logger.trace("Execution of task with id='{}'", execution.getCurrentActivityId());

			doExecute(execution, variables);
		}
		// Error boundary event, do not stop process execution
		catch (BpmnError error)
		{
			logger.debug("Error while executing service delegate " + getClass().getName(), error);
			logger.error(
					"Process {} encountered error boundary event in step {} for task {}, error-code: {}, message: {}",
					execution.getProcessDefinitionId(), execution.getActivityInstanceId(),
					api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getMainTask()), error.getErrorCode(),
					error.getMessage());

			throw error;
		}
		// Not an error boundary event, stop process execution
		catch (Exception exception)
		{
			logger.debug("Error while executing service delegate " + getClass().getName(), exception);
			logger.error("Process {} has fatal error in step {} for task {}, reason: {} - {}",
					execution.getProcessDefinitionId(), execution.getActivityInstanceId(),
					api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getMainTask()),
					exception.getClass().getName(), exception.getMessage());

			String errorMessage = "Process " + execution.getProcessDefinitionId() + " has fatal error in step "
					+ execution.getActivityInstanceId() + ", reason: " + exception.getMessage();

			updateFailedIfInprogress(variables.getTasks(), errorMessage);

			// TODO evaluate throwing exception as alternative to stopping the process instance
			execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(),
					exception.getMessage());
		}
	}

	/**
	 * @param execution
	 *            Process instance information and variables
	 * @param variables
	 *            DSF process variables
	 * @throws BpmnError
	 *             Thrown when an error boundary event should be called
	 * @throws Exception
	 *             Uncaught exceptions will result in task status failed, the exception message will be written as an
	 *             error output
	 */
	protected abstract void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception;

	private void updateFailedIfInprogress(List<Task> tasks, String errorMessage)
	{
		for (int i = tasks.size() - 1; i >= 0; i--)
		{
			Task task = tasks.get(i);

			if (TaskStatus.INPROGRESS.equals(task.getStatus()))
			{
				task.setStatus(Task.TaskStatus.FAILED);
				task.addOutput(new TaskOutputComponent(new CodeableConcept(BpmnMessage.error()),
						new StringType(errorMessage)));
				updateAndHandleException(task);
			}
			else
			{
				logger.debug("Not updating Task {} with status: {}",
						api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task), task.getStatus());
			}
		}
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
}
