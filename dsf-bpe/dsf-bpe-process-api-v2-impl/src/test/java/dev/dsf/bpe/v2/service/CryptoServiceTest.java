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
package dev.dsf.bpe.v2.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;

import org.apache.commons.io.input.NullInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import de.hsheilbronn.mi.utils.crypto.kem.AbstractKemAesGcm;
import de.hsheilbronn.mi.utils.crypto.kem.EcDhKemAesGcm;
import de.hsheilbronn.mi.utils.crypto.kem.RsaKemAesGcm;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairGeneratorFactory;
import dev.dsf.bpe.v2.service.CryptoService.Kem;
import dev.dsf.bpe.v2.service.CryptoServiceImpl.KemDelegate;
import net.sourceforge.plantuml.NullOutputStream;

@RunWith(Parameterized.class)
public class CryptoServiceTest
{
	@Parameters
	public static Object[][] data()
	{
		CryptoService cryptoService = new CryptoServiceImpl();

		return new Object[][] {
				{ cryptoService.createKeyPairGeneratorX25519AndInitialize().genKeyPair(), cryptoService.createEcDhKem(),
						new EcDhKemAesGcm() },
				{ cryptoService.createKeyPairGeneratorX448AndInitialize().genKeyPair(), cryptoService.createEcDhKem(),
						new EcDhKemAesGcm() },
				{ KeyPairGeneratorFactory.rsa1024().initialize().generateKeyPair(), cryptoService.createRsaKem(),
						new RsaKemAesGcm() },
				{ cryptoService.createKeyPairGeneratorRsa4096AndInitialize().generateKeyPair(),
						cryptoService.createRsaKem(), new RsaKemAesGcm() } };
	}

	@Parameter(0)
	public KeyPair keyPair;

	@Parameter(1)
	public Kem kem;

	@Parameter(2)
	public AbstractKemAesGcm noLimitKem;

	@Test
	public void testEncryptionLimit() throws Exception
	{
		try
		{
			kem.encrypt(new NullInputStream(300 * 1024 * 1024), keyPair.getPublic()).transferTo(new NullOutputStream());

			fail("Expected IOException");
		}
		catch (IOException e)
		{
			assertEquals("Stream limit of " + KemDelegate.ENCRYPT_LIMIT + " bytes exceeded", e.getMessage());
		}
	}

	@Test
	public void testDecryptLimit() throws Exception
	{
		try
		{
			InputStream encrypted = noLimitKem.encrypt(new NullInputStream(KemDelegate.DECRYPT_LIMIT + 1),
					keyPair.getPublic());

			kem.decrypt(encrypted, keyPair.getPrivate()).transferTo(new NullOutputStream());

			fail("Expected IOException");
		}
		catch (IOException e)
		{
			assertEquals("Stream limit of " + KemDelegate.DECRYPT_LIMIT + " bytes exceeded", e.getMessage());
		}
	}
}
