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
package dev.dsf.bpe.engine;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.bpmn.behavior.ClassDelegateActivityBehavior;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParser;
import org.operaton.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.impl.task.TaskDefinition;
import org.operaton.bpm.engine.impl.util.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;

public class MultiVersionBpmnParse extends BpmnParse
{
	private static final Logger logger = LoggerFactory.getLogger(MultiVersionBpmnParse.class);

	protected static final String TAGNAME_PROCESS = "process";
	protected static final String TAGNAME_EXTENSIONELEMENTS = "extensionElements";
	protected static final String TAGNAME_PROPERTIES = "properties";
	protected static final String TAGNAME_PROPERTY = "property";
	protected static final String PROPERTYNAME_ID = "id";
	protected static final String PROPERTYNAME_VERSION = "http://camunda.org/schema/1.0/bpmn:versionTag";

	private final DelegateProvider delegateProvider;

	public MultiVersionBpmnParse(BpmnParser parser, DelegateProvider delegateProvider)
	{
		super(parser);

		this.delegateProvider = delegateProvider;
	}

	@Override
	public void parseServiceTaskLike(ActivityImpl activity, String elementName, Element serviceTaskElement,
			Element operatonPropertiesElement)
	{
		super.parseServiceTaskLike(activity, elementName, serviceTaskElement, operatonPropertiesElement);

		if (activity.getActivityBehavior() instanceof ClassDelegateActivityBehavior)
		{
			String className = serviceTaskElement.attributeNS(OPERATON_BPMN_EXTENSIONS_NS, PROPERTYNAME_CLASS);
			List<FieldDeclaration> fieldDeclarations = parseFieldDeclarations(serviceTaskElement);

			logger.debug("Modifying {} for {} in BPMN element with id '{}'",
					activity.getActivityBehavior().getClass().getSimpleName(), className,
					getElementId(serviceTaskElement));
			activity.setActivityBehavior(
					new MultiVersionClassDelegateActivityBehavior(className, fieldDeclarations, delegateProvider));
		}
		else
			logger.debug("Not modifying {} in BPMN element with id '{}'",
					activity.getActivityBehavior().getClass().getCanonicalName(), getElementId(serviceTaskElement));
	}

	@Override
	protected void parseTaskListeners(Element taskListenerElement, ActivityImpl timerActivity,
			TaskDefinition taskDefinition)
	{
		super.parseTaskListeners(taskListenerElement, timerActivity, taskDefinition);

		ProcessIdAndVersion processKeyAndVersion = getProcessIdAndVersion();

		Class<?> defaultUserTaskListenerClass = delegateProvider.getDefaultUserTaskListenerClass(processKeyAndVersion);

		if (taskDefinition.getTaskListeners().getOrDefault(TaskListener.EVENTNAME_CREATE, new ArrayList<>()).stream()
				.filter(l -> l instanceof MultiVersionClassDelegateTaskListener)
				.map(l -> (MultiVersionClassDelegateTaskListener) l).map(l -> l.getClassName())
				.noneMatch(cN -> delegateProvider.isDefaultUserTaskListenerOrSuperClassOf(processKeyAndVersion, cN)))
		{
			// adds default user task listener if no listener is already added that is or extends the default listener

			logger.debug("Adding new {} for event '{}' to BPMN element with id '{}'",
					defaultUserTaskListenerClass.getName(), TaskListener.EVENTNAME_CREATE,
					getElementId(taskListenerElement));

			List<FieldDeclaration> fieldDeclarations = parseFieldDeclarations(taskListenerElement);
			TaskListener defaultUserTaskListener = new MultiVersionClassDelegateTaskListener(
					defaultUserTaskListenerClass.getName(), fieldDeclarations, delegateProvider);
			taskDefinition.addTaskListener(TaskListener.EVENTNAME_CREATE, defaultUserTaskListener);
		}
		else
		{
			logger.debug("Custom UserTaskListener extending {} is defined for event '{}' in BPMN element with id '{}'",
					defaultUserTaskListenerClass.getName(), TaskListener.EVENTNAME_CREATE,
					getElementId(taskListenerElement));
		}
	}

	private ProcessIdAndVersion getProcessIdAndVersion()
	{
		Element process = getRootElement().elements().stream().filter(e -> TAGNAME_PROCESS.equals(e.getTagName()))
				.findFirst().orElseThrow(() -> new RuntimeException("Root element does not contain process element"));

		return new ProcessIdAndVersion(getElementId(process), getElementVersion(process));
	}

	@Override
	protected TaskListener parseTaskListener(Element taskListenerElement, String taskElementId)
	{
		String className = taskListenerElement.attribute(PROPERTYNAME_CLASS);

		if (className != null)
		{
			List<FieldDeclaration> fieldDeclarations = parseFieldDeclarations(taskListenerElement);

			logger.debug("Modifying {} for {} in BPMN element with id '{}'",
					MultiVersionClassDelegateTaskListener.class.getName(), className,
					getElementId(taskListenerElement));
			return new MultiVersionClassDelegateTaskListener(className, fieldDeclarations, delegateProvider);
		}
		else
		{
			TaskListener taskListener = super.parseTaskListener(taskListenerElement, taskElementId);
			logger.debug("Not modifying {} in BPMN element with id '{}", taskListener.getClass().getName(),
					getElementId(taskListenerElement));
			return taskListener;
		}
	}

	@Override
	public ExecutionListener parseExecutionListener(Element executionListenerElement, String ancestorElementId)
	{
		String className = executionListenerElement.attribute(PROPERTYNAME_CLASS);

		if (className != null)
		{
			List<FieldDeclaration> fieldDeclarations = parseFieldDeclarations(executionListenerElement);

			logger.debug("Modifying {} for {} in BPMN element with id '{}'",
					MultiVersionClassDelegateTaskListener.class.getName(), className,
					getElementId(executionListenerElement));
			return new MultiVersionClassDelegateExecutionListener(className, fieldDeclarations, delegateProvider);
		}
		else
		{
			ExecutionListener executionListener = super.parseExecutionListener(executionListenerElement,
					ancestorElementId);
			logger.debug("Not modifying {} in BPMN element with id '{}'", executionListener.getClass().getName(),
					getElementId(executionListenerElement));
			return executionListener;
		}
	}

	private String getElementId(Element element)
	{
		return element.attribute(PROPERTYNAME_ID);
	}

	private String getElementVersion(Element element)
	{
		return element.attribute(PROPERTYNAME_VERSION);
	}
}
