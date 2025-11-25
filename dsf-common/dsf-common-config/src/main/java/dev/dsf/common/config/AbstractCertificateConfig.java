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
package dev.dsf.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateValidator;
import de.hsheilbronn.mi.utils.crypto.io.KeyStoreReader;
import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairValidator;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;

public abstract class AbstractCertificateConfig
{
	private static final class RuntimeIOException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public RuntimeIOException(IOException cause)
		{
			super(cause);
		}

		@Override
		public synchronized IOException getCause()
		{
			return (IOException) super.getCause();
		}
	}

	private String propertyToEnvironmentVariableName(String propertyName)
	{
		return propertyName.toUpperCase(Locale.ENGLISH).replace('.', '_');
	}

	protected String errorMessage(String propertyName, String message)
	{
		return "Config property " + propertyName + " / environment variable "
				+ propertyToEnvironmentVariableName(propertyName) + ": " + message;
	}

	protected String errorMessage(String propertyName1, String propertyName2, String message)
	{
		return "Config properties " + propertyName1 + ", " + propertyName2 + " / environment variables "
				+ propertyToEnvironmentVariableName(propertyName1) + ", "
				+ propertyToEnvironmentVariableName(propertyName2) + ": " + message;
	}

	protected RuntimeException propertyNotDefined(String propertyName)
	{
		return new RuntimeException(errorMessage(propertyName, "not defined"));
	}

	private Path checkFileOrFolder(String folder, String propertyName) throws IOException
	{
		if (folder == null || folder.isBlank())
			throw propertyNotDefined(propertyName);

		Path path = Paths.get(folder);

		if (Files.isRegularFile(path) && Files.isReadable(path))
			return path;
		else if (Files.isDirectory(path) && Files.isReadable(path))
			return path;
		else
			throw new IOException(
					errorMessage(propertyName, path.toAbsolutePath().toString() + " not a readable file or directory"));
	}

	protected Path checkFile(String file, String propertyName) throws IOException
	{
		if (file == null || file.isBlank())
			throw propertyNotDefined(propertyName);

		Path path = Paths.get(file);

		if (!Files.isReadable(path))
			throw new IOException(errorMessage(propertyName, path.toAbsolutePath().toString() + " not readable"));

		return path;
	}

	protected Path checkOptionalFile(String file, String propertyName) throws IOException
	{
		if (file == null || file.isBlank())
			return null;
		else
		{
			Path path = Paths.get(file);

			if (!Files.isReadable(path))
				throw new IOException(errorMessage(propertyName, path.toAbsolutePath().toString() + " not readable"));

			return path;
		}
	}

	private Path checkOptionalFileOrFolder(String value, String propertyName) throws IOException
	{
		Objects.requireNonNull(propertyName, "propertyName");

		if (value == null || value.isBlank())
			return null;
		else
		{
			Path path = Paths.get(value);

			if (Files.isRegularFile(path) && Files.isReadable(path))
				return path;
			else if (Files.isDirectory(path) && Files.isReadable(path))
				return path;
			else
				throw new IOException(errorMessage(propertyName,
						path.toAbsolutePath().toString() + " not a readable file or directory"));
		}
	}

	private KeyStore createTrustStore(Path path, String propertyName) throws IOException
	{
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(propertyName, "propertyName");

		if (Files.isRegularFile(path) && Files.isReadable(path))
			return createTrustStoreFromFile(path);
		else if (Files.isDirectory(path) && Files.isReadable(path))
			return createTrustStoreFromDirectory(path);
		else
			throw new IOException(
					errorMessage(propertyName, path.toAbsolutePath().toString() + " not a readable file or directory"));
	}

	private KeyStore createTrustStoreFromFile(Path path) throws IOException
	{
		List<X509Certificate> certificates = PemReader.readCertificates(path);

		return KeyStoreCreator.jksForTrustedCertificates(certificates);
	}

	private KeyStore createTrustStoreFromDirectory(Path path) throws IOException
	{
		try
		{
			List<X509Certificate> certificates = Files.list(path).filter(Files::isReadable).filter(f ->
			{
				String filename = f.getFileName().toString();
				return filename.endsWith(".pem") || filename.endsWith(".crt");
			}).flatMap(f ->
			{
				try
				{
					return PemReader.readCertificates(f).stream();
				}
				catch (IOException e)
				{
					throw new RuntimeIOException(e);
				}
			}).toList();

			return KeyStoreCreator.jksForTrustedCertificates(certificates);
		}
		catch (RuntimeIOException e)
		{
			throw e.getCause();
		}
	}

	protected KeyStore createOptionalTrustStore(String propertyValue, String propertyName)
	{
		try
		{
			Path path = checkOptionalFileOrFolder(propertyValue, propertyName);

			return path == null ? null : createTrustStore(path, propertyName);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected KeyStore createTrustStore(String propertyValue, String propertyName)
	{
		try
		{
			Path path = checkFileOrFolder(propertyValue, propertyName);

			return createTrustStore(path, propertyName);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected KeyStore createClientKeyStore(String certificateFile, String privateKeyFile, char[] privateKeyPassword,
			char[] keyStorePassword, String certificatePropertyName, String privateKeyPropertyName)
	{
		try
		{
			Path certificatePath = checkFile(certificateFile, certificatePropertyName);
			Path privateKeyPath = checkFile(privateKeyFile, privateKeyPropertyName);

			List<X509Certificate> certificates = PemReader.readCertificates(certificatePath);
			PrivateKey privateKey = PemReader.readPrivateKey(privateKeyPath, privateKeyPassword);

			return createClientKeyStore(keyStorePassword, certificatePropertyName, privateKeyPropertyName,
					certificatePath, privateKeyPath, certificates, privateKey);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected KeyStore createOptionalClientKeyStore(String certificateFile, String privateKeyFile,
			char[] privateKeyPassword, char[] keyStorePassword, String certificatePropertyName,
			String privateKeyPropertyName)
	{
		try
		{
			Path certificatePath = checkOptionalFile(certificateFile, certificatePropertyName);
			Path privateKeyPath = checkOptionalFile(privateKeyFile, privateKeyPropertyName);

			if (certificatePath == null && privateKeyPath == null)
				return null;
			else if (certificatePath == null)
				throw propertyNotDefined(certificatePropertyName);
			else if (privateKeyPath == null)
				throw propertyNotDefined(privateKeyPropertyName);

			List<X509Certificate> certificates = PemReader.readCertificates(certificatePath);
			PrivateKey privateKey = PemReader.readPrivateKey(privateKeyPath, privateKeyPassword);

			return createClientKeyStore(keyStorePassword, certificatePropertyName, privateKeyPropertyName,
					certificatePath, privateKeyPath, certificates, privateKey);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private KeyStore createClientKeyStore(char[] keyStorePassword, String certificatePropertyName,
			String privateKeyPropertyName, Path certificatePath, Path privateKeyPath,
			List<X509Certificate> certificates, PrivateKey privateKey) throws IOException
	{
		if (certificates.isEmpty())
		{
			String errorMessage = errorMessage(certificatePropertyName,
					"No certificates in '" + certificatePath.normalize().toAbsolutePath().toString() + "'");
			throw new IOException(errorMessage);
		}
		else if (!CertificateValidator.isClientCertificate(certificates.get(0)))
		{
			String errorMessage = errorMessage(certificatePropertyName, "First certificate from '"
					+ certificatePath.normalize().toAbsolutePath().toString() + "' not a client certificate");
			throw new IOException(errorMessage);
		}
		else if (!KeyPairValidator.matches(privateKey, certificates.get(0).getPublicKey()))
		{
			String errorMessage = errorMessage(certificatePropertyName, privateKeyPropertyName,
					"Private-key at '" + privateKeyPath.normalize().toAbsolutePath().toString()
							+ "' not matching Public-key from " + (certificates.size() > 1 ? "first " : "")
							+ "certificate at '" + certificatePath.normalize().toAbsolutePath().toString() + "'");
			throw new IOException(errorMessage);
		}

		return KeyStoreCreator.jksForPrivateKeyAndCertificateChain(privateKey, keyStorePassword, certificates);
	}

	protected KeyStore createKeyStoreFromP12(String file, char[] keyStorePassword, String propertyName)
	{
		try
		{
			Path path = checkOptionalFile(file, propertyName);

			KeyStore keyStore = KeyStoreReader.readPkcs12(path, keyStorePassword);

			List<String> aliases = Collections.list(keyStore.aliases());
			if (aliases.size() != 1)
				throw new IOException(
						errorMessage(propertyName, "KeyStore at '" + path.normalize().toAbsolutePath().toString()
								+ "' has " + aliases.size() + " entries " + aliases + ", expected 1"));
			if (keyStore.getCertificateChain(aliases.get(0)) == null)
				throw new IOException(
						errorMessage(propertyName, "KeyStore at '" + path.normalize().toAbsolutePath().toString()
								+ "' has no certificate chain for entry " + aliases.get(0)));
			if (!keyStore.isKeyEntry(aliases.get(0)))
				throw new IOException(errorMessage(propertyName, "KeyStore at '"
						+ path.normalize().toAbsolutePath().toString() + "' has no key for entry " + aliases.get(0)));

			return keyStore;
		}
		catch (IOException | KeyStoreException e)
		{
			throw new RuntimeException(e);
		}
	}
}
