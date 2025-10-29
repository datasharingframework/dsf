package dev.dsf.bpe.v1.plugin;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.bpmn.parser.FieldDeclaration;
import org.camunda.bpm.engine.impl.util.ClassDelegateUtil;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
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
import dev.dsf.bpe.api.logging.PluginMdc;
import dev.dsf.bpe.api.plugin.AbstractProcessPlugin;
import dev.dsf.bpe.api.plugin.FhirResourceModifier;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.api.plugin.ProcessPluginDeploymentListener;
import dev.dsf.bpe.api.plugin.ProcessPluginFhirConfig;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.ProcessPluginDefinition;
import dev.dsf.bpe.v1.ProcessPluginDeploymentStateListener;
import dev.dsf.bpe.v1.activity.DefaultUserTaskListener;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v1.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v1.constants.NamingSystems.TaskIdentifier;
import dev.dsf.bpe.v1.logging.PluginMdcImpl;
import dev.dsf.bpe.v1.variables.FhirResourceValues;
import dev.dsf.bpe.v1.variables.VariablesImpl;

public class ProcessPluginImpl extends AbstractProcessPlugin<TaskListener> implements ProcessPlugin
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginImpl.class);

	private final ProcessPluginDefinition processPluginDefinition;
	private final ProcessPluginApi processPluginApi;

	private final PluginMdcImpl pluginMdc;

	public ProcessPluginImpl(ProcessPluginDefinition processPluginDefinition, int processPluginApiVersion,
			boolean draft, Path jarFile, ClassLoader classLoader, ConfigurableEnvironment environment,
			ApplicationContext apiApplicationContext, String serverBaseUrl)
	{
		super(ProcessPluginDefinition.class, processPluginApiVersion, draft, jarFile, classLoader, environment,
				apiApplicationContext, ApiServicesSpringConfiguration.class, JavaDelegate.class, JavaDelegate.class,
				TaskListener.class, ExecutionListener.class, JavaDelegate.class, JavaDelegate.class,
				DefaultUserTaskListener.class);

		this.processPluginDefinition = processPluginDefinition;
		processPluginApi = apiApplicationContext.getBean(ProcessPluginApi.class);

		pluginMdc = new PluginMdcImpl(processPluginApiVersion, processPluginDefinition.getName(),
				processPluginDefinition.getVersion(), jarFile.toString(), serverBaseUrl, VariablesImpl::new);
	}

	@Override
	public PluginMdc getPluginMdc()
	{
		return pluginMdc;
	}

	@Override
	protected ProcessPluginFhirConfig<Resource, ActivityDefinition, CodeSystem, Library, Measure, NamingSystem, Questionnaire, StructureDefinition, Task, ValueSet> createFhirConfig()
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
				.filter(i -> i.getType().getCoding().stream().anyMatch(c -> BpmnMessage.URL.equals(c.getSystem())
						&& BpmnMessage.Codes.MESSAGE_NAME.equals(c.getCode())))
				.count() == 1;

		Function<StructureDefinition, Optional<String>> getStructureDefinitionBaseDefinition = s -> s
				.hasBaseDefinitionElement() && s.getBaseDefinitionElement().hasValue()
						? Optional.of(s.getBaseDefinitionElement().getValue())
						: Optional.empty();

		Function<Resource, List<String>> getProfiles = r -> r.hasMeta() && r.getMeta().hasProfile() ? r.getMeta()
				.getProfile().stream().filter(CanonicalType::hasValue).map(CanonicalType::getValue).toList()
				: List.of();

		Consumer<ActivityDefinition> modifyActivityDefinition = a ->
		{
			List<CanonicalType> oldProfiles = a.getMeta().getProfile();
			List<CanonicalType> newProfiles = Stream
					.concat(Stream.of("http://dsf.dev/fhir/StructureDefinition/activity-definition"), oldProfiles
							.stream().filter(CanonicalType::hasValue).map(CanonicalType::getValue).map(p -> switch (p)
							{
								case "http://dsf.dev/fhir/StructureDefinition/activity-definition|1.0.0" -> "http://dsf.dev/fhir/StructureDefinition/activity-definition";
								default -> p;
							}))
					.distinct().map(CanonicalType::new).toList();

			a.getMeta().setProfile(newProfiles);
		};

		Consumer<CodeSystem> modifyCodeSystem = c ->
		{
			List<CanonicalType> oldProfiles = c.getMeta().getProfile();
			List<CanonicalType> newProfiles = Stream
					.concat(Stream.of("http://dsf.dev/fhir/StructureDefinition/code-system"), oldProfiles.stream()
							.filter(CanonicalType::hasValue).map(CanonicalType::getValue).map(p -> switch (p)
							{
								case "http://dsf.dev/fhir/StructureDefinition/code-system|1.0.0" -> "http://dsf.dev/fhir/StructureDefinition/code-system";
								default -> p;
							}))
					.distinct().map(CanonicalType::new).toList();

			c.getMeta().setProfile(newProfiles);
		};

		Consumer<Library> modifyLibrary = l ->
		{
			List<CanonicalType> oldProfiles = l.getMeta().getProfile();
			List<CanonicalType> newProfiles = Stream
					.concat(Stream.of("http://dsf.dev/fhir/StructureDefinition/library"),
							oldProfiles.stream().filter(CanonicalType::hasValue).map(CanonicalType::getValue))
					.distinct().map(CanonicalType::new).toList();

			l.getMeta().setProfile(newProfiles);
		};

		Consumer<Measure> modifyMeasure = m ->
		{
			List<CanonicalType> oldProfiles = m.getMeta().getProfile();
			List<CanonicalType> newProfiles = Stream
					.concat(Stream.of("http://dsf.dev/fhir/StructureDefinition/measure"),
							oldProfiles.stream().filter(CanonicalType::hasValue).map(CanonicalType::getValue))
					.distinct().map(CanonicalType::new).toList();

			m.getMeta().setProfile(newProfiles);
		};

		Consumer<NamingSystem> modifyNamingSystem = n ->
		{
			List<CanonicalType> oldProfiles = n.getMeta().getProfile();
			List<CanonicalType> newProfiles = Stream
					.concat(Stream.of("http://dsf.dev/fhir/StructureDefinition/naming-system"),
							oldProfiles.stream().filter(CanonicalType::hasValue).map(CanonicalType::getValue))
					.distinct().map(CanonicalType::new).toList();

			n.getMeta().setProfile(newProfiles);
		};

		Consumer<Questionnaire> modifyQuestionnaire = q ->
		{
			List<CanonicalType> oldProfiles = q.getMeta().getProfile();
			List<CanonicalType> newProfiles = oldProfiles.stream().filter(CanonicalType::hasValue)
					.map(CanonicalType::getValue).map(p -> switch (p)
					{
						case "http://dsf.dev/fhir/StructureDefinition/questionnaire|1.0.0", "http://dsf.dev/fhir/StructureDefinition/questionnaire|1.5.0" -> "http://dsf.dev/fhir/StructureDefinition/questionnaire";
						default -> p;
					}).distinct().map(CanonicalType::new).toList();

			q.getMeta().setProfile(newProfiles);

			setRequired(q.getItem());
		};

		Consumer<StructureDefinition> modifyStructureDefinition = s ->
		{
			String oldBaseDefinition = s.getBaseDefinition();
			String newBaseDefinition = switch (oldBaseDefinition)
			{
				case "http://dsf.dev/fhir/StructureDefinition/task-base", "http://dsf.dev/fhir/StructureDefinition/task-base|1.0.0" -> "http://dsf.dev/fhir/StructureDefinition/task";
				default -> oldBaseDefinition;
			};
			s.setBaseDefinition(newBaseDefinition);

			s.getDifferential().getElement().stream().filter(ElementDefinition::hasBinding)
					.map(ElementDefinition::getBinding).filter(ElementDefinitionBindingComponent::hasValueSet)
					.forEach(b ->
					{
						String newValueSet = switch (b.getValueSet())
						{
							case "http://dsf.dev/fhir/CodeSystem/bpmn-message|1.0.0", "http://dsf.dev/fhir/CodeSystem/bpmn-message|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/bpmn-message";
							case "http://dsf.dev/fhir/CodeSystem/organization-role|1.0.0", "http://dsf.dev/fhir/CodeSystem/organization-role|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/organization-role";
							case "http://dsf.dev/fhir/CodeSystem/practitioner-role|1.0.0", "http://dsf.dev/fhir/CodeSystem/practitioner-role|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/practitioner-role";
							case "http://dsf.dev/fhir/CodeSystem/process-authorization|1.0.0", "http://dsf.dev/fhir/CodeSystem/process-authorization|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/process-authorization";
							case "http://dsf.dev/fhir/CodeSystem/read-access-tag|1.0.0", "http://dsf.dev/fhir/CodeSystem/read-access-tag|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/read-access-tag";
							default -> b.getValueSet();
						};

						b.setValueSet(newValueSet);
					});

			List<CanonicalType> oldProfiles = s.getMeta().getProfile();
			List<CanonicalType> newProfiles = Stream
					.concat(Stream.of("http://dsf.dev/fhir/StructureDefinition/structure-definition"),
							oldProfiles.stream().filter(CanonicalType::hasValue).map(CanonicalType::getValue))
					.distinct().map(CanonicalType::new).toList();

			s.getMeta().setProfile(newProfiles);
		};

		Consumer<ValueSet> modifyValueSet = v ->
		{
			List<CanonicalType> oldProfiles = v.getMeta().getProfile();
			List<CanonicalType> newProfiles = Stream
					.concat(Stream.of("http://dsf.dev/fhir/StructureDefinition/value-set"), oldProfiles.stream()
							.filter(CanonicalType::hasValue).map(CanonicalType::getValue).map(p -> switch (p)
							{
								case "http://dsf.dev/fhir/StructureDefinition/value-set|1.0.0" -> "http://dsf.dev/fhir/StructureDefinition/value-set";
								default -> p;
							}))
					.distinct().map(CanonicalType::new).toList();

			v.getMeta().setProfile(newProfiles);
		};

		Predicate<Questionnaire> hasQuestionnaireItemsWithRequired = q -> !q.hasItem() || hasRequired(q.getItem());

		Predicate<StructureDefinition> hasStructureDefinitionTaskDsfValueSetBindingsWithoutVersion = s -> s
				.hasDifferential()
				&& s.getDifferential().getElement().stream().filter(ElementDefinition::hasBinding)
						.map(ElementDefinition::getBinding).filter(ElementDefinitionBindingComponent::hasValueSet)
						.allMatch(b ->
						{
							return switch (b.getValueSet())
							{
								case "http://dsf.dev/fhir/CodeSystem/bpmn-message|1.0.0", "http://dsf.dev/fhir/CodeSystem/bpmn-message|2.0.0" -> false;
								case "http://dsf.dev/fhir/CodeSystem/organization-role|1.0.0", "http://dsf.dev/fhir/CodeSystem/organization-role|2.0.0" -> false;
								case "http://dsf.dev/fhir/CodeSystem/practitioner-role|1.0.0", "http://dsf.dev/fhir/CodeSystem/practitioner-role|2.0.0" -> false;
								case "http://dsf.dev/fhir/CodeSystem/process-authorization|1.0.0", "http://dsf.dev/fhir/CodeSystem/process-authorization|2.0.0" -> false;
								case "http://dsf.dev/fhir/CodeSystem/read-access-tag|1.0.0", "http://dsf.dev/fhir/CodeSystem/read-access-tag|2.0.0" -> false;
								default -> true;
							};
						});

		return new ProcessPluginFhirConfig<>(Resource.class, ActivityDefinition.class, CodeSystem.class, Library.class,
				Measure.class, NamingSystem.class, Questionnaire.class, StructureDefinition.class, Task.class,
				ValueSet.class, OrganizationIdentifier.SID, TaskIdentifier.SID, TaskStatus.DRAFT.toCode(),
				BpmnMessage.URL, BpmnMessage.Codes.MESSAGE_NAME, parseResource, encodeResource, getResourceName,
				hasMetadataResourceUrl, hasMetadataResourceVersion, getMetadataResourceVersion,
				getActivityDefinitionUrl, NamingSystem::hasName, getTaskInstantiatesCanonical, getTaskIdentifierValue,
				isTaskStatusDraft, getRequester, getRecipient, Task::hasInput, hasTaskInputMessageName, Task::hasOutput,
				getStructureDefinitionBaseDefinition, StructureDefinition::setBaseDefinition, getProfiles,
				modifyActivityDefinition, modifyCodeSystem, modifyLibrary, modifyMeasure, modifyNamingSystem,
				modifyQuestionnaire, modifyStructureDefinition, modifyValueSet, hasQuestionnaireItemsWithRequired,
				hasStructureDefinitionTaskDsfValueSetBindingsWithoutVersion);
	}

	private void setRequired(List<QuestionnaireItemComponent> items)
	{
		items.stream().filter(QuestionnaireItemComponent::hasLinkId).filter(QuestionnaireItemComponent::hasType)
				.filter(i -> !QuestionnaireItemType.DISPLAY.equals(i.getType())).filter(i -> !i.hasRequired())
				.forEach(i ->
				{
					switch (i.getLinkId())
					{
						case "business-key", "user-task-id" -> i.setRequired(true);
						default -> i.setRequired(false);
					}

					if (i.hasItem())
						setRequired(i.getItem());
				});
	}

	private boolean hasRequired(List<QuestionnaireItemComponent> items)
	{
		return items.stream().filter(QuestionnaireItemComponent::hasLinkId).filter(QuestionnaireItemComponent::hasType)
				.filter(i -> !QuestionnaireItemType.DISPLAY.equals(i.getType())).allMatch(i ->
				{
					return switch (i.getLinkId())
					{
						case "business-key", "user-task-id" -> i.hasRequired() && i.getRequired();
						default -> i.hasRequired();
					} && (!i.hasItem() || hasRequired(i.getItem()));
				});
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
	public String getPluginDefinitionPackageName()
	{
		return processPluginDefinition.getClass().getPackageName();
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
	public ProcessPluginDeploymentListener getProcessPluginDeploymentListener()
	{
		return allActiveProcesses ->
		{
			List<String> activePluginProcesses = getActivePluginProcesses(allActiveProcesses);

			getApplicationContext().getBeansOfType(ProcessPluginDeploymentStateListener.class).values().stream()
					.forEach(l -> handleProcessPluginDeploymentStateListenerError(
							() -> l.onProcessesDeployed(activePluginProcesses),
							ProcessPluginDeploymentStateListener.class, l.getClass()));
		};
	}

	private <T> T get(Class<T> targetInterface, String className, List<FieldDeclaration> fieldDeclarations)
	{
		try
		{
			Class<?> targetImplClass = getProcessPluginClassLoader().loadClass(className);
			Object target = getApplicationContext().getBean(targetImplClass);
			ClassDelegateUtil.applyFieldDeclaration(fieldDeclarations, target);

			return targetInterface.cast(target);
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
		return get(JavaDelegate.class, className, fieldDeclarations);
	}

	@Override
	public JavaDelegate getServiceTask(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		return get(JavaDelegate.class, className, fieldDeclarations);
	}

	@Override
	public JavaDelegate getMessageEndEvent(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		return get(JavaDelegate.class, className, fieldDeclarations);
	}

	@Override
	public JavaDelegate getMessageIntermediateThrowEvent(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		return get(JavaDelegate.class, className, fieldDeclarations);
	}

	@Override
	public ExecutionListener getExecutionListener(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		return get(ExecutionListener.class, className, fieldDeclarations);
	}

	@Override
	public TaskListener getTaskListener(String className, List<FieldDeclaration> fieldDeclarations,
			VariableScope variableScope)
	{
		return get(TaskListener.class, className, fieldDeclarations);
	}

	@Override
	public FhirResourceModifier getFhirResourceModifier()
	{
		return FhirResourceModifier.identity();
	}
}
