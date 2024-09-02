package dev.dsf.bpe.v2.client;

import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

public interface ReferenceExtractor
{
	Stream<Reference> getReferences(Resource resource);
}