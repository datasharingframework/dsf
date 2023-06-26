package dev.dsf.bpe.plugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SendTask;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaField;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaTaskListener;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.bpe.v1.constants.CodeSystems;
import dev.dsf.bpe.v1.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v1.constants.NamingSystems.TaskIdentifier;

public abstract class AbstractProcessPlugin<D, A> implements ProcessPlugin<D, A>
{
	private static final class FileAndResource
	{
		final String file;
		final Resource resource;

		FileAndResource(String file, Resource resource)
		{
			Objects.requireNonNull(file, "file");
			Objects.requireNonNull(resource, "resource");

			this.file = file;
			this.resource = resource;
		}

		static FileAndResource of(String file, Resource resource)
		{
			return new FileAndResource(file, resource);
		}

		String getFile()
		{
			return file;
		}

		Resource getResource()
		{
			return resource;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(AbstractProcessPlugin.class);

	private static final String BPMN_SUFFIX = ".bpmn";
	private static final String JSON_SUFFIX = ".json";
	private static final String XML_SUFFIX = ".xml";

	private static final String RESOURCE_VERSION_PATTERN_STRING = "(?<resourceVersion>\\d+\\.\\d+)";
	private static final Pattern RESOURCE_VERSION_PATTERN = Pattern.compile(RESOURCE_VERSION_PATTERN_STRING);
	private static final String VERSION_PATTERN_STRING = "(?<pluginVersion>" + RESOURCE_VERSION_PATTERN_STRING
			+ "\\.\\d+\\.\\d+)";
	private static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_PATTERN_STRING);

	private static final String VERSION_PLACEHOLDER_PATTERN_STRING = "#{version}";
	private static final Pattern VERSION_PLACEHOLDER_PATTERN = Pattern
			.compile(Pattern.quote(VERSION_PLACEHOLDER_PATTERN_STRING));

	private static final String DATE_PLACEHOLDER_PATTERN_STRING = "#{date}";
	private static final Pattern DATE_PLACEHOLDER_PATTERN = Pattern
			.compile(Pattern.quote(DATE_PLACEHOLDER_PATTERN_STRING));
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private static final String ORGANIZATION_PLACEHOLDER_PATTERN_STRING = "#{organization}";
	private static final Pattern ORGANIZATION_PLACEHOLDER_PATTERN = Pattern
			.compile(Pattern.quote(ORGANIZATION_PLACEHOLDER_PATTERN_STRING));

	private static final String PLACEHOLDER_PREFIX_SPRING = "${";
	private static final String PLACEHOLDER_PREFIX_SPRING_ESCAPED = "\\${";
	private static final String PLACEHOLDER_PREFIX_TMP = "§{";
	private static final String PLACEHOLDER_PREFIX = "#{";

	private static final Pattern PLACEHOLDER_PREFIX_PATTERN_SPRING = Pattern
			.compile(Pattern.quote(PLACEHOLDER_PREFIX_SPRING));
	private static final Pattern PLACEHOLDER_PREFIX_PATTERN_TMP = Pattern
			.compile(Pattern.quote(PLACEHOLDER_PREFIX_TMP));
	private static final Pattern PLACEHOLDER_PREFIX_PATTERN = Pattern.compile(Pattern.quote(PLACEHOLDER_PREFIX));

	private static final String ACTIVITY_DEFINITION_URL_PATTERN_STRING = "^(?<processUrl>http[s]{0,1}://(?<domain>(?:(?:[a-zA-Z0-9]{1,63}|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])\\.)+(?:[a-zA-Z0-9]{1,63}))"
			+ "/bpe/Process/(?<processName>[a-zA-Z0-9-]+))$";
	private static final Pattern ACTIVITY_DEFINITION_URL_PATTERN = Pattern
			.compile(ACTIVITY_DEFINITION_URL_PATTERN_STRING);

	private static final String INSTANTIATES_CANONICAL_PATTERN_STRING = "(?<processUrl>http[s]{0,1}://(?<domain>(?:(?:[a-zA-Z0-9]{1,63}|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])\\.)+(?:[a-zA-Z0-9]{1,63}))"
			+ "/bpe/Process/(?<processName>[a-zA-Z0-9-]+))\\|(?<processVersion>\\d+\\.\\d+)$";
	private static final Pattern INSTANTIATES_CANONICAL_PATTERN = Pattern
			.compile(INSTANTIATES_CANONICAL_PATTERN_STRING);

	private static final String PROCESS_ID_PATTERN_STRING = "^(?<domainNoDots>[a-zA-Z0-9-]+)_(?<processName>[a-zA-Z0-9-]+)$";
	private static final Pattern PROCESS_ID_PATTERN = Pattern.compile(PROCESS_ID_PATTERN_STRING);

	private final D processPluginDefinition;
	private final A processPluginApi;
	private final boolean draft;
	private final Path jarFile;
	private final ClassLoader processPluginClassLoader;
	private final FhirContext fhirContext;
	private final ConfigurableEnvironment environment;

	private boolean initialized;
	private AnnotationConfigApplicationContext applicationContext;
	private List<BpmnFileAndModel> processModels;
	private Map<ProcessIdAndVersion, List<FileAndResource>> fhirResources;

	public AbstractProcessPlugin(D processPluginDefinition, A processPluginApi, boolean draft, Path jarFile,
			ClassLoader processPluginClassLoader, FhirContext fhirContext, ConfigurableEnvironment environment)
	{
		Objects.requireNonNull(processPluginDefinition, "definition");
		Objects.requireNonNull(processPluginApi, "processPluginApi");
		Objects.requireNonNull(jarFile, "jarFile");
		Objects.requireNonNull(processPluginClassLoader, "processPluginClassLoader");
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(environment, "environment");

		this.processPluginDefinition = processPluginDefinition;
		this.processPluginApi = processPluginApi;
		this.draft = draft;
		this.jarFile = jarFile;
		this.processPluginClassLoader = processPluginClassLoader;
		this.fhirContext = fhirContext;
		this.environment = environment;
	}

	protected abstract List<Class<?>> getDefinitionSpringConfigurations();

	protected abstract String getDefinitionName();

	protected abstract String getDefinitionVersion();

	protected abstract String getDefinitionResourceVersion();

	protected abstract LocalDate getDefinitionReleaseDate();

