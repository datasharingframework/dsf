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
package dev.dsf.bpe.v2.client.fhir;

import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

public interface ClientConfig
{
	/**
	 * @return never <code>null</code>
	 */
	String getFhirServerId();

	/**
	 * @return never <code>null</code>
	 */
	String getBaseUrl();

	boolean isStartupConnectionTestEnabled();

	boolean isDebugLoggingEnabled();

	/**
	 * @return never <code>null</code>
	 */
	Duration getConnectTimeout();

	/**
	 * @return never <code>null</code>
	 */
	Duration getReadTimeout();

	/**
	 * @return never <code>null</code>
	 */
	KeyStore getTrustStore();

	/**
	 * @return may be <code>null</code>
	 */
	CertificateAuthentication getCertificateAuthentication();

	/**
	 * @return may be <code>null</code>
	 */
	BasicAuthentication getBasicAuthentication();

	/**
	 * @return may be <code>null</code>
	 */
	BearerAuthentication getBearerAuthentication();

	/**
	 * @return may be <code>null</code>
	 */
	OidcAuthentication getOidcAuthentication();

	/**
	 * @return <code>null</code> if not enabled or configured as no-proxy url
	 */
	Proxy getProxy();

	interface CertificateAuthentication
	{
		/**
		 * @return not <code>null</code>
		 */
		KeyStore getKeyStore();

		/**
		 * @return may be <code>null</code>
		 */
		char[] getKeyStorePassword();
	}

	interface BasicAuthentication
	{
		/**
		 * @return never <code>null</code>
		 */
		String getUsername();

		/**
		 * @return never <code>null</code>
		 */
		char[] getPassword();
	}

	interface BearerAuthentication
	{
		/**
		 * @return never <code>null</code>
		 */
		char[] getToken();
	}

	interface OidcAuthentication
	{
		/**
		 * @return never <code>null</code>
		 */
		String getBaseUrl();

		/**
		 * @return never <code>null</code>
		 */
		String getDiscoveryPath();

		boolean isStartupConnectionTestEnabled();

		boolean isDebugLoggingEnabled();

		/**
		 * @return never <code>null</code>
		 */
		Duration getConnectTimeout();

		/**
		 * @return never <code>null</code>
		 */
		Duration getReadTimeout();

		/**
		 * @return never <code>null</code>
		 */
		KeyStore getTrustStore();

		/**
		 * @return never <code>null</code>
		 */
		String getClientId();

		/**
		 * @return never <code>null</code>
		 */
		char[] getClientSecret();

		/**
		 * @return never <code>null</code>, may be empty
		 */
		List<String> getRequiredAudiences();

		boolean isVerifyAuthorizedPartyEnabled();

		/**
		 * @return <code>null</code> if not enabled or configured as no-proxy url
		 */
		Proxy getProxy();
	}

	interface Proxy
	{
		/**
		 * @return never <code>null</code>
		 */
		String getUrl();

		/**
		 * @return may be <code>null</code>
		 */
		String getUsername();

		/**
		 * @return may be <code>null</code>
		 */
		char[] getPassword();
	}
}