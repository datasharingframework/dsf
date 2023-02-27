package dev.dsf.fhir.search;

import java.sql.SQLException;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.dao.provider.DaoProvider;

public interface Matcher
{
	void resloveReferencesForMatching(Resource resource, DaoProvider daoProvider) throws SQLException;

	boolean matches(Resource resource);

	Class<? extends Resource> getResourceType();
}
