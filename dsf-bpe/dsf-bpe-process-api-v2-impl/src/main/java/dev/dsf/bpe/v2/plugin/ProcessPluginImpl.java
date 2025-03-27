package dev.dsf.bpe.v2.plugin;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.ClassUtils;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.camunda.bpm.engine.impl.util.ClassDelegateUtil;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.bpe.api.plugin.AbstractProcessPlugin;
import dev.dsf.bpe.api.plugin.FhirResourceModifier;
import dev.dsf.bpe.api.plugin.FhirResourceModifiers;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.api.plugin.ProcessPluginFhirConfig;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.ProcessPluginDefinition;
import dev.dsf.bpe.v2.ProcessPluginDeploymentListener;
import dev.dsf.bpe.v2.activity.Activity;
import dev.dsf.bpe.v2.activity.DefaultUserTaskListener;
import dev.dsf.bpe.v2.activity.ExecutionListener;
import dev.dsf.bpe.v2.activity.ExecutionListenerDelegate;
import dev.dsf.bpe.v2.activity.MessageEndEvent;
import dev.dsf.bpe.v2.activity.MessageEndEventDelegate;
import dev.dsf.bpe.v2.activity.MessageIntermediateThrowEvent;
import dev.dsf.bpe.v2.activity.MessageIntermediateThrowEventDelegate;
import dev.dsf.bpe.v2.activity.MessageSendTask;
import dev.dsf.bpe.v2.activity.MessageSendTaskDelegate;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.activity.ServiceTaskDelegate;
import dev.dsf.bpe.v2.activity.UserTaskListener;
import dev.dsf.bpe.v2.activity.UserTaskListenerDelegate;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v2.constants.NamingSystems.TaskIdentifier;
import dev.dsf.bpe.v2.fhir.FhirResourceModifierDelegate;
import dev.dsf.bpe.v2.variables.FhirResourceValues;

