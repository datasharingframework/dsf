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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.api.client.oidc.Jwks;

public class JwksImplTest
{
	private static final String jwksRsa = """
			{
			  "keys": [
			    {
			      "kid": "LagJsyr6F7gh6SxYXcZEgLNc4vJTJaE5L1RrzOzZVFA",
			      "kty": "RSA",
			      "alg": "RSA-OAEP",
			      "use": "enc",
			      "n": "jByV5hKt5Bmf9PlqLgmiJuyCR2b5CKD_R9MkNrWVXQZd_5fclYHQ8mcF_w22MSV-uFvYI0M8ND4tjUb0ySYQqqjQhrcCoxfyH6XbIaLTQIgvgh4V4atIeca_Blm2_MuwyrO-QpU7CLaCBu45uYUzlcPlLIsJ_NAfALUnSPbJDuJNOfsBkr8QWvzdEqJuRDEXpKsZQ3L89FbsGqN_6x6QmFVqkt4XC6VT4NH8H_XuaA9UAqe0PO-CKPEB7tDCfVmweNaNxrrXiq0tl-NXGyO9Okkc4l_k8NNV9DtaJ34IkG1zmEaf5MstYDEnpCt1KIRPOGQls2T8GuGrk2CNTRFPPQ",
			      "e": "AQAB",
			      "x5c": [
			        "MIICmzCCAYMCBgGG5tRg8TANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtZWRpYzEwHhcNMjMwMzE1MTk1MDIzWhcNMzMwMzE1MTk1MjAzWjARMQ8wDQYDVQQDDAZtZWRpYzEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCMHJXmEq3kGZ/0+WouCaIm7IJHZvkIoP9H0yQ2tZVdBl3/l9yVgdDyZwX/DbYxJX64W9gjQzw0Pi2NRvTJJhCqqNCGtwKjF/IfpdshotNAiC+CHhXhq0h5xr8GWbb8y7DKs75ClTsItoIG7jm5hTOVw+Usiwn80B8AtSdI9skO4k05+wGSvxBa/N0Som5EMRekqxlDcvz0Vuwao3/rHpCYVWqS3hcLpVPg0fwf9e5oD1QCp7Q874Io8QHu0MJ9WbB41o3GuteKrS2X41cbI706SRziX+Tw01X0O1onfgiQbXOYRp/kyy1gMSekK3UohE84ZCWzZPwa4auTYI1NEU89AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAAPP9WczoP64zQwL1fL3TtMz/UvY2YUbPcuzGAJYQ+dXuHSvD/VOLSlmiPZKdZMi+8aFrqqZFbxiSFJeqo1iTqy9VaafvcH/BtugHjE7xoLMI9JqWM1+wVcd/MpxJxNjU5YL8ksT05fdWORk2kR5mr4EdL672z1BlpnUbv7Zbsf38P0QXY4aXi9cJuClZHq3W10PW97jI3IdakThMblMvyTJ6Hc69c8xAW+Xm2vgw+KmStEs2rjzgVGa39c4zVQSoVsZrTtLMqKviuRm0unbjXuTuj3ZImvOWPGcFcP2U6ewjMWBNYfwH5DRNlWPQ/ykWHafionbdy7N2Gqe+osg2hI="
			      ],
			      "x5t": "dde2bv8tqhTWeZrM-DTccwtd_fQ",
			      "x5t#S256": "lXxIN8UdqkEXKp1k7rcgWW-kRZwheMJGoYiETenwxmo"
			    },
			    {
			      "kid": "wvQMhgYeYb-GyI_if_oxGb6AlYSMEjFI-Y5dVx5LYmg",
			      "kty": "RSA",
			      "alg": "RS256",
			      "use": "sig",
			      "n": "sQLMZva07JGJLt_h9NMwNcklJMdDRYmoImRqd2Xn4JxNQGO7dcvDtwPj4hYnKykT_zKT6Tls86gx1lbd1tpbZJe-9zs5wmRzraMZ-_2wN6nD_lWJYVwttCzPVu-HNF8LGlvwepOw_Jn7IPC_uBqPAwzC9H_Lk5iZc_F52jmspOqembV6Pku-fJhXifihQK-jDahp1URt40vCjWzMWlgKkfoJGBgL4c_L1edLjHBOsjRIHGrXw9PTnA2Nz1bh2JEmYW0ARUnQ3R9CHIeBvrCq-OKCIYUkeMAIaOF9PF3OYcbticdoNwMCLBoTGUI5qyDLB6TfY-GIQ8hAHVlVf5LWUw",
			      "e": "AQAB",
			      "x5c": [
			        "MIICmzCCAYMCBgGG5tRgdDANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtZWRpYzEwHhcNMjMwMzE1MTk1MDIzWhcNMzMwMzE1MTk1MjAzWjARMQ8wDQYDVQQDDAZtZWRpYzEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCxAsxm9rTskYku3+H00zA1ySUkx0NFiagiZGp3ZefgnE1AY7t1y8O3A+PiFicrKRP/MpPpOWzzqDHWVt3W2ltkl773OznCZHOtoxn7/bA3qcP+VYlhXC20LM9W74c0XwsaW/B6k7D8mfsg8L+4Go8DDML0f8uTmJlz8XnaOayk6p6ZtXo+S758mFeJ+KFAr6MNqGnVRG3jS8KNbMxaWAqR+gkYGAvhz8vV50uMcE6yNEgcatfD09OcDY3PVuHYkSZhbQBFSdDdH0Ich4G+sKr44oIhhSR4wAho4X08Xc5hxu2Jx2g3AwIsGhMZQjmrIMsHpN9j4YhDyEAdWVV/ktZTAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAD+CVB9j9LG13DYowfsZpZdVin5gDjtqsVXX78oWjpzpNAz4KvPVVEfznZfg2SdBBaVASjq3b3lkpbHrSrB6cwxpd2dXF1mVWXUcdq6M7nbxKe0QZxVJR2xEGov+lrrI0qtJ28KMz9o3qmUqcqvXOuw46gFsuDMOCdO+tQHzOWc0JYm0g2cNOW5AhHlz+YPXtrK1Tu8kHB98dLITb0W3miyf3PQwlMywyiwrtpvOfF38On5FkGmqF2HCQc2HglGlnixjReLkX8I3ltpijo3LkBAJ7Ob9V2334+DrP7OpirJo8GeE/MMPC6pTZT8EjY+H10AyhoFlvKaqYXBmJ87XQ2I="
			      ],
			      "x5t": "goOVPKacQT3JfbghWIXFEqU8_S4",
			      "x5t#S256": "LsJXJVnEFe_ICgU3aW27vWCZ1XM6O4-PwjOT5j_ZXdE"
			    }
			  ]
			}""";

