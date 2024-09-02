package dev.dsf.fhir.service;

import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Resource;

public interface ReferenceExtractor
{
	Stream<ResourceReference> getReferences(Resource resource);
}