package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.IdType;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public BatchCommandList(DataSource dataSource, ExceptionHandler exceptionHandler, List<? extends Command> commands,
			ValidationHelper validationHelper, SnapshotGenerator snapshotGenerator, EventHandler eventHandler,
			ResponseGenerator responseGenerator)
	{
		super(dataSource, exceptionHandler, commands);

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

			Map<Integer, Exception> caughtExceptions = new HashMap<>((int) (commands.size() / 0.75) + 1);
			Map<String, IdType> idTranslationTable = new HashMap<>();

			if (hasModifyingCommands)
			{
				connection.setReadOnly(false);
				connection.setAutoCommit(false);
				connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			}

			commands.forEach(preExecute(idTranslationTable, connection, caughtExceptions));

			commands.forEach(execute(idTranslationTable, connection, caughtExceptions));

			if (hasModifyingCommands)
			{
				connection.setReadOnly(initialReadOnly);
				connection.setAutoCommit(initialAutoCommit);
				connection.setTransactionIsolation(initialTransactionIsolationLevel);
			}

			Map<Integer, BundleEntryComponent> results = new HashMap<>((int) (commands.size() / 0.75) + 1);

			commands.forEach(postExecute(connection, caughtExceptions, results));
			caughtExceptions.forEach((k, v) -> results.put(k, toEntry(v)));

			results.entrySet().stream().sorted(Comparator.comparing(Entry::getKey)).forEach(e ->
			{
				Command command = commands.get(e.getKey());
				BundleEntryComponent result = e.getValue();
				auditLogResult(command, result);
			});

			Bundle result = new Bundle();
			result.setType(BundleType.BATCHRESPONSE);
			results.entrySet().stream().sorted(Comparator.comparing(Entry::getKey)).map(Entry::getValue)
					.forEach(result::addEntry);

			return result;
		}
		catch (Exception e)
		{
			throw exceptionHandler.internalServerErrorBundleTransaction(e);
		}
	}

	private Consumer<Command> preExecute(Map<String, IdType> idTranslationTable, Connection connection,
			Map<Integer, Exception> caughtExceptions)
	{
		return command ->
		{
			try
			{
				if (!caughtExceptions.containsKey(command.getIndex()))
				{
					logger.debug("Running pre-execute of command {} for entry at index {}",
							command.getClass().getName(), command.getIndex());
					command.preExecute(idTranslationTable, connection, validationHelper, snapshotGenerator);
				}
				else
				{
					logger.info("Skipping pre-execute of command {} for entry at index {}, caught exception {}",
							command.getClass().getName(), command.getIndex(),
							caughtExceptions.get(command.getIndex()).getClass().getName() + ": "
									+ caughtExceptions.get(command.getIndex()).getMessage());
				}
			}
			catch (Exception e)
			{
				logger.debug("Error while running pre-execute of command {} for entry at index {}",
						command.getClass().getName(), command.getIndex(), e);
				logger.warn("Error while running pre-execute of command {} for entry at index {}: {} - {}",
						command.getClass().getName(), command.getIndex(), e.getClass().getName(), e.getMessage());

				caughtExceptions.put(command.getIndex(), e);
			}
		};
	}

	private Consumer<Command> execute(Map<String, IdType> idTranslationTable, Connection connection,
			Map<Integer, Exception> caughtExceptions)
	{
		return command ->
		{
			try
			{
				if (!caughtExceptions.containsKey(command.getIndex()))
				{
					logger.debug("Running execute of command {} for entry at index {}", command.getClass().getName(),
							command.getIndex());
					command.execute(idTranslationTable, connection, validationHelper, snapshotGenerator);
				}
				else
				{
					logger.info("Skipping execute of command {} for entry at index {}, caught exception {}",
							command.getClass().getName(), command.getIndex(),
							caughtExceptions.get(command.getIndex()).getClass().getName() + ": "
									+ caughtExceptions.get(command.getIndex()).getMessage());
				}

				if (!connection.getAutoCommit())
					connection.commit();
			}
			catch (Exception e)
			{
				logger.debug("Error while executing command {}, rolling back transaction for entry at index {}",
						command.getClass().getName(), command.getIndex(), e);
				logger.warn("Error while executing command {}, rolling back transaction for entry at index {}: {} - {}",
						command.getClass().getName(), command.getIndex(), e.getClass().getName(), e.getMessage());

				if (e instanceof PSQLException s && PSQLState.UNIQUE_VIOLATION.getState().equals(s.getSQLState()))
					caughtExceptions.put(command.getIndex(),
							new WebApplicationException(responseGenerator.dupicateResourceExists()));
				else
					caughtExceptions.put(command.getIndex(), e);


				try
				{
					if (!connection.getAutoCommit())
						connection.rollback();
				}
				catch (SQLException e1)
				{
					logger.debug(
							"Error while executing command {}, error while rolling back transaction for entry at index {}",
							command.getClass().getName(), command.getIndex(), e1);
					logger.warn(
							"Error while executing command {}, error while rolling back transaction for entry at index {}: {} - {}",
							command.getClass().getName(), command.getIndex(), e1.getClass().getName(), e1.getMessage());

					caughtExceptions.put(command.getIndex(), e1);
				}
			}
		};
	}

	private Consumer<Command> postExecute(Connection connection, Map<Integer, Exception> caughtExceptions,
			Map<Integer, BundleEntryComponent> results)
	{
		return command ->
		{
			try
			{
				if (!caughtExceptions.containsKey(command.getIndex()))
				{
					logger.debug("Running post-execute of command {} for entry at index {}",
							command.getClass().getName(), command.getIndex());

					Optional<BundleEntryComponent> optResult = command.postExecute(connection, eventHandler);
					optResult.ifPresent(result -> results.put(command.getIndex(), result));
				}
				else
				{
					logger.info("Skipping post-execute of command {} for entry at index {}, caught exception {}",
							command.getClass().getName(), command.getIndex(),
							caughtExceptions.get(command.getIndex()).getClass().getName() + ": "
									+ caughtExceptions.get(command.getIndex()).getMessage());
				}
			}
			catch (Exception e)
			{
				logger.debug("Error while running post-execute of command {} for entry at index {}",
						command.getClass().getName(), command.getIndex(), e);
				logger.warn("Error while running post-execute of command {} for entry at index {}: {} - {}",
						command.getClass().getName(), command.getIndex(), e.getClass().getName(), e.getMessage());

				caughtExceptions.put(command.getIndex(), e);
			}
		};
	}

	@Override
	protected Exception internalServerError(Exception exception)
	{
		return exceptionHandler.internalServerErrorBundleBatch(exception);
	}
}
