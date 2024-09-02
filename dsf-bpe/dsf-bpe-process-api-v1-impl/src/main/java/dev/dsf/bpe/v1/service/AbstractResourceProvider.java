package dev.dsf.bpe.v1.service;

import static org.hl7.fhir.instance.model.api.IBaseBundle.LINK_NEXT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractResourceProvider implements InitializingBean
{
	protected final FhirWebserviceClientProvider clientProvider;
	protected final String localEndpointAddress;

	public AbstractResourceProvider(FhirWebserviceClientProvider clientProvider, String localEndpointAddress)
	{
		this.clientProvider = clientProvider;
		this.localEndpointAddress = localEndpointAddress;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(clientProvider, "clientProvider");
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
		List<R> organizations = new ArrayList<>();

		boolean hasMore = true;
		int page = 1;
		while (hasMore)
		{
			Bundle resultBundle = search(searchType, searchParameters, page++);

			organizations.addAll(resultBundle.getEntry().stream().filter(BundleEntryComponent::hasSearch)
					.filter(e -> targetMode.equals(e.getSearch().getMode())).filter(BundleEntryComponent::hasResource)
					.map(BundleEntryComponent::getResource).filter(targetType::isInstance).map(targetType::cast)
					.filter(filter).toList());

			hasMore = resultBundle.getLink(LINK_NEXT) != null;
		}

		return organizations;
	}

	private Bundle search(Class<? extends Resource> searchType, Map<String, List<String>> parameters, int page)
	{
		Map<String, List<String>> parametersAndPage = new HashMap<>(parameters);
		parametersAndPage.put("_page", Collections.singletonList(String.valueOf(page)));
		if (!parameters.containsKey("_sort"))
			parametersAndPage.put("_sort", Collections.singletonList("_id"));

		return clientProvider.getLocalWebserviceClient().searchWithStrictHandling(searchType, parametersAndPage);
	}
}
