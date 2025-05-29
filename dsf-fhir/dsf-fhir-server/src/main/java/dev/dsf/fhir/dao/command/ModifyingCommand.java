package dev.dsf.fhir.dao.command;

import java.sql.Connection;

import dev.dsf.fhir.dao.jdbc.LargeObjectManager;

public interface ModifyingCommand extends Command
{
	LargeObjectManager createLargeObjectManager(Connection connection);
}
