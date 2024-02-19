package dev.dsf.bpe.service;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.hl7.fhir.r4.model.Organization;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.v1.service.OrganizationProvider;

public class LocalOrganizationProviderImpl implements LocalOrganizationProvider, InitializingBean
{
	private record OrganizationEntry(Optional<Organization> organization, LocalDateTime readTime)
	{
	}

	private final AtomicReference<OrganizationEntry> organization = new AtomicReference<>();

	private final TemporalAmount cacheTimeout;
	private final OrganizationProvider delegate;

	public LocalOrganizationProviderImpl(TemporalAmount cacheTimeout, OrganizationProvider delegate)
	{
		this.cacheTimeout = cacheTimeout;
		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(cacheTimeout, "cacheTimeout");
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public Optional<Organization> getLocalOrganization()
	{
		OrganizationEntry entry = organization.get();
		if (entry == null || entry.organization().isEmpty()
				|| LocalDateTime.now().isAfter(entry.readTime().plus(cacheTimeout)))
		{
			Optional<Organization> o = delegate.getLocalOrganization();
			if (organization.compareAndSet(entry, new OrganizationEntry(o, LocalDateTime.now())))
				return o;
			else
				return organization.get().organization();
		}
		else
			return entry.organization();
	}
}
