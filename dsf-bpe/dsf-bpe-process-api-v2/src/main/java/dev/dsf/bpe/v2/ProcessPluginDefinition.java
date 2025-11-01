package dev.dsf.bpe.v2;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import dev.dsf.bpe.v2.activity.ExecutionListener;
import dev.dsf.bpe.v2.activity.MessageEndEvent;
import dev.dsf.bpe.v2.activity.MessageIntermediateThrowEvent;
import dev.dsf.bpe.v2.activity.MessageSendTask;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.activity.UserTaskListener;
import dev.dsf.bpe.v2.documentation.ProcessDocumentation;
import dev.dsf.bpe.v2.fhir.FhirResourceModifier;
import dev.dsf.bpe.v2.spring.ActivityPrototypeBeanCreator;

/**
 * A provider configuration file named "dev.dsf.ProcessPluginDefinition" containing the canonical name of the class
 * implementing this interface needs to be part of the process plugin at "/META-INF/services/". For more details on the
 * content of the provider configuration file, see {@link ServiceLoader}.
 *
 * @see AbstractProcessPluginDefinition
 */
public interface ProcessPluginDefinition
{
	String RESOURCE_VERSION_PATTERN_STRING = "(?<resourceVersion>\\d+\\.\\d+)";
	String PLUGIN_VERSION_PATTERN_STRING = "(?<pluginVersion>" + RESOURCE_VERSION_PATTERN_STRING + "\\.\\d+\\.\\d+)";
	Pattern PLUGIN_VERSION_PATTERN = Pattern.compile(PLUGIN_VERSION_PATTERN_STRING);

	/**
	 * @return process plugin name, same as jar name excluding suffix <code>-&lt;version&gt;.jar</code>, same as
	 *         "artifactId" when using maven
	 */
	String getName();

	/**
	 * @return version of the process plugin, must match {@value #PLUGIN_VERSION_PATTERN_STRING}
	 */
	String getVersion();

	/**
	 * <i>Placeholder <code>#{version}</code> in FHIR and BPMN files will be replaced with the returned value.</i>
	 *
	 * @return version of FHIR and BPMN resources, must match {@value #RESOURCE_VERSION_PATTERN_STRING}
	 */
	default String getResourceVersion()
	{
		if (getVersion() == null)
			return null;

		Matcher matcher = PLUGIN_VERSION_PATTERN.matcher(getVersion());
		if (!matcher.matches())
			return null;
		else
			return matcher.group("resourceVersion");
	}

	/**
	 * @return the release date of the process plugin
	 */
	LocalDate getReleaseDate();

	/**
	 * <i>Placeholder <code>#{date}</code> in FHIR and BPMN files will be replaced with the returned value.</i>
	 *
	 * @return the release date of FHIR resources and BPMN files
	 */
	default LocalDate getResourceReleaseDate()
	{
		return getReleaseDate();
	}

	/**
	 * @return process plugin human readable name, <code>null</code> by default
	 */
	default String getTitle()
	{
		return null;
	}

	/**
	 * @return process plugin publisher name, <code>null</code> by default
	 */
	default String getPublisher()
	{
		return null;
	}

	/**
	 * @return process plugin publisher e-mail, <code>null</code> by default
	 */
	default String getPublisherEmail()
	{
		return null;
	}

	enum License
	{
		Apache2, MIT, Other
	}

	/**
	 * @return process plugin license, <code>null</code> by default, {@link License#Other} if specified via
	 *         {@link #getLicenseFile()}
	 */
	default License getLicense()
	{
		return null;
	}

