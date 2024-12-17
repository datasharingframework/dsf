package dev.dsf.fhir.dao.command;

import java.sql.Connection;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.conf.Identity;
import jakarta.ws.rs.WebApplicationException;

public interface AuthorizationHelper
{
	void checkCreateAllowed(int index, Connection connection, Identity identity, Resource newResource)
			throws WebApplicationException;

	void checkReadAllowed(int index, Connection connection, Identity identity, Resource existingResource)
			throws WebApplicationException;

	void checkUpdateAllowed(int index, Connection connection, Identity identity, Resource oldResource,
			Resource newResource) throws WebApplicationException;

	void checkDeleteAllowed(int index, Connection connection, Identity identity, Resource oldResource)
			throws WebApplicationException;

	void checkSearchAllowed(int index, Identity identity, String resourceTypeName) throws WebApplicationException;

	void filterIncludeResults(int index, Connection connection, Identity identity, Bundle multipleResult);
}
