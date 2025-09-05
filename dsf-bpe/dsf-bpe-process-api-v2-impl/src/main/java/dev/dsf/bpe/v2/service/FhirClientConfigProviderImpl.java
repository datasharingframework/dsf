package dev.dsf.bpe.v2.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.springframework.beans.factory.InitializingBean;

import de.hsheilbronn.mi.utils.crypto.context.SSLContextFactory;
import dev.dsf.bpe.v2.client.fhir.ClientConfig;
import dev.dsf.bpe.v2.client.fhir.ClientConfigs;

public class FhirClientConfigProviderImpl implements FhirClientConfigProvider, InitializingBean
{
	private final Map<String, ClientConfig> clientConfigsByFhirServerId = new HashMap<>();
	private final KeyStore defaultTrustStore;

	public FhirClientConfigProviderImpl(KeyStore defaultTrustStore, ClientConfigs clientConfigs)
	{
		this.defaultTrustStore = defaultTrustStore;

		if (clientConfigs != null)
			clientConfigsByFhirServerId.putAll(clientConfigs.getConfigs().stream()
					.collect(Collectors.toMap(ClientConfig::getFhirServerId, Function.identity())));
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(defaultTrustStore, "defaultTrustStore");
	}

	@Override
	public SSLContext createDefaultSslContext()
	{
		try
		{
			return SSLContextFactory.createSSLContext(defaultTrustStore);
		}
		catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public KeyStore createDefaultTrustStore()
	{
		try
		{
			char[] password = UUID.randomUUID().toString().toCharArray();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			defaultTrustStore.store(out, password);

			KeyStore store = KeyStore.getInstance(defaultTrustStore.getType(), defaultTrustStore.getProvider());
			store.load(new ByteArrayInputStream(out.toByteArray()), password);

			return store;
		}
		catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<ClientConfig> getClientConfig(String fhirServerId)
	{
		if (fhirServerId == null || fhirServerId.isBlank())
			return Optional.empty();

		return Optional.ofNullable(clientConfigsByFhirServerId.get(fhirServerId));
	}
}
