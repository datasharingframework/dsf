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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateValidator;
import de.hsheilbronn.mi.utils.crypto.io.KeyStoreReader;
import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairValidator;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;

public record FhirClientConfigYaml(@JsonProperty(FhirClientConfigYaml.PROPERTY_BASE_URL) String baseUrl,
		@JsonProperty(FhirClientConfigYaml.PROPERTY_TEST_CONNECTION_ON_STARTUP) Boolean startupConnectionTestEnabled,
		@JsonProperty(FhirClientConfigYaml.PROPERTY_ENABLE_DEBUG_LOGGING) Boolean debugLoggingEnabled,
		@JsonProperty(FhirClientConfigYaml.PROPERTY_CONNECT_TIMEOUT) Duration connectTimeout,
		@JsonProperty(FhirClientConfigYaml.PROPERTY_READ_TIMEOUT) Duration readTimeout,
		@JsonProperty(FhirClientConfigYaml.PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE) String trustedRootCertificatesFile,
		@JsonProperty(FhirClientConfigYaml.PROPERTY_CERT_AUTH) CertificateAuth certAuth,
		@JsonProperty(FhirClientConfigYaml.PROPERTY_BASIC_AUTH) BasicAuth basicAuth,
		@JsonProperty(FhirClientConfigYaml.PROPERTY_BEARER_AUTH) BearerAuth bearerAuth,
		@JsonProperty(FhirClientConfigYaml.PROPERTY_OIDC_AUTH) OidcAuth oidcAuth) implements WithValidation
{

	private static final Logger logger = LoggerFactory.getLogger(FhirClientConfigYaml.class);

	public static final TypeReference<Map<String, FhirClientConfigYaml>> MAP_OF_CONFIGS = new TypeReference<>()
	{
	};

	public static final String PROPERTY_BASE_URL = "base-url";
	public static final String PROPERTY_TEST_CONNECTION_ON_STARTUP = "test-connection-on-startup";
	public static final String PROPERTY_ENABLE_DEBUG_LOGGING = "enable-debug-logging";
	public static final String PROPERTY_CONNECT_TIMEOUT = "connect-timeout";
	public static final String PROPERTY_READ_TIMEOUT = "read-timeout";
	public static final String PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE = "trusted-root-certificates-file";
	public static final String PROPERTY_CERT_AUTH = "cert-auth";
	public static final String PROPERTY_BASIC_AUTH = "basic-auth";
	public static final String PROPERTY_BEARER_AUTH = "bearer-auth";
	public static final String PROPERTY_OIDC_AUTH = "oidc-auth";

	public static record CertificateAuth(@JsonProperty(CertificateAuth.PROPERTY_P12_FILE) String p12File,
			@JsonProperty(CertificateAuth.PROPERTY_PRIVATE_KEY_FILE) String privateKeyFile,
			@JsonProperty(CertificateAuth.PROPERTY_CERTIFICATE_FILE) String certificateFile,
			@JsonProperty(CertificateAuth.PROPERTY_PASSWORD) char[] password,
			@JsonProperty(CertificateAuth.PROPERTY_PASSWORD_FILE) String passwordFile) implements WithValidation
	{

		public static final String PROPERTY_P12_FILE = "p12-file";
		public static final String PROPERTY_PRIVATE_KEY_FILE = "private-key-file";
		public static final String PROPERTY_CERTIFICATE_FILE = "certificate-file";
		public static final String PROPERTY_PASSWORD = "password";
		public static final String PROPERTY_PASSWORD_FILE = "password-file";

		@JsonCreator
		public CertificateAuth(@JsonProperty(CertificateAuth.PROPERTY_P12_FILE) String p12File,
				@JsonProperty(CertificateAuth.PROPERTY_PRIVATE_KEY_FILE) String privateKeyFile,
				@JsonProperty(CertificateAuth.PROPERTY_CERTIFICATE_FILE) String certificateFile,
				@JsonProperty(CertificateAuth.PROPERTY_PASSWORD) char[] password,
				@JsonProperty(CertificateAuth.PROPERTY_PASSWORD_FILE) String passwordFile)
		{
			this.p12File = p12File;
			this.privateKeyFile = privateKeyFile;
			this.certificateFile = certificateFile;
			this.password = password;
			this.passwordFile = passwordFile;
		}

		@Override
		public String toString()
		{
			return "CertificateAuth[p12File=" + p12File + ", privateKeyFile=" + privateKeyFile + ", certificateFile="
					+ certificateFile + ", password=" + (password != null ? "***" : "null") + ", passwordFile="
					+ passwordFile + "]";
		}

		public char[] readPassword() throws IOException
		{
			return doReadCharArray(this::password, this::passwordFile, PROPERTY_PASSWORD, PROPERTY_PASSWORD_FILE);
		}

		public KeyStore readKeyStore() throws IOException
		{
			if (p12File == null && (certificateFile == null || privateKeyFile == null))
				return null;
			else if (p12File != null && certificateFile == null && privateKeyFile == null)
				return KeyStoreReader.readPkcs12(Paths.get(p12File), readPassword());
			else if (p12File == null && certificateFile != null && privateKeyFile != null)
			{
				List<X509Certificate> certificates = PemReader.readCertificates(Paths.get(certificateFile));
				PrivateKey privateKey = PemReader.readPrivateKey(Paths.get(privateKeyFile), readPassword());

				if (certificates.isEmpty())
					throw new IOException("no certificates");
				else if (!CertificateValidator.isClientCertificate(certificates.get(0)))
					throw new IOException("not a client certificate");
				else if (!KeyPairValidator.matches(privateKey, certificates.get(0).getPublicKey()))
					throw new IOException("private-key not matching public-key from "
							+ (certificates.size() > 1 ? "first " : "") + "certificate");

				return KeyStoreCreator.jksForPrivateKeyAndCertificateChain(privateKey, readPassword(), certificates);
			}
			else
				return null;
		}

		private ValidationError validateKeyStore(String propertyPrefix)
		{
			ValidationError p12FilePropertyFileNotReadableOrBlank = propertyFileNotReadableOrBlank(p12File,
					propertyPrefix, PROPERTY_P12_FILE);
			if (p12FilePropertyFileNotReadableOrBlank != null)
				return p12FilePropertyFileNotReadableOrBlank;

			ValidationError privateKeyFilePropertyFileNotReadableOrBlank = propertyFileNotReadableOrBlank(
					privateKeyFile, propertyPrefix, PROPERTY_PRIVATE_KEY_FILE);
			if (privateKeyFilePropertyFileNotReadableOrBlank != null)
				return privateKeyFilePropertyFileNotReadableOrBlank;

			ValidationError certificateFilePropertyFileNotReadableOrBlank = propertyFileNotReadableOrBlank(
					certificateFile, propertyPrefix, PROPERTY_CERTIFICATE_FILE);
			if (certificateFilePropertyFileNotReadableOrBlank != null)
				return certificateFilePropertyFileNotReadableOrBlank;

			try
			{
				KeyStore keyStore = readKeyStore();

				if (keyStore != null)
				{
					List<String> aliases = Collections.list(keyStore.aliases());
					if (aliases.size() != 1)
						return keyStoreError(propertyPrefix,
								"KeyStore has " + aliases.size() + " entries " + aliases + ", expected 1");
					if (keyStore.getCertificateChain(aliases.get(0)) == null)
						return keyStoreError(propertyPrefix,
								"KeyStore has no certificate chain for entry " + aliases.get(0));
					if (!keyStore.isKeyEntry(aliases.get(0)))
						return keyStoreError(propertyPrefix, "KeyStore has no key for entry " + aliases.get(0));
				}

				return null;
			}
			catch (IOException e)
			{
				logger.debug("Unable to read key store", e);

				if (p12File() != null)
					return error(propertyPrefix, PROPERTY_P12_FILE, e.getMessage());
				else if (certificateFile() != null && privateKeyFile() != null)
					return error(propertyPrefix, PROPERTY_CERTIFICATE_FILE, PROPERTY_PRIVATE_KEY_FILE, e.getMessage());
				else
					throw new IllegalStateException("not valid: " + PROPERTY_P12_FILE + " vs. "
							+ PROPERTY_CERTIFICATE_FILE + " / " + PROPERTY_PRIVATE_KEY_FILE);
			}
			catch (KeyStoreException e)
			{
				return keyStoreError(propertyPrefix, e.getMessage());
			}
		}

		private PropertiesValidationError keyStoreError(String propertyPrefix, String message)
		{
			return new PropertiesValidationError(List.of(propertyPrefix + PROPERTY_P12_FILE,
					propertyPrefix + PROPERTY_CERTIFICATE_FILE, propertyPrefix + PROPERTY_PRIVATE_KEY_FILE), message);
		}

		private ValidationError validatePasswordVsP12CertKey(String propertyPrefix)
		{
			if (password != null || (passwordFile != null && !passwordFile.isBlank()))
			{
				if (p12File == null && certificateFile == null && privateKeyFile == null)
					return new PropertiesValidationError(List.of(propertyPrefix + PROPERTY_P12_FILE,
							propertyPrefix + PROPERTY_CERTIFICATE_FILE, propertyPrefix + PROPERTY_PRIVATE_KEY_FILE),
							"not defined or blank");
			}

			return null;
		}

		@Override
		public Stream<ValidationError> validate(String propertyPrefix)
		{
			return Stream.of(
					propertyMissing(privateKeyFile, certificateFile, propertyPrefix, PROPERTY_PRIVATE_KEY_FILE,
							PROPERTY_CERTIFICATE_FILE),
					propertiesConflicting(p12File, privateKeyFile, propertyPrefix, PROPERTY_P12_FILE,
							PROPERTY_PRIVATE_KEY_FILE),
					propertiesConflicting(p12File, certificateFile, propertyPrefix, PROPERTY_P12_FILE,
							PROPERTY_CERTIFICATE_FILE),
					propertyFileNotReadableOrNotSingleLine(passwordFile, propertyPrefix, PROPERTY_PASSWORD_FILE),
					propertiesConflicting(password, passwordFile, propertyPrefix, PROPERTY_PASSWORD,
							PROPERTY_PASSWORD_FILE),
					validatePasswordVsP12CertKey(propertyPrefix), validateKeyStore(propertyPrefix));
		}
	}

	public static record BasicAuth(@JsonProperty(BasicAuth.PROPERTY_USERNAME) String username,
			@JsonProperty(BasicAuth.PROPERTY_PASSWORD) char[] password,
			@JsonProperty(BasicAuth.PROPERTY_PASSWORD_FILE) String passwordFile) implements WithValidation
	{

		public static final String PROPERTY_USERNAME = "username";
		public static final String PROPERTY_PASSWORD = "password";
		public static final String PROPERTY_PASSWORD_FILE = "password-file";

		@JsonCreator
		public BasicAuth(@JsonProperty(BasicAuth.PROPERTY_USERNAME) String username,
				@JsonProperty(BasicAuth.PROPERTY_PASSWORD) char[] password,
				@JsonProperty(BasicAuth.PROPERTY_PASSWORD_FILE) String passwordFile)
		{
			this.username = username;
			this.password = password;
			this.passwordFile = passwordFile;
		}

		@Override
		public String toString()
		{
			return "BasicAuth[username=" + username + ", password=" + (password != null ? "***" : "null")
					+ ", passwordFile=" + passwordFile + "]";
		}

		public char[] readPassword() throws IOException
		{
			return doReadCharArray(this::password, this::passwordFile, PROPERTY_PASSWORD, PROPERTY_PASSWORD_FILE);
		}

		private ValidationError validatePassword(String propertyPrefix)
		{
			try
			{
				char[] readPassword = readPassword();
				if (readPassword != null
						&& !StandardCharsets.ISO_8859_1.newEncoder().canEncode(String.valueOf(readPassword)))
				{
					if (password != null)
						return error(propertyPrefix, PROPERTY_PASSWORD,
								"not encodable with " + StandardCharsets.ISO_8859_1.name());
					else if (passwordFile != null)
						return error(propertyPrefix, PROPERTY_PASSWORD_FILE,
								"not encodable with " + StandardCharsets.ISO_8859_1.name());
				}
			}
			catch (IOException e)
			{
				logger.debug("Unable to read password", e);
			}

			return null;
		}

		private ValidationError validateUsernameVsPassword(String propertyPrefix)
		{
			if (username != null && !username.isBlank() && password == null
					&& (passwordFile == null || passwordFile.isBlank()))
				return error(propertyPrefix, PROPERTY_PASSWORD, PROPERTY_PASSWORD_FILE, "not defined");
			else
				return null;
		}

		@Override
		public Stream<ValidationError> validate(String propertyPrefix)
		{
			return Stream.of(propertyNullOrBlank(username, propertyPrefix, PROPERTY_USERNAME),
					propertyBlank(password, propertyPrefix, PROPERTY_PASSWORD), validatePassword(propertyPrefix),
					propertyFileNotReadableOrNotSingleLine(passwordFile, propertyPrefix, PROPERTY_PASSWORD_FILE),
					propertiesConflicting(password, passwordFile, propertyPrefix, PROPERTY_PASSWORD,
							PROPERTY_PASSWORD_FILE),
					validateUsernameVsPassword(propertyPrefix));
		}
	}

	public static record BearerAuth(@JsonProperty(BearerAuth.PROPERTY_TOKEN) char[] token,
			@JsonProperty(BearerAuth.PROPERTY_TOKEN_FILE) String tokenFile) implements WithValidation
	{

		public static final String PROPERTY_TOKEN = "token";
		public static final String PROPERTY_TOKEN_FILE = "token-file";

		@JsonCreator
		public BearerAuth(@JsonProperty(BearerAuth.PROPERTY_TOKEN) char[] token,
				@JsonProperty(BearerAuth.PROPERTY_TOKEN_FILE) String tokenFile)
		{
			this.token = token;
			this.tokenFile = tokenFile;
		}

		public char[] readToken() throws IOException
		{
			return doReadCharArray(this::token, this::tokenFile, PROPERTY_TOKEN, PROPERTY_TOKEN_FILE);
		}

		@Override
		public String toString()
		{
			return "BearerAuth[token=" + (token != null ? "***" : "null") + ", tokenFile=" + tokenFile + "]";
		}

		@Override
		public Stream<ValidationError> validate(String propertyPrefix)
		{
			return Stream.of(propertyBlank(token, propertyPrefix, PROPERTY_TOKEN),
					propertyFileNotReadableOrNotSingleLine(tokenFile, propertyPrefix, PROPERTY_TOKEN_FILE),
					propertiesConflicting(token, tokenFile, propertyPrefix, PROPERTY_TOKEN, PROPERTY_TOKEN_FILE));
		}
	}

	public static record OidcAuth(@JsonProperty(OidcAuth.PROPERTY_BASE_URL) String baseUrl,
			@JsonProperty(OidcAuth.PROPERTY_DISCOVERY_PATH) String discoveryPath,
			@JsonProperty(OidcAuth.PROPERTY_TEST_CONNECTION_ON_STARTUP) Boolean startupConnectionTestEnabled,
			@JsonProperty(OidcAuth.PROPERTY_ENABLE_DEBUG_LOGGING) Boolean debugLoggingEnabled,
			@JsonProperty(OidcAuth.PROPERTY_CONNECT_TIMEOUT) Duration connectTimeout,
			@JsonProperty(OidcAuth.PROPERTY_READ_TIMEOUT) Duration readTimeout,
			@JsonProperty(OidcAuth.PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE) String trustedRootCertificatesFile,
			@JsonProperty(OidcAuth.PROPERTY_CLIENT_ID) String clientId,
			@JsonProperty(OidcAuth.PROPERTY_CLIENT_SECRET) char[] clientSecret,
			@JsonProperty(OidcAuth.PROPERTY_CLIENT_SECRET_FILE) String clientSecretFile,
			@JsonProperty(OidcAuth.PROPERTY_REQUIRED_AUDIENCE) @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<String> requiredAudiences,
			@JsonProperty(OidcAuth.PROPERTY_VERIFY_AUTHORIZED_PARTY) Boolean verifyAuthorizedParty)
			implements WithValidation
	{

		public static final String PROPERTY_BASE_URL = "base-url";
		public static final String PROPERTY_DISCOVERY_PATH = "discovery-path";
		public static final String PROPERTY_TEST_CONNECTION_ON_STARTUP = "test-connection-on-startup";
		public static final String PROPERTY_ENABLE_DEBUG_LOGGING = "enable-debug-logging";
		public static final String PROPERTY_CONNECT_TIMEOUT = "connect-timeout";
		public static final String PROPERTY_READ_TIMEOUT = "read-timeout";
		public static final String PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE = "trusted-root-certificates-file";
		public static final String PROPERTY_CLIENT_ID = "client-id";
		public static final String PROPERTY_CLIENT_SECRET = "client-secret";
		public static final String PROPERTY_CLIENT_SECRET_FILE = "client-secret-file";
		public static final String PROPERTY_REQUIRED_AUDIENCE = "required-audience";
		public static final String PROPERTY_VERIFY_AUTHORIZED_PARTY = "verify-authorized-party";

		@JsonCreator
		public OidcAuth(@JsonProperty(OidcAuth.PROPERTY_BASE_URL) String baseUrl,
				@JsonProperty(OidcAuth.PROPERTY_DISCOVERY_PATH) String discoveryPath,
				@JsonProperty(OidcAuth.PROPERTY_TEST_CONNECTION_ON_STARTUP) Boolean startupConnectionTestEnabled,
				@JsonProperty(OidcAuth.PROPERTY_ENABLE_DEBUG_LOGGING) Boolean debugLoggingEnabled,
				@JsonProperty(OidcAuth.PROPERTY_CONNECT_TIMEOUT) Duration connectTimeout,
				@JsonProperty(OidcAuth.PROPERTY_READ_TIMEOUT) Duration readTimeout,
				@JsonProperty(OidcAuth.PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE) String trustedRootCertificatesFile,
				@JsonProperty(OidcAuth.PROPERTY_CLIENT_ID) String clientId,
				@JsonProperty(OidcAuth.PROPERTY_CLIENT_SECRET) char[] clientSecret,
				@JsonProperty(OidcAuth.PROPERTY_CLIENT_SECRET_FILE) String clientSecretFile,
				@JsonProperty(OidcAuth.PROPERTY_REQUIRED_AUDIENCE) List<String> requiredAudiences,
				@JsonProperty(OidcAuth.PROPERTY_VERIFY_AUTHORIZED_PARTY) Boolean verifyAuthorizedParty)
		{
			this.baseUrl = baseUrl;
			this.discoveryPath = discoveryPath;
			this.startupConnectionTestEnabled = startupConnectionTestEnabled;
			this.debugLoggingEnabled = debugLoggingEnabled;
			this.connectTimeout = connectTimeout;
			this.readTimeout = readTimeout;
			this.trustedRootCertificatesFile = trustedRootCertificatesFile;
			this.clientId = clientId;
			this.clientSecret = clientSecret;
			this.clientSecretFile = clientSecretFile;
			this.requiredAudiences = requiredAudiences;
			this.verifyAuthorizedParty = verifyAuthorizedParty;
		}

		public String baseUrl()
		{
			if (baseUrl != null && baseUrl.endsWith("/"))
				return baseUrl.substring(0, baseUrl.length() - 1);
			else
				return baseUrl;
		}

		public String discoveryPath()
		{
			if (discoveryPath != null && !discoveryPath.startsWith("/"))
				return "/" + discoveryPath;
			else
				return discoveryPath;
		}

		@Override
		public String toString()
		{
			return "OidcAuth[baseUrl=" + baseUrl + ", discoveryPath=" + discoveryPath
					+ ", startupConnectionTestEnabled=" + startupConnectionTestEnabled + ", debugLoggingEnabled="
					+ debugLoggingEnabled + ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout
					+ ", trustedRootCertificatesFile=" + trustedRootCertificatesFile + ", clientId=" + clientId
					+ ", clientSecret=" + (clientSecret != null ? "***" : "null") + ", clientSecretFile="
					+ clientSecretFile + ", requiredAudience=" + requiredAudiences + ", verifyAuthorizedParty="
					+ verifyAuthorizedParty + "]";
		}

		public KeyStore readTrustStore() throws IOException
		{
			return doReadTrustStore(this::trustedRootCertificatesFile, PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE);
		}

		public char[] readClientSecret() throws IOException
		{
			return doReadCharArray(this::clientSecret, this::clientSecretFile, PROPERTY_CLIENT_SECRET,
					PROPERTY_CLIENT_SECRET_FILE);
		}

		private ValidationError validateClientSecretVsClientSecretFile(String propertyPrefix)
		{
			if (clientSecret == null && (clientSecretFile == null || clientSecretFile.isBlank()))
				return error(propertyPrefix, PROPERTY_CLIENT_SECRET, PROPERTY_CLIENT_SECRET_FILE, "not defined");
			else
				return null;
		}

		private ValidationError validateTrustStore(String propertyPrefix)
		{
			ValidationError propertyFileNotReadableOrBlank = propertyFileNotReadableOrBlank(trustedRootCertificatesFile,
					propertyPrefix, PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE);

			if (propertyFileNotReadableOrBlank != null)
				return propertyFileNotReadableOrBlank;

			try
			{
				readTrustStore();
				return null;
			}
			catch (IOException e)
			{
				logger.debug("Unable to read trust store", e);

				return error(propertyPrefix, PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE, e.getMessage());
			}
		}

		@Override
		public Stream<ValidationError> validate(String propertyPrefix)
		{
			return Stream.of(propertyNullOrBlank(baseUrl, propertyPrefix, PROPERTY_BASE_URL),
					propertyValueToLarge(connectTimeout, propertyPrefix, PROPERTY_CONNECT_TIMEOUT),
					propertyValueToLarge(readTimeout, propertyPrefix, PROPERTY_READ_TIMEOUT),
					propertyNullOrBlank(clientId, propertyPrefix, PROPERTY_CLIENT_ID),
					propertyBlank(clientSecret, propertyPrefix, PROPERTY_CLIENT_SECRET),
					propertyFileNotReadableOrNotSingleLine(clientSecretFile, propertyPrefix,
							PROPERTY_CLIENT_SECRET_FILE),
					propertiesConflicting(clientSecret, clientSecretFile, propertyPrefix, PROPERTY_CLIENT_SECRET,
							PROPERTY_CLIENT_SECRET_FILE),
					propertyValuesNullOrBlank(requiredAudiences, propertyPrefix, PROPERTY_REQUIRED_AUDIENCE),
					validateClientSecretVsClientSecretFile(propertyPrefix), validateTrustStore(propertyPrefix));
		}
	}

	@JsonCreator
	public FhirClientConfigYaml(@JsonProperty(FhirClientConfigYaml.PROPERTY_BASE_URL) String baseUrl,
			@JsonProperty(FhirClientConfigYaml.PROPERTY_TEST_CONNECTION_ON_STARTUP) Boolean startupConnectionTestEnabled,
			@JsonProperty(FhirClientConfigYaml.PROPERTY_ENABLE_DEBUG_LOGGING) Boolean debugLoggingEnabled,
			@JsonProperty(FhirClientConfigYaml.PROPERTY_CONNECT_TIMEOUT) Duration connectTimeout,
			@JsonProperty(FhirClientConfigYaml.PROPERTY_READ_TIMEOUT) Duration readTimeout,
			@JsonProperty(FhirClientConfigYaml.PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE) String trustedRootCertificatesFile,
			@JsonProperty(FhirClientConfigYaml.PROPERTY_CERT_AUTH) CertificateAuth certAuth,
			@JsonProperty(FhirClientConfigYaml.PROPERTY_BASIC_AUTH) BasicAuth basicAuth,
			@JsonProperty(FhirClientConfigYaml.PROPERTY_BEARER_AUTH) BearerAuth bearerAuth,
			@JsonProperty(FhirClientConfigYaml.PROPERTY_OIDC_AUTH) OidcAuth oidcAuth)
	{
		this.baseUrl = baseUrl;
		this.startupConnectionTestEnabled = startupConnectionTestEnabled;
		this.debugLoggingEnabled = debugLoggingEnabled;
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
		this.trustedRootCertificatesFile = trustedRootCertificatesFile;
		this.certAuth = certAuth;
		this.basicAuth = basicAuth;
		this.bearerAuth = bearerAuth;
		this.oidcAuth = oidcAuth;
	}

	public String baseUrl()
	{
		if (baseUrl != null && baseUrl.endsWith("/"))
			return baseUrl.substring(0, baseUrl.length() - 1);
		else
			return baseUrl;
	}

	@Override
	public String toString()
	{
		return "FhirClientYamlConfig[baseUrl=" + baseUrl + ", startupConnectionTestEnabled="
				+ startupConnectionTestEnabled + ", debugLoggingEnabled=" + debugLoggingEnabled + ", connectTimeout="
				+ connectTimeout + ", readTimeout=" + readTimeout + ", trustedRootCertificatesFile="
				+ trustedRootCertificatesFile + ", certAuth=" + certAuth + ", basicAuth=" + basicAuth + ", bearerAuth="
				+ bearerAuth + ", oidcAuth=" + oidcAuth + "]";
	}

	private ValidationError validateTrustStore(String propertyPrefix)
	{
		ValidationError propertyFileNotReadableOrBlank = propertyFileNotReadableOrBlank(trustedRootCertificatesFile,
				propertyPrefix, PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE);

		if (propertyFileNotReadableOrBlank != null)
			return propertyFileNotReadableOrBlank;

		try
		{
			readTrustStore();
			return null;
		}
		catch (IOException e)
		{
			logger.debug("Unable to read trust store", e);

			return error(propertyPrefix, PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE, e.getMessage());
		}
	}

	public KeyStore readTrustStore() throws IOException
	{
		return doReadTrustStore(this::trustedRootCertificatesFile, PROPERTY_TRUSTED_ROOT_CERTIFICATES_FILE);
	}

	@Override
	public Stream<ValidationError> validate(String propertyPrefix)
	{
		if (!propertyPrefix.endsWith("."))
			propertyPrefix += ".";

		Stream<ValidationError> errors = Stream.of(propertyNullOrBlank(baseUrl, propertyPrefix, PROPERTY_BASE_URL),
				propertyValueToLarge(connectTimeout, propertyPrefix, PROPERTY_CONNECT_TIMEOUT),
				propertyValueToLarge(readTimeout, propertyPrefix, PROPERTY_READ_TIMEOUT),
				validateTrustStore(propertyPrefix));

		return Stream
				.of(errors, certAuth == null ? null : certAuth.validate(propertyPrefix + PROPERTY_CERT_AUTH + "."),
						basicAuth == null ? null : basicAuth.validate(propertyPrefix + PROPERTY_BASIC_AUTH + "."),
						bearerAuth == null ? null : bearerAuth.validate(propertyPrefix + PROPERTY_BEARER_AUTH + "."),
						oidcAuth == null ? null : oidcAuth.validate(propertyPrefix + PROPERTY_OIDC_AUTH + "."))
				.filter(Objects::nonNull).flatMap(Function.identity()).filter(Objects::nonNull);
	}

	private static ValidationError propertyValueToLarge(Duration value, String propertyPrefix, String propertyName)
	{
		if (value != null && value.toMillis() > Integer.MAX_VALUE)
			return error(propertyPrefix, propertyName, "too large, max. P24DT20H31M23.647S");
		else
			return null;
	}

	private static ValidationError propertyNullOrBlank(String value, String propertyPrefix, String propertyName)
	{
		if (value == null || value.isBlank())
			return error(propertyPrefix, propertyName, "not defined or blank");
		else
			return null;
	}

	private static ValidationError propertyBlank(char[] value, String propertyPrefix, String propertyName)
	{
		return propertyBlank(value == null ? null : String.valueOf(value), propertyPrefix, propertyName);
	}

	private static ValidationError propertyBlank(String value, String propertyPrefix, String propertyName)
	{
		if (value == null)
			return null;
		else if (value.isBlank())
			return error(propertyPrefix, propertyName, "blank");
		else
			return null;
	}

	private static ValidationError propertyFileNotReadableOrBlank(String value, String propertyPrefix,
			String propertyName)
	{
		return propertyFileNotReadableOr(value, propertyPrefix, propertyName, path ->
		{
			try
			{
				if (Files.size(path) <= 0)
					return error(propertyPrefix, propertyName, "file empty");
			}
			catch (IOException e)
			{
				logger.debug("Unable to determine file size of {}", path.normalize().toAbsolutePath().toString(), e);

				return error(propertyPrefix, propertyName, e.getMessage());
			}

			return null;
		});
	}

	private static ValidationError propertyFileNotReadableOrNotSingleLine(String value, String propertyPrefix,
			String propertyName)
	{
		return propertyFileNotReadableOr(value, propertyPrefix, propertyName, path ->
		{
			try (BufferedReader reader = Files.newBufferedReader(path))
			{
				String firstLine = reader.readLine();
				if (firstLine == null || firstLine.isBlank())
					return error(propertyPrefix, propertyName, "first line empty or blank");

				int read = reader.read();
				if (read >= 0)
					return error(propertyPrefix, propertyName, "more than one line");
			}
			catch (IOException e)
			{
				logger.debug("Unable to read content of {}", path.normalize().toAbsolutePath().toString(), e);

				return error(propertyPrefix, propertyName, e.getMessage());
			}

			return null;
		});
	}

	private static ValidationError propertyFileNotReadableOr(String value, String propertyPrefix, String propertyName,
			Function<Path, ValidationError> contentChecker)
	{
		if (value != null && value.isBlank())
			return error(propertyPrefix, propertyName, "blank");
		else if (value != null && !Files.isReadable(Paths.get(value)))
			return error(propertyPrefix, propertyName, "file '" + value + "' not readable");
		else if (value != null)
			return contentChecker.apply(Paths.get(value));
		else
			return null;
	}

	private static ValidationError propertiesConflicting(Object value1, Object value2, String propertyPrefix,
			String propertyName1, String propertyName2)
	{
		if (value1 != null && value2 != null)
			return error(propertyPrefix, propertyName1, propertyName2, "defined");
		else
			return null;
	}

	private static ValidationError propertyMissing(String value1, String value2, String propertyPrefix,
			String propertyName1, String propertyName2)
	{
		if (value1 != null && (value2 == null || value2.isBlank()))
			return error(propertyPrefix, propertyName2, "not defined or blank");
		else if ((value1 == null || value1.isBlank()) && value2 != null)
			return error(propertyPrefix, propertyName1, "not defined or blank");
		else
			return null;
	}

	private static ValidationError propertyValuesNullOrBlank(List<String> values, String propertyPrefix,
			String propertyName)
	{
		if (values == null)
			return null;

		for (int i = 0; i < values.size(); i++)
		{
			String value = values.get(i);
			if (value == null || value.isBlank())
				return error(propertyPrefix, propertyName, "value at index " + i + " not defined or blank");
		}

		return null;
	}

	private static char[] doReadCharArray(Supplier<char[]> value, Supplier<String> valueFile, String valueProperty,
			String valueFileProperty) throws IOException
	{
		if (value.get() == null && valueFile.get() == null)
			return null;
		else if (value.get() != null)
			return value.get();
		else if (valueFile.get() != null)
		{
			String content = Files.readAllLines(Paths.get(valueFile.get()), StandardCharsets.UTF_8).get(0);
			return content.toCharArray();
		}
		else
			throw new IllegalStateException("not valid: " + valueProperty + " vs. " + valueFileProperty);
	}

	private static KeyStore doReadTrustStore(Supplier<String> valueFile, String valueProperty) throws IOException
	{
		if (valueFile.get() == null)
			return null;
		else
		{
			List<X509Certificate> certificates = PemReader.readCertificates(Paths.get(valueFile.get()));

			if (certificates.isEmpty())
				throw new IOException("no certificates");

			return KeyStoreCreator.jksForTrustedCertificates(certificates);
		}
	}

	private static ValidationError error(String prefix, String name, String message)
	{
		return new PropertyValidationError(prefix + name, message);
	}

	private static ValidationError error(String prefix, String name1, String name2, String message)
	{
		return new PropertiesValidationError(List.of(prefix + name1, prefix + name2), message);
	}
}
