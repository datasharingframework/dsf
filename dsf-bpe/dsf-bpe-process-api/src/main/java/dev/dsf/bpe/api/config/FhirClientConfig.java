package dev.dsf.bpe.api.config;

import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

public interface FhirClientConfig
{
	String DSF_CLIENT_FHIR_SERVER_ID = "#dsf";

	/**
	 * @return never <code>null</code>
	 */
	String fhirServerId();

	/**
	 * @return never <code>null</code>
	 */
	String baseUrl();

	boolean startupConnectionTestEnabled();

	boolean debugLoggingEnabled();

	/**
	 * @return never <code>null</code>
	 */
	Duration connectTimeout();

	/**
	 * @return never <code>null</code>
	 */
	Duration readTimeout();

	/**
	 * @return never <code>null</code>
	 */
	KeyStore trustStore();

	/**
	 * @return may be <code>null</code>
	 */
	CertificateAuthentication certificateAuthentication();

	default boolean hasCertificateAuthentication()
	{
		return certificateAuthentication() != null;
	}

	/**
	 * @return may be <code>null</code>
	 */
	BasicAuthentication basicAuthentication();

	default boolean hasBbasicAuthentication()
	{
		return basicAuthentication() != null;
	}

	/**
	 * @return may be <code>null</code>
	 */
	BearerAuthentication bearerAuthentication();

	default boolean hasBearerAuthentication()
	{
		return bearerAuthentication() != null;
	}

	/**
	 * @return may be <code>null</code>
	 */
	OidcAuthentication oidcAuthentication();

	default boolean hasOidcAuthentication()
	{
		return oidcAuthentication() != null;
	}

	interface CertificateAuthentication
	{
		/**
		 * @return not <code>null</code>
		 */
		KeyStore keyStore();

		/**
		 * @return may be <code>null</code>
		 */
		char[] keyStorePassword();
	}

	interface BasicAuthentication
	{
		/**
		 * @return never <code>null</code>
		 */
		String username();

		/**
		 * @return never <code>null</code>
		 */
		char[] password();
	}

	interface BearerAuthentication
	{
		/**
		 * @return never <code>null</code>
		 */
		char[] token();
	}

	interface OidcAuthentication
	{
		/**
		 * @return never <code>null</code>
		 */
		String baseUrl();

		/**
		 * @return never <code>null</code>
		 */
		String discoveryPath();

		boolean startupConnectionTestEnabled();

		boolean debugLoggingEnabled();

		/**
		 * @return never <code>null</code>
		 */
		Duration connectTimeout();

		/**
		 * @return never <code>null</code>
		 */
		Duration readTimeout();

		/**
		 * @return never <code>null</code>
		 */
		KeyStore trustStore();

		/**
		 * @return never <code>null</code>
		 */
		String clientId();

		/**
		 * @return never <code>null</code>
		 */
		char[] clientSecret();

		/**
		 * @return never <code>null</code>, may be empty
		 */
		List<String> requiredAudiences();

		boolean verifyAuthorizedParty();
	}
}
