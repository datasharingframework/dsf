package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;

import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.validation.SnapshotGenerator;

public interface Command
{
	String URL_UUID_PREFIX = "urn:uuid:";

	int getIndex();

	int getTransactionPriority();

	default void preExecute(Map<String, IdType> idTranslationTable, Connection connection,
			ValidationHelper validationHelper, SnapshotGenerator snapshotGenerator)

	{
	}

	void execute(Map<String, IdType> idTranslationTable, Connection connection, ValidationHelper validationHelper,
			SnapshotGenerator snapshotGenerator) throws SQLException, WebApplicationException;

	default Optional<BundleEntryComponent> postExecute(Connection connection, EventHandler eventHandler)
	{
		return Optional.empty();
	}
}
