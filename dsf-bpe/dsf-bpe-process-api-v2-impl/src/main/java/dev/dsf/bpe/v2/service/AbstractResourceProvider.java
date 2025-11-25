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
package dev.dsf.bpe.v2.service;

import static org.hl7.fhir.instance.model.api.IBaseBundle.LINK_NEXT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.v2.client.dsf.DsfClient;

public abstract class AbstractResourceProvider implements InitializingBean
{
	protected final Supplier<DsfClient> localDsfClient;
	protected final String localEndpointAddress;

	public AbstractResourceProvider(Supplier<DsfClient> localDsfClient, String localEndpointAddress)
	{
		this.localDsfClient = localDsfClient;
		this.localEndpointAddress = localEndpointAddress;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(localDsfClient, "localDsfClient");
		Objects.requireNonNull(localEndpointAddress, "localEndpointAddress");
	}

	protected final String toSearchParameter(Identifier identifier)
	{
		return (identifier.hasSystem() ? identifier.getSystem() + "|" : "") + identifier.getValue();
	}

	protected final String toSearchParameter(Coding coding)
	{
		return (coding.hasSystem() ? coding.getSystem() + "|" : "") + coding.getCode();
	}

	protected final <R extends Resource> List<R> search(Class<? extends Resource> searchType,
			Map<String, List<String>> searchParameters, SearchEntryMode targetMode, Class<R> targetType,
			Predicate<R> filter)
	{
		return search(searchType, searchParameters).filter(e -> targetMode.equals(e.getSearch().getMode()))
				.filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.filter(targetType::isInstance).map(targetType::cast).filter(filter).toList();
	}

	protected final Stream<BundleEntryComponent> search(Class<? extends Resource> searchType,
			Map<String, List<String>> searchParameters)
	{
		List<Stream<BundleEntryComponent>> resources = new ArrayList<>();

		boolean hasMore = true;
		int page = 1;
		while (hasMore)
		{
			Bundle resultBundle = search(searchType, searchParameters, page++);

			resources.add(resultBundle.getEntry().stream().filter(BundleEntryComponent::hasSearch)
					.filter(BundleEntryComponent::hasResource));

			hasMore = resultBundle.getLink(LINK_NEXT) != null;
		}

		return resources.stream().flatMap(Function.identity());
	}

	private Bundle search(Class<? extends Resource> searchType, Map<String, List<String>> parameters, int page)
	{
		Map<String, List<String>> parametersAndPage = new HashMap<>(parameters);
		parametersAndPage.put("_page", List.of(String.valueOf(page)));
		if (!parameters.containsKey("_sort"))
			parametersAndPage.put("_sort", List.of("_id"));

		return localDsfClient.get().searchWithStrictHandling(searchType, parametersAndPage);
	}
}
