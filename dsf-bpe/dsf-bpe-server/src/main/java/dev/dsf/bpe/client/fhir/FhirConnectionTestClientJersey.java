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
package dev.dsf.bpe.client.fhir;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.bpe.api.client.oidc.OidcClient;
import dev.dsf.bpe.api.config.FhirClientConfig;
import dev.dsf.bpe.api.service.BpeOidcClientProvider;
import dev.dsf.bpe.client.dsf.FhirAdapter;
import dev.dsf.common.config.ProxyConfig;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class FhirConnectionTestClientJersey implements FhirConnectionTestClient
{
	private static final Logger logger = LoggerFactory.getLogger(FhirConnectionTestClientJersey.class);

	private static final java.util.logging.Logger requestDebugLogger;
	static
	{
		requestDebugLogger = java.util.logging.Logger.getLogger(FhirConnectionTestClientJersey.class.getName());
		requestDebugLogger.setLevel(Level.INFO);
	}

	private final Client client;
	private final FhirClientConfig fhirClientConfig;

	public FhirConnectionTestClientJersey(FhirClientConfig fhirClientConfig, ProxyConfig proxyConfig,
			String userAgentValue, FhirContext fhirContext, BpeOidcClientProvider bpeOidcClientProvider)
	{
		Objects.requireNonNull(fhirClientConfig, "fhirClientConfig");
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(userAgentValue, "userAgentValue");
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(bpeOidcClientProvider, "bpeOidcClientProvider");

		SslConfigurator sslConfigurator = SslConfigurator.newInstance().trustStore(fhirClientConfig.trustStore());

		if (fhirClientConfig.certificateAuthentication() != null)
		{
			sslConfigurator = sslConfigurator.keyStore(fhirClientConfig.certificateAuthentication().keyStore())
					.keyStorePassword(fhirClientConfig.certificateAuthentication().keyStorePassword());
		}

		ClientBuilder builder = ClientBuilder.newBuilder().sslContext(sslConfigurator.createSSLContext())
				.readTimeout(fhirClientConfig.readTimeout().toMillis(), TimeUnit.MILLISECONDS)
				.connectTimeout(fhirClientConfig.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
				.register((ClientRequestFilter) r -> r.getHeaders().add(HttpHeaders.USER_AGENT, userAgentValue));

		ClientConfig config = new ClientConfig();
		config.connectorProvider(new ApacheConnectorProvider());
		if (proxyConfig.isEnabled(fhirClientConfig.baseUrl()))
		{
			config.property(ClientProperties.PROXY_URI, proxyConfig.getUrl());
			config.property(ClientProperties.PROXY_USERNAME, proxyConfig.getUsername());
			config.property(ClientProperties.PROXY_PASSWORD,
					proxyConfig.getPassword() == null ? null : String.valueOf(proxyConfig.getPassword()));
		}
		builder = builder.withConfig(config);

		if (fhirClientConfig.basicAuthentication() != null)
		{
			builder = builder
					.register(HttpAuthenticationFeature.basic(fhirClientConfig.basicAuthentication().username(),
							String.valueOf(fhirClientConfig.basicAuthentication().password())));
		}

		if (fhirClientConfig.bearerAuthentication() != null)
		{
			builder = builder.register(bearerAuthentication(fhirClientConfig.bearerAuthentication()::token));
		}

		if (fhirClientConfig.oidcAuthentication() != null)
		{
			OidcClient oidcClient = bpeOidcClientProvider.getOidcClient(fhirClientConfig.oidcAuthentication());
			builder = builder.register(bearerAuthentication(oidcClient::getAccessToken));
		}

		builder = builder.register(new FhirAdapter(fhirContext));

		if (fhirClientConfig.debugLoggingEnabled())
		{
			builder = builder.register(new LoggingFeature(requestDebugLogger, Level.INFO, Verbosity.PAYLOAD_ANY,
					LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
		}

		client = builder.build();
		this.fhirClientConfig = fhirClientConfig;
	}

	private ClientRequestFilter bearerAuthentication(Supplier<char[]> value)
	{
		return r -> r.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + String.valueOf(value.get()));
	}

	private WebTarget getResource()
	{
		return client.target(fhirClientConfig.baseUrl().endsWith("/") ? fhirClientConfig.baseUrl()
				: fhirClientConfig.baseUrl() + "/");
	}

	@Override
	public boolean testConnection()
	{
		logger.info("Testing connection with '{}' at {} ...", fhirClientConfig.fhirServerId(),
				fhirClientConfig.baseUrl());

		try
		{
			Response response = getResource().path("metadata").request()
					.accept(Constants.CT_FHIR_JSON_NEW + "; fhirVersion=4.0").get();

			if (Status.OK.getStatusCode() == response.getStatus())
			{
				CapabilityStatement statement = response.readEntity(CapabilityStatement.class);

				logger.info("Testing connection with '{}' at {} [OK] -> {} - {}", fhirClientConfig.fhirServerId(),
						fhirClientConfig.baseUrl(), statement.getSoftware().getName(),
						statement.getSoftware().getVersion());
				return true;
			}
			else
			{
				String result;
				if (MediaType.valueOf(Constants.CT_FHIR_JSON_NEW).equals(response.getMediaType()))
					result = toString(response.readEntity(OperationOutcome.class));
				else
					result = response.readEntity(String.class);

				logger.warn("Testing connection with '{}' at {} [Failed] -> status: {} {}, message: {}",
						fhirClientConfig.fhirServerId(), fhirClientConfig.baseUrl(),
						response.getStatusInfo().getStatusCode(), response.getStatusInfo().getReasonPhrase(), result);
				return false;
			}
		}
		catch (ProcessingException e)
		{
			logger.warn("Testing connection with '{}' at {} [Failed] -> {} - {}", fhirClientConfig.fhirServerId(),
					fhirClientConfig.baseUrl(), e.getClass().getName(), e.getMessage());
			return false;
		}
		catch (Exception e)
		{
			logger.warn("Testing connection with '{}' at {} [Failed]", fhirClientConfig.fhirServerId(),
					fhirClientConfig.baseUrl(), e);
			return false;
		}
	}

	private String toString(OperationOutcome outcome)
	{
		return outcome == null ? "" : outcome.getIssue().stream().map(this::toString).collect(Collectors.joining("\n"));
	}

	private String toString(OperationOutcomeIssueComponent issue)
	{
		return issue == null ? "" : issue.getSeverity() + " " + issue.getCode() + " " + issue.getDiagnostics();
	}
}
