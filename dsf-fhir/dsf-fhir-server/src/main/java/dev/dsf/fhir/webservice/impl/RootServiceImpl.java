package dev.dsf.fhir.webservice.impl;

import java.util.Objects;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.dao.command.CommandFactory;
import dev.dsf.fhir.dao.command.CommandList;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.history.HistoryService;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.webservice.base.AbstractBasicService;
import dev.dsf.fhir.webservice.specification.RootService;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

public class RootServiceImpl extends AbstractBasicService implements RootService, InitializingBean
{
	public static final String ROOT_GET = RootServiceImpl.class.getCanonicalName() + ".get";

	private final CommandFactory commandFactory;
	private final ResponseGenerator responseGenerator;
	private final ParameterConverter parameterConverter;
	private final ExceptionHandler exceptionHandler;
	private final ReferenceCleaner referenceCleaner;
	private final HistoryService historyService;

	public RootServiceImpl(CommandFactory commandFactory, ResponseGenerator responseGenerator,
			ParameterConverter parameterConverter, ExceptionHandler exceptionHandler, ReferenceCleaner referenceCleaner,
			HistoryService historyService)
	{
		this.commandFactory = commandFactory;
		this.responseGenerator = responseGenerator;
		this.parameterConverter = parameterConverter;
		this.exceptionHandler = exceptionHandler;
		this.referenceCleaner = referenceCleaner;
		this.historyService = historyService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(commandFactory, "commandFactory");
		Objects.requireNonNull(responseGenerator, "responseGenerator");
		Objects.requireNonNull(parameterConverter, "parameterConverter");
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		Objects.requireNonNull(referenceCleaner, "referenceCleaner");
		Objects.requireNonNull(historyService, "historyService");
	}

	@Override
	public Response root(UriInfo uri, HttpHeaders headers)
	{
		OperationOutcome outcome = responseGenerator.createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"This is the base URL of the FHIR server. GET method not allowed, use POST to execute batch and transaction Bundle resources");
		outcome.setUserData(ROOT_GET, true);

		return responseGenerator
				.response(Status.METHOD_NOT_ALLOWED, outcome,
						parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers))
				.allow(HttpMethod.POST).build();
	}

	@Override
	public Response handleBundle(Bundle bundle, UriInfo uri, HttpHeaders headers)
	{
		CommandList commands = exceptionHandler
				.handleBadBundleException(() -> commandFactory.createCommands(bundle, getCurrentIdentity(),
						parameterConverter.getPreferReturn(headers), parameterConverter.getPreferHandling(headers)));

		Bundle result = commands.execute(); // throws WebApplicationException

		return responseGenerator
				.response(Status.OK, result, parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers)).build();
	}

	@Override
	public Response history(UriInfo uri, HttpHeaders headers)
	{
		Bundle history = historyService.getHistory(getCurrentIdentity(), uri, headers);

		return responseGenerator.response(Status.OK, referenceCleaner.cleanLiteralReferences(history),
				parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers)).build();
	}
}
