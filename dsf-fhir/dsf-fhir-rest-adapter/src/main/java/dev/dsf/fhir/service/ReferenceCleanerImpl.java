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
package dev.dsf.fhir.service;

import java.util.Objects;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceCleanerImpl implements ReferenceCleaner
{
	private static final Logger logger = LoggerFactory.getLogger(ReferenceCleanerImpl.class);

	private final ReferenceExtractor referenceExtractor;

	public ReferenceCleanerImpl(ReferenceExtractor referenceExtractor)
	{
		this.referenceExtractor = Objects.requireNonNull(referenceExtractor, "referenceExtractor");
	}

	@Override
	public <R extends Resource> R cleanLiteralReferences(R resource)
	{
		if (resource == null)
			return null;

		Stream<ResourceReference> references = referenceExtractor.getReferences(resource);
		references.forEach(this::cleanupReference);

		return resource;
	}

	private void cleanupReference(ResourceReference resourceReference)
	{
		if (resourceReference.hasReference())
		{
			Reference ref = resourceReference.getReference();
			if (ref.hasIdentifier() && ref.hasReference())
				ref.setReferenceElement((IdType) null);
		}
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
			Stream<ResourceReference> references = referenceExtractor.getReferences(resource);

			references.filter(ResourceReference::hasReference).forEach(r -> r.getReference().setResource(null));

			if (resource instanceof DomainResource d && d.hasContained())
			{
				logger.warn("{} has contained resources, removing resources", resource.getClass().getName());
				d.setContained(null);
			}
		}
	}
}
