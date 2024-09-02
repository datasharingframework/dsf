package dev.dsf.bpe.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.listener.ListenerFactory;

public class DefaultBpmnParseListener extends AbstractBpmnParseListener
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultBpmnParseListener.class);

	private final Map<String, ListenerFactory> listenerFactoriesByApiVersion = new HashMap<>();

	public DefaultBpmnParseListener(Stream<? extends ListenerFactory> listenerFactories)
	{
		if (listenerFactories != null)
			this.listenerFactoriesByApiVersion.putAll(listenerFactories
					.collect(Collectors.toMap(f -> String.valueOf(f.getApiVersion()), Function.identity())));
	}

	private Optional<ListenerFactory> getListenerFactory(ActivityImpl element)
	{
		ProcessDefinitionImpl processDefinition = element.getProcessDefinition();

		if (processDefinition instanceof ProcessDefinition withTenant)
		{
			String apiVersion = withTenant.getTenantId();

			if (apiVersion == null)
				return Optional.empty();
			else
				return Optional.ofNullable(listenerFactoriesByApiVersion.get(apiVersion));
		}
		else
			return Optional.empty();
	}

	@Override
	public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl startEventActivity)
	{
		getListenerFactory(startEventActivity).ifPresent(listenerFactory ->
		{
			Element messageEventDefinition = startEventElement.element(BpmnParse.MESSAGE_EVENT_DEFINITION);
			if (messageEventDefinition != null)
				startEventActivity.addListener(ExecutionListener.EVENTNAME_START, listenerFactory.getStartListener());
			else
				logger.debug("Not adding Listener to StartEvent {}", startEventActivity.getId());
		});
	}

	@Override
	public void parseEndEvent(Element endEventElement, ScopeImpl scope, ActivityImpl endEventActivity)
	{
		getListenerFactory(endEventActivity).ifPresent(listenerFactory ->
		{
			/*
			 * Adding at index 0 to the end phase of the EndEvent, so processes can execute listeners after the Task
			 * resource has been updated. Listeners added to the end phase of the EndEvent via BPMN are execute after
			 * this listener
			 */
			endEventActivity.addListener(ExecutionListener.EVENTNAME_END, listenerFactory.getEndListener(), 0);
		});
	}

	@Override
	public void parseIntermediateMessageCatchEventDefinition(Element messageEventDefinition,
			ActivityImpl nestedActivity)
	{
		getListenerFactory(nestedActivity).ifPresent(listenerFactory ->
		{
			/*
			 * Adding at index 0 to the end phase of the IntermediateMessageCatchEvent, so processes can execute
			 * listeners after variables has been updated. Listeners added to the end phase of the
			 * IntermediateMessageCatchEvent via BPMN are execute after this listener
			 */
			nestedActivity.addListener(ExecutionListener.EVENTNAME_END, listenerFactory.getContinueListener(), 0);
		});
	}

	@Override
	public void parseReceiveTask(Element receiveTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		getListenerFactory(activity).ifPresent(listenerFactory ->
		{
			/*
			 * Adding at index 0 to the end phase of the IntermediateMessageCatchEvent, so processes can execute
			 * listeners after variables has been updated. Listeners added to the end phase of the
			 * IntermediateMessageCatchEvent via BPMN are execute after this listener
			 */
			activity.addListener(ExecutionListener.EVENTNAME_END, listenerFactory.getContinueListener(), 0);
		});
	}
}
