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
package dev.dsf.common.auth;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateFormatter;
import de.hsheilbronn.mi.utils.crypto.cert.CertificateFormatter.X500PrincipalFormat;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreFormatter;

public class ClientCertificateAuthenticator extends LoginAuthenticator
{
	private static final Logger logger = LoggerFactory.getLogger(ClientCertificateAuthenticator.class);

	private final X509TrustManager x509TrustManager;

	public ClientCertificateAuthenticator(KeyStore clientTrustStore)
	{
		x509TrustManager = createX509TrustManager(Objects.requireNonNull(clientTrustStore, "clientTrustStore"));
	}

	@Override
	public String getAuthenticationType()
	{
		return Authenticator.CERT_AUTH;
	}

	@Override
	public AuthenticationState validateRequest(Request request, Response response, Callback callback)
			throws ServerAuthException
	{
		X509Certificate[] certificates = (X509Certificate[]) request
				.getAttribute("jakarta.servlet.request.X509Certificate");

		if (certificates == null || certificates.length <= 0)
		{
			logger.warn("X509Certificate could not be retrieved, sending unauthorized");
			return null;
		}

		try
		{
			x509TrustManager.checkClientTrusted(certificates, "RSA");
		}
		catch (CertificateException e)
		{
			logger.debug("Unable to validate client certificates, sending unauthorized", e);
			logger.warn("Unable to validate client certificates, sending unauthorized: {} - {}", e.getClass().getName(),
					e.getMessage());

			return null;
		}

		UserIdentity user = login(null, certificates, request, response);
		if (user == null)
		{
			logger.warn("User '{}' not found, sending unauthorized",
					CertificateFormatter.toSubjectName(certificates[0], X500PrincipalFormat.RFC1779));
			return null;
		}

		return new UserAuthenticationSucceeded(getAuthenticationType(), user);
	}

	private X509TrustManager createX509TrustManager(KeyStore clientTrustStore)
	{
		logger.info("Using trust-store with {} to validate client certificates",
				KeyStoreFormatter.toSubjectsFromCertificates(clientTrustStore, X500PrincipalFormat.RFC1779).values()
						.stream().collect(Collectors.joining("; ", "[", "]")));

		try
		{
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(clientTrustStore);
			return (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
		}
		catch (NoSuchAlgorithmException | KeyStoreException e)
		{
			logger.debug("Unable to create trust manager", e);
			logger.warn("Unable to create trust manager: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}
}
