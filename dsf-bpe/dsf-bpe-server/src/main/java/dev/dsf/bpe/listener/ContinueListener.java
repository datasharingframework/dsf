package dev.dsf.bpe.listener;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.subscription.TaskHandler;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.variables.VariablesImpl;

public class ContinueListener extends AbstractListener implements ExecutionListener
{
	private static final Logger logger = LoggerFactory.getLogger(ContinueListener.class);

	public ContinueListener(String serverBaseUrl)
	{
		super(serverBaseUrl);
	}

	@Override
	public void doNotify(DelegateExecution execution, VariablesImpl variables) throws Exception
	{
		Task task = variables.getResource(TaskHandler.TASK_VARIABLE);

		if (task != null)
		{
			variables.onContinue(task);
			boolean subProcess = execution.getParentId() != null
					&& !execution.getParentId().equals(execution.getProcessInstanceId());
			logContinue(logger, subProcess, task, subProcess ? variables.getMainTask() : null);
		}
		else
			logger.warn("Variable 'task' null, not updating tasks");
	}

	private void logContinue(Logger logger, boolean subProcess, Task continueTask, Task mainTask)
	{
		String processUrl = continueTask.getInstantiatesCanonical();
		String messageName = getFirstInputParameter(continueTask, BpmnMessage.messageName());
		String businessKey = getFirstInputParameter(continueTask, BpmnMessage.businessKey());
		String correlationKey = getFirstInputParameter(continueTask, BpmnMessage.correlationKey());
		String continueTaskUrl = getLocalVersionlessAbsoluteUrl(continueTask);

		String mainTaskUrl = getLocalVersionlessAbsoluteUrl(mainTask);

		if (subProcess)
		{
			if (correlationKey != null)
				logger.info(
						"Continuing subprocess of {} [task: {}, business-key: {}, correlation-key: {}, message: {}, main-task: {}]",
						processUrl, continueTaskUrl, businessKey, correlationKey, messageName, mainTaskUrl);
			else
				logger.info("Continuing subprocess of {} [task: {}, business-key: {}, message: {}, main-task: {}]",
						processUrl, continueTaskUrl, businessKey, messageName, mainTaskUrl);
		}
		else
		{
			if (correlationKey != null)
				logger.info("Continuing process {} [task: {}, business-key: {}, correlation-key: {}, message: {}]",
						processUrl, continueTaskUrl, businessKey, correlationKey, messageName);
			else
				logger.info("Continuing process {} [task: {}, business-key: {}, message: {}]", processUrl,
						continueTaskUrl, businessKey, messageName);
		}

	}
}
