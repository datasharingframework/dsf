package dev.dsf.fhir.client;

import java.security.KeyStore;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.config.ProxyConfig;
import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.service.ReferenceCleaner;

public class ClientProviderImpl implements ClientProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ClientProviderImpl.class);

	private final KeyStore webserviceTrustStore;
	private final KeyStore webserviceKeyStore;
	private final char[] webserviceKeyStorePassword;

	private final int remoteReadTimeout;
	private final int remoteConnectTimeout;
	private ProxyConfig proxyConfig;
	private final boolean logRequests;
	private final FhirContext fhirContext;
	private final ReferenceCleaner referenceCleaner;
	private final EndpointDao endpointDao;
	private final ExceptionHandler exceptionHandler;

	public ClientProviderImpl(KeyStore webserviceTrustStore, KeyStore webserviceKeyStore,
			char[] webserviceKeyStorePassword, int remoteReadTimeout, int remoteConnectTimeout, ProxyConfig proxyConfig,
			boolean logRequests, FhirContext fhirContext, ReferenceCleaner referenceCleaner, EndpointDao endpointDao,
			ExceptionHandler exceptionHandler)
	{
		this.webserviceTrustStore = webserviceTrustStore;
		this.webserviceKeyStore = webserviceKeyStore;
		this.webserviceKeyStorePassword = webserviceKeyStorePassword;
		this.remoteReadTimeout = remoteReadTimeout;
		this.remoteConnectTimeout = remoteConnectTimeout;
		this.proxyConfig = proxyConfig;
		this.logRequests = logRequests;
		this.fhirContext = fhirContext;
		this.referenceCleaner = referenceCleaner;
		this.endpointDao = endpointDao;
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(webserviceTrustStore, "webserviceTrustStore");
		Objects.requireNonNull(webserviceKeyStore, "webserviceKeyStore");
		Objects.requireNonNull(webserviceKeyStorePassword, "webserviceKeyStorePassword");

		Objects.requireNonNull(proxyConfig, "proxyConfig");

		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(referenceCleaner, "referenceCleaner");
		Objects.requireNonNull(endpointDao, "endpointDao");
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
	}

	@Override
	public Optional<FhirWebserviceClient> getClient(String serverBase)
	{
		if (endpointExists(serverBase))
		{
			String proxyUrl = proxyConfig.isEnabled(serverBase) ? proxyConfig.getUrl() : null;
			String proxyUsername = proxyConfig.isEnabled(serverBase) ? proxyConfig.getUsername() : null;
			char[] proxyPassword = proxyConfig.isEnabled(serverBase) ? proxyConfig.getPassword() : null;

			FhirWebserviceClient client = new FhirWebserviceClientJersey(serverBase, webserviceTrustStore,
					webserviceKeyStore, webserviceKeyStorePassword, proxyUrl, proxyUsername, proxyPassword,
					remoteConnectTimeout, remoteReadTimeout, logRequests, null, fhirContext, referenceCleaner);

			return Optional.of(client);
		}
		else
			return Optional.empty();
	}

	@Override
	public boolean endpointExists(String serverBase)
	{
		boolean endpointExists = exceptionHandler
				.handleSqlException(() -> endpointDao.existsActiveNotDeletedByAddress(serverBase));

		if (!endpointExists)
			logger.warn("No active, not deleted Endpoint with address {} found", serverBase);

		return endpointExists;
	}
}
