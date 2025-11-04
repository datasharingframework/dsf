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
package dev.dsf.fhir.subscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hl7.fhir.r4.model.Resource;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.search.Matcher;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.SearchQuery;

public class MatcherFactory
{
	private final Map<String, ResourceDao<? extends Resource>> daosByResourceName = new HashMap<>();

	public MatcherFactory(Map<String, ResourceDao<? extends Resource>> daosByResourceName)
	{
		if (daosByResourceName != null)
			this.daosByResourceName.putAll(daosByResourceName);
	}

	public Optional<Matcher> createMatcher(String uri)
	{
		UriComponents componentes = UriComponentsBuilder.fromUriString(uri).build();
		String path = componentes.getPath();

		MultiValueMap<String, String> queryParameters = componentes.getQueryParams();

		if (daosByResourceName.containsKey(path))
		{
			ResourceDao<? extends Resource> dao = daosByResourceName.get(path);
			SearchQuery<? extends Resource> query = dao.createSearchQueryWithoutUserFilter(PageAndCount.exists());
			query.configureParameters(queryParameters);
			return Optional.of(query);
		}
		else
			return Optional.empty();
	}
}
