package dev.dsf.common.jetty;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.rwh.utils.crypto.io.PemIo;

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
	public void customize(Connector connector, HttpConfiguration channelConfig, Request request)
	{
		X509Certificate clientCert = getClientCert(request);

		if (clientCert != null)
			request.setAttribute("jakarta.servlet.request.X509Certificate", new X509Certificate[] { clientCert });
	}

	private X509Certificate getClientCert(Request request)
	{
		String clientCertString = request.getHeader(clientCertHeaderName);

		if (clientCertString == null)
		{
			logger.warn("No {} header found", clientCertHeaderName);
			return null;
		}
		if (clientCertString.isEmpty())
		{
			logger.warn("{} header empty", clientCertHeaderName);
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
			return PemIo.readX509CertificateFromPem(clientCertString);
		}
		catch (CertificateException | IOException e)
		{
			return null;
		}
	}
}
