package dev.dsf.bpe.v2.client.dsf;

import java.util.Objects;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class ReferenceCleanerImpl implements ReferenceCleaner, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ReferenceCleanerImpl.class);

	private final ReferenceExtractor referenceExtractor;

	public ReferenceCleanerImpl(ReferenceExtractor referenceExtractor)
	{
		this.referenceExtractor = referenceExtractor;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(referenceExtractor, "referenceExtractor");
	}

	@Override
	public <R extends Resource> R cleanReferenceResourcesIfBundle(R resource)
	{
		if (resource == null)
			return null;

		if (resource instanceof Bundle b)
			b.getEntry().stream().map(BundleEntryComponent::getResource).forEach(this::fixBundleEntry);

		return resource;
	}

	private void fixBundleEntry(Resource resource)
	{
		if (resource instanceof Bundle)
		{
			cleanReferenceResourcesIfBundle(resource);
		}
		else
		{
			Stream<Reference> references = referenceExtractor.getReferences(resource);

			references.filter(r -> r != null).forEach(r -> r.setResource(null));

			if (resource instanceof DomainResource d && d.hasContained())
			{
				logger.warn("{} has contained resources, removing resources", resource.getClass().getName());
				d.setContained(null);
			}
		}
	}
}
