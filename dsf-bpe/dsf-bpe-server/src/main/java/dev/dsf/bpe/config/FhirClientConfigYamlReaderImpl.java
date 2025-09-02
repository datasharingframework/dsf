package dev.dsf.bpe.config;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dev.dsf.bpe.api.config.FhirClientConfig;
import dev.dsf.bpe.api.config.FhirClientConfig.BasicAuthentication;
import dev.dsf.bpe.api.config.FhirClientConfig.BearerAuthentication;
import dev.dsf.bpe.api.config.FhirClientConfig.CertificateAuthentication;
import dev.dsf.bpe.api.config.FhirClientConfig.OidcAuthentication;
import dev.dsf.bpe.api.config.FhirClientConfigs;
import dev.dsf.bpe.config.FhirClientConfigImpl.BasicAuthenticationImpl;
import dev.dsf.bpe.config.FhirClientConfigImpl.BearerAuthenticationImpl;
import dev.dsf.bpe.config.FhirClientConfigImpl.CertificateAuthenticationImpl;
import dev.dsf.bpe.config.FhirClientConfigImpl.OidcAuthenticationImpl;
import dev.dsf.bpe.config.FhirClientConfigYaml.BasicAuth;
import dev.dsf.bpe.config.FhirClientConfigYaml.BearerAuth;
import dev.dsf.bpe.config.FhirClientConfigYaml.CertificateAuth;
import dev.dsf.bpe.config.FhirClientConfigYaml.OidcAuth;

public class FhirClientConfigYamlReaderImpl implements InitializingBean, FhirClientConfigYamlReader
{
	@FunctionalInterface
	private static interface SupplierWithIOException<T>
	{
		T get() throws IOException;
	}

	private static final class RuntimeIOException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public RuntimeIOException(IOException cause)
		{
			super(cause);
		}