	/**
	 * <i>Return <code>List.of("foo.bpmn");</code> for a foo.bpmn file located in the root folder of the process plugin
	 * jar. The returned files will be read via {@link ClassLoader#getResourceAsStream(String)}.</i>
	 * <p>
	 * <i>Occurrences of</i> <code>#{version}</code> <i>will be replaced with the value of
	 * {@link #getResourceVersion()}<br>
	 * Occurrences of</i> <code>#{date}</code> <i>will be replaced with the value of
	 * {@link #getResourceReleaseDate()}<br>
	 * Occurrences of</i> <code>#{organization}</code> <i>will be replaced with the local organization DSF identifier
	 * value, or</i> <code>"null"</code> <i>if no local organization can be found in the allow list<br>
	 * Other placeholders of the form</i> <code>#{property.name}</code> <i>will be replaced with values from equivalent
	 * environment variable, e.g.</i> <code>PROPERTY_NAME</code>
	 *
	 * @return *.bpmn files inside the process plugin jar, paths relative to root folder of process plugin
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	List<String> getProcessModels();

	/**
	 * <i>Return <code>Map.of("testcom_process", List.of("foo.xml"));</code> for a foo.xml file located in the root
	 * folder of the process plugin jar needed for a process called testcom_process. The returned files will be read via
	 * {@link ClassLoader#getResourceAsStream(String)}.</i>
	 * <p>
	 * <i>Supported metadata resource types are ActivityDefinition, CodeSystem, Library, Measure, NamingSystem,
	 * Questionnaire, StructureDefinition, Task and ValueSet.</i>
	 * <p>
	 * <i>Occurrences of</i> <code>#{version}</code> <i>will be replaced with the value of
	 * {@link #getResourceVersion()}<br>
	 * Occurrences of</i> <code>#{date}</code> <i>will be replaced with the value of
	 * {@link #getResourceReleaseDate()}<br>
	 * Occurrences of</i> <code>#{organization}</code> <i>will be replaced with the local organization DSF identifier
	 * value, or</i> <code>"null"</code> <i>if no local organization can be found in the allow list<br>
	 * Other placeholders of the form</i> <code>#{property.name}</code> <i>will be replaced with values from equivalent
	 * environment variable, e.g.</i> <code>PROPERTY_NAME</code>
	 *
	 * @return *.xml or *.json files inside the process plugin jar per process, paths relative to root folder of process
	 *         plugin
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	Map<String, List<String>> getFhirResourcesByProcessId();

	/**
	 * List of {@link Configuration} annotated spring configuration classes.
	 * <p>
	 * <i>All services defined in {@link ProcessPluginApi} and {@link ProcessPluginApi} itself can be {@link Autowired}
	 * in {@link Configuration} classes.</i>
	 * <p>
	 * <i>All implementations used for BPMN service tasks, message send tasks and throw events as well as task- and user
	 * task listeners need to be declared as spring {@link Bean}s with {@link Scope}</i> <code>"prototype"</code><i>.
	 * Other classes not directly used within BPMN activities should be declared with the default singleton scope.</i>
	 * <p>
	 * <i>Configuration classes that defined private fields annotated with {@link Value} defining property placeholders,
	 * can be configured via environment variables. A field</i> <code>private boolean specialFunction;</code>
	 * <i>annotated with</i> <code>&#64;Value("${org.test.process.special:false}")</code> <i>can be configured with the
	 * environment variable</i> <code>ORG_TEST_PROCESS_SPECIAL</code>. To take advantage of the
	 * "dsf-tools-documentation-generator" maven plugin to generate a markdown file with configuration options for the
	 * plugin also add the {@link ProcessDocumentation} annotation.
	 *
	 * @return {@link Configuration} annotated classes, defining {@link Bean} annotated factory methods
	 * @see ExecutionListener
	 * @see MessageEndEvent
	 * @see MessageIntermediateThrowEvent
	 * @see MessageSendTask
	 * @see ServiceTask
	 * @see UserTaskListener
	 * @see ActivityPrototypeBeanCreator
	 * @see ProcessPluginDeploymentListener
	 * @see FhirResourceModifier
	 * @see ConfigurableBeanFactory#SCOPE_PROTOTYPE
	 */
	List<Class<?>> getSpringConfigurations();

	/**
	 * @return location of the optional plugin description markdown file
	 */
	default String getDescriptionFile()
	{
		return "doc/description.md";
	}

	/**
	 * @return location of the optional plugin configuration markdown file
	 */
	default String getConfigurationFile()
	{
		return "doc/configuration.md";
	}

	/**
	 * If the license file exists, {@link #getLicense()} is expected to return {@link License#Other}.
	 *
	 * @return location of the optional plugin license markdown file
	 */
	default String getLicenseFile()
	{
		return "doc/license.md";
	}
}
