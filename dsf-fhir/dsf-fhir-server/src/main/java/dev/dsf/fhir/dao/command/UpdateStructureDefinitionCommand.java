package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.exception.ResourceVersionNoMatchException;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.prefer.PreferReturnType;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.SnapshotGenerator.SnapshotWithValidationMessages;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class UpdateStructureDefinitionCommand extends UpdateCommand<StructureDefinition, StructureDefinitionDao>
		implements Command
{
	private static final Logger logger = LoggerFactory.getLogger(UpdateStructureDefinitionCommand.class);

	private final StructureDefinitionDao snapshotDao;

	private StructureDefinition resourceWithSnapshot;

	public UpdateStructureDefinitionCommand(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper,
			StructureDefinition resource, StructureDefinitionDao dao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, ResponseGenerator responseGenerator,
			ReferenceExtractor referenceExtractor, ReferenceResolver referenceResolver,
			ReferenceCleaner referenceCleaner, EventGenerator eventGenerator, StructureDefinitionDao snapshotDao)
	{
		super(index, identity, returnType, bundle, entry, serverBase, authorizationHelper, resource, dao,
				exceptionHandler, parameterConverter, responseGenerator, referenceExtractor, referenceResolver,
				referenceCleaner, eventGenerator);

		this.snapshotDao = snapshotDao;
	}

	@Override
	public void preExecute(Map<String, IdType> idTranslationTable, Connection connection,
			ValidationHelper validationHelper, SnapshotGenerator snapshotGenerator)
	{
		resourceWithSnapshot = resource.hasSnapshot() ? resource.copy()
				: generateSnapshot(snapshotGenerator, resource.copy());
		resource.setSnapshot(null);

		super.preExecute(idTranslationTable, connection, validationHelper, snapshotGenerator);
	}

	private StructureDefinition generateSnapshot(SnapshotGenerator snapshotGenerator, StructureDefinition resource)
	{
		logger.debug("Generating snapshot for bundle entry at index {}", index);
		SnapshotWithValidationMessages s = snapshotGenerator.generateSnapshot(resource);

		if (s.getMessages().stream()
				.anyMatch(m -> IssueSeverity.FATAL.equals(m.getLevel()) || IssueSeverity.ERROR.equals(m.getLevel())))
		{
			Response response = responseGenerator.unableToGenerateSnapshot(resource, index, s.getMessages());
			throw new WebApplicationException(response);
		}

		return s.getSnapshot();
	}

	@Override
	protected StructureDefinition createWithTransactionAndId(Connection connection, StructureDefinition resource,
			UUID uuid) throws SQLException
	{
		StructureDefinition created = super.createWithTransactionAndId(connection, resource, uuid);

		if (resourceWithSnapshot != null)
		{
			try
			{
				snapshotDao.createWithTransactionAndId(connection, resourceWithSnapshot, uuid);
			}
			catch (SQLException e)
			{
				logger.debug("Error while creating StructureDefinition snapshot", e);
				logger.warn("Error while creating StructureDefinition snapshot: {} - {}", e.getClass().getName(),
						e.getMessage());

				throw e;
			}
		}

		return created;
	}

	@Override
	protected StructureDefinition updateWithTransaction(Connection connection, StructureDefinition resource,
			Long expectedVersion) throws SQLException, ResourceNotFoundException, ResourceVersionNoMatchException
	{
		StructureDefinition updated = super.updateWithTransaction(connection, resource, expectedVersion);

		if (resourceWithSnapshot != null)
		{
			if (!resourceWithSnapshot.hasId() && resource.hasId())
				resourceWithSnapshot.setIdElement(resource.getIdElement().copy());

			try
			{
				snapshotDao.updateWithTransaction(connection, resourceWithSnapshot, expectedVersion);
			}
			catch (SQLException | ResourceNotFoundException | ResourceVersionNoMatchException e)
			{
				logger.debug("Error while updating StructureDefinition snapshot", e);
				logger.warn("Error while updating StructureDefinition snapshot: {} - {}", e.getClass().getName(),
						e.getMessage());

				throw e;
			}
		}

		return updated;
	}
}
