package dev.dsf.bpe.plugin;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import dev.dsf.bpe.process.ProcessKeyAndVersion;
import dev.dsf.bpe.process.ProcessStateChangeOutcome;

public interface ProcessPluginProvider
{
	/**
	 * Expected folder/file structure:
	 *
	 * <pre>
	 * pluginDirectory/foo_plugin.jar
	 *
	 * pluginDirectory/bar_plugin.jar
	 *
	 * pluginDirectory/baz_plugin.jar
	 * </pre>
	 *
	 * The folder/file structure above will result in three separate class loaders being used.
	 *
	 * @return plugin definitions found in jar files within a pluginDirectory
	 */
	List<ProcessPluginDefinitionAndClassLoader> getDefinitions();

	/**
	 * @return definitions by {@link ProcessKeyAndVersion}
	 * @see #getDefinitions()
	 */
	Map<ProcessKeyAndVersion, ProcessPluginDefinitionAndClassLoader> getDefinitionByProcessKeyAndVersion();

	/**
	 * @return class loaders for process plugin jars by process definition key / version
	 * @see #getDefinitions()
	 */
	Map<ProcessKeyAndVersion, ClassLoader> getClassLoadersByProcessDefinitionKeyAndVersion();

	/**
	 * @return application contexts for process plugin jars by process definition key / version
	 * @see #getDefinitions()
	 */
	Map<ProcessKeyAndVersion, ApplicationContext> getApplicationContextsByProcessDefinitionKeyAndVersion();

	List<ProcessKeyAndVersion> getProcessKeyAndVersions();

	List<ProcessKeyAndVersion> getDraftProcessKeyAndVersions();

	void onProcessesDeployed(List<ProcessStateChangeOutcome> changes);
}
