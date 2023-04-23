package dev.dsf.bpe.listener;

import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.variables.VariablesImpl;
import dev.dsf.fhir.client.FhirWebserviceClient;

public class EndListener extends AbstractListener implements ExecutionListener
{
	private static final Logger logger = LoggerFactory.getLogger(EndListener.class);

	private final FhirWebserviceClient webserviceClient;

	public EndListener(String serverBaseUrl, FhirWebserviceClient fhirWebserviceClient)
	{
		super(serverBaseUrl);

		this.webserviceClient = fhirWebserviceClient;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(webserviceClient, "webserviceClient");
	}

	@Override
	public void doNotify(DelegateExecution execution, VariablesImpl variables) throws Exception
	{
		List<Task> tasks = variables.getCurrentTasks();

		for (int i = tasks.size() - 1; i >= 0; i--)
		{
			Task task = tasks.get(i);
			updateIfInprogress(task);
			boolean subProcess = execution.getParentId() != null
					&& !execution.getParentId().equals(execution.getProcessInstanceId());
			logEnd(logger, subProcess, task, subProcess ? variables.getMainTask() : null);
		}

		variables.onEnd();
	}

	private void updateIfInprogress(Task task)
	{
		if (TaskStatus.INPROGRESS.equals(task.getStatus()))
		{
			task.setStatus(TaskStatus.COMPLETED);
			updateAndHandleException(task);
		}
		else
		{
			logger.debug("Not updating Task {} with status: {}", getLocalVersionlessAbsoluteUrl(task),
					task.getStatus());
		}
	}

	private void updateAndHandleException(Task task)
	{
		try
		{
			logger.debug("Updating Task {}, new status: {}", getLocalVersionlessAbsoluteUrl(task),
					task.getStatus().toCode());

			webserviceClient.withMinimalReturn().update(task);
		}
		catch (Exception e)
		{
			logger.error("Unable to update Task " + getLocalVersionlessAbsoluteUrl(task), e);
		}
	}

	private void logEnd(Logger logger, boolean subProcess, Task endTask, Task mainTask)
	{
		String processUrl = endTask.getInstantiatesCanonical();
		String businessKey = getFirstInputParameter(endTask, BpmnMessage.businessKey());
		String correlationKey = getFirstInputParameter(endTask, BpmnMessage.correlationKey());
		String endTaskUrl = getLocalVersionlessAbsoluteUrl(endTask);

		String mainTaskUrl = getLocalVersionlessAbsoluteUrl(mainTask);

		if (subProcess)
		{
			if (correlationKey != null)
				logger.info(
						"Subprocess of {} finished [task: {}, business-key: {}, correlation-key: {}, main-task: {}]",
						processUrl, endTaskUrl, businessKey, correlationKey, mainTaskUrl);
			else
				logger.info("Subprocess of {} finished [task: {}, business-key: {}, main-task: {}]", processUrl,
						endTaskUrl, businessKey, mainTaskUrl);
		}
		else
		{
			if (correlationKey != null)
				logger.info("Process {} finished [task: {}, business-key: {}, correlation-key: {}]", processUrl,
						endTaskUrl, businessKey, correlationKey);
			else
				logger.info("Process {} finished [task: {}, business-key: {}]", processUrl, endTaskUrl, businessKey,
						correlationKey);
		}
	}
}