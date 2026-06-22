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
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import dev.dsf.maven.exception.RuntimeIOException;

public class KeyWriter extends AbstractIo
{
	private static final Logger logger = LoggerFactory.getLogger(KeyWriter.class);

	private final KeyGenerator keyGenerator;

	public KeyWriter(Path projectBasedir, char[] privateKeyPassword, KeyGenerator keyGenerator)
	{
		super(projectBasedir, privateKeyPassword);

		this.keyGenerator = Objects.requireNonNull(keyGenerator, "keyGenerator");
	}

	public void write(List<Key> keys)
	{
		if (keys != null)
			keys.forEach(this::write);
	}

	private void write(Key key) throws RuntimeIOException
	{
		Optional<KeyPair> keyPair = keyGenerator.getKeyPair(key);
		keyPair.ifPresent(kp -> key.getTargets().stream().map(File::toPath).forEach(target ->
		{
			if (target.getFileName().toString().endsWith(".key"))
				toRuntimeException(() -> writePrivateKey("id", key.getId(), kp.getPrivate(), target));
			else if (target.getFileName().toString().endsWith(".key.plain"))
				toRuntimeException(() -> writePrivateKeyPlain("id", key.getId(), kp.getPrivate(), target));
			else if (target.getFileName().toString().endsWith(".key.password"))
				toRuntimeException(() -> writePassword("id", key.getId(), target));
			else if (target.getFileName().toString().endsWith(".pub"))
				toRuntimeException(() -> writePublicKey(key.getId(), kp.getPublic(), target));
			else
				logger.warn("Key (id: {}) target filetype not supported: {}", key.getId(), target.getFileName());
		}));
	}

	private void writePublicKey(String id, PublicKey publicKey, Path target) throws IOException
	{
		logger.info("Writing public-key (id: {}) to {}", id, projectBasedir.relativize(target));

		PemWriter.writePublicKey(publicKey, target);
	}
}
