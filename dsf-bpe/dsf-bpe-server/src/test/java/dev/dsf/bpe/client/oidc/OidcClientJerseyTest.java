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
package dev.dsf.bpe.client.oidc;

import static org.junit.Assert.assertNotNull;

import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.Duration;

import org.junit.Ignore;
import org.junit.Test;

import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;

@Ignore
// Needs keycloak service from 3dic-ttp dev setup, "Service accounts roles" needs to be activated for dic1-fhir client
public class OidcClientJerseyTest
{
	@Test
	public void getAccessToken() throws Exception
	{
		KeyStore trustStore = KeyStoreCreator.jksForTrustedCertificates(PemReader
				.readCertificate(Paths.get("../../dsf-tools/dsf-tools-test-data-generator/cert/DSF_Dev_Root_CA.pem")));

		OidcClientJersey client = new OidcClientJersey("https://keycloak:8443/realms/dic1",
				"/.well-known/openid-configuration", "dic1-fhir", "mF0GEtjFoyWIM3in4VCwifGI3azb4DTn".toCharArray(),
				trustStore, null, null, null, null, null, "Test Client", Duration.ofSeconds(10), Duration.ofSeconds(5),
				true, Duration.ofSeconds(10), null, false);

		char[] accessToken = client.asOidcClientWithDecodedJwt().getAccessToken();
		assertNotNull(accessToken);
	}
}