package dev.dsf.bpe.camunda;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.ClassDelegateActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParser;
import org.camunda.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;

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
			Element camundaPropertiesElement, ScopeImpl scope)
	{
		super.parseServiceTaskLike(activity, elementName, serviceTaskElement, camundaPropertiesElement, scope);

		if (activity.getActivityBehavior() instanceof ClassDelegateActivityBehavior)
		{
			String className = serviceTaskElement.attributeNS(CAMUNDA_BPMN_EXTENSIONS_NS, PROPERTYNAME_CLASS);
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

		Class<? extends TaskListener> defaultUserTaskListenerClass = delegateProvider
				.getDefaultUserTaskListenerClass(getProcessPluginApiVersion());

		if (taskDefinition.getTaskListeners().getOrDefault(TaskListener.EVENTNAME_CREATE, new ArrayList<>()).stream()
				.filter(l -> l instanceof MultiVersionClassDelegateTaskListener)
				.map(l -> (MultiVersionClassDelegateTaskListener) l)
				.noneMatch(containsDefaultUserTaskListenerOrSuperClassOf(defaultUserTaskListenerClass)))
		{
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

	private String getProcessPluginApiVersion()
	{
		Element process = getRootElement().elements().stream().filter(e -> TAGNAME_PROCESS.equals(e.getTagName()))
				.findFirst().orElseThrow(() -> new RuntimeException("Root element does not contain process element"));
		Element extensionElements = process.elements().stream()
				.filter(e -> TAGNAME_EXTENSIONELEMENTS.equals(e.getTagName())).findFirst()
				.orElseThrow(() -> new RuntimeException("Process element does not contain extensionElements element"));
		Element properties = extensionElements.elements().stream()
				.filter(e -> TAGNAME_PROPERTIES.equals(e.getTagName())).findFirst().orElseThrow(
						() -> new RuntimeException("ExtensionElements element does not contain properties element"));
		Element property = properties.elements().stream().filter(e -> TAGNAME_PROPERTY.equals(e.getTagName()))
				.filter(e -> e.attributes().contains("name")
						&& ProcessPlugin.MODEL_ATTRIBUTE_PROCESS_API_VERSION.equals(e.attribute("name"))
						&& e.attributes().contains("value"))
				.findFirst()
				.orElseThrow(() -> new RuntimeException(
						"Properties element does not contain property element with attribute name = "
								+ ProcessPlugin.MODEL_ATTRIBUTE_PROCESS_API_VERSION + " and attribute value"));
		return property.attribute("value");
	}

	private Predicate<MultiVersionClassDelegateTaskListener> containsDefaultUserTaskListenerOrSuperClassOf(
			Class<? extends TaskListener> defaultUserTaskListenerClass)
	{
		return multiVersionClassDelegateTaskListener ->
		{
			try
			{
				Element process = getRootElement().elements().stream()
						.filter(e -> TAGNAME_PROCESS.equals(e.getTagName())).findFirst()
						.orElseThrow(() -> new RuntimeException("Root element does not contain process element"));

				ProcessIdAndVersion processKeyAndVersion = new ProcessIdAndVersion(getElementId(process),
						getElementVersion(process));

				Class<?> clazz = delegateProvider.getClassLoader(processKeyAndVersion)
						.loadClass(multiVersionClassDelegateTaskListener.getClassName());

				return defaultUserTaskListenerClass.isAssignableFrom(clazz);
			}
			catch (Exception exception)
			{
				throw new RuntimeException("Could not check if '" + defaultUserTaskListenerClass.getName()
						+ "' is assignable from '" + multiVersionClassDelegateTaskListener.getClassName() + "'",
						exception);
			}
		};
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
