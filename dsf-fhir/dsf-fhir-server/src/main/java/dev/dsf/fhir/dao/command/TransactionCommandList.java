package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.validation.SnapshotGenerator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class TransactionCommandList extends AbstractCommandList implements CommandList
{
	private static final Logger logger = LoggerFactory.getLogger(TransactionCommandList.class);

	private final Function<Connection, TransactionResources> transactionResourceFactory;

	public TransactionCommandList(DataSource dataSource, ExceptionHandler exceptionHandler,
			List<? extends Command> commands, Function<Connection, TransactionResources> transactionResourceFactory)
	{
		super(dataSource, exceptionHandler, commands);

		this.transactionResourceFactory = transactionResourceFactory;

		Collections.sort(this.commands,
				Comparator.comparing(Command::getTransactionPriority).thenComparing(Command::getIndex));
	}

	@Override
	public Bundle execute() throws WebApplicationException
	{
		Map<Integer, BundleEntryComponent> results = new HashMap<>((int) ((commands.size() / 0.75) + 1));
		try
		{
			TransactionEventHandler transactionEventHandler;
			try (Connection connection = dataSource.getConnection())
			{
				if (hasModifyingCommands)
				{
					connection.setReadOnly(false);
					connection.setAutoCommit(false);
					connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
				}

				TransactionResources transactionResources = transactionResourceFactory.apply(connection);
				transactionEventHandler = transactionResources.getTransactionEventHandler();
				ValidationHelper validationHelper = transactionResources.getValidationHelper();
				SnapshotGenerator snapshotGenerator = transactionResources.getSnapshotGenerator();

				Map<String, IdType> idTranslationTable = new HashMap<>();
				for (Command c : commands)
				{
					try
					{
						logger.debug("Running pre-execute of command {} for entry at index {}", c.getClass().getName(),
								c.getIndex());
						c.preExecute(idTranslationTable, connection, validationHelper, snapshotGenerator);
					}
					catch (Exception e)
					{
						logger.warn(
								"Error while running pre-execute of command {} for entry at index {}, abborting transaction: {} - {}",
								c.getClass().getSimpleName(), c.getIndex(), e.getClass().getName(), e.getMessage());
						logger.debug(
								"Error while running pre-execute of command {} for entry at index {}, abborting transaction",
								c.getClass().getSimpleName(), c.getIndex(), e);

						try
						{
							commands.stream().limit(c.getIndex()).forEach(this::auditLogAbbort);
							auditLogResult(c, toEntry(e));
						}
						catch (Exception e1)
						{
							logger.warn("Error while writing to audit log: {} - {}", e1.getClass().getName(),
									e1.getMessage());
							logger.debug("Error while writing to audit log", e1);
						}

						throw e;
					}
				}

				for (Command c : commands)
				{
					try
					{
						logger.debug("Running execute of command {} for entry at index {}", c.getClass().getName(),
								c.getIndex());
						c.execute(idTranslationTable, connection, validationHelper, snapshotGenerator);
					}
					catch (Exception e)
					{
						logger.warn(
								"Error while executing command {} for entry at index {}, rolling back transaction: {} - {}",
								c.getClass().getSimpleName(), c.getIndex(), e.getClass().getName(), e.getMessage());
						logger.debug("Error while executing command {} for entry at index {}, rolling back transaction",
								c.getClass().getSimpleName(), c.getIndex(), e);

						if (hasModifyingCommands)
						{
							logger.debug("Rolling back DB transaction");
							connection.rollback();
						}

						try
						{
							commands.stream().limit(c.getIndex()).forEach(this::auditLogAbbort);
							auditLogResult(c, toEntry(e));
						}
						catch (Exception e1)
						{
							logger.warn("Error while writing to audit log: {} - {}", e1.getClass().getName(),
									e1.getMessage());
							logger.debug("Error while writing to audit log", e1);
						}

						throw e;
					}
				}

				for (Command c : commands)
				{
					try
					{
						logger.debug("Running post-execute of command {} for entry at index {}", c.getClass().getName(),
								c.getIndex());
						Optional<BundleEntryComponent> optResult = c.postExecute(connection, transactionEventHandler);
						optResult.ifPresent(result -> results.putIfAbsent(c.getIndex(), result));
					}
					catch (Exception e)
					{
						logger.warn(
								"Error while running post-execute of command {} for entry at index {}, rolling back transaction: {} - {}",
								c.getClass().getSimpleName(), c.getIndex(), e.getClass().getName(), e.getMessage());
						logger.debug(
								"Error while running post-execute of command {} for entry at index {}, rolling back transaction",
								c.getClass().getSimpleName(), c.getIndex(), e);

						if (hasModifyingCommands)
						{
							logger.debug("Rolling back DB transaction");
							connection.rollback();
						}

						try
						{
							commands.stream().limit(c.getIndex()).forEach(this::auditLogAbbort);
							auditLogResult(c, toEntry(e));
						}
						catch (Exception e1)
						{
							logger.warn("Error while writing to audit log: {} - {}", e1.getClass().getName(),
									e1.getMessage());
							logger.debug("Error while writing to audit log", e1);
						}

						throw e;
					}
				}

				if (hasModifyingCommands)
				{
					logger.debug("Committing DB transaction");
					connection.commit();
				}
			}

			try
			{
				logger.debug("Committing events");
				transactionEventHandler.commitEvents();
			}
			catch (Exception e)
			{
				logger.warn("Error while handling events: {} - {}", e.getClass().getName(), e.getMessage());
				logger.debug("Error while handling events", e);
			}

			try
			{
				results.entrySet().stream().sorted(Comparator.comparing(Entry::getKey)).forEach(e ->
				{
					Command command = commands.get(e.getKey());
					BundleEntryComponent result = e.getValue();
					auditLogResult(command, result);
				});
			}
			catch (Exception e)
			{
				logger.warn("Error while writing to audit log: {} - {}", e.getClass().getName(), e.getMessage());
				logger.debug("Error while writing to audit log", e);
			}

			Bundle result = new Bundle();
			result.setType(BundleType.TRANSACTIONRESPONSE);
			results.entrySet().stream().sorted(Comparator.comparing(Entry::getKey)).map(Entry::getValue)
					.forEach(result::addEntry);

			return result;
		}
		catch (WebApplicationException e)
		{
			if (e.getResponse() != null && Status.FORBIDDEN.getStatusCode() == e.getResponse().getStatus())
				throw e;

			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST).entity(e.getResponse().getEntity()).build());
		}
		catch (Exception e)
		{
			throw exceptionHandler.internalServerErrorBundleTransaction(e);
		}
	}
}
