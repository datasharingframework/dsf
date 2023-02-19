package dev.dsf.fhir.integration;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import dev.dsf.common.jetty.AbstractJettyConfig;
import dev.dsf.common.jetty.JettyConfig;
import dev.dsf.fhir.integration.X509Certificates.ClientCertificate;

public class TestJettyConfig extends AbstractJettyConfig implements JettyConfig
{
	private final int statusPort;
	private final int port;
	private final String contextPath;
	private final Path clientTrustStorePath;
	private final Path serverTrustStorePath;
	private final Path serverKeyStorePath;
	private final char[] serverKeyStorePassword;
	private final Path log4JConfigPath;

	private final Properties additionalProperties = new Properties();

	public TestJettyConfig(int statusPort, int port, String contextPath, Path clientTrustStorePath,
			Path serverTrustStorePath, Path serverKeyStorePath, char[] serverKeyStorePassword, Path log4JConfigPath,
			String databaseUrl, String databaseUserUsername, String databaseUserPassword,
			String databaseDeleteUserUsername, String databaseDeleteUserPassword, String serverBaseUrl,
			ClientCertificate clientCertificate, String organizationIdentifierValue, Path fhirBundleFile,
			Path caCertificateFile, Path clientCertificateFile, Path clientCertificatePrivateKeyFile,
			char[] clientCertificatePrivateKeyPassword)
	{
		super(AbstractJettyConfig.httpsConnector());

		this.statusPort = statusPort;
		this.port = port;
		this.contextPath = contextPath;
		this.clientTrustStorePath = clientTrustStorePath;
		this.serverTrustStorePath = serverTrustStorePath;
		this.serverKeyStorePath = serverKeyStorePath;
		this.serverKeyStorePassword = serverKeyStorePassword;
		this.log4JConfigPath = log4JConfigPath;

		additionalProperties.put("dev.dsf.fhir.db.url", databaseUrl);
		additionalProperties.put("dev.dsf.fhir.db.user.username", databaseUserUsername);
		additionalProperties.put("dev.dsf.fhir.db.user.password", databaseUserPassword);
		additionalProperties.put("dev.dsf.fhir.db.user.permanent.delete.username", databaseDeleteUserUsername);
		additionalProperties.put("dev.dsf.fhir.db.user.permanent.delete.password", databaseDeleteUserPassword);
		additionalProperties.put("dev.dsf.fhir.server.base.url", serverBaseUrl);
		additionalProperties.put("dev.dsf.fhir.server.user.thumbprints",
				clientCertificate.getCertificateSha512ThumbprintHex());
		additionalProperties.put("dev.dsf.fhir.server.user.thumbprints.permanent.delete",
				clientCertificate.getCertificateSha512ThumbprintHex());
		additionalProperties.put("dev.dsf.fhir.server.organization.identifier.value", organizationIdentifierValue);
		additionalProperties.put("dev.dsf.fhir.server.init.bundle", fhirBundleFile.toString());
		additionalProperties.put("dev.dsf.fhir.client.trust.certificates", caCertificateFile.toString());
		additionalProperties.put("dev.dsf.fhir.client.certificate", clientCertificateFile.toString());
		additionalProperties.put("dev.dsf.fhir.client.certificate.private.key",
				clientCertificatePrivateKeyFile.toString());
		additionalProperties.put("dev.dsf.fhir.client.certificate.private.key.password",
				String.valueOf(clientCertificatePrivateKeyPassword));
		additionalProperties.put("dev.dsf.fhir.server.roleConfig", "");
	}

	@Override
	public Optional<Integer> getStatusPort()
	{
		return Optional.of(statusPort);
	}

	@Override
	public Optional<Integer> getPort()
	{
		return Optional.of(port);
	}

	@Override
	public Optional<String> getContextPath()
	{
		return Optional.of(contextPath);
	}

	@Override
	public Optional<Path> getClientTrustStorePath()
	{
		return Optional.of(clientTrustStorePath);
	}

	@Override
	public Optional<Path> getServerTrustStorePath()
	{
		return Optional.of(serverTrustStorePath);
	}

	@Override
	public Optional<Path> getServerKeyStorePath()
	{
		return Optional.of(serverKeyStorePath);
	}

	@Override
	public Optional<char[]> getServerKeyStorePassword()
	{
		return Optional.of(serverKeyStorePassword);
	}

	@Override
	public Optional<Path> getLog4JConfigPath()
	{
		return Optional.of(log4JConfigPath);
	}

	@Override
	public Map<String, String> getAllProperties()
	{
		Map<String, String> all = new HashMap<>(super.getAllProperties());
		additionalProperties.forEach((k, v) -> all.put(Objects.toString(k), Objects.toString(v)));
		return all;
	}
}