	private static final String jwksEc = """
			{
			  "keys": [
			    {
			      "kid": "kid_value",
			      "kty": "EC",
			      "alg": "ES256",
			      "crv": "P-256",
			      "x": "SVqB4JcUD6lsfvqMr-OKUNUphdNn64Eay60978ZlL74",
			      "y": "lf0u0pMj4lGAzZix5u4Cm5CMQIgMNpkwy163wtKYVKI"
			    }
			  ]
			}""";


	@Test
	public void parseRsa() throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		Jwks jwks = mapper.readValue(new StringReader(jwksRsa), JwksImpl.class);

		assertNotNull(jwks);
		assertNotNull(jwks.getKeys());
		assertEquals(2, jwks.getKeys().size());

		assertNotNull(jwks.getKey("LagJsyr6F7gh6SxYXcZEgLNc4vJTJaE5L1RrzOzZVFA"));
		assertNotNull(jwks.getKey("wvQMhgYeYb-GyI_if_oxGb6AlYSMEjFI-Y5dVx5LYmg"));
	}

	@Test
	public void parseEc() throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		Jwks jwks = mapper.readValue(new StringReader(jwksEc), JwksImpl.class);

		assertNotNull(jwks);
		assertNotNull(jwks.getKeys());
		assertEquals(1, jwks.getKeys().size());

		assertNotNull(jwks.getKey("kid_value"));
	}
}
