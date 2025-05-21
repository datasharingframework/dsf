package dev.dsf.maven.ca;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generate-default-ca-files", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class GenerateDefaultCaFilesMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File projectBasedir;

	@Parameter(property = "dsf.certFolder", required = true)
	private File certFolder;

	@Parameter(property = "dsf.clientOnlyCaCommonNames", required = true)
	private List<String> clientOnlyCaCommonNames;

	@Parameter(property = "dsf.clientCertIssuingCaFiles", required = true)
	private List<File> clientCertIssuingCaFiles;

	@Parameter(property = "dsf.clientCertCaChainFiles", required = true)
	private List<File> clientCertCaChainFiles;

	@Parameter(property = "dsf.serverCertRootCaFiles", required = true)
	private List<File> serverCertRootCaFiles;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("projectBasedir: " + projectBasedir);
		getLog().debug("certFolder: " + certFolder);
		getLog().debug("clientOnlyCaCommonNames: " + clientOnlyCaCommonNames);
		getLog().debug("clientCertIssuingCaFiles: " + clientCertIssuingCaFiles);
		getLog().debug("clientCertCaChainFiles: " + clientCertCaChainFiles);
		getLog().debug("serverCertRootCaFiles: " + serverCertRootCaFiles);

		try
		{
			new DefaultCaFilesGenerator(projectBasedir.toPath(), certFolder.toPath(), clientOnlyCaCommonNames)
					.createFiles(clientCertIssuingCaFiles.stream().map(File::toPath),
							clientCertCaChainFiles.stream().map(File::toPath),
							serverCertRootCaFiles.stream().map(File::toPath));
		}
		catch (IOException e)
		{
			throw new MojoFailureException(e);
		}
	}
}
