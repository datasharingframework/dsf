/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.operaton.bpm.engine.impl.pvm.process.ScopeImpl;
import org.operaton.bpm.engine.impl.util.xml.Element;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.listener.ListenerFactory;
import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.engine.ProcessPluginConsumer;

public class DefaultBpmnParseListener implements BpmnParseListener, ProcessPluginConsumer
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultBpmnParseListener.class);

	private final Map<String, ListenerFactory> listenerFactoriesByApiVersion = new HashMap<>();
	private final Map<ProcessIdAndVersion, ProcessPlugin> processPluginsByProcessIdAndVersion = new HashMap<>();

	public DefaultBpmnParseListener(Stream<? extends ListenerFactory> listenerFactories)
	{
		if (listenerFactories != null)
			this.listenerFactoriesByApiVersion.putAll(listenerFactories
					.collect(Collectors.toMap(f -> String.valueOf(f.getApiVersion()), Function.identity())));
	}

	@Override
	public void setProcessPlugins(List<ProcessPlugin> plugins,
			Map<ProcessIdAndVersion, ProcessPlugin> processPluginsByProcessIdAndVersion)
	{
		this.processPluginsByProcessIdAndVersion.putAll(processPluginsByProcessIdAndVersion);
	}

	private ProcessPlugin getPlugin(ProcessIdAndVersion processIdAndVersion)
	{
		return processPluginsByProcessIdAndVersion.get(processIdAndVersion);
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
				startEventActivity.addListener(ExecutionListener.EVENTNAME_START,
						withMdc(listenerFactory.getStartListener()));
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
			endEventActivity.addListener(ExecutionListener.EVENTNAME_END, withMdc(listenerFactory.getEndListener()), 0);
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
			 * listeners after variables have been updated. Listeners added to the end phase of the
			 * IntermediateMessageCatchEvent via BPMN are execute after this listener
			 */
			nestedActivity.addListener(ExecutionListener.EVENTNAME_END, withMdc(listenerFactory.getContinueListener()),
					0);
		});
	}

	@Override
	public void parseReceiveTask(Element receiveTaskElement, ScopeImpl scope, ActivityImpl activity)
	{
		getListenerFactory(activity).ifPresent(listenerFactory ->
		{
			/*
			 * Adding at index 0 to the end phase of the IntermediateMessageCatchEvent, so processes can execute
			 * listeners after variables have been updated. Listeners added to the end phase of the
			 * IntermediateMessageCatchEvent via BPMN are execute after this listener
			 */
			activity.addListener(ExecutionListener.EVENTNAME_END, withMdc(listenerFactory.getContinueListener()), 0);
		});
	}

	public ExecutionListener withMdc(ExecutionListener delegate)
	{
		return execution ->
		{
			ExecutionEntity e = (ExecutionEntity) execution;
			ProcessIdAndVersion processKeyAndVersion = new ProcessIdAndVersion(e.getProcessDefinition().getKey(),
					e.getProcessDefinition().getVersionTag());

			getPlugin(processKeyAndVersion).getPluginMdc().executeWithProcessMdc(execution, delegate::notify);
		};
	}
}
