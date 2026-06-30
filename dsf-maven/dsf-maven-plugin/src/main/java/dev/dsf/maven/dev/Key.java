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
package dev.dsf.maven.dev;

import java.io.File;
import java.security.AsymmetricKey;
import java.util.List;
import java.util.function.Predicate;

import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairGeneratorFactory;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairValidator;

public class Key
{
	public static enum Type
	{
		RSA1024(KeyPairGeneratorFactory.rsa1024(), "RSA 1024", KeyPairValidator::isRsa1024),

		RSA2048(KeyPairGeneratorFactory.rsa2048(), "RSA 2048", KeyPairValidator::isRsa2048),

		RSA3072(KeyPairGeneratorFactory.rsa3072(), "RSA 3072", KeyPairValidator::isRsa3072),

		RSA4096(KeyPairGeneratorFactory.rsa4096(), "RSA 4096", KeyPairValidator::isRsa4096),

		SECP256R1(KeyPairGeneratorFactory.secp256r1(), "Secp256r1", KeyPairValidator::isSecp256r1),

		SECP384R1(KeyPairGeneratorFactory.secp384r1(), "Secp384r1", KeyPairValidator::isSecp384r1),

		SECP521R1(KeyPairGeneratorFactory.secp521r1(), "Secp521r1", KeyPairValidator::isSecp521r1),

		ED25519(KeyPairGeneratorFactory.ed25519(), "ED25519", KeyPairValidator::isEd25519),

		ED448(KeyPairGeneratorFactory.ed448(), "ED448", KeyPairValidator::isEd448),

		X25519(KeyPairGeneratorFactory.x25519(), "X25519", KeyPairValidator::isX25519),

		X448(KeyPairGeneratorFactory.x448(), "X448", KeyPairValidator::isX448);

		private final KeyPairGeneratorFactory keyPairGeneratorFactory;
		private final String keyType;
		private final Predicate<AsymmetricKey> isKeyType;

		private Type(KeyPairGeneratorFactory keyPairGeneratorFactory, String keyType,
				Predicate<AsymmetricKey> isKeyType)
		{
			this.keyPairGeneratorFactory = keyPairGeneratorFactory;
			this.keyType = keyType;
			this.isKeyType = isKeyType;
		}

		public KeyPairGeneratorFactory getKeyPairGeneratorFactory()
		{
			return keyPairGeneratorFactory;
		}

		public String getKeyType()
		{
			return keyType;
		}

		public boolean isKeyType(AsymmetricKey key)
		{
			return isKeyType.test(key);
		}
	}

	private String id;
	private Type type;
	private List<File> targets;

	public String getId()
	{
		return id;
	}

	public Type getType()
	{
		return type;
	}

	public List<File> getTargets()
	{
		return targets;
	}

	@Override
	public String toString()
	{
		return "Key [" + (id != null ? "id=" + id + ", " : "") + (type != null ? "type=" + type + ", " : "")
				+ (targets != null ? "targets=" + targets + ", " : "") + "]";
	}
}
