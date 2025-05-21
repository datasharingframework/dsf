package dev.dsf.maven.dev;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import dev.dsf.maven.exception.RuntimeIOException;

@Mojo(name = "clean-dev-setup-cert-files", defaultPhase = LifecyclePhase.CLEAN, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true, aggregator = true)
public class CleanDevSetupCertFilesMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File projectBasedir;

	@Parameter(defaultValue = "${project.build.sourceEncoding}", readonly = true, required = true)
	private String encoding;

	@Parameter(required = true, property = "dsf.certDir", defaultValue = "cert")
	private File certDir;

	@Parameter
	private List<Cert> certs;

	@Parameter
	private RootCa rootCa;

	@Parameter
	private IssuingCa issuingCa;

	@Parameter
	private CaChain caChain;

	@Parameter
	private List<Template> templates;

	@Parameter(required = true, property = "dsf.includeCertDir", defaultValue = "false")
	private boolean includeCertDir;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("certDir: " + certDir);
		getLog().debug("certs: " + certs);
		getLog().debug("rootCa: " + rootCa);
		getLog().debug("issuingCa: " + issuingCa);
		getLog().debug("caChain: " + caChain);
		getLog().debug("templates: " + templates);
		getLog().debug("includeCertDir: " + includeCertDir);

		FileRemover fileRemover = new FileRemover(projectBasedir.toPath(), certDir.toPath());

		try
		{
			fileRemover.deleteCerts(certs);
			fileRemover.delete(rootCa);
			fileRemover.delete(issuingCa);
			fileRemover.delete(caChain);
			fileRemover.deleteTemplates(templates);

			if (includeCertDir)
				fileRemover.deleteFilesInCertDir(certs);
		}
		catch (RuntimeIOException e)
		{
			throw new MojoFailureException(e);
		}
	}
}
