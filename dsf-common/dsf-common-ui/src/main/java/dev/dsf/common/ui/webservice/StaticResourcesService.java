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
package dev.dsf.common.ui.webservice;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

@Path(StaticResourcesService.PATH)
public class StaticResourcesService
{
	public static final String PATH = "static";

	private static final java.nio.file.Path OVERRIDE_RESOURCE_FOLDER = Paths.get("ui");

	private static CacheControl NO_TRANSFORM = new CacheControl();
	private static CacheControl NO_CACHE_NO_TRANSFORM = new CacheControl();
	static
	{
		// no-transform set by default
		NO_CACHE_NO_TRANSFORM.setNoCache(true);
	}

	private record CacheEntry(byte[] data, EntityTag tag, String mimeType)
	{
		CacheEntry(byte[] data, String fileName)
		{
			this(data, tag(data), mimeType(fileName));
		}

		private static EntityTag tag(byte[] data)
		{
			try
			{
				MessageDigest digest = MessageDigest.getInstance("SHA256");
				return new EntityTag(Hex.encodeHexString(digest.digest(data)));
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new RuntimeException(e);
			}
		}

		private static String mimeType(String fileName)
		{
			String[] parts = fileName.split("\\.");
			return MIME_TYPE_BY_SUFFIX.get(parts[parts.length - 1]);
		}
	}

	private static abstract class AbstractCache
	{
		final String baseFolder;

		AbstractCache(String baseFolder)
		{
			this.baseFolder = Objects.requireNonNull(baseFolder, "baseFolder");
		}

		abstract Optional<CacheEntry> get(String fileName);

		CacheEntry read(InputStream stream, String fileName) throws IOException
		{
			byte[] data = stream.readAllBytes();

			return new CacheEntry(data, fileName);
		}

		InputStream getStream(String fileName) throws IOException
		{
			java.nio.file.Path target = OVERRIDE_RESOURCE_FOLDER.resolve(fileName).normalize();
			if (target.getParent() == null || !target.getParent().equals(OVERRIDE_RESOURCE_FOLDER))
				return null;
			else if (Files.isReadable(target))
				return Files.newInputStream(target);
			else
				return StaticResourcesService.class.getResourceAsStream(baseFolder + "/static/" + fileName);
		}
	}

	private static final class Cache extends AbstractCache
	{
		private final Map<String, SoftReference<CacheEntry>> entries = new HashMap<>();

		Cache(String baseFolder)
		{
			super(baseFolder);
		}

		@Override
		Optional<CacheEntry> get(String fileName)
		{
			SoftReference<CacheEntry> entry = entries.get(fileName);
			if (entry == null || entry.get() == null)
				return read(fileName);
			else
				return Optional.of(entry.get());
		}

		Optional<CacheEntry> read(String fileName)
		{
			try (InputStream stream = getStream(fileName))
			{
				if (stream == null)
					return Optional.empty();
				else
				{
					CacheEntry entry = read(stream, fileName);
					entries.put(fileName, new SoftReference<>(entry));
					return Optional.of(entry);
				}
			}
			catch (IOException e)
			{
				throw new WebApplicationException(e);
			}
		}
	}

	private static final class NoCache extends AbstractCache
	{
		NoCache(String baseFolder)
		{
			super(baseFolder);
		}

		@Override
		Optional<CacheEntry> get(String fileName)
		{
			try (InputStream stream = getStream(fileName))
			{
				if (stream == null)
					return Optional.empty();
				else
					return Optional.of(read(stream, fileName));
			}
			catch (IOException e)
			{
				throw new WebApplicationException(e);
			}
		}
	}

	private static final String FILENAME_PATTERN_STRING = "^[0-9a-zA-Z_-]+\\.[0-9a-zA-Z]+$";
	private static final Pattern FILENAME_PATTERN = Pattern.compile(FILENAME_PATTERN_STRING);

	private static final Map<String, String> MIME_TYPE_BY_SUFFIX = Map.of("css", "text/css; charset=utf-8", "js",
			"text/javascript; charset=utf-8", "html", "text/html; charset=utf-8", "pdf", "application/pdf", "png",
			"image/png", "svg", "image/svg+xml; charset=utf-8", "jpg", "image/jpeg");

	private final AbstractCache cache;
	private final CacheControl cacheControl;

	public StaticResourcesService(String baseFolder, boolean cacheEnabled)
	{
		Objects.requireNonNull(baseFolder, "baseFolder");
		if (!baseFolder.startsWith("/") || baseFolder.endsWith("/"))
			throw new IllegalArgumentException("baseFolder must start with '/' and not end with '/'");

		cache = cacheEnabled ? new Cache(baseFolder) : new NoCache(baseFolder);
		cacheControl = cacheEnabled ? NO_TRANSFORM : NO_CACHE_NO_TRANSFORM;
	}

	@GET
	@Path("/{fileName}")
	public Response getFile(@PathParam("fileName") String fileName, @Context HttpHeaders headers)
	{
		if (fileName == null || fileName.isBlank() || !FILENAME_PATTERN.matcher(fileName).matches())
			return Response.status(Status.NOT_FOUND).build();
		else if (!MIME_TYPE_BY_SUFFIX.keySet().stream().anyMatch(key -> fileName.endsWith(key)))
			return Response.status(Status.NOT_FOUND).build();
		else
		{
			Optional<CacheEntry> entry = cache.get(fileName);
			Optional<String> matchTag = Arrays
					.asList(HttpHeaders.IF_NONE_MATCH, HttpHeaders.IF_NONE_MATCH.toLowerCase()).stream()
					.map(name -> headers.getHeaderString(name)).filter(h -> h != null).findFirst();

			return entry.map(toNotModifiedOrOkResponse(matchTag.orElse(""))).orElse(Response.status(Status.NOT_FOUND))
					.build();
		}
	}

	private Function<CacheEntry, ResponseBuilder> toNotModifiedOrOkResponse(String matchTag)
	{
		return entry ->
		{
			if (entry.tag().getValue().equals(matchTag.replace("\"", "")))
				return Response.status(Status.NOT_MODIFIED);
			else
				return Response.ok(entry.data(), MediaType.valueOf(entry.mimeType())).tag(entry.tag())
						.cacheControl(cacheControl);
		};
	}
}
