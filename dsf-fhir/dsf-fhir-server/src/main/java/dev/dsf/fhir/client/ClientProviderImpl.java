/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.client;

import java.security.KeyStore;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.buildinfo.BuildInfoReader;
import dev.dsf.common.config.ProxyConfig;
import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.service.ReferenceCleaner;

public class ClientProviderImpl implements ClientProvider
{
	private static final Logger logger = LoggerFactory.getLogger(ClientProviderImpl.class);
	private static final String USER_AGENT_VALUE = "DSF/";

	private final KeyStore webserviceTrustStore;
	private final KeyStore webserviceKeyStore;
	private final char[] webserviceKeyStorePassword;

	private final Duration remoteReadTimeout;
	private final Duration remoteConnectTimeout;
	private final ProxyConfig proxyConfig;
	private final boolean logRequests;
	private final FhirContext fhirContext;
	private final ReferenceCleaner referenceCleaner;
	private final EndpointDao endpointDao;
	private final ExceptionHandler exceptionHandler;
	private final BuildInfoReader buildInfoReader;

	/**
	 * @param webserviceTrustStore
	 *            not <code>null</code>
	 * @param webserviceKeyStore
	 *            not <code>null</code>
	 * @param webserviceKeyStorePassword
	 *            not <code>null</code>
	 * @param remoteReadTimeout
	 *            not <code>null</code>, not negative, 0 represents infinity
	 * @param remoteConnectTimeout
	 *            not <code>null</code>, not negative, 0 represents infinity
	 * @param proxyConfig
	 *            not <code>null</code>
	 * @param logRequests
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param referenceCleaner
	 *            not <code>null</code>
	 * @param endpointDao
	 *            not <code>null</code>
	 * @param exceptionHandler
	 *            not <code>null</code>
	 * @param buildInfoReader
	 *            not <code>null</code>
	 */
	public ClientProviderImpl(KeyStore webserviceTrustStore, KeyStore webserviceKeyStore,
			char[] webserviceKeyStorePassword, Duration remoteReadTimeout, Duration remoteConnectTimeout,
			ProxyConfig proxyConfig, boolean logRequests, FhirContext fhirContext, ReferenceCleaner referenceCleaner,
			EndpointDao endpointDao, ExceptionHandler exceptionHandler, BuildInfoReader buildInfoReader)
	{
		this.webserviceTrustStore = Objects.requireNonNull(webserviceTrustStore, "webserviceTrustStore");
		this.webserviceKeyStore = Objects.requireNonNull(webserviceKeyStore, "webserviceKeyStore");
		this.webserviceKeyStorePassword = Objects.requireNonNull(webserviceKeyStorePassword,
				"webserviceKeyStorePassword");
		this.remoteReadTimeout = Objects.requireNonNull(remoteReadTimeout, "remoteReadTimeout");
		if (remoteReadTimeout.isNegative())
			throw new IllegalArgumentException("remoteReadTimeout negative");
		this.remoteConnectTimeout = Objects.requireNonNull(remoteConnectTimeout, "remoteConnectTimeout");
		if (remoteConnectTimeout.isNegative())
			throw new IllegalArgumentException("remoteConnectTimeout negative");
		this.proxyConfig = Objects.requireNonNull(proxyConfig, "proxyConfig");
		this.logRequests = logRequests;
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
		this.referenceCleaner = Objects.requireNonNull(referenceCleaner, "referenceCleaner");
		this.endpointDao = Objects.requireNonNull(endpointDao, "endpointDao");
		this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		this.buildInfoReader = Objects.requireNonNull(buildInfoReader, "buildInfoReader");
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
					webserviceKeyStore, webserviceKeyStorePassword, null, proxyUrl, proxyUsername, proxyPassword,
					remoteConnectTimeout, remoteReadTimeout, logRequests,
					USER_AGENT_VALUE + buildInfoReader.getProjectVersion(), fhirContext, referenceCleaner);

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
