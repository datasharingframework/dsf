package dev.dsf.fhir.dao.command;

import java.sql.Connection;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.authentication.User;

public interface AuthorizationHelper
{
	void checkCreateAllowed(Connection connection, User user, Resource newResource);

	void checkReadAllowed(Connection connection, User user, Resource existingResource);

	void checkUpdateAllowed(Connection connection, User user, Resource oldResource, Resource newResource);

	void checkDeleteAllowed(Connection connection, User user, Resource oldResource);

	void checkSearchAllowed(User user, String resourceTypeName);

	void filterIncludeResults(Connection connection, User user, Bundle multipleResult);
}
