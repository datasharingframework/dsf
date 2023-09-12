package dev.dsf.bpe.listener;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class DebugLoggingBpmnParseListener extends AbstractBpmnParseListener
		implements BpmnParseListener, InitializingBean
{
	private static final class ExecutionListenerLogger implements ExecutionListener
	{
		final boolean logVariables;
		final boolean logVariablesLocal;

		ExecutionListenerLogger(boolean logVariables, boolean logVariablesLocal)
		{
			this.logVariables = logVariables;
			this.logVariablesLocal = logVariablesLocal;
		}

		@Override
		public void notify(DelegateExecution execution) throws Exception
		{
			if (execution != null)
			{
				logger.debug(
						"EventName: '{}', ActivityInstanceId: '{}', BusinessKey: '{}', CurrentActivityId: '{}', "
								+ "CurrentActivityName: '{}', ProcessDefinitionId: '{}', ProcessInstanceId: '{}'",
						execution.getEventName(), execution.getActivityInstanceId(), execution.getBusinessKey(),
						execution.getCurrentActivityId(), execution.getCurrentActivityName(),
						execution.getProcessDefinitionId(), execution.getProcessInstanceId());

				if (logVariables)
					logger.debug("Variables: {}", execution.getVariables());

				if (logVariablesLocal)
					logger.debug("VariablesLocal: {}", execution.getVariablesLocal());
			}
			else
			{
				logger.warn("Can't log, DelegateExecution is 'null'");
			}
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(DebugLoggingBpmnParseListener.class);

	private final boolean logActivityStart;
	private final boolean logActivityEnd;
	private final boolean logVariables;
	private final boolean logVariablesLocal;

	private final ExecutionListener listener;


	public DebugLoggingBpmnParseListener(boolean logActivityStart, boolean logActivityEnd, boolean logVariables,
			boolean logVariablesLocal)
	{
		this.logActivityStart = logActivityStart;
		this.logActivityEnd = logActivityEnd;
		this.logVariables = logVariables;
		this.logVariablesLocal = logVariablesLocal;

		listener = new ExecutionListenerLogger(logVariables, logVariablesLocal);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		if (logActivityStart)
			logger.warn(
					"Process activity start debug logging enabled. This should only be activated during process plugin development");

		if (logActivityEnd)
			logger.warn(
					"Process activity end debug logging enabled. This should only be activated during process plugin development");

		if (logVariables)
			logger.warn(
					"Process variable debug logging enabled. This should only be activated during process plugin development. WARNNING: Confidential information may be leaked via the debug log!");

		if (logVariablesLocal)
			logger.warn(
					"Process local variable debug logging enabled. This should only be activated during process plugin development. WARNNING: Confidential information may be leaked via the debug log!");
	}

	private void addListeners(ActivityImpl activity)
	{
		if (logActivityStart)
			activity.addListener(ExecutionListener.EVENTNAME_START, listener, 0);

		if (logActivityEnd)
			activity.addListener(ExecutionListener.EVENTNAME_END, listener, 0);
	}

	@Override
	public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseExclusiveGateway(Element exclusiveGwElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseInclusiveGateway(Element inclusiveGwElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseParallelGateway(Element parallelGwElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseScriptTask(Element scriptTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseBusinessRuleTask(Element businessRuleTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseTask(Element taskElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseManualTask(Element manualTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseEndEvent(Element endEventElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseSubProcess(Element subProcessElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseCallActivity(Element callActivityElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseSendTask(Element sendTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseReceiveTask(Element receiveTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseEventBasedGateway(Element eventBasedGwElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseTransaction(Element transactionElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseIntermediateThrowEvent(Element intermediateEventElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseIntermediateCatchEvent(Element intermediateEventElement, ScopeImpl scope, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseBoundaryEvent(Element boundaryEventElement, ScopeImpl scopeElement, ActivityImpl activity)
	{
		addListeners(activity);
	}

	@Override
	public void parseMultiInstanceLoopCharacteristics(Element activityElement,
			Element multiInstanceLoopCharacteristicsElement, ActivityImpl activity)
	{
		addListeners(activity);
	}
}
