package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.util.Map;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.service.ResourceReference;
import jakarta.ws.rs.WebApplicationException;

public interface ReferencesHelper<R extends Resource>
{
	void resolveTemporaryAndConditionalReferencesOrLiteralInternalRelatedArtifactOrAttachmentUrls(
			Map<String, IdType> idTranslationTable, Connection connection) throws WebApplicationException;

	void resolveLogicalReferences(Connection connection) throws WebApplicationException;

	void checkReferences(Map<String, IdType> idTranslationTable, Connection connection,
			Predicate<ResourceReference> checkReference) throws WebApplicationException;
}