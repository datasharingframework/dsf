package dev.dsf.bpe.service;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.client.LocalFhirClientProvider;

public class LocalOrganizationProviderImpl implements LocalOrganizationProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(LocalOrganizationProviderImpl.class);

	private record OrganizationEntry(Optional<Organization> organization, LocalDateTime readTime)
	{
	}

	private final AtomicReference<OrganizationEntry> organization = new AtomicReference<>();

	private final TemporalAmount cacheTimeout;
	private final LocalFhirClientProvider clientProvider;
	private final String localEndpointAddress;

	public LocalOrganizationProviderImpl(TemporalAmount cacheTimeout, LocalFhirClientProvider clientProvider,
			String localEndpointAddress)
	{
		this.cacheTimeout = cacheTimeout;
		this.clientProvider = clientProvider;
		this.localEndpointAddress = localEndpointAddress;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(cacheTimeout, "cacheTimeout");
		Objects.requireNonNull(clientProvider, "clientProvider");
		Objects.requireNonNull(localEndpointAddress, "localEndpointAddress");
	}

	@Override
	public Optional<Organization> getLocalOrganization()
	{
		OrganizationEntry entry = organization.get();
		if (entry == null || entry.organization().isEmpty()
				|| LocalDateTime.now().isAfter(entry.readTime().plus(cacheTimeout)))
		{
			Optional<Organization> o = doGetLocalOrganization();
			if (organization.compareAndSet(entry, new OrganizationEntry(o, LocalDateTime.now())))
				return o;
			else
				return organization.get().organization();
		}
		else
			return entry.organization();
	}

	private Optional<Organization> doGetLocalOrganization()
	{
		Bundle resultBundle = clientProvider.getLocalWebserviceClient().searchWithStrictHandling(Endpoint.class,
				Map.of("status", Collections.singletonList("active"), "address",
						Collections.singletonList(localEndpointAddress), "_include",
						Collections.singletonList("Endpoint:organization")));

		if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getEntry().size() != 2
				|| resultBundle.getEntry().get(0).getResource() == null
				|| !(resultBundle.getEntry().get(0).getResource() instanceof Endpoint)
				|| resultBundle.getEntry().get(1).getResource() == null
				|| !(resultBundle.getEntry().get(1).getResource() instanceof Organization))
		{
			logger.warn("No active (or more than one) Endpoint found for address '{}'", localEndpointAddress);
			return Optional.empty();
		}
		else if (getActiveOrganizationFromIncludes(resultBundle).count() != 1)
		{
			logger.warn("No active (or more than one) Organization found by active Endpoint with address '{}'",
					localEndpointAddress);
			return Optional.empty();
		}

		return getActiveOrganizationFromIncludes(resultBundle).findFirst();
	}

	private Stream<Organization> getActiveOrganizationFromIncludes(Bundle resultBundle)
	{
		return resultBundle.getEntry().stream().filter(BundleEntryComponent::hasSearch)
				.filter(e -> SearchEntryMode.INCLUDE.equals(e.getSearch().getMode()))
				.filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.filter(r -> r instanceof Organization).map(r -> (Organization) r).filter(Organization::getActive);
	}
}
