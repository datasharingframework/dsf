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
package dev.dsf.maven.bundle;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleEntryFileVisitor implements FileVisitor<Path>
{
	private static final Logger logger = LoggerFactory.getLogger(BundleEntryFileVisitor.class);

	private final Path baseFolder;
	private final BundleEntryPutReader putReader;
	private final BundleEntryPostReader postReader;
	private final BundleEntryIgnoreReader ignoreReader;

	private Class<Resource> resource;

	public BundleEntryFileVisitor(Path baseFolder, BundleEntryPutReader putReader, BundleEntryPostReader postReader,
			BundleEntryIgnoreReader ignoreReader)
	{
		this.baseFolder = Objects.requireNonNull(baseFolder, "baseFolder");
		this.putReader = Objects.requireNonNull(putReader, "putReader");
		this.postReader = Objects.requireNonNull(postReader, "postReader");
		this.ignoreReader = Objects.requireNonNull(ignoreReader, "ignoreReader");
	}

	@Override
	@SuppressWarnings("unchecked")
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
	{
		if (dir.equals(baseFolder))
			return FileVisitResult.CONTINUE;
		else if (dir.getParent().equals(baseFolder))
		{
			try
			{
				Class<?> c = Class.forName(Resource.class.getPackageName() + "." + dir.getFileName());
				if (Resource.class.isAssignableFrom(c))
				{
					resource = (Class<Resource>) c;
				}
				else
					logger.error("{} not a Resource", c.getName());

				return FileVisitResult.CONTINUE;
			}
			catch (ClassNotFoundException e)
			{
				logger.error("Error while visiting folder {}, class with name org.hl7.fhir.r4.model.{} not found.",
						dir.toString(), dir.getFileName().toString());
				return FileVisitResult.SKIP_SUBTREE;
			}
		}
		else
		{
			logger.warn("Skipping subtree {}", dir.toString());
			return FileVisitResult.SKIP_SUBTREE;
		}
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
	{
		if (resource != null && file.getFileName().toString().endsWith(".xml"))
		{
			Path putFile = file.resolveSibling(file.getFileName().toString() + ".put");
			Path postFile = file.resolveSibling(file.getFileName().toString() + ".post");
			Path ignoreFile = file.resolveSibling(file.getFileName().toString() + ".ignore");

			if (!Files.isReadable(putFile) && !Files.isReadable(postFile) && !Files.isReadable(ignoreFile))
			{
				logger.error("put, post or ignore file for {} at {} not readable. Readable file {}, {} or {} expected",
						resource.getSimpleName(), file.toString(), putFile.toString(), postFile.toString(),
						ignoreFile.toString());
				throw new IOException("put file " + putFile.toString() + ", post file " + postFile.toString()
						+ " or ignore file " + ignoreFile.toString() + " not readable");
			}
			else if (Stream.of(Files.isReadable(putFile), Files.isReadable(postFile), Files.isReadable(ignoreFile))
					.filter(b -> b).count() > 1)
			{
				logger.error("For {} at {} only one readable file {}, {} or {} expected", resource.getSimpleName(),
						file.toString(), putFile.toString(), postFile.toString(), ignoreFile.toString());
				throw new IOException("More then one of the put file " + putFile.toString() + ", post file "
						+ postFile.toString() + " and ignore file " + ignoreFile.toString() + " readable");
			}
			else if (Files.isReadable(putFile))
				putReader.read(resource, file, putFile);
			else if (Files.isReadable(postFile))
				postReader.read(resource, file, postFile);
			else if (Files.isReadable(ignoreFile))
				ignoreReader.read(resource, file, ignoreFile);
		}
		else
			logger.debug("Ignoring {}", file.toString());

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
	{
		logger.error("Error while reading file at {}", file.toString(), exc);
		return FileVisitResult.TERMINATE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
	{
		resource = null;
		return FileVisitResult.CONTINUE;
	}
}