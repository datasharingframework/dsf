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
package dev.dsf.bpe.config;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.x500.X500Principal;

import dev.dsf.bpe.api.config.FhirClientConfig;

public record FhirClientConfigImpl(String fhirServerId, String baseUrl, boolean startupConnectionTestEnabled,
		boolean debugLoggingEnabled, Duration connectTimeout, Duration readTimeout, KeyStore trustStore,
		CertificateAuthentication certificateAuthentication, BasicAuthentication basicAuthentication,
		BearerAuthentication bearerAuthentication, OidcAuthentication oidcAuthentication) implements FhirClientConfig
{

	/**
	 * @param fhirServerId
	 *            not <code>null</code>
	 * @param baseUrl
	 *            not <code>null</code>
	 * @param startupConnectionTestEnabled
	 * @param debugLoggingEnabled
	 * @param connectTimeout
	 *            not <code>null</code>
	 * @param readTimeout
	 *            not <code>null</code>
	 * @param trustStore
	 *            not <code>null</code>
	 * @param certificateAuthentication
	 *            may be <code>null</code>
	 * @param basicAuthentication
	 *            may be <code>null</code>
	 * @param bearerAuthentication
	 *            may be <code>null</code>
	 * @param oidcAuthentication
	 *            may be <code>null</code>
	 */
	public FhirClientConfigImpl
	{
		Objects.requireNonNull(fhirServerId, "fhirServerId");
		Objects.requireNonNull(baseUrl, "baseUrl");
		Objects.requireNonNull(connectTimeout, "connectTimeout");
		Objects.requireNonNull(readTimeout, "readTimeout");
		Objects.requireNonNull(trustStore, "trustStore");
		// certificateAuthentication may be null
		// basicAuthentication may be null
		// bearerAuthentication may be null
		// oidcAuthentication may be null
	}

	@Override
	public String toString()
	{
		return "[fhirServerId: " + fhirServerId + ", baseUrl: " + baseUrl + ", startupConnectionTestEnabled: "
				+ startupConnectionTestEnabled + ", debugLoggingEnabled: " + debugLoggingEnabled + ", connectTimeout: "
				+ connectTimeout + ", readTimeout: " + readTimeout + ", trusted-certificates: "
				+ trustStoreToString(trustStore) + ", "
				+ (certificateAuthentication != null ? "cert-auth: " + certificateAuthentication + ", " : "")
				+ (basicAuthentication != null ? "basic-auth: " + basicAuthentication + ", " : "")
				+ (bearerAuthentication != null ? "bearer-auth: " + bearerAuthentication + ", " : "")
				+ (oidcAuthentication != null ? "oidc-auth: " + oidcAuthentication : "") + "]";
	}

	private static String trustStoreToString(KeyStore trustStore)
	{
		if (trustStore == null)
			return null;

		try
		{
			return Collections.list(trustStore.aliases()).stream().map(getCertificate(trustStore))
					.filter(Objects::nonNull).map(X509Certificate::getSubjectX500Principal).map(X500Principal::getName)
					.collect(Collectors.joining(", "));
		}
		catch (RuntimeException | KeyStoreException e)
		{
			return "?";
		}
	}

	private static String secretToString(char[] secret)
	{
		return secret == null ? "null" : "***";
	}

	private static Function<String, X509Certificate> getCertificate(KeyStore trustStore)
	{
		return a ->
		{
			try
			{
				Certificate c = trustStore.getCertificate(a);
				return c instanceof X509Certificate x ? x : null;
			}
			catch (KeyStoreException e)
			{
				throw new RuntimeException(e);
			}
		};
	}

	public static record CertificateAuthenticationImpl(KeyStore keyStore, char[] keyStorePassword)
			implements CertificateAuthentication
	{
		/**
		 * @param keyStore
		 *            not <code>null</code>
		 * @param keyStorePassword
		 *            may be <code>null</code> ({@link KeyStore} without encryption)
		 */
		public CertificateAuthenticationImpl
		{
			Objects.requireNonNull(keyStore, "keyStore");
			// keyStorePassword may be null (KeyStore without encryption)
		}

		@Override
		public final String toString()
		{
			return "[certificate-chain: " + getCertificateChain(keyStore).map(X509Certificate::getSubjectX500Principal)
					.map(X500Principal::getName).collect(Collectors.joining(", ")) + "]";
		}

		private Stream<X509Certificate> getCertificateChain(KeyStore trustStore)
		{
			try
			{
				List<String> alisases = Collections.list(trustStore.aliases());
				Certificate[] certificates = trustStore.getCertificateChain(alisases.get(0));

				return certificates == null ? null
						: Arrays.stream(certificates).filter(c -> c instanceof X509Certificate)
								.map(c -> (X509Certificate) c);
			}
			catch (KeyStoreException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	public static record BasicAuthenticationImpl(String username, char[] password) implements BasicAuthentication
	{
		/**
		 * @param username
		 *            not <code>null</code>
		 * @param password
		 *            not <code>null</code>
		 */
		public BasicAuthenticationImpl
		{
			Objects.requireNonNull(username, "username");
			Objects.requireNonNull(password, "password");
		}

		@Override
		public final String toString()
		{
			return "[username: " + username + ", password: " + secretToString(password) + "]";
		}
	}

	public static record BearerAuthenticationImpl(char[] token) implements BearerAuthentication
	{
		/**
		 * @param token
		 *            not <code>null</code>
		 */
		public BearerAuthenticationImpl
		{
			Objects.requireNonNull(token, "token");
		}

		@Override
		public final String toString()
		{
			return "[token: " + secretToString(token) + "]";
		}
	}

	public static record OidcAuthenticationImpl(String baseUrl, String discoveryPath,
			boolean startupConnectionTestEnabled, boolean debugLoggingEnabled, Duration connectTimeout,
			Duration readTimeout, KeyStore trustStore, String clientId, char[] clientSecret,
			List<String> requiredAudiences, boolean verifyAuthorizedParty) implements OidcAuthentication
	{
		/**
		 * @param baseUrl
		 *            not <code>null</code>
		 * @param discoveryPath
		 *            not <code>null</code>
		 * @param startupConnectionTestEnabled
		 * @param debugLoggingEnabled
		 * @param connectTimeout
		 *            not <code>null</code>
		 * @param readTimeout
		 *            not <code>null</code>
		 * @param trustStore
		 *            not <code>null</code>
		 * @param clientId
		 *            not <code>null</code>
		 * @param clientSecret
		 *            not <code>null</code>
		 * @param requiredAudiences
		 *            not <code>null</code>
		 * @param verifyAuthorizedParty
		 */
		public OidcAuthenticationImpl
		{
			Objects.requireNonNull(baseUrl, "baseUrl");
			Objects.requireNonNull(discoveryPath, "discoveryPath");
			Objects.requireNonNull(connectTimeout, "connectTimeout");
			Objects.requireNonNull(readTimeout, "readTimeout");
			Objects.requireNonNull(trustStore, "trustStore");
			Objects.requireNonNull(clientId, "clientId");
			Objects.requireNonNull(clientSecret, "clientSecret");
			Objects.requireNonNull(requiredAudiences, "requiredAudiences");
		}

		@Override
		public String toString()
		{
			return "[baseUrl: " + baseUrl + ", discoveryPath: " + discoveryPath + ", startupConnectionTestEnabled: "
					+ startupConnectionTestEnabled + ", debugLoggingEnabled: " + debugLoggingEnabled
					+ ", connectTimeout: " + connectTimeout + ", readTimeout: " + readTimeout + ", trustStore: "
					+ trustStoreToString(trustStore) + ", clientId: " + clientId + ", clientSecret: "
					+ secretToString(clientSecret) + ", requiredAudiences: " + requiredAudiences
					+ ", verifyAuthorizedParty: " + verifyAuthorizedParty + "]";
		}
	}
}