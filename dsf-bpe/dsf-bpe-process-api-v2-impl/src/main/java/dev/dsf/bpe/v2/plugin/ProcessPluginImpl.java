package dev.dsf.bpe.v2.plugin;

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
import java.util.function.Function;
import java.util.function.Predicate;

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
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.bpe.api.plugin.AbstractProcessPlugin;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.api.plugin.ProcessPluginFhirConfig;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.ProcessPluginDefinition;
import dev.dsf.bpe.v2.ProcessPluginDeploymentListener;
import dev.dsf.bpe.v2.constants.CodeSystems;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v2.constants.NamingSystems.TaskIdentifier;
import dev.dsf.bpe.v2.variables.FhirResourceValues;

public class ProcessPluginImpl extends AbstractProcessPlugin implements ProcessPlugin
{
	private final ProcessPluginDefinition processPluginDefinition;
	private final ProcessPluginApi processPluginApi;

	public ProcessPluginImpl(ProcessPluginDefinition processPluginDefinition, int processPluginApiVersion,
			boolean draft, Path jarFile, ClassLoader classLoader, ConfigurableEnvironment environment,
			ApplicationContext apiApplicationContext)
	{
		super(ProcessPluginDefinition.class, processPluginApiVersion, draft, jarFile, classLoader, environment,
				apiApplicationContext, ApiServicesSpringConfiguration.class);

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

		Predicate<Task> hasTaskInputMessageName = t -> t
				.getInput().stream().filter(
						i -> i.getType().getCoding().stream()
								.anyMatch(c -> CodeSystems.BpmnMessage.URL.equals(c.getSystem())
										&& CodeSystems.BpmnMessage.Codes.MESSAGE_NAME.equals(c.getCode())))
				.count() == 1;

		return new ProcessPluginFhirConfig<>(ActivityDefinition.class, CodeSystem.class, Library.class, Measure.class,
				NamingSystem.class, Questionnaire.class, StructureDefinition.class, Task.class, ValueSet.class,
				OrganizationIdentifier.SID, TaskIdentifier.SID, TaskStatus.DRAFT.toCode(), CodeSystems.BpmnMessage.URL,
				CodeSystems.BpmnMessage.Codes.MESSAGE_NAME, parseResource, encodeResource, getResourceName,
				hasMetadataResourceUrl, hasMetadataResourceVersion, getMetadataResourceVersion,
				getActivityDefinitionUrl, NamingSystem::hasName, getTaskInstantiatesCanonical, getTaskIdentifierValue,
				isTaskStatusDraft, getRequester, getRecipient, Task::hasInput, hasTaskInputMessageName,
				Task::hasOutput);
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
}