public class ProcessPluginImpl extends AbstractProcessPlugin<UserTaskListener> implements ProcessPlugin
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginImpl.class);

	private final ProcessPluginDefinition processPluginDefinition;
	private final ProcessPluginApi processPluginApi;

	public ProcessPluginImpl(ProcessPluginDefinition processPluginDefinition, int processPluginApiVersion,
			boolean draft, Path jarFile, ClassLoader classLoader, ConfigurableEnvironment environment,
			ApplicationContext apiApplicationContext)
	{
		super(ProcessPluginDefinition.class, processPluginApiVersion, draft, jarFile, classLoader, environment,
				apiApplicationContext, ApiServicesSpringConfiguration.class, ServiceTask.class, MessageSendTask.class,
				UserTaskListener.class, ExecutionListener.class, MessageIntermediateThrowEvent.class,
				MessageEndEvent.class, DefaultUserTaskListener.class);

		this.processPluginDefinition = processPluginDefinition;
		processPluginApi = apiApplicationContext.getBean(ProcessPluginApi.class);
	}

	@Override
	protected ProcessPluginFhirConfig<ActivityDefinition, CodeSystem, Library, Measure, NamingSystem, Questionnaire, StructureDefinition, Task, ValueSet> createFhirConfig()
	{
		BiFunction<String, String, Object> parseResource = (String filename, String content) ->
		{
			if (filename.endsWith(JSON_SUFFIX))
				return newJsonParser().parseResource(content);
			else if (filename.endsWith(XML_SUFFIX))
				return newXmlParser().parseResource(content);
			else
				throw new IllegalArgumentException("FHIR resource filename not ending in .json or .xml");
		};

		Function<Object, byte[]> encodeResource = resource ->
		{
			try (ByteArrayOutputStream out = new ByteArrayOutputStream();
					Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8))
			{
				newJsonParser().encodeResourceToWriter((IBaseResource) resource, w);
				return out.toByteArray();
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		};

		Function<Object, Optional<String>> getResourceName = resource -> Optional
				.ofNullable(resource instanceof Resource r ? r.getResourceType().name() : null);

		Predicate<Object> hasMetadataResourceUrl = resource -> resource instanceof MetadataResource m && m.hasUrl();
		Predicate<Object> hasMetadataResourceVersion = resource -> resource instanceof MetadataResource m
				&& m.hasVersion();

		Function<Object, Optional<String>> getMetadataResourceVersion = resource -> Optional
				.ofNullable(resource instanceof MetadataResource m ? m.getVersion() : null);

		Function<ActivityDefinition, Optional<String>> getActivityDefinitionUrl = a -> Optional
				.ofNullable(a.hasUrlElement() && a.getUrlElement().hasValue() ? a.getUrlElement().getValue() : null);

		Function<Task, Optional<String>> getTaskInstantiatesCanonical = resource -> Optional
				.ofNullable(resource instanceof Task t && t.hasInstantiatesCanonicalElement()
						&& t.getInstantiatesCanonicalElement().hasValue()
								? t.getInstantiatesCanonicalElement().getValue()
								: null);

		Function<Task, Optional<ProcessPluginFhirConfig.Identifier>> getTaskIdentifierValue = t -> TaskIdentifier
				.findFirst(t)
				.map(i -> new ProcessPluginFhirConfig.Identifier(
						i.hasSystem() ? Optional.of(i.getSystem()) : Optional.empty(),
						i.hasValue() ? Optional.of(i.getValue()) : Optional.empty()));

		Predicate<Task> isTaskStatusDraft = t -> t.hasStatusElement() && t.getStatusElement().hasValue()
				&& TaskStatus.DRAFT.equals(t.getStatus());

		Function<Task, Optional<ProcessPluginFhirConfig.Reference>> getRequester = t -> t.hasRequester()
				? Optional.ofNullable(t.getRequester()).map(r ->
				{
					Identifier i = r.getIdentifier();
					return new ProcessPluginFhirConfig.Reference(
							Optional.ofNullable(i.getSystemElement()).filter(e -> e.hasValue()).map(e -> e.getValue()),
							Optional.ofNullable(i.getValueElement()).filter(e -> e.hasValue()).map(e -> e.getValue()),
							Optional.ofNullable(r.getTypeElement()).filter(e -> e.hasValue()).map(e -> e.getValue()));
				})
				: Optional.empty();

		Function<Task, Optional<ProcessPluginFhirConfig.Reference>> getRecipient = t -> t.hasRestriction()
				&& t.getRestriction().hasRecipient() && t.getRestriction().getRecipient().size() == 1
						? Optional.ofNullable(t.getRestriction().getRecipientFirstRep()).map(r ->
						{
							Identifier i = r.getIdentifier();
							return new ProcessPluginFhirConfig.Reference(
									Optional.ofNullable(i.getSystemElement()).filter(e -> e.hasValue())
											.map(e -> e.getValue()),
									Optional.ofNullable(i.getValueElement()).filter(e -> e.hasValue())
											.map(e -> e.getValue()),
									Optional.ofNullable(r.getTypeElement()).filter(e -> e.hasValue())
											.map(e -> e.getValue()));
						})
						: Optional.empty();

		Predicate<Task> hasTaskInputMessageName = t -> t.getInput().stream()
				.filter(i -> i.getType().getCoding().stream().anyMatch(BpmnMessage::isMessageName)).count() == 1;

		return new ProcessPluginFhirConfig<>(ActivityDefinition.class, CodeSystem.class, Library.class, Measure.class,
				NamingSystem.class, Questionnaire.class, StructureDefinition.class, Task.class, ValueSet.class,
				OrganizationIdentifier.SID, TaskIdentifier.SID, TaskStatus.DRAFT.toCode(), BpmnMessage.SYSTEM,
				BpmnMessage.Codes.MESSAGE_NAME, parseResource, encodeResource, getResourceName, hasMetadataResourceUrl,
				hasMetadataResourceVersion, getMetadataResourceVersion, getActivityDefinitionUrl, NamingSystem::hasName,
				getTaskInstantiatesCanonical, getTaskIdentifierValue, isTaskStatusDraft, getRequester, getRecipient,
				Task::hasInput, hasTaskInputMessageName, Task::hasOutput);
	}

	private IParser newXmlParser()
	{
		return newParser(FhirContext::newXmlParser);
	}

	private IParser newJsonParser()
	{
		return newParser(FhirContext::newJsonParser);
	}

	private IParser newParser(Function<FhirContext, IParser> parserFactor)
	{
		IParser p = parserFactor.apply(processPluginApi.getFhirContext());
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);

		return p;
	}

	@Override
	protected List<Class<?>> getDefinitionSpringConfigurations()
	{
		return processPluginDefinition.getSpringConfigurations();
	}

	@Override
	protected String getDefinitionName()
	{
		return processPluginDefinition.getName();
	}

	@Override
	protected String getDefinitionVersion()
	{
		return processPluginDefinition.getVersion();
	}

	@Override
	protected String getDefinitionResourceVersion()
	{
		return processPluginDefinition.getResourceVersion();
	}

	@Override
	protected LocalDate getDefinitionReleaseDate()
	{
		return processPluginDefinition.getReleaseDate();
	}

	@Override
	protected LocalDate getDefinitionResourceReleaseDate()
	{
		return processPluginDefinition.getResourceReleaseDate();
	}

	@Override
	protected Map<String, List<String>> getDefinitionFhirResourcesByProcessId()
	{
		return processPluginDefinition.getFhirResourcesByProcessId();
	}

	@Override
	protected List<String> getDefinitionProcessModels()
	{
		return processPluginDefinition.getProcessModels();
	}

	@Override
	public PrimitiveValue<?> createFhirTaskVariable(String taskJson)
	{
		Task task = newJsonParser().parseResource(Task.class, taskJson);
		return FhirResourceValues.create(task);
	}

	@Override
	public PrimitiveValue<?> createFhirQuestionnaireResponseVariable(String questionnaireResponseJson)
	{
		QuestionnaireResponse questionnaireResponse = newJsonParser().parseResource(QuestionnaireResponse.class,
				questionnaireResponseJson);
		return FhirResourceValues.create(questionnaireResponse);
	}

	@Override
	public dev.dsf.bpe.api.plugin.ProcessPluginDeploymentListener getProcessPluginDeploymentListener()
	{
		return allActiveProcesses ->
		{
			List<String> activePluginProcesses = getActivePluginProcesses(allActiveProcesses);

			getApplicationContext().getBeansOfType(ProcessPluginDeploymentListener.class).values().stream()
					.forEach(l -> handleProcessPluginDeploymentStateListenerError(
							() -> l.onProcessesDeployed(activePluginProcesses), ProcessPluginDeploymentListener.class,
							l.getClass()));
		};
	}

	private <T> T get(Class<T> targetInterface, String className)
	{
		try
		{
			Class<?> targetImplClass = getProcessPluginClassLoader().loadClass(className);
			Object targetObject = getApplicationContext().getBean(targetImplClass);

			return targetInterface.cast(targetObject);
		}
		catch (BeansException | ClassNotFoundException | ClassCastException e)
		{
			logger.debug("Unable to create {} for {}", targetInterface.getName(), className, e);
			logger.warn("Unable to create {} for {}: {} - {}", targetInterface.getName(), className,
					e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	@Override
	public JavaDelegate getMessageSendTask(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		MessageSendTask target = get(MessageSendTask.class, className);
		injectFields(target, filterFhirTaskValues(fieldDeclarations), variableScope);

		SendTaskValues sendTaskValues = getSendTaskValues(fieldDeclarations, variableScope)
				.orElseThrow(noOrIncompleteFhirTaskFields("MessageSendTask", className));

		return new MessageSendTaskDelegate(processPluginApi, target, sendTaskValues);
	}

	@Override
	public JavaDelegate getServiceTask(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		ServiceTask target = get(ServiceTask.class, className);
		injectFields(target, fieldDeclarations, variableScope);

		return new ServiceTaskDelegate(processPluginApi, target);
	}

	@Override
	public JavaDelegate getMessageEndEvent(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		MessageEndEvent target = get(MessageEndEvent.class, className);
		injectFields(target, filterFhirTaskValues(fieldDeclarations), variableScope);

		SendTaskValues sendTaskValues = getSendTaskValues(fieldDeclarations, variableScope)
				.orElseThrow(noOrIncompleteFhirTaskFields("MessageEndEvent", className));

		return new MessageEndEventDelegate(processPluginApi, target, sendTaskValues);
	}

	@Override
	public JavaDelegate getMessageIntermediateThrowEvent(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		MessageIntermediateThrowEvent target = get(MessageIntermediateThrowEvent.class, className);
		injectFields(target, filterFhirTaskValues(fieldDeclarations), variableScope);

		SendTaskValues sendTaskValues = getSendTaskValues(fieldDeclarations, variableScope)
				.orElseThrow(noOrIncompleteFhirTaskFields("MessageIntermediateThrowEvent", className));

		return new MessageIntermediateThrowEventDelegate(processPluginApi, target, sendTaskValues);
	}

	@Override
	public org.camunda.bpm.engine.delegate.ExecutionListener getExecutionListener(String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		ExecutionListener target = get(ExecutionListener.class, className);
		injectFields(target, fieldDeclarations, variableScope);

		return new ExecutionListenerDelegate(processPluginApi, target);
	}

	@Override
	public TaskListener getTaskListener(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		UserTaskListener target = get(UserTaskListener.class, className);
		ClassDelegateUtil.applyFieldDeclaration(fieldDeclarations, target);

		return new UserTaskListenerDelegate(processPluginApi, target);
	}

	private List<FieldDeclaration> filterFhirTaskValues(List<FieldDeclaration> fieldDeclarations)
	{
		return fieldDeclarations.stream().filter(isTaskField("instantiatesCanonical").negate()
				.and(isTaskField("messageName").negate().and(isTaskField("profile").negate()))).toList();
	}

	private void injectFields(Activity target, List<FieldDeclaration> fieldDeclarations, VariableScope variableScope)
	{
		fieldDeclarations.stream().forEach(fd ->
		{
			String name = fd.getName();
			String setMethodName = "set" + Character.toTitleCase(name.charAt(0))
					+ (name.length() > 1 ? name.substring(1) : "");
			Object value = getValue(fd, variableScope);

			Optional<Method> setMethod = Arrays.stream(target.getClass().getMethods())
					.filter(m -> setMethodName.equals(m.getName())).filter(m -> m.getParameterCount() == 1)
					.filter(m -> ClassUtils.isAssignable(value.getClass(), m.getParameters()[0].getType(), true))
					.findFirst();

			try
			{
				if (setMethod.isEmpty())
					throw new RuntimeException(
							"Field inject set-method with name '" + setMethodName + "' and single parameter of type '"
									+ value.getClass().getName() + "' missing in class " + target.getClass().getName());
				else
					setMethod.get().invoke(target, value);
			}
			catch (IllegalAccessException | InvocationTargetException e)
			{
				throw new RuntimeException(
						"Unable to inject field using '" + setMethodName + "' with single parameter of type '"
								+ value.getClass().getName() + "' on class " + target.getClass().getName());
			}
		});
	}

	private Optional<SendTaskValues> getSendTaskValues(List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		Optional<String> instantiatesCanonical = getStringValue(fieldDeclarations, "instantiatesCanonical",
				variableScope);
		Optional<String> messageName = getStringValue(fieldDeclarations, "messageName", variableScope);
		Optional<String> profile = getStringValue(fieldDeclarations, "profile", variableScope);

		if (instantiatesCanonical.isPresent() && messageName.isPresent() && profile.isPresent())
			return Optional.of(new SendTaskValues(instantiatesCanonical.get(), messageName.get(), profile.get()));
		else
		{
			if (instantiatesCanonical.isEmpty())
				noValueWarning("instantiatesCanonical");
			if (messageName.isEmpty())
				noValueWarning("messageName");
			if (profile.isEmpty())
				noValueWarning("profile");

			return Optional.empty();
		}
	}

	private void noValueWarning(String fieldName)
	{
		logger.warn(
				"No String value in '{}' field. Bad expression declaration or no String value in current variable scope",
				fieldName);
	}

	private Optional<String> getStringValue(List<FieldDeclaration> fieldDeclarations, String name,
			VariableScope variableScope)
	{
		return fieldDeclarations.stream().filter(isTaskField(name)).map(fd -> getValue(fd, variableScope))
				.filter(o -> o instanceof String).map(o -> (String) o).findFirst();
	}

	private Predicate<FieldDeclaration> isTaskField(String name)
	{
		Objects.requireNonNull(name, "name");

		return fd ->
		{
			return name.equals(fd.getName()) && fd.getValue() instanceof Expression;
		};
	}

	private Object getValue(FieldDeclaration fieldDeclaration, VariableScope variableScope)
	{
		Expression value = (Expression) fieldDeclaration.getValue();
		return value.getValue(variableScope);
	}

	private Supplier<RuntimeException> noOrIncompleteFhirTaskFields(String activityName, String className)
	{
		return () -> new RuntimeException(
				"No or incomplete FHIR Task message activity fields for " + activityName + " (" + className + ")");
	}

	@Override
	public FhirResourceModifier getFhirResourceModifier()
	{
		List<FhirResourceModifierDelegate> modifiers = getApplicationContext()
				.getBeansOfType(dev.dsf.bpe.v2.fhir.FhirResourceModifier.class).values().stream()
				.map(FhirResourceModifierDelegate::new).toList();

		return new FhirResourceModifiers(modifiers);
	}
}
