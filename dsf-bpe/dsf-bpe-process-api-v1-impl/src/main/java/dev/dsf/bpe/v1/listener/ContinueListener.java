package dev.dsf.bpe.v1.listener;

import java.util.function.Function;

import org.hl7.fhir.r4.model.Task;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.Constants;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;

public class ContinueListener extends AbstractListener implements ExecutionListener
{
	private static final Logger logger = LoggerFactory.getLogger(ContinueListener.class);

	public ContinueListener(String serverBaseUrl, Function<DelegateExecution, ListenerVariables> variablesFactory)
	{
		super(serverBaseUrl, variablesFactory);
	}

	@Override
	public void doNotify(DelegateExecution execution, ListenerVariables variables) throws Exception
	{
		Task task = variables.getResource(Constants.TASK_VARIABLE);
		execution.removeVariable(Constants.TASK_VARIABLE);

		if (task != null)
		{
			variables.onContinue(task);
			boolean subProcess = execution.getParentId() != null
					&& !execution.getParentId().equals(execution.getProcessInstanceId());
			logContinue(logger, subProcess, task, subProcess ? variables.getStartTask() : null);
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
		String requester = getRequesterIdentifierValue(continueTask);

		String mainTaskUrl = getLocalVersionlessAbsoluteUrl(mainTask);

		if (subProcess)
		{
			if (correlationKey != null)
				logger.info(
						"Continuing subprocess of {} at {} [task: {}, requester: {}, business-key: {}, correlation-key: {}, message: {}, main-task: {}]",
						processUrl, getCurrentTime(), continueTaskUrl, requester, businessKey, correlationKey,
						messageName, mainTaskUrl);
			else
				logger.info(
						"Continuing subprocess of {} at {} [task: {}, requester: {}, business-key: {}, message: {}, main-task: {}]",
						processUrl, getCurrentTime(), continueTaskUrl, requester, businessKey, messageName,
						mainTaskUrl);
		}
		else
		{
			if (correlationKey != null)
				logger.info(
						"Continuing process {} at {} [task: {}, requester: {}, business-key: {}, correlation-key: {}, message: {}]",
						processUrl, getCurrentTime(), continueTaskUrl, requester, businessKey, correlationKey,
						messageName);
			else
				logger.info("Continuing process {} at {} [task: {}, requester: {}, business-key: {}, message: {}]",
						processUrl, getCurrentTime(), continueTaskUrl, requester, businessKey, messageName);
		}
	}
}
