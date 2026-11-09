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
import java.security.PrivateKey;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.io.PemReader;
import de.hsheilbronn.mi.utils.crypto.io.PemWriter;

public class AbstractGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractGenerator.class);

	public static final String POSTFIX_PRIVATE_KEY = ".key";

	private final Path baseDir;
	private final char[] privateKeyPassword;

	public AbstractGenerator(Path baseDir, char[] privateKeyPassword)
	{
		Objects.requireNonNull(baseDir, "baseDir");
		Objects.requireNonNull(privateKeyPassword, "privateKeyPassword");

		this.baseDir = baseDir;
		this.privateKeyPassword = privateKeyPassword;
	}

	protected void writePrivateKey(String commonName, PrivateKey privateKey) throws RuntimeException
	{
		Path file = toPath(commonName, POSTFIX_PRIVATE_KEY);

		try
		{
			PemWriter.writePrivateKey(privateKey).asPkcs8().encryptedAes128(privateKeyPassword).toFile(file);
		}
		catch (IOException e)
		{
			logger.error("Unable to write private-key {}: {} - {}", file.toAbsolutePath().normalize(),
					e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}

	protected Optional<PrivateKey> readPrivateKey(String commonName) throws RuntimeException
	{
		Path file = toPath(commonName, POSTFIX_PRIVATE_KEY);

		if (!Files.isReadable(file))
			return Optional.empty();

		try
		{
			return Optional.of(PemReader.readPrivateKey(file, privateKeyPassword));
		}
		catch (IOException e)
		{
			logger.error("Unable to read private-key {}: {} - {}", file.toAbsolutePath().normalize(),
					e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	protected Path toPath(String id, String postFix)
	{
		return baseDir.resolve(id.replaceAll(" ", "_") + postFix);
	}
}
