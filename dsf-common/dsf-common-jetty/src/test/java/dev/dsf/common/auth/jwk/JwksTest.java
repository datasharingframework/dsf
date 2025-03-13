package dev.dsf.common.auth.jwk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JwksTest
{
	private static final String jwksString = """
			{
			    "keys": [
			        {
			            "kid": "kncc6492FTtclCO8qJvhS2PvYap_VabfAPOLhK3mkfA",
			            "kty": "RSA",
			            "alg": "RS256",
			            "use": "sig",
			            "n": "5XJkcdAy5gNu9KUrPDAnVK3hsiIT9EMDM81TN_qCcg4eDkn_MWUwwfj8gqaYj5qhJe6rmwhAedUTOvQtZcbKeIrNLPyDCGaQ4R7uIe37sxq6tPiezMBHSLpsLeb2LI2NHuY58HISAjL1cM3pEr6VatbXjomUdLWm6EcNc5ZJg9QuOXkymVdzVVDNy19EyyIzPyrBnl3_6zqwD6r031BNR7pHRstmPxqyiyc_F8559NF34Pnm0R777ItV4M1HRY580V9E84Vv1baBaeC-t_EldVCZo7hkvt5tFoiGm0xecXTKdsFgiKzp1MX8U1P-ci0BPGjYVSjJlQOc9VXGOGEk4Q",
			            "e": "AQAB",
			            "x5c": [
			                "MIIClTCCAX0CBgGG0YH3jjANBgkqhkiG9w0BAQsFADAOMQwwCgYDVQQDDANkc2YwHhcNMjMwMzExMTYyODIxWhcNMzMwMzExMTYzMDAxWjAOMQwwCgYDVQQDDANkc2YwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDlcmRx0DLmA270pSs8MCdUreGyIhP0QwMzzVM3+oJyDh4OSf8xZTDB+PyCppiPmqEl7qubCEB51RM69C1lxsp4is0s/IMIZpDhHu4h7fuzGrq0+J7MwEdIumwt5vYsjY0e5jnwchICMvVwzekSvpVq1teOiZR0taboRw1zlkmD1C45eTKZV3NVUM3LX0TLIjM/KsGeXf/rOrAPqvTfUE1HukdGy2Y/GrKLJz8Xznn00Xfg+ebRHvvsi1XgzUdFjnzRX0TzhW/VtoFp4L638SV1UJmjuGS+3m0WiIabTF5xdMp2wWCIrOnUxfxTU/5yLQE8aNhVKMmVA5z1VcY4YSThAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAA74Cd8sgFwzcvaqYp8DRVehsl+ObzkQ/RTSQ4I8FEA5eu0526NwGP4AaYeMETkl22N7oG04Bl50oLW5kQTaPU91J3IknXPUME3EadVaXWLgheC1AEDvP2Oe7xwRwl7rJRUxfJGDs0q3onRLHNTzP8eQAHHJoJDU35EkI14TNebdDDfSGNWWB5Ucics0Gb4+WoIVWLFlndkYmPnm1a4+C3p/SXO/9ZGM35tpeHfpk8Ov+0gfYvaG9NutpfZpH1XfzgAmJGDUjufYvm2qCFVv0pqNmVrBawj/zx6g1zFCOuDcdxYG7EYxKeuCYfIMVWdIqPkCr8FISNnwhNhlqRXWvMg="
			            ],
			            "x5t": "FXkv4BHgjJ2Qix-TESdDXlcwTOw",
			            "x5t#S256": "OISxUBXQFT3gnnZFlmYeBozuqcygK7_Cebq0QYh-E7g"
			        },
			        {
			            "kid": "Zp7ockRwsxqM6FrZlDJUOVwAxPICO2jBW0Rbk25oYGk",
			            "kty": "RSA",
			            "alg": "RSA-OAEP",
			            "use": "enc",
			            "n": "lmmb56KwnX296BFUirn4voNIbDOwSJImsSTsP8hR2yLuDRUhFQ8gey-KA0Bw-rf6O0nk0ud5iA4pKNP42Q7-aflCUUA5NYN5N9pwgRXIjIKJIRhpHoO49NAS9w0FK1mZrg_Z3isVTso-2xjlSbdzAEabGQzysHcHeE2v9FryM4G4nOMG2xnFLa7suQ3t3YWFgGg-s6Mtwkr2Fyzemi5Oe7uCfI43s-Gs-B6v1sXmasOql8KJwtf9U1aybXlA9MJMi--ykzwPgao_lAh0krGUhrb2j4Y-WDOBrDKohJYUpKtnf19297Ax5Z0nVx96AXbg2CFyorRaOLajK6hZa94tJw",
			            "e": "AQAB",
			            "x5c": [
			                "MIIClTCCAX0CBgGG0YH4fTANBgkqhkiG9w0BAQsFADAOMQwwCgYDVQQDDANkc2YwHhcNMjMwMzExMTYyODIxWhcNMzMwMzExMTYzMDAxWjAOMQwwCgYDVQQDDANkc2YwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCWaZvnorCdfb3oEVSKufi+g0hsM7BIkiaxJOw/yFHbIu4NFSEVDyB7L4oDQHD6t/o7SeTS53mIDiko0/jZDv5p+UJRQDk1g3k32nCBFciMgokhGGkeg7j00BL3DQUrWZmuD9neKxVOyj7bGOVJt3MARpsZDPKwdwd4Ta/0WvIzgbic4wbbGcUtruy5De3dhYWAaD6zoy3CSvYXLN6aLk57u4J8jjez4az4Hq/WxeZqw6qXwonC1/1TVrJteUD0wkyL77KTPA+Bqj+UCHSSsZSGtvaPhj5YM4GsMqiElhSkq2d/X3b3sDHlnSdXH3oBduDYIXKitFo4tqMrqFlr3i0nAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAEHiBIjRkZ/BgEm80DtHKpaBgV5WvTCcHKPdn7ix1tGElGlBktH08fvOcRGcxvFEtp1jlmuYmg5sM3FIiPCThA1hlLvLUpnjwJV0DGfU62qQjBTW1zlmOsLRVCbbZam2tTS5+827iB9rb4SB62BJe/onRS+5gZHbF3buv3rJX7xwQSMVokXsq55JWI44tT5G/rQLlvSaqnc9QxoPe+p299hCad4isdpYkKGdiVtAs5Py+pwpJalUtCgtr6RSPBFv4Lz8BZ4SS0gdnaELjYLSg0BK7+GycayFfXVHFsS3FBQ7o3bTmM7StKc9FT9na1tVyTlBx8yhvIOAPgRZSho8JGA="
			            ],
			            "x5t": "dQL-LEROCVCUfvs0W_5ayioFWjA",
			            "x5t#S256": "yi-b9TklWk5X5d_Pr_moQVmdkdVa4wZTuYnDxWXrXag"
			        }
			    ]
			}""";

	@Test
	public void testDecodeJwks() throws Exception
	{
		Jwks jwks = Jwks.from(jwksString);

		assertNotNull(jwks);
		assertNotNull(jwks.getAllKeys());
		assertEquals(2, jwks.getAllKeys().size());

		Jwk jwk0 = jwks.getAllKeys().get(0);
		Jwk jwk1 = jwks.getAllKeys().get(1);

		assertNotNull(jwk0);
		assertNotNull(jwk1);

		assertNotNull(jwk0.getId());
		assertEquals("kncc6492FTtclCO8qJvhS2PvYap_VabfAPOLhK3mkfA", jwk0.getId());
		assertNotNull(jwk1.getId());
		assertEquals("Zp7ockRwsxqM6FrZlDJUOVwAxPICO2jBW0Rbk25oYGk", jwk1.getId());

		assertEquals(jwk0, jwks.getKey(jwk0.getId()));
		assertEquals(jwk1, jwks.getKey(jwk1.getId()));

		assertTrue(jwk0.getPublicKey().isPresent());
		assertTrue(jwk1.getPublicKey().isPresent());

		assertEquals("RSA", jwk0.getPublicKey().get().getAlgorithm());
		assertEquals("RSA", jwk1.getPublicKey().get().getAlgorithm());
	}
}
