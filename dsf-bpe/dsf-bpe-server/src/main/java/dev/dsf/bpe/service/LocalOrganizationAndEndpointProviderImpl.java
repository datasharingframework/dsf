package dev.dsf.bpe.service;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.client.dsf.ClientProvider;

public class LocalOrganizationAndEndpointProviderImpl implements LocalOrganizationAndEndpointProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(LocalOrganizationAndEndpointProviderImpl.class);

	private record OrganizationAndEndpoint(Organization organization, Endpoint endpoint)
	{
	}

	private record Entry(Optional<OrganizationAndEndpoint> organizationAndEndpoint, LocalDateTime readTime)
	{
	}

	private final AtomicReference<Entry> entry = new AtomicReference<>();

	private final TemporalAmount cacheTimeout;
	private final ClientProvider clientProvider;
	private final String localEndpointAddress;

	public LocalOrganizationAndEndpointProviderImpl(TemporalAmount cacheTimeout, ClientProvider clientProvider,
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
		return getLocalOrganizationAndEndpoint().map(OrganizationAndEndpoint::organization);
	}

	@Override
	public Optional<Endpoint> getLocalEndpoint()
	{
		return getLocalOrganizationAndEndpoint().map(OrganizationAndEndpoint::endpoint);
	}

	private Optional<OrganizationAndEndpoint> getLocalOrganizationAndEndpoint()
	{
		Entry e = entry.get();
		if (e == null || e.organizationAndEndpoint().isEmpty()
				|| LocalDateTime.now().isAfter(e.readTime().plus(cacheTimeout)))
		{
			Optional<OrganizationAndEndpoint> oAndE = doGetLocalOrganizationAndEndpoint();
			if (entry.compareAndSet(e, new Entry(oAndE, LocalDateTime.now())))
				return oAndE;
			else
				return entry.get().organizationAndEndpoint();
		}
		else
			return e.organizationAndEndpoint();
	}

	private Optional<OrganizationAndEndpoint> doGetLocalOrganizationAndEndpoint()
	{
		Bundle resultBundle = clientProvider.getWebserviceClient().searchWithStrictHandling(Endpoint.class,
				Map.of("status", List.of("active"), "address", List.of(localEndpointAddress), "_include",
						List.of("Endpoint:organization")));

		if (resultBundle != null && resultBundle.getEntry() != null && resultBundle.getEntry().size() == 2
				&& resultBundle.getEntry().get(0).getResource() instanceof Endpoint endpoint
				&& resultBundle.getEntry().get(1).getResource() instanceof Organization organization)
		{
			return Optional.of(new OrganizationAndEndpoint(organization, endpoint));
		}
		else
		{
			logger.warn("No active Endpoint/Organization found for address '{}'", localEndpointAddress);
			return Optional.empty();
		}
	}
}
