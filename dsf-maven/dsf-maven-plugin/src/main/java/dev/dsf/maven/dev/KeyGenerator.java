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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import de.hsheilbronn.mi.utils.crypto.keypair.KeyPairValidator;

public class KeyGenerator extends AbstractGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(KeyGenerator.class);

	public static final String POSTFIX_PUBLIC_KEY = ".pub";

	private final List<Key> keys = new ArrayList<>();

	private Map<String, KeyPair> keyPairsById;

	public KeyGenerator(Path keyDir, char[] privateKeyPassword, List<Key> keys)
	{
		super(keyDir, privateKeyPassword);

		if (keys != null)
			this.keys.addAll(keys);
	}

	public void initialize()
	{
		logger.info("Initializing key generator ...");

		keyPairsById = keys.stream().collect(Collectors.toMap(Key::getId, this::initKeyPair));
	}

	public boolean isInitialized()
	{
		return keyPairsById != null;
	}

	private void checkInitialized()
	{
		if (!isInitialized())
			throw new IllegalStateException("not initialized");
	}

	private KeyPair initKeyPair(Key key)
	{
		return readKeyPair(key).orElseGet(() -> createKeyPair(key));
	}

	public Optional<KeyPair> getKeyPair(Key key)
	{
		checkInitialized();

		return Optional.ofNullable(keyPairsById.get(key.getId()));
	}

	private Optional<KeyPair> readKeyPair(Key key)
	{
		Optional<PublicKey> publicKey = readPublicKey(key.getId());
		if (publicKey.isEmpty())
		{
			logger.debug("{} public-key for '{}' not found", key.getType().getKeyType(), key.getId());
			return Optional.empty();
		}

		Optional<PrivateKey> privateKey = readPrivateKey(key.getId());
		if (privateKey.isEmpty())
		{
			logger.debug("{} private-key for '{}' not found", key.getType().getKeyType(), key.getId());
			return Optional.empty();
		}

		if (!KeyPairValidator.matches(privateKey.get(), publicKey.get()))
		{
			logger.warn("Found {} public-key and private-key for '{}' not matching", key.getType().getKeyType(),
					key.getId());
			return Optional.empty();
		}

		logger.info("Using existing {} key pair for '{}'", key.getType().getKeyType(), key.getId());
		return Optional.of(new KeyPair(publicKey.get(), privateKey.get()));
	}

	private KeyPair createKeyPair(Key key)
	{
		logger.info("Creating {} key pair '{}'", key.getType().getKeyType(), key.getId());

		KeyPair keyPair = key.getType().getKeyPairGeneratorFactory().initialize().generateKeyPair();

		writePrivateKey(key.getId(), keyPair.getPrivate());
		writePublicKey(key.getId(), keyPair.getPublic());

		return keyPair;
	}

	private void writePublicKey(String id, PublicKey publicKey) throws RuntimeException
	{
		Path file = toPath(id, POSTFIX_PUBLIC_KEY);

		try
		{
			PemWriter.writePublicKey(publicKey, file);
		}
		catch (IOException e)
		{
			logger.error("Unable to write public-key {}: {} - {}", file.toAbsolutePath().normalize(),
					e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private Optional<PublicKey> readPublicKey(String id) throws RuntimeException
	{
		Path file = toPath(id, POSTFIX_PUBLIC_KEY);

		if (!Files.isReadable(file))
			return Optional.empty();

		try
		{
			return Optional.of(PemReader.readPublicKey(file));
		}
		catch (IOException e)
		{
			logger.error("Unable to read public-key {}: {} - {}", file.toAbsolutePath().normalize(),
					e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}
}