	protected abstract LocalDate getDefinitionResourceReleaseDate();

	protected abstract Map<String, List<String>> getDefinitionFhirResourcesByProcessId();

	protected abstract List<String> getDefinitionProcessModels();

	protected abstract Class<?> getDefaultSpringConfiguration();

	protected abstract String getProcessPluginApiVersion();

	@Override
	public boolean initializeAndValidateResources(String localOrganizationIdentifierValue)
	{
		if (initialized)
			return true;

		boolean pluginDefinitionOk = validatePluginDefinitionValues();
		if (!pluginDefinitionOk)
			return false;

		Map<ProcessIdAndVersion, List<FileAndResource>> resources = loadFhirResources(localOrganizationIdentifierValue);
		if (resources.isEmpty())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: No valid FHIR resources", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString());
			return false;
		}

		List<BpmnFileAndModel> models = filterNonValidBpmnModels(loadBpmnModels(localOrganizationIdentifierValue));
		if (models.isEmpty())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: No valid processes", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString());
			return false;
		}

		Map<String, Integer> processCounts = models.stream()
				.collect(Collectors.toMap(m -> m.getProcessIdAndVersion().getId(), m -> 1, (c1, c2) -> c1 + c2));
		if (processCounts.values().stream().anyMatch(c -> c > 1))
		{
			logger.warn("Ignoring process plugin {}-{} from {}: Processes with duplicate IDs found {}",
					getDefinitionName(), getDefinitionVersion(), getJarFile().toString(),
					processCounts.entrySet().stream().filter(e -> e.getValue() > 1).map(Entry::getKey).toList());
			return false;
		}

		AnnotationConfigApplicationContext context = createApplicationContext();
		if (context == null)
		{
			logger.warn("Ignoring process plugin {}-{} from {}: Unable to initialize spring context",
					getDefinitionName(), getDefinitionVersion(), getJarFile().toString());
			return false;
		}

