package dev.dsf.fhir.dao.command;

import java.sql.Connection;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.Identity;

public interface AuthorizationHelper
{
	void checkCreateAllowed(int index, Connection connection, Identity identity, Resource newResource);

	void checkReadAllowed(int index, Connection connection, Identity identity, Resource existingResource);

	void checkUpdateAllowed(int index, Connection connection, Identity identity, Resource oldResource,
			Resource newResource);

	void checkDeleteAllowed(int index, Connection connection, Identity identity, Resource oldResource);

	void checkSearchAllowed(int index, Identity identity, String resourceTypeName);

	void filterIncludeResults(int index, Connection connection, Identity identity, Bundle multipleResult);
}
