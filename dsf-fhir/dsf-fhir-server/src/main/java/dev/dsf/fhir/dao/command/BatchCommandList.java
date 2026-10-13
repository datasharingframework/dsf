/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.IdType;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.dao.jdbc.LargeObjectManager;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.validation.SnapshotGenerator;
import jakarta.ws.rs.WebApplicationException;

public class BatchCommandList extends AbstractCommandList implements CommandList
{
	private static final Logger logger = LoggerFactory.getLogger(BatchCommandList.class);

	private final ValidationHelper validationHelper;
	private final SnapshotGenerator snapshotGenerator;
	private final EventHandler eventHandler;
	private final ResponseGenerator responseGenerator;

	public BatchCommandList(DataSource dataSource, DataSource permanentDeleteDataSource, String selectUpdateUser,
			ExceptionHandler exceptionHandler, List<? extends Command> commands, ValidationHelper validationHelper,
			SnapshotGenerator snapshotGenerator, EventHandler eventHandler, ResponseGenerator responseGenerator)
	{
		super(dataSource, permanentDeleteDataSource, selectUpdateUser, exceptionHandler, commands, (_, _) -> 0);

		this.validationHelper = validationHelper;
		this.snapshotGenerator = snapshotGenerator;
		this.eventHandler = eventHandler;
		this.responseGenerator = responseGenerator;
	}

	@Override
	public Bundle execute() throws WebApplicationException
	{
		try (Connection connection = dataSource.getConnection())
		{
			boolean initialReadOnly = connection.isReadOnly();
			boolean initialAutoCommit = connection.getAutoCommit();
			int initialTransactionIsolationLevel = connection.getTransactionIsolation();

			Map<String, IdType> idTranslationTable = new HashMap<>();
			Set<Integer> indexesWithError = new HashSet<>();

			commands.forEach(cR ->
			{
				preExecute(cR, connection, idTranslationTable);

				if (cR.hasException())
					indexesWithError.add(cR.getCommand().getIndex());
			});

			if (hasModifyingCommands)
			{
				connection.setReadOnly(false);
				connection.setAutoCommit(false);
				connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			}

			commands.stream().filter(Predicate.not(CommandResult::hasException)).forEach(cR ->
			{
				if (!indexesWithError.contains(cR.getCommand().getIndex()))
					execute(cR, connection, idTranslationTable);

				if (cR.hasException())
					indexesWithError.add(cR.getCommand().getIndex());
			});

			if (hasModifyingCommands)
			{
				connection.setReadOnly(initialReadOnly);
				connection.setAutoCommit(initialAutoCommit);
				connection.setTransactionIsolation(initialTransactionIsolationLevel);
			}

			commands.stream().filter(Predicate.not(CommandResult::hasException)).forEach(cR ->
			{
				if (!indexesWithError.contains(cR.getCommand().getIndex()))
					postExecute(cR, connection);

				if (cR.hasException())
					indexesWithError.add(cR.getCommand().getIndex());
			});

			Bundle result = new Bundle();
			result.setType(BundleType.BATCHRESPONSE);

			commands.forEach(c ->
			{
				c.convertExceptionToResult(this::internalServerError);

				if (c.hasResult())
				{
					auditLogResult(c);
					result.addEntry(c.getResult());
				}
			});

			return result;
		}
		catch (Exception e)
		{
			throw exceptionHandler.internalServerErrorBundleTransaction(e);
		}
	}

	private void preExecute(CommandResult commandResult, Connection connection, Map<String, IdType> idTranslationTable)
	{
		if (commandResult.hasException())
			throw new IllegalStateException("Command has exception");

		Command command = commandResult.getCommand();

		try
		{
			logger.debug("Running pre-execute of {} for entry at index {}", command.getClass().getSimpleName(),
					command.getIndex());
			command.preExecute(idTranslationTable, connection, validationHelper, snapshotGenerator);
		}
		catch (Exception e)
		{
			logger.debug("Error while running pre-execute of {} for entry at index {}",
					command.getClass().getSimpleName(), command.getIndex(), e);
			logger.warn("Error while running pre-execute of {} for entry at index {}: {} - {}",
					command.getClass().getSimpleName(), command.getIndex(), e.getClass().getName(), e.getMessage());

			commandResult.setException(e);
		}
	}

