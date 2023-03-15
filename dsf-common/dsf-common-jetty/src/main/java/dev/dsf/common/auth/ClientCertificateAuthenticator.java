package dev.dsf.common.auth;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Constraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class ClientCertificateAuthenticator extends LoginAuthenticator
{
	private static final Logger logger = LoggerFactory.getLogger(ClientCertificateAuthenticator.class);

	private final X509TrustManager x509TrustManager;

	public ClientCertificateAuthenticator(KeyStore clientTrustStore)
	{
		x509TrustManager = createX509TrustManager(Objects.requireNonNull(clientTrustStore, "clientTrustStore"));
	}

	@Override
	public String getAuthMethod()
	{
		return Constraint.__CERT_AUTH;
	}

	@Override
	public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory)
			throws ServerAuthException
	{
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		X509Certificate[] certificates = (X509Certificate[]) httpRequest
				.getAttribute("jakarta.servlet.request.X509Certificate");

		if (certificates == null || certificates.length <= 0)
		{
			logger.warn("X509Certificate could not be retrieved, sending unauthorized");
			return Authentication.UNAUTHENTICATED;
		}

		try
		{
			x509TrustManager.checkClientTrusted(certificates, "RSA");
		}
		catch (CertificateException e)
		{
			logger.warn("Unable to validate client certificates, sending unauthorized: {} - {}", e.getClass().getName(),
					e.getMessage());
			return Authentication.UNAUTHENTICATED;
		}

		UserIdentity user = login(null, certificates, httpRequest);
		if (user == null)
		{
			logger.warn("User '{}' not found, sending unauthorized", getSubjectDn(certificates));
			return Authentication.UNAUTHENTICATED;
		}

		return new UserAuthentication(getAuthMethod(), user);
	}

	private X509TrustManager createX509TrustManager(KeyStore clientTrustStore)
	{
		logger.info("Using [{}] to validate client certificates", getSubjectDn(getCaCertificates(clientTrustStore)));

		try
		{
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(clientTrustStore);
			return (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
		}
		catch (NoSuchAlgorithmException | KeyStoreException e)
		{
			logger.warn("Unable to create trust manager: {} - {}", e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private X509Certificate[] getCaCertificates(KeyStore keyStore)
	{
		try
		{
			PKIXParameters params = new PKIXParameters(keyStore);
			return params.getTrustAnchors().stream().map(TrustAnchor::getTrustedCert).toArray(X509Certificate[]::new);
		}
		catch (KeyStoreException | InvalidAlgorithmParameterException e)
		{
			logger.warn("Unable to extract trust anchors: {} - {}", e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}


	private String getSubjectDn(X509Certificate[] certificates)
	{
		return Stream.of(certificates).map(this::getSubjectDn).collect(Collectors.joining(";"));
	}

	private String getSubjectDn(X509Certificate certificate)
	{
		return certificate.getSubjectX500Principal().getName(X500Principal.RFC1779);
	}

	@Override
	public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory,
			User validatedUser) throws ServerAuthException
	{
		return true; // nothing to do
	}
}
