package dev.dsf.maven.dev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileRemover extends AbstractIo
{
	private static final Logger logger = LoggerFactory.getLogger(FileRemover.class);

	private final Path projectBasedir;
	private final Path certDir;

	public FileRemover(Path projectBasedir, Path certDir)
	{
		this.projectBasedir = Objects.requireNonNull(projectBasedir, "projectBasedir");
		this.certDir = Objects.requireNonNull(certDir, "certDir");
	}

	public void deleteCerts(List<Cert> certs)
	{
		if (certs != null)
			certs.forEach(this::delete);
	}

	private void delete(Cert cert)
	{
		if (cert == null)
			return;

		cert.getTargets().stream().filter(Objects::nonNull).map(File::toPath).forEach(target ->
		{
			if (target.getFileName().toString().endsWith(".crt"))
				toRuntimeException(() -> delete(target));
			else if (target.getFileName().toString().endsWith(".key"))
				toRuntimeException(() -> delete(target));
			else if (target.getFileName().toString().endsWith(".key.plain"))
				toRuntimeException(() -> delete(target));
			else if (target.getFileName().toString().endsWith(".p12"))
				toRuntimeException(() -> delete(target));
			else
				logger.warn("Cert (cn: {}) target filetype not supported: {}", cert.getCn(), target.getFileName());
		});
	}

	public void delete(RootCa rootCa)
	{
		if (rootCa == null)
			return;

		rootCa.getTargets().stream().filter(Objects::nonNull).map(File::toPath).forEach(target ->
		{
			if (target.getFileName().toString().endsWith(".crt"))
				toRuntimeException(() -> delete(target));
			else if (target.getFileName().toString().endsWith(".jks"))
				toRuntimeException(() -> delete(target));
			else
				logger.warn("RootCa target filetype not supported: {}", target.getFileName());
		});
	}

	public void delete(IssuingCa issuingCa)
	{
		if (issuingCa == null)
			return;

		issuingCa.getTargets().stream().filter(Objects::nonNull).map(File::toPath).forEach(target ->
		{
			if (target.getFileName().toString().endsWith(".crt"))
				toRuntimeException(() -> delete(target));
			else if (target.getFileName().toString().endsWith(".jks"))
				toRuntimeException(() -> delete(target));
			else
				logger.warn("IssuingCa target filetype not supported: {}", target.getFileName());
		});
	}

	public void delete(CaChain caChain)
	{
		if (caChain == null)
			return;

		caChain.getTargets().stream().filter(Objects::nonNull).map(File::toPath).forEach(target ->
		{
			if (target.getFileName().toString().endsWith(".crt"))
				toRuntimeException(() -> delete(target));
			else if (target.getFileName().toString().endsWith(".jks"))
				toRuntimeException(() -> delete(target));
			else
				logger.warn("CaChain target filetype not supported: {}", target.getFileName());
		});
	}

	public void deleteTemplates(List<Template> templates)
	{
		if (templates != null)
			templates.forEach(this::delete);
	}

	private void delete(Template template)
	{
		if (template != null && template.getTarget() != null)
			toRuntimeException(() -> delete(template.getTarget().toPath()));
	}

	private void delete(Path target) throws IOException
	{
		if (Files.exists(target))
		{
			logger.info("Deleting {}", projectBasedir.relativize(target));
			Files.deleteIfExists(target);
		}
	}

	private Path toPath(String commonName, String postFix)
	{
		return certDir.resolve(commonName.replaceAll(" ", "_") + postFix);
	}

	public void deleteFilesInCertDir(List<Cert> certs)
	{
		Stream<String> commonNamesToDelete = Stream.concat(
				certs == null ? Stream.empty() : certs.stream().map(Cert::getCn).filter(Objects::nonNull),
				Stream.of(CertificateGenerator.SUBJECT_CN_ISSUING_CA, CertificateGenerator.SUBJECT_CN_ROOT_CA));

		commonNamesToDelete.forEach(cn ->
		{
			toRuntimeException(() -> delete(toPath(cn, CertificateGenerator.POSTFIX_PRIVATE_KEY)));
			toRuntimeException(() -> delete(toPath(cn, CertificateGenerator.POSTFIX_CERTIFICATE)));
		});
	}
}
