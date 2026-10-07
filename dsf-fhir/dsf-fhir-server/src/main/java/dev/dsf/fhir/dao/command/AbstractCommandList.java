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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.fhir.help.ExceptionHandler;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;

abstract class AbstractCommandList
{
	private static final Logger audit = LoggerFactory.getLogger("dsf-audit-logger");

	public static final class CommandResult
	{
		private final Command command;

		private BundleEntryComponent result;
		private Exception exception;

		public CommandResult(Command command)
		{
			this.command = command;
		}

		public Command getCommand()
		{
			return command;
		}

		public void setResult(BundleEntryComponent result)
		{
			Objects.requireNonNull(result, "result");
			if (hasException())
				throw new IllegalStateException("Exception set");

			this.result = result;
		}

		public BundleEntryComponent getResult()
		{
			return result;
		}

		public boolean hasResult()
		{
			return result != null;
		}

		public void setException(Exception exception)
		{
			Objects.requireNonNull(exception, "exception");
			if (hasException())
				throw new IllegalStateException("Exception set");

			this.exception = exception;
		}

		public void addSuppressedException(Throwable exception)
		{
			Objects.requireNonNull(exception, "exception");
			if (!hasException())
				throw new IllegalStateException("Exception not set");

			this.exception.addSuppressed(exception);
		}

		public Exception getException()
		{
			return exception;
		}

		public boolean hasException()
		{
			return exception != null;
		}

		public void convertExceptionToResult(Function<Exception, Exception> internalServerErrorConverter)
		{
			if (hasException())
			{
				result = toEntry(exception, internalServerErrorConverter);
				exception = null;
			}
		}

		private BundleEntryComponent toEntry(Exception exception,
				Function<Exception, Exception> internalServerErrorConverter)
		{
			BundleEntryComponent entry = new BundleEntryComponent();
			BundleEntryResponseComponent response = entry.getResponse();

			if (!(exception instanceof WebApplicationException w)
					|| !(w.getResponse().getEntity() instanceof OperationOutcome))
			{
				exception = internalServerErrorConverter.apply(exception);
			}

			Response httpResponse = ((WebApplicationException) exception).getResponse();
			response.setStatus(httpResponse.getStatusInfo().getStatusCode() + " "
					+ httpResponse.getStatusInfo().getReasonPhrase());
			response.setOutcome((OperationOutcome) httpResponse.getEntity());

			return entry;
		}
	}

	protected final DataSource dataSource;
	protected final DataSource permanentDeleteDataSource;
	protected final String dbUsersGroup;
	protected final ExceptionHandler exceptionHandler;

	protected final List<CommandResult> commands;
	protected final boolean hasModifyingCommands;
	protected final boolean hasBinaryModifyingCommands;

	protected AbstractCommandList(DataSource dataSource, DataSource permanentDeleteDataSource, String dbUsersGroup,
			ExceptionHandler exceptionHandler, List<? extends Command> commands, Comparator<Command> sortOrder)
	{
		this.dataSource = dataSource;
		this.permanentDeleteDataSource = permanentDeleteDataSource;
		this.dbUsersGroup = dbUsersGroup;
		this.exceptionHandler = exceptionHandler;

		this.commands = commands == null ? List.of()
				: commands.stream().sorted(sortOrder).map(CommandResult::new).toList();

		hasModifyingCommands = hasModifyingCommands(commands);
		hasBinaryModifyingCommands = hasModifyingCommands && hasBinaryModifyingCommands(commands);
	}

	private static boolean hasModifyingCommands(List<? extends Command> commands)
	{
		return commands != null && commands.stream().anyMatch(c -> c instanceof ModifyingCommand);
	}

	private static boolean hasBinaryModifyingCommands(List<? extends Command> commands)
	{
		return commands != null && commands.stream().anyMatch(c -> c instanceof ModifyingCommand m
				&& Binary.class.getAnnotation(ResourceDef.class).name().equals(m.getResourceTypeName()));
	}

	protected void auditLogResult(CommandResult commandResult)
	{
		if (commandResult.hasException())
			throw new IllegalArgumentException("CommandResult has exception");

		auditLogResult(commandResult.getCommand(), commandResult.getResult());
	}

	protected void auditLogResult(Command command, BundleEntryComponent result)
	{
		Objects.requireNonNull(command, "command");
		Objects.requireNonNull(result, "result");

		String resultOutcome = failed(result) ? "failed" : "successful";

		if (command instanceof DeleteCommand)
		{
			audit.info("Delete of {} for identity '{}' via bundle at index {} {}, status: {}",
					command.getResourceTypeName(), command.getIdentity().getName(), command.getIndex(), resultOutcome,
					result.getResponse().getStatus());
		}
		else if (command instanceof CreateCommand)
		{
			audit.info("Create of {} for identity '{}' via bundle at index {} {}, status: {}",
					command.getResourceTypeName(), command.getIdentity().getName(), command.getIndex(), resultOutcome,
					result.getResponse().getStatus());
		}
		else if (command instanceof UpdateCommand)
		{
			audit.info("Update of {} for identity '{}' via bundle at index {} {}, status: {}",
					command.getResourceTypeName(), command.getIdentity().getName(), command.getIndex(), resultOutcome,
					result.getResponse().getStatus());
		}
		else if (command instanceof ReadCommand)
		{
			audit.info("{} of {} for identity '{}' via bundle at index {} {}, status: {}",
					((ReadCommand) command).isSearch() ? "Search" : "Read", command.getResourceTypeName(),
					command.getIdentity().getName(), command.getIndex(), resultOutcome,
					result.getResponse().getStatus());
		}
	}

	protected void auditLogAbort(Command command)
	{
		if (command instanceof DeleteCommand)
		{
			audit.info("Delete of {} for identity '{}' via bundle at index {} aborted", command.getResourceTypeName(),
					command.getIdentity().getName(), command.getIndex());
		}
		else if (command instanceof CreateCommand)
		{
			audit.info("Create of {} for identity '{}' via bundle at index {} aborted", command.getResourceTypeName(),
					command.getIdentity().getName(), command.getIndex());
		}
		else if (command instanceof UpdateCommand)
		{
			audit.info("Update of {} for identity '{}' via bundle at index {} aborted", command.getResourceTypeName(),
					command.getIdentity().getName(), command.getIndex());
		}
		else if (command instanceof ReadCommand r)
		{
			audit.info("{} of {} for identity '{}' via bundle at index {} aborted", r.isSearch() ? "Search" : "Read",
					command.getResourceTypeName(), command.getIdentity().getName(), command.getIndex());
		}
	}

	private boolean failed(BundleEntryComponent result)
	{
		if (result.hasResponse() && result.getResponse().hasStatus())
		{
			String status = result.getResponse().getStatus();
			if (status.length() >= 3 && status.matches("[0-9]{3}.*"))
				return !Family.SUCCESSFUL
						.equals(Status.fromStatusCode(Integer.parseInt(status.substring(0, 3))).getFamily());
		}

		return false;
	}

	protected abstract Exception internalServerError(Exception exception);
}
