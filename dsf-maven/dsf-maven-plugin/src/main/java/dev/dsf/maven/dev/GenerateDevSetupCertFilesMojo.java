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

@Mojo(name = "generate-dev-setup-cert-files", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true, aggregator = true)
public class GenerateDevSetupCertFilesMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File projectBasedir;

	@Parameter(defaultValue = "${project.build.sourceEncoding}", readonly = true, required = true)
	private String encoding;

	@Parameter(required = true, property = "dsf.certDir", defaultValue = "cert")
	private File certDir;

	@Parameter(required = true, property = "dsf.privateKeyPassword", defaultValue = "password")
	private String privateKeyPassword;

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

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("certDir: " + certDir);
		getLog().debug("certs: " + certs);
		getLog().debug("rootCa: " + rootCa);
		getLog().debug("issuingCa: " + issuingCa);
		getLog().debug("caChain: " + caChain);
		getLog().debug("templates: " + templates);

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
