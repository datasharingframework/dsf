package dev.dsf.bpe.v1;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A provider configuration file named "dev.dsf.ProcessPluginDefinition" containing the canonical name of the class
 * implementing this interface needs to be part of the process plugin at "/META-INF/services/". For more details on the
 * content of the provider configuration file, see {@link ServiceLoader}.
 */
public interface ProcessPluginDefinition
{
	String RESOURCE_VERSION_PATTERN_STRING = "(?<resourceVersion>\\d+\\.\\d+)";
	String PLUGIN_VERSION_PATTERN_STRING = "(?<pluginVersion>" + RESOURCE_VERSION_PATTERN_STRING + "\\.\\d+\\.\\d+)";
	Pattern PLUGIN_VERSION_PATTERN = Pattern.compile(PLUGIN_VERSION_PATTERN_STRING);

	/**
	 * @return process plugin name, same as jar name excluding suffix <code>-&lt;version&gt;.jar</code>
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
	 * <i>Return <code>List.of("foo.bpmn");</code> for a foo.bpmn file located in the root folder of the process plugin
	 * jar. The returned files will be read via {@link ClassLoader#getResourceAsStream(String)}.</i>
	 * <p>
	 * <i>Occurrences of</i> <code>#{version}</code> <i>will be replaced with the value of
	 * {@link #getResourceVersion()}<br>
	 * Occurrences of</i> <code>#{date}</code> </i>will be replaced with the value of
	 * {@link #getResourceReleaseDate()}<br>
	 * Occurrences of</i> <code>#{organization}</code> </i>will be replaced with the local organization DSF identifier
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
	 * <i>All services defined in {@link ProcessPluginApi} and {@link ProcessPluginApi} itself can be {@link Autowired}
	 * in {@link Configuration} classes</i>
	 *
	 * @return {@link Configuration} annotated classes, defining {@link Bean} annotated factory methods
	 */
	List<Class<?>> getSpringConfigurations();
}