		models = filterBpmnModelsWithoutMatchingActivityDefinitions(resources,
				filterBpmnModelsWithNotAvailableBeans(models, context));
		if (models.isEmpty())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: No valid processes", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString());
			return false;
		}

		applicationContext = context;
		processModels = models;
		fhirResources = filterResourcesOfNotAvailableProcesses(resources, models);
		initialized = true;

		return true;
	}

	private boolean validatePluginDefinitionValues()
	{
		boolean nameOk = validateName();
		boolean versionOk = validateVersion();
		boolean resourceVersionOk = validateResourceVersion();
		boolean releaseDateOk = validateReleaseDate();
		boolean resourceReleaseDateOk = validateResourceReleaseDate();
		boolean springConfigurationOk = validateSpringConfigurations();
		boolean fhirResourcesOk = validateFhirResources();
		boolean processModelsOk = validateProcessModels();

		// logs all errors before deciding
		return nameOk && versionOk && resourceVersionOk && releaseDateOk && resourceReleaseDateOk
				&& springConfigurationOk && fhirResourcesOk && processModelsOk;
	}

	private boolean validateSpringConfigurations()
	{
		List<Class<?>> springConfigurations = getDefinitionSpringConfigurations();

		if (springConfigurations == null)
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} spring configurations null", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		if (springConfigurations.isEmpty())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} spring configurations empty", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		List<String> invalidConfigurationClasses = springConfigurations.stream()
				.filter(c -> c.getAnnotation(Configuration.class) == null).map(Class::getName).toList();
		if (!invalidConfigurationClasses.isEmpty())
		{
			logger.warn(
					"Ignoring process plugin {}-{} from {}: {} spring configuration classes without {} annotation: {}",
					getDefinitionName(), getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName(), Configuration.class.getName(),
					invalidConfigurationClasses.toString());
			return false;
		}

		return true;
	}

	private boolean validateName()
	{
		String name = getDefinitionName();

		if (name == null)
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} name null", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		if (name.isBlank())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} name blank", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		return true;
	}

	private boolean validateVersion()
	{
		String version = getDefinitionVersion();

		if (version == null)
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} version null", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		if (version.isBlank())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} version blank", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		if (!VERSION_PATTERN.matcher(version).matches())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} version not matching {}", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(), processPluginDefinition.getClass().getSimpleName(),
					VERSION_PATTERN_STRING);
			return false;
		}

		return true;
	}

	private boolean validateResourceVersion()
	{
		String resourceVersion = getDefinitionResourceVersion();

		if (resourceVersion == null)
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} resource version null", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		if (resourceVersion.isBlank())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} resource version blank", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		if (!RESOURCE_VERSION_PATTERN.matcher(resourceVersion).matches())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} version not matching {}", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(), processPluginDefinition.getClass().getSimpleName(),
					RESOURCE_VERSION_PATTERN_STRING);
			return false;
		}

		return true;
	}

	private boolean validateReleaseDate()
	{
		LocalDate releaseDate = getDefinitionReleaseDate();

		if (releaseDate == null)
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} release date null", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		return true;
	}

	private boolean validateResourceReleaseDate()
	{
		LocalDate resourceReleaseDate = getDefinitionResourceReleaseDate();
		if (resourceReleaseDate == null)
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} resource release date null", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		return true;
	}

	private boolean validateFhirResources()
	{
		Map<String, List<String>> fhirResources = getDefinitionFhirResourcesByProcessId();

		if (fhirResources == null)
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} fhir resources map null", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		if (fhirResources.isEmpty())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} fhir resources map empty", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		return true;
	}

	private boolean validateProcessModels()
	{
		List<String> processModels = getDefinitionProcessModels();

		if (processModels == null)
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} process models null", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		if (processModels.isEmpty())
		{
			logger.warn("Ignoring process plugin {}-{} from {}: {} process models empty", getDefinitionName(),
					getDefinitionVersion(), getJarFile().toString(),
					processPluginDefinition.getClass().getSimpleName());
			return false;
		}

		return true;
	}

	@Override
	public D getProcessPluginDefinition()
	{
		return processPluginDefinition;
	}

	@Override
	public A getProcessPluginApi()
	{
		return processPluginApi;
	}

	@Override
	public boolean isDraft()
	{
		return draft;
	}

	@Override
	public Path getJarFile()
	{
		return jarFile;
	}

	@Override
	public ClassLoader getProcessPluginClassLoader()
	{
		return processPluginClassLoader;
	}

	@Override
	public ApplicationContext getApplicationContext()
	{
		if (!initialized)
			throw new IllegalStateException("not initialized");

		return applicationContext;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public List<TypedValueSerializer> getTypedValueSerializers()
	{
		if (!initialized)
			throw new IllegalStateException("not initialized");

		return applicationContext.getBeansOfType(TypedValueSerializer.class).values().stream().distinct().toList();
	}

	@Override
	public List<ProcessIdAndVersion> getProcessKeysAndVersions()
	{
		return getProcessModels().stream().map(BpmnFileAndModel::getProcessIdAndVersion).toList();
	}

	@Override
	public List<BpmnFileAndModel> getProcessModels()
	{
		if (!initialized)
			throw new IllegalStateException("not initialized");

		return Collections.unmodifiableList(processModels);
	}

	@Override
	public Map<ProcessIdAndVersion, List<Resource>> getFhirResources()
	{
		if (!initialized)
			throw new IllegalStateException("not initialized");

		return fhirResources.entrySet().stream().collect(Collectors.toUnmodifiableMap(Entry::getKey,
				e -> e.getValue().stream().map(FileAndResource::getResource).toList()));
	}

	private ApplicationContext createParentApplicationContext()
	{
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerSingleton("processPluginApi", getProcessPluginApi());
		GenericApplicationContext context = new GenericApplicationContext(factory);
		context.refresh();
		return context;
	}

	private AnnotationConfigApplicationContext createApplicationContext()
	{
		try
		{
			var context = new AnnotationConfigApplicationContext();
			context.setParent(createParentApplicationContext());
			context.setClassLoader(getProcessPluginClassLoader());
			context.register(Stream
					.concat(Stream.of(getDefaultSpringConfiguration()), getDefinitionSpringConfigurations().stream())
					.toArray(Class<?>[]::new));
			context.setEnvironment(environment);
			context.refresh();
			return context;
		}
		catch (BeanCreationException e)
		{
			logger.error("Unable to create spring application context for process plugin {}-{}: {} {}",
					getDefinitionName(), getDefinitionVersion(), e.getClass().getSimpleName(), e.getMessage());
			logger.debug("Unable to create spring application context for process plugin " + getDefinitionName() + "-"
					+ getDefinitionVersion() + ", bean with error " + e.getBeanName(), e);

			return null;
		}
		catch (Exception e)
		{
			logger.error("Unable to create spring application context for process plugin {}-{}: {} {}",
					getDefinitionName(), getDefinitionVersion(), e.getClass().getSimpleName(), e.getMessage());
			logger.debug("Unable to create spring application context for process plugin " + getDefinitionName() + "-"
					+ getDefinitionVersion(), e);

			return null;
		}
	}

	private Stream<BpmnFileAndModel> loadBpmnModels(String localOrganizationIdentifierValue)
	{
		return getDefinitionProcessModels().stream().map(loadBpmnModelOrNull(localOrganizationIdentifierValue))
				.filter(Objects::nonNull);
	}

	private Function<String, BpmnFileAndModel> loadBpmnModelOrNull(String localOrganizationIdentifierValue)
	{
		return file ->
		{
			if (!file.endsWith(BPMN_SUFFIX))
			{
				logger.warn("Ignoring BPMN model {} from process plugin {}-{}: Filename not ending in '{}'", file,
						getDefinitionName(), getDefinitionVersion(), BPMN_SUFFIX);
				return null;
			}

			String resourceDateValue = getDefinitionResourceReleaseDate().format(DATE_FORMAT);
			logger.debug(
					"Reading BPMN model {} from process plugin {}-{} and replacing all occurrences of {} with {}, {} with {} and {} with {}",
					file, getDefinitionName(), getDefinitionVersion(), VERSION_PLACEHOLDER_PATTERN_STRING,
					getDefinitionResourceVersion(), DATE_PLACEHOLDER_PATTERN_STRING, resourceDateValue,
					ORGANIZATION_PLACEHOLDER_PATTERN_STRING, localOrganizationIdentifierValue);

			try (InputStream in = getProcessPluginClassLoader().getResourceAsStream(file))
			{
				if (in == null)
				{
					logger.warn(
							"Ignoring BPMN model {} from process plugin {}-{}: File not readable, process plugin class loader getResourceAsStream returned null",
							file, getDefinitionName(), getDefinitionVersion());
					return null;
				}

				String content = IOUtils.toString(in, StandardCharsets.UTF_8);

				content = VERSION_PLACEHOLDER_PATTERN.matcher(content).replaceAll(getDefinitionResourceVersion());
				content = DATE_PLACEHOLDER_PATTERN.matcher(content).replaceAll(resourceDateValue);
				content = ORGANIZATION_PLACEHOLDER_PATTERN.matcher(content).replaceAll(
						localOrganizationIdentifierValue != null ? localOrganizationIdentifierValue : "null");

				// escape bpmn placeholders
				content = PLACEHOLDER_PREFIX_PATTERN_SPRING.matcher(content).replaceAll(PLACEHOLDER_PREFIX_TMP);
				// make dsf placeholders look like spring placeholders
				// when calling replaceAll with ${ the $ needs to be escaped using \${
				content = PLACEHOLDER_PREFIX_PATTERN.matcher(content).replaceAll(PLACEHOLDER_PREFIX_SPRING_ESCAPED);
				// resolve dsf placeholders
				content = environment.resolveRequiredPlaceholders(content);
				// revert bpmn placeholders
				// when calling replaceAll with ${ the $ needs to be escaped using \${
				content = PLACEHOLDER_PREFIX_PATTERN_TMP.matcher(content).replaceAll(PLACEHOLDER_PREFIX_SPRING_ESCAPED);

				BpmnModelInstance model = Bpmn
						.readModelFromStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

				// store API version as ExtensionElements property
				Collection<Process> processes = model.getModelElementsByType(Process.class);
				processes.forEach(process ->
				{
					ExtensionElements ext = getOrCreateExtensionElements(process);

					CamundaProperties properties = ext.getChildElementsByType(CamundaProperties.class).stream()
							.findFirst().orElseGet(() ->
							{
								CamundaProperties p = ext.getModelInstance().newInstance(CamundaProperties.class);
								ext.addChildElement(p);
								return p;
							});

					CamundaProperty property = properties.getCamundaProperties().stream()
							.filter(p -> MODEL_ATTRIBUTE_PROCESS_API_VERSION.equals(p.getCamundaName())).findFirst()
							.orElseGet(() ->
							{
								CamundaProperty p = properties.getModelInstance().newInstance(CamundaProperty.class);
								properties.addChildElement(p);
								return p;
							});

					property.setCamundaName(MODEL_ATTRIBUTE_PROCESS_API_VERSION);
					property.setCamundaValue(getProcessPluginApiVersion());
				});

				return new BpmnFileAndModel(draft, file, model, getJarFile());
			}
			catch (IOException e)
			{
				logger.warn("Ignoring BPMN model {} from process plugin {}-{}: {} - {}", file, getDefinitionName(),
						getDefinitionVersion(), e.getClass().getName(), e.getMessage());
				return null;
			}
		};
	}

	private ExtensionElements getOrCreateExtensionElements(Process process)
	{
		ExtensionElements ext = process.getExtensionElements();
		if (ext == null)
		{
			ext = process.getModelInstance().newInstance(ExtensionElements.class);
			process.setExtensionElements(ext);
		}
		return ext;
	}

	private List<BpmnFileAndModel> filterNonValidBpmnModels(Stream<BpmnFileAndModel> models)
	{
		return models.filter(this::isValid).toList();
	}

	private boolean isValid(BpmnFileAndModel fileAndModel)
	{
		try
		{
			Bpmn.validateModel(fileAndModel.getModel());
		}
		catch (Exception e)
		{
			logger.warn("BPMN file {} not valid: {} - {}", fileAndModel.getFile(), e.getClass().getName(),
					e.getMessage());
			return false;
		}

		Collection<Process> processes = fileAndModel.getModel().getModelElementsByType(Process.class);
		if (processes.size() != 1)
		{
			logger.warn("BPMN file {} contains {} processes, expected 1", fileAndModel.getFile(), processes.size());
			return false;
		}

		ProcessIdAndVersion processKeyAndVersion = fileAndModel.getProcessIdAndVersion();
		if (!getDefinitionResourceVersion().equals(processKeyAndVersion.getVersion()))
		{
			logger.warn(
					"Camunda version tag of process in '{}' does not match process plugin version (tag: {} vs. plugin: {})",
					fileAndModel.getFile(), processKeyAndVersion.getVersion(), getDefinitionVersion());
			return false;
		}
		if (!PROCESS_ID_PATTERN.matcher(processKeyAndVersion.getId()).matches())
		{
			logger.warn("ID of process in '{}' does not match {}", fileAndModel.getFile(), PROCESS_ID_PATTERN_STRING);
			return false;
		}

		return true;
	}

	// TODO filter BPMN Models that use UserTasks but either do not declare a camunda formKey or do not contain the
	// matching questionnaire resource
	private Stream<BpmnFileAndModel> filterBpmnModelsWithNotAvailableBeans(List<BpmnFileAndModel> models,
			ApplicationContext applicationContext)
	{
		return models.stream().filter(beanAvailableForModel(applicationContext));
	}

	private Predicate<BpmnFileAndModel> beanAvailableForModel(ApplicationContext applicationContext)
	{
		return fileAndModel ->
		{
			Collection<Process> processes = fileAndModel.getModel().getModelElementsByType(Process.class);
			return processes.stream().allMatch(beanAvailable(applicationContext));
		};
	}

	private Predicate<Process> beanAvailable(ApplicationContext applicationContext)
	{
		return process -> beanAvailable(process, process, applicationContext);
	}

	private boolean beanAvailable(ModelElementInstance parent, Process process, ApplicationContext applicationContext)
	{
		// service tasks
		boolean serviceTasksOk = parent.getChildElementsByType(ServiceTask.class).stream().filter(Objects::nonNull)
				.allMatch(t -> beanAvailable(process, t.getId(), t.getCamundaClass(), JavaDelegate.class,
						applicationContext));

		// message send tasks
		boolean sendTasksOk = parent.getChildElementsByType(SendTask.class).stream().filter(Objects::nonNull).allMatch(
				t -> beanAvailable(process, t.getId(), t.getCamundaClass(), JavaDelegate.class, applicationContext)
						&& taskFieldsAvailable(process, "SendTask", t.getId(), t.getExtensionElements()));

		// user tasks: task listeners
		boolean userTasksTaskListenersOk = parent.getChildElementsByType(UserTask.class).stream()
				.filter(Objects::nonNull).allMatch(t ->
				{
					return t.getChildElementsByType(ExtensionElements.class).stream().filter(Objects::nonNull)
							.flatMap(e -> e.getChildElementsByType(CamundaTaskListener.class).stream())
							.filter(Objects::nonNull).allMatch(l -> beanAvailable(process, t.getId(),
									l.getCamundaClass(), TaskListener.class, applicationContext));
				});

		// all elements: execution listeners
		boolean allElementsExecutionListenersOk = parent.getChildElementsByType(FlowNode.class).stream()
				.filter(Objects::nonNull).allMatch(n ->
				{
					return n.getChildElementsByType(ExtensionElements.class).stream().filter(Objects::nonNull)
							.flatMap(e -> e.getChildElementsByType(CamundaExecutionListener.class).stream())
							.filter(Objects::nonNull).allMatch(l -> beanAvailable(process, n.getId(),
									l.getCamundaClass(), ExecutionListener.class, applicationContext));
				});

		// intermediate message throw events
		boolean intermediateMessageThrowEventsOk = parent.getChildElementsByType(IntermediateThrowEvent.class).stream()
				.filter(Objects::nonNull)
				.flatMap(
						e -> e.getEventDefinitions().stream().filter(Objects::nonNull)
								.filter(def -> def instanceof MessageEventDefinition))
				.map(def -> (MessageEventDefinition) def)
				.allMatch(def -> beanAvailable(process, def.getId(), def.getCamundaClass(), JavaDelegate.class,
						applicationContext)
						&& taskFieldsAvailable(process, "IntermediateThrowEvent", def.getId(),
								def.getExtensionElements()));

		// message end events
		boolean endEventsOk = parent.getChildElementsByType(EndEvent.class).stream().filter(Objects::nonNull)
				.allMatch(e ->
				{
					return e.getEventDefinitions().stream().filter(Objects::nonNull)
							.filter(def -> def instanceof MessageEventDefinition)
							.map(def -> (MessageEventDefinition) def)
							.allMatch(def -> beanAvailable(process, def.getId(), def.getCamundaClass(),
									JavaDelegate.class, applicationContext)
									&& taskFieldsAvailable(process, "MessageEndEvent", e.getId(),
											def.getExtensionElements()));
				});

		// sub processes
		boolean subProcessesOk = parent.getChildElementsByType(SubProcess.class).stream().filter(Objects::nonNull)
				.allMatch(subProcess -> beanAvailable(subProcess, process, applicationContext));

		return serviceTasksOk && sendTasksOk && userTasksTaskListenersOk && allElementsExecutionListenersOk
				&& intermediateMessageThrowEventsOk && endEventsOk && subProcessesOk;
	}

	public boolean taskFieldsAvailable(Process process, String elementType, String elementId,
			ExtensionElements extensionElements)
	{
		Collection<CamundaField> fields = extensionElements == null ? Collections.emptySet()
				: extensionElements.getChildElementsByType(CamundaField.class);

		String instantiatesCanonical = null;
		String messageName = null;
		String profile = null;

		for (CamundaField field : fields)
		{
			if ("profile".equals(field.getCamundaName()))
				profile = field.getTextContent();
			else if ("messageName".equals(field.getCamundaName()))
				messageName = field.getTextContent();
			else if ("instantiatesCanonical".equals(field.getCamundaName()))
				instantiatesCanonical = field.getTextContent();
		}

		if (instantiatesCanonical == null || instantiatesCanonical.isBlank() || messageName == null
				|| messageName.isBlank() || profile == null || profile.isBlank())
		{
			String noInstantiatesCanonical = instantiatesCanonical == null || instantiatesCanonical.isBlank()
					? "instantiatesCanonical"
					: null;
			String noMessageName = messageName == null || messageName.isBlank() ? "messageName" : null;
			String noProfile = profile == null || profile.isBlank() ? "profile" : null;

			String message = Stream.of(noInstantiatesCanonical, noMessageName, noProfile).filter(Objects::nonNull)
					.collect(Collectors.joining(", "));

			logger.warn("Mandatory fields in {} with id {} of process {}|{} not defined: {} missing", elementType,
					elementId, process.getId(), process.getCamundaVersionTag(), message);
		}

		return instantiatesCanonical != null && !instantiatesCanonical.isBlank() && messageName != null
				&& !messageName.isBlank() && profile != null && !profile.isBlank();
	}

	private boolean beanAvailable(Process process, String elementId, String className, Class<?> expectedInterface,
			ApplicationContext applicationContext)
	{
		if (className == null || className.isBlank())
			return true;

		ProcessIdAndVersion processKeyAndVersion = new ProcessIdAndVersion(process.getId(),
				process.getCamundaVersionTag());

		Class<?> serviceClass = loadClass(processKeyAndVersion, elementId, expectedInterface, className);
		if (serviceClass == null)
			return false;

		return isPrototypeBeanAvailable(processKeyAndVersion, elementId, expectedInterface, applicationContext,
				serviceClass);
	}

	private Class<?> loadClass(ProcessIdAndVersion processKeyAndVersion, String elementId, Class<?> expectedInterface,
			String className)
	{
		try
		{
			ClassLoader classLoader = getProcessPluginClassLoader();
			return classLoader.loadClass(className);
		}
		catch (ClassNotFoundException e)
		{
			logger.warn("{} '{}' defined in process {}, element {} not found", expectedInterface.getSimpleName(),
					className, processKeyAndVersion.toString(), elementId);
			logger.debug(expectedInterface.getSimpleName() + " '" + className + "' defined in process "
					+ processKeyAndVersion.toString() + " not found", e);
			return null;
		}
	}

	private boolean isPrototypeBeanAvailable(ProcessIdAndVersion processKeyAndVersion, String elementId,
			Class<?> expectedInterface, ApplicationContext applicationContext, Class<?> serviceClass)
	{
		String[] beanNames = applicationContext.getBeanNamesForType(serviceClass);
		if (beanNames.length <= 0)
		{
			logger.warn("Unable to find prototype bean of type {} for element {} in process {}", serviceClass.getName(),
					elementId, processKeyAndVersion.toString());
			return false;
		}
		else if (beanNames.length > 1)
		{
			logger.warn("Unable to find unique prototype bean of type {} for element {} in process {}, found {}",
					serviceClass.getName(), elementId, processKeyAndVersion.toString(), beanNames.length);
			return false;
		}
		else
		{
			boolean isPrototype = applicationContext.isPrototype(beanNames[0]);
			boolean implementsInterface = expectedInterface.isAssignableFrom(serviceClass);

			if (!isPrototype || !implementsInterface)
			{
				String notPrototype = !isPrototype ? "Bean not declared with 'prototype' scope" : null;
				String notImplementingInterface = !implementsInterface
						? serviceClass.getSimpleName() + " not implementing " + expectedInterface.getSimpleName()
						: null;
				String message = Stream.of(notPrototype, notImplementingInterface).filter(Objects::nonNull)
						.collect(Collectors.joining(", "));

				logger.warn("Unable to find prototype bean of type {} implementing {} for element {} in process {}: {}",
						serviceClass.getName(), expectedInterface.getName(), elementId, processKeyAndVersion.toString(),
						message);
			}

			return isPrototype && implementsInterface;
		}
	}

	private Map<ProcessIdAndVersion, List<FileAndResource>> loadFhirResources(String localOrganizationIdentifierValue)
	{
		Map<String, Resource> resourcesByFilename = getDefinitionFhirResourcesByProcessId().entrySet().stream()
				.map(Entry::getValue).flatMap(List::stream).distinct()
				.map(loadFhirResourceOrNull(localOrganizationIdentifierValue)).filter(Objects::nonNull)
				.collect(Collectors.toMap(FileAndResource::getFile, FileAndResource::getResource));

		return getDefinitionFhirResourcesByProcessId().entrySet().stream()
				.collect(Collectors.toMap(e -> new ProcessIdAndVersion(e.getKey(), getDefinitionResourceVersion()),
						e -> e.getValue().stream().filter(resourcesByFilename::containsKey)
								.map(file -> FileAndResource.of(file, resourcesByFilename.get(file))).toList()));
	}

	private Function<String, FileAndResource> loadFhirResourceOrNull(String localOrganizationIdentifierValue)
	{
		return file ->
		{
			if (!file.endsWith(JSON_SUFFIX) && !file.endsWith(XML_SUFFIX))
			{
				logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: Filename not ending in '{}' or '{}'",
						file, getDefinitionName(), getDefinitionVersion(), JSON_SUFFIX, XML_SUFFIX);
				return null;
			}

			String resourceDateValue = getDefinitionResourceReleaseDate().format(DATE_FORMAT);

			logger.debug(
					"Reading FHIR resource {} from process plugin {}-{} and replacing all occurrences of {} with {}, {} with {} and {} with {}",
					file, getDefinitionName(), getDefinitionVersion(), VERSION_PLACEHOLDER_PATTERN_STRING,
					getDefinitionResourceVersion(), DATE_PLACEHOLDER_PATTERN_STRING, resourceDateValue,
					ORGANIZATION_PLACEHOLDER_PATTERN_STRING, localOrganizationIdentifierValue);

			try (InputStream in = getProcessPluginClassLoader().getResourceAsStream(file))
			{
				if (in == null)
				{
					logger.warn(
							"Ignoring FHIR resource {} from process plugin {}-{}: Not readable, process plugin class loader getResourceAsStream returned null",
							file, getDefinitionName(), getDefinitionVersion());
					return null;
				}

				String content = IOUtils.toString(in, StandardCharsets.UTF_8);

				content = VERSION_PLACEHOLDER_PATTERN.matcher(content).replaceAll(getDefinitionResourceVersion());
				content = DATE_PLACEHOLDER_PATTERN.matcher(content).replaceAll(resourceDateValue);
				content = ORGANIZATION_PLACEHOLDER_PATTERN.matcher(content).replaceAll(
						localOrganizationIdentifierValue != null ? localOrganizationIdentifierValue : "null");

				// when calling replaceAll with ${ the $ needs to be escaped using \${
				content = PLACEHOLDER_PREFIX_PATTERN.matcher(content).replaceAll(PLACEHOLDER_PREFIX_SPRING_ESCAPED);
				content = environment.resolveRequiredPlaceholders(content);

				IBaseResource resource = newParser(file).parseResource(content);

				if (resource instanceof ActivityDefinition && isValid((ActivityDefinition) resource, file))
					return FileAndResource.of(file, (Resource) resource);
				else if (resource instanceof CodeSystem && isValid((CodeSystem) resource, file))
					return FileAndResource.of(file, (Resource) resource);
				else if (resource instanceof Library && isValid((Library) resource, file))
					return FileAndResource.of(file, (Resource) resource);
				else if (resource instanceof Measure && isValid((Measure) resource, file))
					return FileAndResource.of(file, (Resource) resource);
				else if (resource instanceof NamingSystem && isValid((NamingSystem) resource, file))
					return FileAndResource.of(file, (Resource) resource);
				else if (resource instanceof Questionnaire && isValid((Questionnaire) resource, file))
					return FileAndResource.of(file, (Resource) resource);
				else if (resource instanceof StructureDefinition && isValid((StructureDefinition) resource, file))
					return FileAndResource.of(file, (Resource) resource);
				else if (resource instanceof Task && isValid((Task) resource, file, localOrganizationIdentifierValue))
					return FileAndResource.of(file, (Resource) resource);
				else if (resource instanceof ValueSet && isValid((ValueSet) resource, file))
					return FileAndResource.of(file, (Resource) resource);
				else
				{
					logger.warn(
							"Ignoring FHIR resource {} from process plugin {}-{}: Not a ActivityDefinition, CodeSystem, Library, Measure, NamingSystem, Questionnaire, StructureDefinition, Task or ValueSet",
							file, getDefinitionName(), getDefinitionVersion());
					return null;
				}
			}
			catch (IOException e)
			{
				logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: {} - {}", file, getDefinitionName(),
						getDefinitionVersion(), e.getClass().getName(), e.getMessage());
				return null;
			}
		};
	}

	private IParser newParser(String file)
	{
		if (file.endsWith(JSON_SUFFIX))
			return fhirContext.newJsonParser();
		else if (file.endsWith(XML_SUFFIX))
			return fhirContext.newXmlParser();
		else
			throw new IllegalArgumentException("FHIR resource filename not ending in .json or .xml");
	}

	private boolean isValidMetadataResouce(MetadataResource resource, String file)
	{
		boolean urlOk = resource.hasUrl();
		boolean versionDefined = resource.hasVersion();
		boolean versionOk = versionDefined && resource.getVersion().equals(getDefinitionResourceVersion());

		if (!urlOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: {}.url empty", file, getDefinitionName(),
					getDefinitionVersion(), resource.getResourceType().name());
		}

		if (!versionDefined)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: {}.version empty", file,
					getDefinitionName(), getDefinitionVersion(), resource.getResourceType().name());
		}
		else if (!versionOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: {}.version not equal to {} but {}", file,
					getDefinitionName(), getDefinitionVersion(), resource.getResourceType().name(),
					getDefinitionResourceVersion(), resource.getVersion());
		}

		return urlOk && versionOk;
	}

	private boolean isValid(ActivityDefinition resource, String file)
	{
		boolean metadataResourceOk = isValidMetadataResouce(resource, file);
		boolean urlOk = ACTIVITY_DEFINITION_URL_PATTERN.matcher(resource.getUrl()).matches();

		if (!urlOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: ActivityDefinition.url not matching {}",
					file, getDefinitionName(), getDefinitionVersion(), ACTIVITY_DEFINITION_URL_PATTERN_STRING);
		}

		return metadataResourceOk && urlOk;
	}

	private boolean isValid(CodeSystem resource, String file)
	{
		// TODO add additional validation steps
		return isValidMetadataResouce(resource, file);
	}

	private boolean isValid(Library resource, String file)
	{
		// TODO add additional validation steps
		return isValidMetadataResouce(resource, file);
	}

	private boolean isValid(Measure resource, String file)
	{
		// TODO add additional validation steps
		return isValidMetadataResouce(resource, file);
	}

	private boolean isValid(NamingSystem resource, String file)
	{
		boolean nameOk = resource.hasName();
		boolean versionDefined = resource.hasVersion();
		boolean versionOk = versionDefined && resource.getVersion().equals(getDefinitionResourceVersion());

		if (!nameOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: NamingSystem.name empty", file,
					getDefinitionName(), getDefinitionVersion());
		}

		if (!versionDefined)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: NamingSystem.version empty", file,
					getDefinitionName(), getDefinitionVersion());
		}
		else if (!versionOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: NamingSystem.version not equal to {}",
					file, getDefinitionName(), getDefinitionVersion(), getDefinitionResourceVersion());
		}

		return nameOk && versionOk;
	}

	private boolean isValid(Questionnaire resource, String file)
	{
		// TODO add additional validation steps
		return isValidMetadataResouce(resource, file);
	}

	private boolean isValid(StructureDefinition resource, String file)
	{
		// TODO add additional validation steps
		return isValidMetadataResouce(resource, file);
	}

	private boolean isValid(Task resource, String file, String localOrganizationIdentifierValue)
	{
		Optional<Identifier> identifier = TaskIdentifier.findFirst(resource);
		boolean identifierOk = false;
		if (identifier.isEmpty())
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: No Task.identifier with system '{}'",
					file, getDefinitionName(), getDefinitionVersion(), TaskIdentifier.SID);
		}
		else
		{
			identifierOk = identifier.get().hasValue() && !identifier.get().getValue().contains("|");

			if (!identifierOk)
				logger.warn(
						"Ignoring FHIR resource {} from process plugin {}-{}: No Task.identifier with system '{}' and value, or value contains | character",
						file, getDefinitionName(), getDefinitionVersion(), TaskIdentifier.SID);
			// Additional checks see instantiatesCanonicalMatchesProcessIdAndIdentifierValid(...)
		}

		boolean statusOk = TaskStatus.DRAFT.equals(resource.getStatus());
		if (!statusOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: Task.status not '{}'", file,
					getDefinitionName(), getDefinitionVersion(), TaskStatus.DRAFT.toCode());
		}

		boolean requesterOk = false;
		if (!resource.hasRequester())
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: Task.requester not defined", file,
					getDefinitionName(), getDefinitionVersion());
		}
		else
		{
			requesterOk = isLocalOrganization(resource.getRequester(), "requester", file,
					localOrganizationIdentifierValue);
		}

		boolean recipientOk = false;
		if (!resource.hasRestriction() || !resource.getRestriction().hasRecipient()
				|| resource.getRestriction().getRecipient().size() != 1)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: Task.restriction.recipient not defined",
					file, getDefinitionName(), getDefinitionVersion());
		}
		else
		{
			recipientOk = isLocalOrganization(resource.getRestriction().getRecipientFirstRep(), "restriction.recipient",
					file, localOrganizationIdentifierValue);
		}

		boolean instantiatesCanonicalOk = INSTANTIATES_CANONICAL_PATTERN.matcher(resource.getInstantiatesCanonical())
				.matches();
		if (!instantiatesCanonicalOk)
		{
			logger.warn(
					"Ignoring FHIR resource {} from process plugin {}-{}: Task.instantiatesCanonical not matching {}",
					file, getDefinitionName(), getDefinitionVersion(), INSTANTIATES_CANONICAL_PATTERN_STRING);
			// Additional checks see instantiatesCanonicalMatchesProcessIdAndIdentifierValid(...)
		}

		boolean inputOk = false;
		if (!resource.hasInput())
		{
			logger.warn(
					"Ignoring FHIR resource {} from process plugin {}-{}: Task.input empty, input parameter with {}|{} expected",
					file, getDefinitionName(), getDefinitionVersion(), CodeSystems.BpmnMessage.URL,
					CodeSystems.BpmnMessage.Codes.MESSAGE_NAME);
		}
		else
		{
			inputOk = resource
					.getInput().stream().filter(
							i -> i.getType().getCoding().stream()
									.anyMatch(c -> CodeSystems.BpmnMessage.URL.equals(c.getSystem())
											&& CodeSystems.BpmnMessage.Codes.MESSAGE_NAME.equals(c.getCode())))
					.count() == 1;

			if (!inputOk)
				logger.warn(
						"Ignoring FHIR resource {} from process plugin {}-{}: One input parameter with {}|{} expected",
						file, getDefinitionName(), getDefinitionVersion(), CodeSystems.BpmnMessage.URL,
						CodeSystems.BpmnMessage.Codes.MESSAGE_NAME);
		}

		boolean outputOk = !resource.hasOutput();
		if (!outputOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: Task.output not empty", file,
					getDefinitionName(), getDefinitionVersion());
		}

		// TODO add additional validation steps
		return identifierOk && statusOk && requesterOk && recipientOk && instantiatesCanonicalOk && inputOk && outputOk;
	}

	private boolean isLocalOrganization(Reference reference, String refLocation, String file,
			String localOrganizationIdentifierValue)
	{
		if (localOrganizationIdentifierValue == null)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: Local organization identifier unknown",
					file, getDefinitionName(), getDefinitionVersion());
			return false;
		}

		boolean typeOk = ResourceType.Organization.name().equals(reference.getType());
		boolean identifierSystemOk = reference.hasIdentifier()
				&& OrganizationIdentifier.SID.equals(reference.getIdentifier().getSystem());
		boolean identifierValueOk = reference.hasIdentifier()
				&& localOrganizationIdentifierValue.equals(reference.getIdentifier().getValue());

		if (!typeOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: Task.{}.type not '{}'", file,
					getDefinitionName(), getDefinitionVersion(), refLocation, ResourceType.Organization.name());
		}
		if (!identifierSystemOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: Task.{}.identifier.system not '{}'", file,
					getDefinitionName(), getDefinitionVersion(), refLocation, OrganizationIdentifier.SID);
		}
		if (!identifierValueOk)
		{
			logger.warn("Ignoring FHIR resource {} from process plugin {}-{}: Task.{}.identifier.value not '{}'", file,
					getDefinitionName(), getDefinitionVersion(), refLocation, localOrganizationIdentifierValue);
		}

		return typeOk && identifierSystemOk && identifierValueOk;
	}

	private boolean isValid(ValueSet resource, String file)
	{
		// TODO add additional validation steps
		return isValidMetadataResouce(resource, file);
	}

	private List<BpmnFileAndModel> filterBpmnModelsWithoutMatchingActivityDefinitions(
			Map<ProcessIdAndVersion, List<FileAndResource>> fhirResources, Stream<BpmnFileAndModel> models)
	{
		return models.filter(hasMatchingActivityDefinition(fhirResources)).toList();
	}

	private Predicate<BpmnFileAndModel> hasMatchingActivityDefinition(
			Map<ProcessIdAndVersion, List<FileAndResource>> fhirResources)
	{
		return model ->
		{
			ProcessIdAndVersion processIdAndVersion = model.getProcessIdAndVersion();

			List<FileAndResource> resources = fhirResources.getOrDefault(processIdAndVersion, Collections.emptyList());
			if (resources.isEmpty())
			{
				logger.warn(
						"Ignoring BPMN model {} from process plugin {}-{}: No FHIR metadata resources found for process-id '{}'",
						model.getFile(), getDefinitionName(), getDefinitionVersion(),
						model.getProcessIdAndVersion().getId());
				return false;
			}

			List<FileAndResource> definitions = resources.stream()
					.filter(r -> r.getResource() instanceof ActivityDefinition).toList();

			if (definitions.size() != 1)
			{
				logger.warn(
						"Ignoring BPMN model {} from process plugin {}-{}: No ActivityDefinition found for process-id '{}'",
						model.getFile(), getDefinitionName(), getDefinitionVersion(),
						model.getProcessIdAndVersion().getId());
				return false;
			}

			String url = ((ActivityDefinition) definitions.get(0).getResource()).getUrl();
			Matcher urlMatcher = ACTIVITY_DEFINITION_URL_PATTERN.matcher(url);
			if (urlMatcher.matches())
			{
				String processDomain = urlMatcher.group("domain").replace(".", "");
				String processName = urlMatcher.group("processName");
				String processId = processDomain + "_" + processName;

				if (!processId.equals(processIdAndVersion.getId()))
				{
					logger.warn(
							"Ignoring BPMN model {} from process plugin {}-{}: Found ActivityDefinition.url does not match process id (url: '{}' vs. process-id '{}')",
							model.getFile(), getDefinitionName(), getDefinitionVersion(), url,
							model.getProcessIdAndVersion().getId());
					return false;
				}
			}

			return true;
		};
	}

	private Map<ProcessIdAndVersion, List<FileAndResource>> filterResourcesOfNotAvailableProcesses(
			Map<ProcessIdAndVersion, List<FileAndResource>> resources, List<BpmnFileAndModel> models)
	{
		Set<ProcessIdAndVersion> processIds = models.stream().map(BpmnFileAndModel::getProcessIdAndVersion)
				.collect(Collectors.toSet());
		return resources.entrySet().stream().filter(e -> processIds.contains(e.getKey()))
				.collect(Collectors.toMap(Entry::getKey, this::filterTasksNotMatchingProcessId));
	}

	private List<FileAndResource> filterTasksNotMatchingProcessId(
			Entry<ProcessIdAndVersion, List<FileAndResource>> entry)
	{
		return entry.getValue().stream().filter(fileAndResource ->
		{
			if (fileAndResource.getResource() instanceof Task)
				return instantiatesCanonicalMatchesProcessIdAndIdentifierValid(entry.getKey(), fileAndResource);
			else
				return true;
		}).toList();
	}

	private boolean instantiatesCanonicalMatchesProcessIdAndIdentifierValid(
			ProcessIdAndVersion expectedProcessIdAndVersion, FileAndResource fileAndResource)
	{
		String instantiatesCanonical = ((Task) fileAndResource.getResource()).getInstantiatesCanonical();
		String identifierValue = TaskIdentifier.findFirst(((Task) fileAndResource.getResource()))
				.map(Identifier::getValue).get();

		Matcher instantiatesCanonicalMatcher = INSTANTIATES_CANONICAL_PATTERN.matcher(instantiatesCanonical);
		if (instantiatesCanonicalMatcher.matches())
		{
			String processDomain = instantiatesCanonicalMatcher.group("domain").replace(".", "");
			String processName = instantiatesCanonicalMatcher.group("processName");
			String processVersion = instantiatesCanonicalMatcher.group("processVersion");
			String processUrl = instantiatesCanonicalMatcher.group("processUrl");
			String processId = processDomain + "_" + processName;

			boolean processIdOk = expectedProcessIdAndVersion.getId().equals(processId);
			if (!processIdOk)
			{
				logger.warn(
						"Ignoring FHIR resource {} from process plugin {}-{} for process {}: Task.instantiatesCanonical does not match process id (instantiatesCanonical: '{}' vs. process-id '{}')",
						fileAndResource.getFile(), getDefinitionName(), getDefinitionVersion(),
						expectedProcessIdAndVersion.getId(), instantiatesCanonical,
						expectedProcessIdAndVersion.getId());
			}

			boolean processVersionOk = expectedProcessIdAndVersion.getVersion().equals(processVersion);
			if (!processVersionOk)
			{
				logger.warn(
						"Ignoring FHIR resource {} from process plugin {}-{} for process {}: Task.instantiatesCanonical|version does not match declared resource version (instantiatesCanonical: '{}' vs. resource-version '{}')",
						fileAndResource.getFile(), getDefinitionName(), getDefinitionVersion(),
						expectedProcessIdAndVersion.getId(), instantiatesCanonical,
						expectedProcessIdAndVersion.getVersion());
			}

			String expectedIdentifierValueStart = processUrl + "/" + processVersion + "/";
			boolean identifierValueOk = identifierValue.startsWith(expectedIdentifierValueStart);
			if (!identifierValueOk)
			{
				logger.warn(
						"Ignoring FHIR resource {} from process plugin {}-{} for process {}: Task.identifier.value is invalid (identifier.value: '{}' not starting with '{}')",
						fileAndResource.getFile(), getDefinitionName(), getDefinitionVersion(),
						expectedProcessIdAndVersion.getId(), identifierValue, expectedIdentifierValueStart);
			}

			return processIdOk && processVersionOk && identifierValueOk;
		}
		else
			// no log, already tested
			return false;
	}
}