	private void execute(CommandResult commandResult, Connection connection, Map<String, IdType> idTranslationTable)
	{
		if (commandResult.hasException())
			throw new IllegalStateException("Command has exception");

		Command command = commandResult.getCommand();

		LargeObjectManager largeObjectManager = command instanceof ModifyingCommand m
				? m.createLargeObjectManager(connection)
				: LargeObjectManager.NO_OP;

		try
		{
			logger.debug("Running execute and check-references of {} for entry at index {}",
					command.getClass().getSimpleName(), command.getIndex());
			command.execute(idTranslationTable, largeObjectManager, connection, validationHelper);

			command.checkReferences(idTranslationTable, connection);

			if (!connection.getAutoCommit())
				connection.commit();
		}
		catch (Exception e)
		{
			logger.debug("Error while executing {}, rolling back transaction for entry at index {}",
					command.getClass().getSimpleName(), command.getIndex(), e);
			logger.warn("Error while executing {}, rolling back transaction for entry at index {}: {} - {}",
					command.getClass().getSimpleName(), command.getIndex(), e.getClass().getName(), e.getMessage());

			if (e instanceof PSQLException s && PSQLState.UNIQUE_VIOLATION.getState().equals(s.getSQLState()))
				commandResult.setException(new WebApplicationException(responseGenerator.duplicateResourceExists()));
			else
				commandResult.setException(e);

			try
			{
				if (!connection.getAutoCommit())
					connection.rollback();
			}
			catch (SQLException rollbackException)
			{
				logger.debug("Error while executing {}, error while rolling back transaction for entry at index {}",
						command.getClass().getSimpleName(), command.getIndex(), rollbackException);
				logger.warn(
						"Error while executing {}, error while rolling back transaction for entry at index {}: {} - {}",
						command.getClass().getSimpleName(), command.getIndex(), rollbackException.getClass().getName(),
						rollbackException.getMessage());

				commandResult.addSuppressedException(rollbackException);
			}

			try
			{
				if (!connection.getAutoCommit())
					largeObjectManager.rollback();
			}
			catch (SQLException rollbackException)
			{
				logger.debug("Error while executing {}, error while rolling back DB large object for entry at index {}",
						command.getClass().getSimpleName(), command.getIndex(), rollbackException);
				logger.warn(
						"Error while executing {}, error while rolling back DB large object for entry at index {}: {} - {}",
						command.getClass().getSimpleName(), command.getIndex(), rollbackException.getClass().getName(),
						rollbackException.getMessage());

				commandResult.addSuppressedException(rollbackException);
			}
		}
	}

	private void postExecute(CommandResult commandResult, Connection connection)
	{
		if (commandResult.hasException())
			throw new IllegalStateException("Command has exception");

		Command command = commandResult.getCommand();

		try
		{
			logger.debug("Running post-execute of {} for entry at index {}", command.getClass().getSimpleName(),
					command.getIndex());

			command.postExecute(connection, eventHandler).ifPresent(commandResult::setResult);
		}
		catch (Exception e)
		{
			logger.debug("Error while running post-execute of {} for entry at index {}",
					command.getClass().getSimpleName(), command.getIndex(), e);
			logger.warn("Error while running post-execute of {} for entry at index {}: {} - {}",
					command.getClass().getSimpleName(), command.getIndex(), e.getClass().getName(), e.getMessage());

			commandResult.setException(e);
		}
	}

	@Override
	protected Exception internalServerError(Exception exception)
	{
		return exceptionHandler.internalServerErrorBundleBatch(exception);
	}
}
