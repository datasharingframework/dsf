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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.io.PemWriter;
import dev.dsf.maven.exception.RuntimeIOException;

public abstract class AbstractIo
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractIo.class);

	protected static interface RunnableWithIoException
	{
		void run() throws IOException;
	}

	protected final Path projectBasedir;
	protected final char[] privateKeyPassword;

	public AbstractIo(Path projectBasedir, char[] privateKeyPassword)
	{
		this.projectBasedir = Objects.requireNonNull(projectBasedir, "projectBasedir");
		this.privateKeyPassword = privateKeyPassword;
	}

	protected final void toRuntimeException(RunnableWithIoException runnable) throws RuntimeIOException
	{
		try
		{
			runnable.run();
		}
		catch (IOException e)
		{
			throw new RuntimeIOException(e);
		}
	}

	protected void writePrivateKey(String type, String id, PrivateKey privateKey, Path target) throws IOException
	{
		logger.info("Writing private-key encrypted ({}: {}) to {}", type, id, projectBasedir.relativize(target));

		PemWriter.writePrivateKey(privateKey).asPkcs8().encryptedAes128(privateKeyPassword).toFile(target);
	}

	protected void writePrivateKeyPlain(String type, String id, PrivateKey privateKey, Path target) throws IOException
	{
		logger.info("Writing private-key unencrypted ({}: {}) to {}", type, id, projectBasedir.relativize(target));

		PemWriter.writePrivateKey(privateKey).asPkcs8().notEncrypted().toFile(target);
	}

	protected final void writePassword(String type, String id, Path target) throws IOException
	{
		logger.info("Writing key password ({}: {}) to {}", type, id, projectBasedir.relativize(target));

		Files.writeString(target, new String(privateKeyPassword));
	}
}
