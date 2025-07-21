package dev.dsf.maven.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class ConfigDocGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(ConfigDocGenerator.class);

	private static final String ENV_VARIABLE_PLACEHOLDER = "${env_variable}";
	private static final String PROPERTY_NAME_PLACEHOLDER = " ${property_name}";

	private static final Pattern DEFAULT_VALUE_PATTERN_1 = Pattern.compile("#\\{(.*)\\}");
	private static final Pattern DEFAULT_VALUE_PATTERN_2 = Pattern.compile(".*\\$\\{(.*)\\}.*");

	private static record DocumentationEntry(String propertyName, String value)
	{
	}

	private final Path projectBuildDirectory;
	private final URLClassLoader classLoader;
	private final Class<? extends Annotation> coreDocumentationAnnotationClass;

	public ConfigDocGenerator(Path projectBuildDirectory, List<String> compileClasspathElements)
	{
		Objects.requireNonNull(projectBuildDirectory, "projectBuildDirectory");

		this.projectBuildDirectory = projectBuildDirectory;
		classLoader = classLoader(compileClasspathElements);
		coreDocumentationAnnotationClass = loadCoreDocumentationAnnotation(classLoader);
	}

	private static URLClassLoader classLoader(List<String> compileClasspathElements)
	{
		URL[] classpathElements = compileClasspathElements.stream().map(ConfigDocGenerator::toUrl)
				.filter(Objects::nonNull).toArray(URL[]::new);

		return new URLClassLoader(classpathElements, Thread.currentThread().getContextClassLoader());
	}

	private static URL toUrl(String path)
	{
		try
		{
			return new File(path).toURI().toURL();
		}
		catch (MalformedURLException exception)
		{
			logger.warn("Could not transform path '{}' to url, returning null - {}", path, exception.getMessage());
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends Annotation> loadCoreDocumentationAnnotation(URLClassLoader classLoader)
	{
		try
		{
			Class<?> clazz = classLoader.loadClass("dev.dsf.common.documentation.Documentation");
			if (clazz.isAnnotation())
				return (Class<? extends Annotation>) clazz;
		}
		catch (ClassNotFoundException e)
		{
			logger.info(
					"DSF core documentation annotation not found on classpath, for process plugins this is not an error");
		}

		return null;
	}

	public void generateDocumentation(List<String> configDocPackages)
	{
		configDocPackages.forEach(this::generateDocumentation);
	}

	private void generateDocumentation(String configDocPackage)
	{
		Path file = projectBuildDirectory.resolve("ConfigDoc_" + configDocPackage + ".md");
		logger.info("Generating documentation for package {} in file {}", configDocPackage, file);

		moveExistingToBackup(file);

		Reflections reflections = createReflections(classLoader, configDocPackage);

		if (coreDocumentationAnnotationClass != null)
		{
			Set<Field> dsfFields = reflections.getFieldsAnnotatedWith(coreDocumentationAnnotationClass);
			if (!dsfFields.isEmpty())
				writeFields(dsfFields, dsfDocumentationGenerator(), file, configDocPackage);
		}

		// v1
		Set<Field> processFieldsV1 = reflections
				.getFieldsAnnotatedWith(dev.dsf.bpe.v1.documentation.ProcessDocumentation.class);
		if (!processFieldsV1.isEmpty())
		{
			List<String> pluginProcessNames = getPluginProcessNames(dev.dsf.bpe.v1.ProcessPluginDefinition.class,
					classLoader, configDocPackage, this::getV1ProcessNames);
			writeFields(processFieldsV1, processDocumentationGenerator(pluginProcessNames, ProcessDocumentation::v1),
					file, configDocPackage);
		}

		// v2
		Set<Field> processFieldsV2 = reflections
				.getFieldsAnnotatedWith(dev.dsf.bpe.v2.documentation.ProcessDocumentation.class);
		if (!processFieldsV2.isEmpty())
		{
			List<String> pluginProcessNames = getPluginProcessNames(dev.dsf.bpe.v2.ProcessPluginDefinition.class,
					classLoader, configDocPackage, this::getV2ProcessNames);
			writeFields(processFieldsV2, processDocumentationGenerator(pluginProcessNames, ProcessDocumentation::v2),
					file, configDocPackage);
		}
	}

	private Reflections createReflections(ClassLoader classLoader, String workingPackage)
	{
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage(workingPackage, classLoader)).addClassLoaders(classLoader)
				.setScanners(Scanners.FieldsAnnotated, Scanners.SubTypes);
		return new Reflections(configurationBuilder);
	}

	private <D> List<String> getPluginProcessNames(Class<D> processPluginDefinitionType, ClassLoader classLoader,
			String workingPackage, BiFunction<D, ClassLoader, List<String>> definitionToProcessNames)
	{
		List<Provider<?>> definitions = ServiceLoader.load(processPluginDefinitionType, classLoader).stream()
				.collect(Collectors.toList());

		if (definitions.size() < 1)
		{
			logger.warn("No ProcessPluginDefinitions found in package {}", workingPackage);
			return List.of();
		}

		if (definitions.size() > 1)
			logger.warn("Found {} ProcessPluginDefinitions ({}) in package {}, using {}", definitions.size(),
					definitions.stream().map(Provider::type).map(Class::getName).toList(), workingPackage,
					definitions.get(0).type().getName());

		try
		{
			@SuppressWarnings("unchecked")
			List<String> processNames = definitionToProcessNames.apply((D) definitions.get(0).get(), classLoader);
			return processNames;
		}
		catch (Exception e)
		{
			logger.error("Could not read process names from package {} and ProcessPluginDefinition with name {}: {} {}",
					workingPackage, definitions.get(0).type().getName(), e.getClass().getSimpleName(), e.getMessage());
			return List.of();
		}
	}

	private List<String> getV1ProcessNames(dev.dsf.bpe.v1.ProcessPluginDefinition definition, ClassLoader classLoader)
	{
		return definition.getProcessModels().stream().map(getProcessName(classLoader)).flatMap(Optional::stream)
				.toList();
	}

	private List<String> getV2ProcessNames(dev.dsf.bpe.v2.ProcessPluginDefinition definition, ClassLoader classLoader)
	{
		return definition.getProcessModels().stream().map(getProcessName(classLoader)).flatMap(Optional::stream)
				.toList();
	}

	private Function<String, Optional<String>> getProcessName(ClassLoader classLoader)
	{
		return bpmnFile ->
		{
			try (InputStream resource = classLoader.getResourceAsStream(bpmnFile))
			{
				return Bpmn.readModelFromStream(resource).getModelElementsByType(Process.class).stream()
						.map(BaseElement::getId).findFirst();
			}
			catch (Exception exception)
			{
				logger.warn("Could not read process name from resource file {}: {}", bpmnFile, exception.getMessage());
				return Optional.empty();
			}
		};
	}

	private void moveExistingToBackup(Path file)
	{
		if (Files.exists(file))
		{
			Path backupFile = file.resolveSibling(file.getFileName() + ".backup");
			try
			{
				logger.warn("Documentation file at {} exists, moving old file to {}", file, backupFile);
				Files.move(file, backupFile, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e)
			{
				logger.error("Could not move {} to {}: {} {}", file, backupFile, e.getClass().getSimpleName(),
						e.getMessage());
			}
		}
	}

	private void writeFields(Collection<? extends Field> fields,
			Function<Field, DocumentationEntry> documentationGenerator, Path file, String workingPackage)
	{
		Iterable<String> entries = fields.stream().map(documentationGenerator)
				.sorted(Comparator.comparing(DocumentationEntry::propertyName, String.CASE_INSENSITIVE_ORDER))
				.map(DocumentationEntry::value)::iterator;
		try
		{
			logger.debug("Writing {} entrie{} ...", fields.size(), fields.size() != 1 ? "s" : "");
			Files.write(file, entries, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
		}
		catch (IOException e)
		{
			logger.error("Could not generate documentation for package {}: {} {}", workingPackage,
					e.getClass().getSimpleName(), e.getMessage());
		}
	}

	private static record ProcessDocumentation(boolean required, String[] processNames, String description,
			String recommendation, String example)
	{
		static ProcessDocumentation v1(Field field)
		{
			dev.dsf.bpe.v1.documentation.ProcessDocumentation documentation = field
					.getAnnotation(dev.dsf.bpe.v1.documentation.ProcessDocumentation.class);

			return new ProcessDocumentation(documentation.required(), documentation.processNames(),
					documentation.description(), documentation.recommendation(), documentation.example());
		}

		static ProcessDocumentation v2(Field field)
		{
			dev.dsf.bpe.v2.documentation.ProcessDocumentation documentation = field
					.getAnnotation(dev.dsf.bpe.v2.documentation.ProcessDocumentation.class);

			return new ProcessDocumentation(documentation.required(), documentation.processNames(),
					documentation.description(), documentation.recommendation(), documentation.example());
		}
	}

	private Function<Field, DocumentationEntry> processDocumentationGenerator(List<String> pluginProcessNames,
			Function<Field, ProcessDocumentation> getProcessDocumentation)
	{
		return field ->
		{
			ProcessDocumentation documentation = getProcessDocumentation.apply(field);
			Value value = field.getAnnotation(Value.class);

			PropertyNameAndDefaultValue propertyNameAndDefaultValue = getPropertyNameAndDefaultValue(value);
			String propertyName = propertyNameAndDefaultValue.propertyName();
			String devalueValue = propertyNameAndDefaultValue.defaultValue();

			String propertyString = getDocumentationString("Property", propertyName);

			String initialEnvironment = propertyName.replace(".", "_").toUpperCase();
			String environment = propertyName.endsWith(".password") || propertyName.endsWith(".secret")
					? String.format("%s or %s_FILE", initialEnvironment, initialEnvironment)
					: initialEnvironment;

			String required = getDocumentationString("Required", documentation.required() ? "Yes" : "No");

			String[] processNames = documentation.processNames();
			String processes = getDocumentationString("Processes",
					getProcessNamesAsString(processNames, pluginProcessNames));

			String description = getDocumentationString("Description", documentation.description());
			String recommendation = getDocumentationString("Recommendation", documentation.recommendation());
			String example = getDocumentationStringMonospace("Example", documentation.example());

			String defaultValueString = devalueValue != null && devalueValue.length() > 1
					&& !"#{null}".equals(devalueValue) ? getDocumentationStringMonospace("Default", devalueValue) : "";

			return new DocumentationEntry(propertyName,
					String.format("### %s%n%s%s%s%s%s%s%s%n", environment, propertyString, required, processes,
							description, recommendation, example, defaultValueString)
							.replace(ENV_VARIABLE_PLACEHOLDER, initialEnvironment)
							.replace(PROPERTY_NAME_PLACEHOLDER, propertyName));
		};
	}

	private static record PropertyNameAndDefaultValue(String propertyName, String defaultValue)
	{
	}

	private PropertyNameAndDefaultValue getPropertyNameAndDefaultValue(Value value)
	{
		Matcher matcher1 = DEFAULT_VALUE_PATTERN_1.matcher(value.value());

		if (matcher1.matches())
		{
			Matcher matcher2 = DEFAULT_VALUE_PATTERN_2.matcher(matcher1.group(1));
			if (matcher2.matches())
			{
				String g = matcher2.group(1);
				int i = g.indexOf(":");
				if (i >= 0)
					return new PropertyNameAndDefaultValue(g.substring(0, i), g.substring(i + 1));
				else
					return new PropertyNameAndDefaultValue(g, null);
			}
		}
		else
		{
			Matcher matcher2 = DEFAULT_VALUE_PATTERN_2.matcher(value.value());
			if (matcher2.matches())
			{
				String g = matcher2.group(1);
				int i = g.indexOf(":");
				if (i >= 0)
					return new PropertyNameAndDefaultValue(g.substring(0, i), g.substring(i + 1));
				else
					return new PropertyNameAndDefaultValue(g, null);
			}
		}

		return new PropertyNameAndDefaultValue(value.value(), null);
	}

	private Function<Field, DocumentationEntry> dsfDocumentationGenerator()
	{
		return field ->
		{
			Object documentation = field.getAnnotation(coreDocumentationAnnotationClass);
			Value value = field.getAnnotation(Value.class);

			PropertyNameAndDefaultValue propertyNameAndDefaultValue = getPropertyNameAndDefaultValue(value);
			String propertyName = propertyNameAndDefaultValue.propertyName();
			String devalueValue = propertyNameAndDefaultValue.defaultValue();

			String propertyString = getDocumentationString("Property", propertyName);

			String initialEnvironment = propertyName.replace(".", "_").toUpperCase();
			String environment = propertyName.endsWith(".password") || propertyName.endsWith(".secret")
					? String.format("%s or %s_FILE", initialEnvironment, initialEnvironment)
					: initialEnvironment;

			String required = getDocumentationString("Required", getBoolean(documentation, "required") ? "Yes" : "No");

			String description = getDocumentationString("Description", getString(documentation, "description"));
			String recommendation = getDocumentationString("Recommendation",
					getString(documentation, "recommendation"));
			String example = getDocumentationStringMonospace("Example", getString(documentation, "example"));

			String defaultValueString = devalueValue != null && devalueValue.length() > 1
					&& !"#{null}".equals(devalueValue) ? getDocumentationStringMonospace("Default", devalueValue) : "";

			return new DocumentationEntry(propertyName,
					String.format("### %s%n%s%s%s%s%s%s%n", environment, propertyString, required, description,
							recommendation, example, defaultValueString)
							.replace(ENV_VARIABLE_PLACEHOLDER, initialEnvironment)
							.replace(PROPERTY_NAME_PLACEHOLDER, propertyName));
		};
	}

	private boolean getBoolean(Object coreDocumentationAnnotation, String methodName)
	{
		try
		{
			return Boolean.TRUE.equals(
					coreDocumentationAnnotationClass.getDeclaredMethod(methodName).invoke(coreDocumentationAnnotation));
		}
		catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	private String getString(Object coreDocumentationAnnotation, String methodName)
	{
		try
		{
			return (String) coreDocumentationAnnotationClass.getDeclaredMethod(methodName)
					.invoke(coreDocumentationAnnotation);
		}
		catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | SecurityException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	private String getDocumentationStringMonospace(String title, String value)
	{
		if (title == null || title.isBlank() || value == null || value.isBlank())
			return "";

		return String.format("- **%s:** `%s`%n", title, value);
	}

	private String getDocumentationString(String title, String value)
	{
		if (title == null || title.isBlank() || value == null || value.isBlank())
			return "";

		return String.format("- **%s:** %s%n", title, value);
	}

	private String getProcessNamesAsString(String[] documentationProcessNames, List<String> pluginProcessNames)
	{
		if (pluginProcessNames.size() == 0)
			return "Could not read process names from ProcessPluginDefinition";

		if (documentationProcessNames.length == 0)
			return String.join(", ", pluginProcessNames);

		for (String documentationProcessName : documentationProcessNames)
		{
			if (!pluginProcessNames.contains(documentationProcessName))
				logger.warn(
						"Documentation contains process with name '{}' which"
								+ " is not part of the processes {} defined in the ProcessPluginDefinition",
						documentationProcessName, pluginProcessNames);
		}

		return String.join(", ", documentationProcessNames);
	}
}
