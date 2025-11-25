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
package dev.dsf.common.jetty;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Objects;

import org.eclipse.jetty.http.HttpFields.Mutable;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.io.PemReader;

public class ForwardedSecureRequestCustomizer implements Customizer
{
	private static final String URL_ENCODED_CERT_BEGIN = "-----BEGIN%20CERTIFICATE-----%0A";
	private static final String URL_ENCODED_CERT_END = "%0A-----END%20CERTIFICATE-----%0A";

	private static final String CERT_BEGIN = "-----BEGIN CERTIFICATE-----";
	private static final String CERT_END = "-----END CERTIFICATE-----";

	private static final Logger logger = LoggerFactory.getLogger(ForwardedSecureRequestCustomizer.class);

	private final String clientCertHeaderName;

	public ForwardedSecureRequestCustomizer(String clientCertHeaderName)
	{
		this.clientCertHeaderName = Objects.requireNonNull(clientCertHeaderName, "clientCertHeaderName");
	}

	@Override
	public Request customize(Request request, Mutable responseHeaders)
	{
		X509Certificate clientCert = getClientCert(request);

		if (clientCert != null)
			request.setAttribute("jakarta.servlet.request.X509Certificate", new X509Certificate[] { clientCert });

		return request;
	}

	private X509Certificate getClientCert(Request request)
	{
		String clientCertString = request.getHeaders().get(clientCertHeaderName);

		if (clientCertString == null)
		{
			logger.debug("No {} header found", clientCertHeaderName);
			return null;
		}
		if (clientCertString.isEmpty())
		{
			logger.debug("{} header empty", clientCertHeaderName);
			return null;
		}

		if (!clientCertString.startsWith(CERT_BEGIN) && !clientCertString.startsWith(URL_ENCODED_CERT_BEGIN))
		{
			logger.warn("{} header does not start with {} or {}", clientCertHeaderName, CERT_BEGIN,
					URL_ENCODED_CERT_BEGIN);
			return null;
		}
		if (!clientCertString.endsWith(CERT_END) && !clientCertString.endsWith(URL_ENCODED_CERT_END))
		{
			logger.warn("{} header does not end with {} or {}", clientCertHeaderName, CERT_END, URL_ENCODED_CERT_END);
			return null;
		}

		if (clientCertString.startsWith(CERT_BEGIN))
		{
			clientCertString = CERT_BEGIN
					+ clientCertString.replace(CERT_BEGIN, "").replace(CERT_END, "").replaceAll(" ", "\n") + CERT_END;
		}
		else
		{
			clientCertString = URLDecoder.decode(clientCertString, StandardCharsets.UTF_8).trim();
		}

		try
		{
			return PemReader.readCertificate(clientCertString);
		}
		catch (IOException e)
		{
			return null;
		}
	}
}
