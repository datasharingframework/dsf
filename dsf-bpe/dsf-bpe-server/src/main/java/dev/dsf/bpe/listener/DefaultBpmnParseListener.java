package dev.dsf.bpe.listener;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class DefaultBpmnParseListener extends AbstractBpmnParseListener implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultBpmnParseListener.class);

	private final StartListener startListener;
	private final EndListener endListener;
	private final ContinueListener continueListener;

	public DefaultBpmnParseListener(StartListener startListener, EndListener endListener,
			ContinueListener continueListener)
	{
		this.startListener = startListener;
		this.endListener = endListener;
		this.continueListener = continueListener;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(startListener, "startListener");
		Objects.requireNonNull(endListener, "endListener");
		Objects.requireNonNull(continueListener, "continueListener");
	}

	@Override
	public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl startEventActivity)
	{
		Element messageEventDefinition = startEventElement.element(BpmnParse.MESSAGE_EVENT_DEFINITION);
		if (messageEventDefinition != null)
			startEventActivity.addListener(ExecutionListener.EVENTNAME_START, startListener);
		else
			logger.debug("Not adding Listener to StartEvent {}", startEventActivity.getId());
	}

	@Override
	public void parseEndEvent(Element endEventElement, ScopeImpl scope, ActivityImpl endEventActivity)
	{
		/*
		 * Adding at index 0 to the end phase of the EndEvent, so processes can execute listeners after the Task
		 * resource has been updated. Listeners added to the end phase of the EndEvent via BPMN are execute after this
		 * listener
		 */
		endEventActivity.addListener(ExecutionListener.EVENTNAME_END, endListener, 0);
	}

	@Override
	public void parseIntermediateMessageCatchEventDefinition(Element messageEventDefinition,
			ActivityImpl nestedActivity)
	{
		/*
		 * Adding at index 0 to the end phase of the IntermediateMessageCatchEvent, so processes can execute listeners
		 * after variables has been updated. Listeners added to the end phase of the IntermediateMessageCatchEvent via
		 * BPMN are execute after this listener
		 */
		nestedActivity.addListener(ExecutionListener.EVENTNAME_END, continueListener, 0);
	}

	@Override
	public void parseReceiveTask(Element receiveTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		/*
		 * Adding at index 0 to the end phase of the IntermediateMessageCatchEvent, so processes can execute listeners
		 * after variables has been updated. Listeners added to the end phase of the IntermediateMessageCatchEvent via
		 * BPMN are execute after this listener
		 */
		activity.addListener(ExecutionListener.EVENTNAME_END, continueListener, 0);
	}
}
