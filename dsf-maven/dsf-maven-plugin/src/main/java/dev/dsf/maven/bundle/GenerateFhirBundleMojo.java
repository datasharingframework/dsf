package dev.dsf.maven.bundle;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generate-fhir-bundle", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class GenerateFhirBundleMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File projectBaseDir;

	@Parameter(defaultValue = "dsf.baseFolder", required = true)
	private File baseFolder;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		new BundleGenerator(projectBaseDir.toPath(), baseFolder.toPath()).generateAndSaveBundle();
	}
}
