package dev.dsf.fhir.dao.command;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.help.ExceptionHandler;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;

class AbstractCommandList
{
	private static final Logger audit = LoggerFactory.getLogger("dsf-audit-logger");

	protected final DataSource dataSource;
	protected final ExceptionHandler exceptionHandler;

	protected final List<Command> commands = new ArrayList<>();
	protected final boolean hasModifyingCommands;

	protected AbstractCommandList(DataSource dataSource, ExceptionHandler exceptionHandler,
			List<? extends Command> commands)
	{
		this.dataSource = dataSource;
		this.exceptionHandler = exceptionHandler;

		if (commands != null)
			this.commands.addAll(commands);

		hasModifyingCommands = hasModifyingCommands(commands);
	}

	private static boolean hasModifyingCommands(List<? extends Command> commands)
	{
		return commands.stream().anyMatch(c -> c instanceof ModifyingCommand);
	}

	protected void auditLogResult(Command command, BundleEntryComponent result)
	{
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

	protected void auditLogAbbort(Command command)
	{
		if (command instanceof DeleteCommand)
		{
			audit.info("Delete of {} for identity '{}' via bundle at index {} abborted", command.getResourceTypeName(),
					command.getIdentity().getName(), command.getIndex());
		}
		else if (command instanceof CreateCommand)
		{
			audit.info("Create of {} for identity '{}' via bundle at index {} abborted", command.getResourceTypeName(),
					command.getIdentity().getName(), command.getIndex());
		}
		else if (command instanceof UpdateCommand)
		{
			audit.info("Update of {} for identity '{}' via bundle at index {} abborted", command.getResourceTypeName(),
					command.getIdentity().getName(), command.getIndex());
		}
		else if (command instanceof ReadCommand)
		{
			audit.info("{} of {} for identity '{}' via bundle at index {} abborted",
					((ReadCommand) command).isSearch() ? "Search" : "Read", command.getResourceTypeName(),
					command.getIdentity().getName(), command.getIndex());
		}
	}

	private boolean failed(BundleEntryComponent result)
	{
		if (result != null && result.hasResponse() && result.getResponse().hasStatus())
		{
			String status = result.getResponse().getStatus();
			if (status.length() >= 3 && status.matches("[0-9]{3}.*"))
				return !Family.SUCCESSFUL
						.equals(Status.fromStatusCode(Integer.parseInt(status.substring(0, 3))).getFamily());
		}

		return false;
	}

	protected BundleEntryComponent toEntry(Exception exception)
	{
		var entry = new BundleEntryComponent();
		var response = entry.getResponse();

		if (!(exception instanceof WebApplicationException)
				|| !(((WebApplicationException) exception).getResponse().getEntity() instanceof OperationOutcome))
		{
			exception = exceptionHandler.internalServerErrorBundleBatch(exception);
		}

		Response httpResponse = ((WebApplicationException) exception).getResponse();
		response.setStatus(
				httpResponse.getStatusInfo().getStatusCode() + " " + httpResponse.getStatusInfo().getReasonPhrase());
		response.setOutcome((OperationOutcome) httpResponse.getEntity());

		return entry;
	}
}
