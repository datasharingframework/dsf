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

/**
 * Cleans up certificate files for a local DSF development setup.
 * <p>
 * This goal deletes all generated certificate files (client, server, CA chain) from the configured target directories.
 */
@Mojo(name = "clean-dev-setup-cert-files", defaultPhase = LifecyclePhase.CLEAN, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true, aggregator = true)
public class CleanDevSetupCertFilesMojo extends AbstractMojo
{
	/**
	 * The base directory of the project.
	 */
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File projectBasedir;

	/**
	 * The directory to write the generated certificate files to.
	 */
	@Parameter(required = true, property = "dsf.certDir", defaultValue = "cert")
	private File certDir;

	/**
	 * The certificates to generate. See <a href="index.html">usage</a> for details.
	 */
	@Parameter
	private List<Cert> certs;

	/**
	 * The root CA configuration.
	 */
	@Parameter
	private RootCa rootCa;

	/**
	 * The issuing CA configuration.
	 */
	@Parameter
	private IssuingCa issuingCa;

	/**
	 * The CA chain configuration.
	 */
	@Parameter
	private CaChain caChain;

	/**
	 * The templates to apply. See <a href="index.html">usage</a> for details.
	 */
	@Parameter
	private List<Template> templates;

	/**
	 * Whether to delete the cert directory with its contents as well.
	 */
	@Parameter(required = true, property = "dsf.includeCertDir", defaultValue = "false")
	private boolean includeCertDir;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("projectBasedir: " + projectBasedir);
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
