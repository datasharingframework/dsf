package dev.dsf.maven.dev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
 * Generates certificates for a local DSF development setup.
 * <p>
 * This goal creates all required certificate files (client, server, CA chain) and copies them to the configured target
 * directories.
 */
@Mojo(name = "generate-dev-setup-cert-files", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true, aggregator = true)
public class GenerateDevSetupCertFilesMojo extends AbstractMojo
{
	/**
	 * The base directory of the project.
	 */
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File projectBasedir;

	/**
	 * The text encoding.
	 */
	@Parameter(defaultValue = "${project.build.sourceEncoding}", readonly = true, required = true)
	private String encoding;

	/**
	 * The directory to write the generated certificate files to.
	 */
	@Parameter(required = true, property = "dsf.certDir", defaultValue = "cert")
	private File certDir;

	/**
	 * The password to protect the private keys.
	 */
	@Parameter(required = true, property = "dsf.privateKeyPassword", defaultValue = "password")
	private String privateKeyPassword;

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

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("projectBasedir: " + projectBasedir);
		getLog().debug("encoding: " + encoding);
		getLog().debug("certDir: " + certDir);
		getLog().debug("privateKeyPassword: "
				+ (privateKeyPassword == null ? null : !privateKeyPassword.isEmpty() ? "***" : ""));
		getLog().debug("certs: " + certs);
		getLog().debug("rootCa: " + rootCa);
		getLog().debug("issuingCa: " + issuingCa);
		getLog().debug("caChain: " + caChain);
		getLog().debug("templates: " + templates);

		if (privateKeyPassword == null)
			throw new MojoExecutionException("privateKeyPassword null");

		try
		{
			Files.createDirectories(certDir.toPath());
		}
		catch (IOException e)
		{
			throw new MojoFailureException(e);
		}

		CertificateGenerator certificateGenerator = new CertificateGenerator(certDir.toPath(),
				privateKeyPassword.toCharArray(), certs.stream().map(Cert::toCertificationRequestConfig).toList());
		CertificateWriter certificateWriter = new CertificateWriter(projectBasedir.toPath(), certificateGenerator,
				privateKeyPassword.toCharArray());
		TemplateHandler templateHandler = new TemplateHandler(projectBasedir.toPath(), certificateGenerator, encoding);

		certificateGenerator.initialize();

		try
		{
			certificateWriter.write(certs);
			certificateWriter.write(rootCa);
			certificateWriter.write(issuingCa);
			certificateWriter.write(caChain);

			templateHandler.applyTemplates(templates);
		}
		catch (RuntimeIOException e)
		{
			throw new MojoFailureException(e);
		}
	}
}
