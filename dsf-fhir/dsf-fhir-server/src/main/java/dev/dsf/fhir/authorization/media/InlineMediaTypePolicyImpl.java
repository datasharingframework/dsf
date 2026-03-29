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
package dev.dsf.fhir.authorization.media;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.MediaType;

public class InlineMediaTypePolicyImpl implements InlineMediaTypePolicy
{
	private static final MediaType PDF = MediaType.valueOf("application/pdf");
	private static final MediaType PNG = MediaType.valueOf("image/png");
	private static final MediaType JPG = MediaType.valueOf("image/jpeg");
	private static final MediaType GIF = MediaType.valueOf("image/gif");
	private static final MediaType WEBP = MediaType.valueOf("image/webp");
	private static final MediaType SVG = MediaType.valueOf("image/svg+xml");
	private static final MediaType AVIF = MediaType.valueOf("image/avif");

	private static final List<MediaType> DISPLAY_ALLOWED = Arrays.asList(MediaType.TEXT_HTML_TYPE,
			MediaType.TEXT_PLAIN_TYPE);

	private static final List<MediaType> OPEN_ALLOWED = Arrays.asList(MediaType.TEXT_HTML_TYPE,
			MediaType.TEXT_PLAIN_TYPE, PDF, PNG, JPG, GIF, WEBP, SVG, AVIF);

	@Override
	public boolean isInlineDisplayAllowed(String mediaType)
	{
		MediaType mt = toMediaType(mediaType);

		return DISPLAY_ALLOWED.stream().anyMatch(m -> m.isCompatible(mt));
	}

	@Override
	public boolean isInlineOpenAllowed(String mediaType)
	{
		MediaType mt = toMediaType(mediaType);

		return OPEN_ALLOWED.stream().anyMatch(m -> m.isCompatible(mt));
	}

	private MediaType toMediaType(String mediaType)
	{
		if (mediaType == null || mediaType.isBlank())
			return null;
		else
			return MediaType.valueOf(mediaType);
	}
}
