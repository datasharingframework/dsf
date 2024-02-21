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
