package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.prefer.PreferReturnType;

public class DeleteStructureDefinitionCommand extends DeleteCommand
{
	private static final Logger logger = LoggerFactory.getLogger(DeleteStructureDefinitionCommand.class);

	private StructureDefinitionDao snapshotDao;

	public DeleteStructureDefinitionCommand(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper,
			ResponseGenerator responseGenerator, DaoProvider daoProvider, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, EventGenerator eventGenerator)
	{
		super(index, identity, returnType, bundle, entry, serverBase, authorizationHelper, responseGenerator,
				daoProvider, exceptionHandler, parameterConverter, eventGenerator);

		snapshotDao = daoProvider.getStructureDefinitionSnapshotDao();
	}

	@Override
	protected boolean deleteWithTransaction(ResourceDao<?> dao, Connection connection, UUID uuid)
			throws SQLException, ResourceNotFoundException
	{
		boolean deleted = super.deleteWithTransaction(dao, connection, uuid);

		try
		{
			snapshotDao.deleteWithTransaction(connection, uuid);
		}
		catch (SQLException | ResourceNotFoundException e)
		{
			logger.warn("Error while deleting StructureDefinition snaphost for id " + uuid.toString()
					+ ", exception will be ignored", e);
		}

		return deleted;
	}
}
