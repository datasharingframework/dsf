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
package dev.dsf.fhir.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.ca.CertificateAuthority;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest;
import de.hsheilbronn.mi.utils.crypto.ca.CertificationRequest.CertificationRequestAndPrivateKey;
import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;

public class X509Certificates extends ExternalResource
{
	public static final String PRACTITIONER_CLIENT_MAIL = "practitioner@invalid";
	public static final String MINIMAL_CLIENT_MAIL = "minimal@invalid";
	public static final String ADMIN_CLIENT_MAIL = "admin@invalid";

	public static record CertificateAndPrivateKey(X509Certificate caCertificate, X509Certificate certificate,
			PrivateKey privateKey)
	{
		public KeyStore trustStore()
		{
			return KeyStoreCreator.jksForTrustedCertificates(caCertificate);
		}

		public KeyStore keyStore()
		{
			return KeyStoreCreator.jksForPrivateKeyAndCertificateChain(privateKey, PASSWORD, certificate);
		}

		public char[] keyStorePassword()
		{
			return PASSWORD;
		}

		public String certificateSha512ThumbprintHex()
		{
			try
			{
				return Hex.encodeHexString(MessageDigest.getInstance("SHA-512").digest(certificate().getEncoded()));
			}
			catch (CertificateEncodingException | NoSuchAlgorithmException e)
			{
				logger.error("Error while calculating SHA-512 certificate thumbprint", e);
				throw new RuntimeException(e);
			}
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(X509Certificates.class);

	public static final char[] PASSWORD = "password".toCharArray();

	private X509Certificate caCertificate;
	private CertificateAndPrivateKey serverCertificate;
	private CertificateAndPrivateKey clientCertificate;
	private CertificateAndPrivateKey practitionerClientCertificate;
	private CertificateAndPrivateKey adminClientCertificate;
	private CertificateAndPrivateKey minimalClientCertificate;
	private CertificateAndPrivateKey externalClientCertificate;

	private Path caCertificateFile;
	private Path clientCertificateFile;
	private Path clientCertificatePrivateKeyFile;
	private Path externalClientCertificateFile;
	private Path externalClientCertificatePrivateKeyFile;
	private Path practitionerClientCertificateFile;
	private Path practitionerClientCertificatePrivateKeyFile;
	private Path adminClientCertificateFile;
	private Path adminClientCertificatePrivateKeyFile;
	private Path minimalClientCertificateFile;
	private Path minimalClientCertificatePrivateKeyFile;

	private List<Path> filesToDelete;

	@Override
	protected void before() throws Throwable
	{
		createX509Certificates();
	}

	@Override
	protected void after()
	{
		deleteX509Certificates();
	}

	public CertificateAndPrivateKey getServerCertificate()
	{
		return serverCertificate;
	}

	public CertificateAndPrivateKey getClientCertificate()
	{
		return clientCertificate;
	}

	public CertificateAndPrivateKey getExternalClientCertificate()
	{
		return externalClientCertificate;
	}

	public CertificateAndPrivateKey getPractitionerClientCertificate()
	{
		return practitionerClientCertificate;
	}

	public CertificateAndPrivateKey getAdminClientCertificate()
	{
		return adminClientCertificate;
	}

	public CertificateAndPrivateKey getMinimalClientCertificate()
	{
		return minimalClientCertificate;
	}

	public X509Certificate getCaCertificate()
	{
		return caCertificate;
	}

	public Path getCaCertificateFile()
	{
		return caCertificateFile;
	}

	public Path getClientCertificateFile()
	{
		return clientCertificateFile;
	}

	public Path getClientCertificatePrivateKeyFile()
	{
		return clientCertificatePrivateKeyFile;
	}

	public Path getExternalClientCertificateFile()
	{
		return externalClientCertificateFile;
	}

	public Path getExternalClientCertificatePrivateKeyFile()
	{
		return externalClientCertificatePrivateKeyFile;
	}

	public Path getPractitionerClientCertificateFile()
	{
		return practitionerClientCertificateFile;
	}

	public Path getPractitionerClientCertificatePrivateKeyFile()
	{
		return practitionerClientCertificatePrivateKeyFile;
	}

	public Path getAdminClientCertificateFile()
	{
		return adminClientCertificateFile;
	}

	public Path getAdminClientCertificatePrivateKeyFile()
	{
		return adminClientCertificatePrivateKeyFile;
	}

	public Path getMinimalClientCertificateFile()
	{
		return minimalClientCertificateFile;
	}

	public Path getMinimalClientCertificatePrivateKeyFile()
	{
		return minimalClientCertificatePrivateKeyFile;
	}

	private void createX509Certificates() throws InvalidKeyException, NoSuchAlgorithmException, KeyStoreException,
			CertificateException, OperatorCreationException, IllegalStateException, IOException, InvalidKeySpecException
	{
		logger.info("Creating certificates ...");

		Path caCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path clientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path clientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path externalClientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path externalClientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path practitionerClientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path practitionerClientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path adminClientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path adminClientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path minimalClientCertificateFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");
		Path minimalClientCertificatePrivateKeyFile = Paths.get("target", UUID.randomUUID().toString() + ".pem");

		CertificateAuthority ca = CertificateAuthority
				.builderSha384EcdsaSecp384r1("DE", null, null, null, null, "Junit Test CA")
				.setValidityPeriod(Period.ofDays(1)).build();
		X509Certificate caCertificate = ca.getCertificate();
		PemWriter.writeCertificate(caCertificate, caCertificateFile);

		// -- server
		CertificationRequestAndPrivateKey serverRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "test-server").generateKeyPair().addDnsName("localhost")
				.build();
		X509Certificate serverCertificate = ca.signServerCertificate(serverRequest, Period.ofDays(1));
		// server --

		// -- client
		CertificationRequestAndPrivateKey clientRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "test-client").generateKeyPair().build();
		X509Certificate clientCertificate = ca.signClientCertificate(clientRequest, Period.ofDays(1));
		PemWriter.writeCertificate(clientCertificate, clientCertificateFile);
		PemWriter.writePrivateKey(clientRequest.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(clientCertificatePrivateKeyFile);
		// client --

		// -- external client
		CertificationRequestAndPrivateKey externalClientRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "external-client").generateKeyPair().build();
		X509Certificate externalClientCertificate = ca.signClientCertificate(externalClientRequest, Period.ofDays(1));
		PemWriter.writeCertificate(externalClientCertificate, externalClientCertificateFile);
		PemWriter.writePrivateKey(externalClientRequest.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(externalClientCertificatePrivateKeyFile);
		// external client --

		// -- practitioner client
		CertificationRequestAndPrivateKey practitionerClientRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "practitioner-client").generateKeyPair()
				.setEmail(PRACTITIONER_CLIENT_MAIL).build();
		X509Certificate practitionerClientCertificate = ca.signClientCertificate(practitionerClientRequest);
		PemWriter.writeCertificate(practitionerClientCertificate, practitionerClientCertificateFile);
		PemWriter.writePrivateKey(practitionerClientRequest.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(practitionerClientCertificatePrivateKeyFile);
		// practitioner client --

		// -- admin client
		CertificationRequestAndPrivateKey adminClientRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "admin-client").generateKeyPair().setEmail(ADMIN_CLIENT_MAIL)
				.build();
		X509Certificate adminClientCertificate = ca.signClientCertificate(adminClientRequest, Period.ofDays(1));
		PemWriter.writeCertificate(adminClientCertificate, adminClientCertificateFile);
		PemWriter.writePrivateKey(adminClientRequest.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(adminClientCertificatePrivateKeyFile);
		// admin client --

		// -- minimal client
		CertificationRequestAndPrivateKey minimalClientRequest = CertificationRequest
				.builder(ca, "DE", null, null, null, null, "minimal-client").generateKeyPair()
				.setEmail(MINIMAL_CLIENT_MAIL).build();
		X509Certificate minimalClientCertificate = ca.signClientCertificate(minimalClientRequest, Period.ofDays(1));
		PemWriter.writeCertificate(minimalClientCertificate, minimalClientCertificateFile);
		PemWriter.writePrivateKey(minimalClientRequest.getPrivateKey()).asPkcs8().encryptedAes128(PASSWORD)
				.toFile(minimalClientCertificatePrivateKeyFile);
		// minimal client --

		this.caCertificate = caCertificate;
		this.serverCertificate = new CertificateAndPrivateKey(caCertificate, serverCertificate,
				serverRequest.getPrivateKey());
		this.clientCertificate = new CertificateAndPrivateKey(caCertificate, clientCertificate,
				clientRequest.getPrivateKey());
		this.externalClientCertificate = new CertificateAndPrivateKey(caCertificate, externalClientCertificate,
				externalClientRequest.getPrivateKey());
		this.practitionerClientCertificate = new CertificateAndPrivateKey(caCertificate, practitionerClientCertificate,
				practitionerClientRequest.getPrivateKey());
		this.adminClientCertificate = new CertificateAndPrivateKey(caCertificate, adminClientCertificate,
				adminClientRequest.getPrivateKey());
		this.minimalClientCertificate = new CertificateAndPrivateKey(caCertificate, minimalClientCertificate,
				minimalClientRequest.getPrivateKey());

		this.caCertificateFile = caCertificateFile;
		this.clientCertificateFile = clientCertificateFile;
		this.clientCertificatePrivateKeyFile = clientCertificatePrivateKeyFile;
		this.externalClientCertificateFile = externalClientCertificateFile;
		this.externalClientCertificatePrivateKeyFile = externalClientCertificatePrivateKeyFile;
		this.practitionerClientCertificateFile = practitionerClientCertificateFile;
		this.practitionerClientCertificatePrivateKeyFile = practitionerClientCertificatePrivateKeyFile;
		this.adminClientCertificateFile = adminClientCertificateFile;
		this.adminClientCertificatePrivateKeyFile = adminClientCertificatePrivateKeyFile;
		this.minimalClientCertificateFile = minimalClientCertificateFile;
		this.minimalClientCertificatePrivateKeyFile = minimalClientCertificatePrivateKeyFile;

		filesToDelete = List.of(caCertificateFile, clientCertificateFile, clientCertificatePrivateKeyFile,
				externalClientCertificateFile, externalClientCertificatePrivateKeyFile,
				practitionerClientCertificateFile, practitionerClientCertificatePrivateKeyFile,
				adminClientCertificateFile, adminClientCertificatePrivateKeyFile, minimalClientCertificateFile,
				minimalClientCertificatePrivateKeyFile);
	}

	private void deleteX509Certificates()
	{
		logger.info("Deleting certificate files {} ...", filesToDelete);
		filesToDelete.forEach(this::deleteFile);
	}

	private void deleteFile(Path file)
	{
		try
		{
			Files.delete(file);
		}
		catch (IOException e)
		{
			logger.error("Error while deleting certificate file {}, error: {}", file.toString(), e.toString());
		}
	}
}