		@Override
		public IOException getCause()
		{
			return (IOException) super.getCause();
		}
	}

	private final YAMLMapper mapper = YAMLMapper.builder().addModule(new JavaTimeModule()).build();

	private final boolean defaultTestConnectionOnStartup;
	private final boolean defaultEnableDebugLogging;
	private final Duration defaultConnectTimeout;
	private final Duration defaultReadTimeout;
	private final KeyStore defaultTrustStore;
	private final String defaultOidcDiscoveryPath;

	public FhirClientConfigYamlReaderImpl(boolean defaultTestConnectionOnStartup, boolean defaultEnableDebugLogging,
			Duration defaultConnectTimeout, Duration defaultReadTimeout, KeyStore defaultTrustStore,
			String defaultOidcDiscoveryPath)
	{
		this.defaultTestConnectionOnStartup = defaultTestConnectionOnStartup;
		this.defaultEnableDebugLogging = defaultEnableDebugLogging;
		this.defaultConnectTimeout = defaultConnectTimeout;
		this.defaultReadTimeout = defaultReadTimeout;
		this.defaultTrustStore = defaultTrustStore;
		this.defaultOidcDiscoveryPath = defaultOidcDiscoveryPath != null && !defaultOidcDiscoveryPath.startsWith("/")
				? ("/" + defaultOidcDiscoveryPath)
				: defaultOidcDiscoveryPath;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(defaultConnectTimeout, "defaultConnectTimeout");
		Objects.requireNonNull(defaultReadTimeout, "defaultReadTimeout");
		Objects.requireNonNull(defaultTrustStore, "defaultTrustStore");
		Objects.requireNonNull(defaultOidcDiscoveryPath, "defaultOidcDiscoveryPath");
	}

	@Override
	public FhirClientConfigs readConfigs(String yaml) throws IOException, ConfigInvalidException
	{
		Objects.requireNonNull(yaml, "yaml");

		if (yaml.isBlank())
			return FhirClientConfigsImpl.empty(defaultTrustStore);

		try (Reader reader = new StringReader(yaml))
		{
			return readConfigs(reader);
		}
	}

	@Override
	public FhirClientConfigs readConfigs(Reader yaml) throws IOException, ConfigInvalidException
	{
		Objects.requireNonNull(yaml, "yaml");

		Map<String, FhirClientConfigYaml> yConfigs = mapper.readValue(yaml, FhirClientConfigYaml.MAP_OF_CONFIGS);

		List<ValidationError> validationErrors = yConfigs.entrySet().stream().flatMap(e ->
		{
			if (e.getValue() == null)
				return Stream.of(new ConfigValidationError(e.getKey(), "not configured"));
			else
				return e.getValue().validate(e.getKey());
		}).toList();

		if (!validationErrors.isEmpty())
			throw new ConfigInvalidException(validationErrors);

		try
		{
			Map<String, FhirClientConfig> configs = yConfigs.entrySet().stream().map(this::toConfig)
					.collect(Collectors.toMap(FhirClientConfig::fhirServerId, Function.identity()));

			return new FhirClientConfigsImpl(configs, defaultTrustStore);
		}
		catch (RuntimeIOException e)
		{
			throw e.getCause();
		}
	}

	private <T> T valueOrDefault(SupplierWithIOException<T> configValue, T defaultValue) throws IOException
	{
		T value = configValue.get();
		return value != null ? value : defaultValue;
	}

	public FhirClientConfig toConfig(Entry<String, FhirClientConfigYaml> entry) throws RuntimeIOException
	{
		final String fhirServerId = entry.getKey();
		final FhirClientConfigYaml yConfig = entry.getValue();

		try
		{
			return new FhirClientConfigImpl(fhirServerId, yConfig.baseUrl(),
					valueOrDefault(yConfig::startupConnectionTestEnabled, defaultTestConnectionOnStartup),
					valueOrDefault(yConfig::debugLoggingEnabled, defaultEnableDebugLogging),
					valueOrDefault(yConfig::connectTimeout, defaultConnectTimeout),
					valueOrDefault(yConfig::readTimeout, defaultReadTimeout),
					valueOrDefault(yConfig::readTrustStore, defaultTrustStore),
					toCertificateAuthentication(yConfig.certAuth()), toBasicAuthentication(yConfig.basicAuth()),
					toBearerAuthentication(yConfig.bearerAuth()), toOidcAuthentication(yConfig.oidcAuth()));
		}
		catch (IOException e)
		{
			throw new RuntimeIOException(e);
		}
	}

	public CertificateAuthentication toCertificateAuthentication(CertificateAuth certAuth) throws IOException
	{
		if (certAuth == null)
			return null;
		else
			return new CertificateAuthenticationImpl(certAuth.readKeyStore(), certAuth.readPassword());
	}

	public BasicAuthentication toBasicAuthentication(BasicAuth basicAuth) throws IOException
	{
		if (basicAuth == null)
			return null;

		return new BasicAuthenticationImpl(basicAuth.username(), basicAuth.readPassword());
	}

	public BearerAuthentication toBearerAuthentication(BearerAuth bearerAuth) throws IOException
	{
		if (bearerAuth == null)
			return null;

		return new BearerAuthenticationImpl(bearerAuth.readToken());
	}

	public OidcAuthentication toOidcAuthentication(OidcAuth oidcAuth) throws IOException
	{
		if (oidcAuth == null)
			return null;

		return new OidcAuthenticationImpl(oidcAuth.baseUrl(),
				valueOrDefault(oidcAuth::discoveryPath, defaultOidcDiscoveryPath),
				valueOrDefault(oidcAuth::startupConnectionTestEnabled, defaultTestConnectionOnStartup),
				valueOrDefault(oidcAuth::debugLoggingEnabled, defaultEnableDebugLogging),
				valueOrDefault(oidcAuth::connectTimeout, defaultConnectTimeout),
				valueOrDefault(oidcAuth::readTimeout, defaultReadTimeout),
				valueOrDefault(oidcAuth::readTrustStore, defaultTrustStore), oidcAuth.clientId(),
				oidcAuth.readClientSecret());
	}
}
