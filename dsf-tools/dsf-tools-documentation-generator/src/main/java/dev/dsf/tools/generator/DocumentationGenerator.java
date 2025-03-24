package dev.dsf.tools.generator;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
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

import dev.dsf.bpe.v1.ProcessPluginDefinition;
import dev.dsf.bpe.v1.documentation.ProcessDocumentation;
import dev.dsf.common.documentation.Documentation;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class DocumentationGenerator extends AbstractMojo
{
	private static final Logger logger = LoggerFactory.getLogger(DocumentationGenerator.class);

	private static final String ENV_VARIABLE_PLACEHOLDER = "${env_variable}";
	private static final String PROPERTY_NAME_PLACEHOLDER = " ${property_name}";

	private record DocumentationEntry(String propertyName, String value)
	{
	}

	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
	private String projectBuildDirectory;

	@Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
	private List<String> compileClasspathElements;

	@Parameter(property = "workingPackages", required = true)
	private List<String> workingPackages;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		workingPackages.forEach(this::generateDocumentation);
	}

	private void generateDocumentation(String workingPackage)
	{
		Path file = Paths.get(projectBuildDirectory, "Documentation_" + workingPackage + ".md");
		logger.info("Generating documentation for package {} in file {}", workingPackage, file);

		moveExistingToBackup(file);

		URLClassLoader classLoader = classLoader();
		Reflections reflections = createReflections(classLoader, workingPackage);

		Set<Field> dsfFields = reflections.getFieldsAnnotatedWith(Documentation.class);
		if (!dsfFields.isEmpty())
		{
			writeFields(dsfFields, dsfDocumentationGenerator(), file, workingPackage);
		}

		// TODO add process API version abstraction
		Set<Field> processFields = reflections.getFieldsAnnotatedWith(ProcessDocumentation.class);
		if (!processFields.isEmpty())
		{
			List<String> pluginProcessNames = getPluginProcessNames(reflections, classLoader, workingPackage);
			writeFields(processFields, processDocumentationGenerator(pluginProcessNames), file, workingPackage);
		}
	}

	private Reflections createReflections(ClassLoader classLoader, String workingPackage)
	{
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage(workingPackage, classLoader)).addClassLoaders(classLoader)
				.setScanners(Scanners.FieldsAnnotated, Scanners.SubTypes);
		return new Reflections(configurationBuilder);
	}

	private URLClassLoader classLoader()
	{
		URL[] classpathElements = compileClasspathElements.stream().map(this::toUrl).filter(Objects::nonNull)
				.toArray(URL[]::new);

		return new URLClassLoader(classpathElements, Thread.currentThread().getContextClassLoader());
	}

	private URL toUrl(String path)
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

	private List<String> getPluginProcessNames(Reflections reflections, ClassLoader classLoader, String workingPackage)
	{
		List<Class<? extends ProcessPluginDefinition>> pluginDefinitionClasses = new ArrayList<>(
				reflections.getSubTypesOf(ProcessPluginDefinition.class));

		if (pluginDefinitionClasses.size() < 1)
		{
			logger.warn("No ProcessPluginDefinitions found in package {}", workingPackage);
			return List.of();
		}

		if (pluginDefinitionClasses.size() > 1)
			logger.warn("Found {} ProcessPluginDefinitions ({}) in package {}, using {}",
					pluginDefinitionClasses.size(), pluginDefinitionClasses, workingPackage,
					pluginDefinitionClasses.get(0).getName());

		try
		{
			ProcessPluginDefinition processPluginDefinition = pluginDefinitionClasses.get(0).getConstructor()
					.newInstance();

			return processPluginDefinition.getProcessModels().stream().map(f -> getProcessName(classLoader, f))
					.filter(Optional::isPresent).map(Optional::get).collect(toList());
		}
		catch (Exception e)
		{
			logger.error("Could not read process names from package {} and ProcessPluginDefinition with name {}: {} {}",
					workingPackage, pluginDefinitionClasses.get(0).getName(), e.getClass().getSimpleName(),
					e.getMessage());
			return List.of();
		}
	}

	private Optional<String> getProcessName(ClassLoader classLoader, String bpmnFile)
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
			Files.write(file, entries, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
		}
		catch (IOException e)
		{
			logger.error("Could not generate documentation for package {}: {} {}", workingPackage,
					e.getClass().getSimpleName(), e.getMessage());
		}
	}

	private Function<Field, DocumentationEntry> processDocumentationGenerator(List<String> pluginProcessNames)
	{
		return field ->
		{
			ProcessDocumentation documentation = field.getAnnotation(ProcessDocumentation.class);
			Value value = field.getAnnotation(Value.class);

			String[] valueSplit = getValueDefaultArray(value);

			String initialProperty = valueSplit.length > 0 ? valueSplit[0] : "";
			String property = getDocumentationString("Property", initialProperty);

			String initialEnvironment = initialProperty.replace(".", "_").toUpperCase();
			String environment = initialProperty.endsWith(".password") || initialProperty.endsWith(".secret")
					? String.format("%s or %s_FILE", initialEnvironment, initialEnvironment)
					: initialEnvironment;

			String required = getDocumentationString("Required", documentation.required() ? "Yes" : "No");

			String[] processNames = documentation.processNames();
			String processes = getDocumentationString("Processes",
					getProcessNamesAsString(processNames, pluginProcessNames));

			String description = getDocumentationString("Description", documentation.description());
			String recommendation = getDocumentationString("Recommendation", documentation.recommendation());
			String example = getDocumentationStringMonospace("Example", documentation.example());


			String defaultValue = valueSplit.length > 1 && !"null".equals(valueSplit[1])
					? getDocumentationStringMonospace("Default", valueSplit[1])
					: "";

			return new DocumentationEntry(initialProperty,
					String.format("### %s%n%s%s%s%s%s%s%s%n", environment, property, required, processes, description,
							recommendation, example, defaultValue).replace(ENV_VARIABLE_PLACEHOLDER, initialEnvironment)
							.replace(PROPERTY_NAME_PLACEHOLDER, initialProperty));
		};
	}

	private Function<Field, DocumentationEntry> dsfDocumentationGenerator()
	{
		return field ->
		{
			Documentation documentation = field.getAnnotation(Documentation.class);
			Value value = field.getAnnotation(Value.class);

			String[] valueSplit = getValueDefaultArray(value);

			String initialProperty = valueSplit.length > 0 ? valueSplit[0] : "";
			String property = getDocumentationString("Property", initialProperty);

			String initialEnvironment = initialProperty.replace(".", "_").toUpperCase();
			String environment = initialProperty.endsWith(".password") || initialProperty.endsWith(".secret")
					? String.format("%s or %s_FILE", initialEnvironment, initialEnvironment)
					: initialEnvironment;

			String required = getDocumentationString("Required", documentation.required() ? "Yes" : "No");

			String description = getDocumentationString("Description", documentation.description());
			String recommendation = getDocumentationString("Recommendation", documentation.recommendation());
			String example = getDocumentationStringMonospace("Example", documentation.example());

			String defaultValue = valueSplit.length > 1 && !"null".equals(valueSplit[1])
					? getDocumentationStringMonospace("Default", valueSplit[1])
					: "";

			return new DocumentationEntry(initialProperty,
					String.format("### %s%n%s%s%s%s%s%s%n", environment, property, required, description,
							recommendation, example, defaultValue).replace(ENV_VARIABLE_PLACEHOLDER, initialEnvironment)
							.replace(PROPERTY_NAME_PLACEHOLDER, initialProperty));
		};
	}

	private String[] getValueDefaultArray(Value value)
	{
		if (value == null)
			return new String[] {};

		String valueString = value.value();

		if (valueString.startsWith("#{'"))
			valueString = valueString.substring(valueString.indexOf("#{'") + 4, valueString.indexOf("}'"));

		return valueString.replaceAll("\\$", "").replace("#", "").replace("{", "").replace("}", "").split(":");
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
