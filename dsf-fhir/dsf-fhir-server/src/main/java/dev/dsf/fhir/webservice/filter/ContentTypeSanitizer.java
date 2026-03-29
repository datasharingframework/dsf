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
package dev.dsf.fhir.webservice.filter;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ContentTypeSanitizer implements ContainerResponseFilter
{
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException
	{
		MediaType mediaType = responseContext.getMediaType();

		if (mediaType != null)
		{
			Map<String, String> params = mediaType.getParameters().entrySet().stream()
					.filter(e -> "charset".equals(e.getKey()) || "boundary".equals(e.getKey()))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));

			String clean = new MediaType(mediaType.getType(), mediaType.getSubtype(), params).toString();

			responseContext.getHeaders().putSingle("Content-Type", clean);
		}
	}
}
