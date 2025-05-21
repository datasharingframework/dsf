package dev.dsf.maven.config;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generate-config-doc", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class GenerateConfigDocMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
	private String projectBuildDirectory;

	@Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
	private List<String> compileClasspathElements;

	@Parameter(property = "dsf.configDocPackages", required = true)
	private List<String> configDocPackages;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("projectBuildDirectory: " + projectBuildDirectory);
		getLog().debug("compileClasspathElements: " + compileClasspathElements);
		getLog().debug("configDocPackages: " + configDocPackages);

		new ConfigDocGenerator(projectBuildDirectory, compileClasspathElements)
				.generateDocumentation(configDocPackages);
	}
}
