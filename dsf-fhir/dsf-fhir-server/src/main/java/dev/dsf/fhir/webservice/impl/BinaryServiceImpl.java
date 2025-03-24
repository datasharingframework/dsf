package dev.dsf.fhir.webservice.impl;

import java.io.InputStream;
import java.util.List;

import org.hl7.fhir.r4.model.Binary;

import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.fhir.authorization.AuthorizationRuleProvider;
import dev.dsf.fhir.dao.BinaryDao;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.history.HistoryService;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.BinaryService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class BinaryServiceImpl extends AbstractResourceServiceImpl<BinaryDao, Binary> implements BinaryService
{
	public BinaryServiceImpl(String path, String serverBase, int defaultPageCount, BinaryDao dao,
			ResourceValidator validator, EventHandler eventHandler, ExceptionHandler exceptionHandler,
			EventGenerator eventGenerator, ResponseGenerator responseGenerator, ParameterConverter parameterConverter,
			ReferenceExtractor referenceExtractor, ReferenceResolver referenceResolver,
			ReferenceCleaner referenceCleaner, AuthorizationRuleProvider authorizationRuleProvider,
			HistoryService historyService, ValidationRules validationRules)
	{
		super(path, Binary.class, serverBase, defaultPageCount, dao, validator, eventHandler, exceptionHandler,
				eventGenerator, responseGenerator, parameterConverter, referenceExtractor, referenceResolver,
				referenceCleaner, authorizationRuleProvider, historyService, validationRules);
	}

	@Override
	public Response create(InputStream in, UriInfo uri, HttpHeaders headers)
	{
		throw new UnsupportedOperationException("Implemented and delegated by jaxrs layer");
	}

	@Override
	public Response update(String id, InputStream in, UriInfo uri, HttpHeaders headers)
	{
		throw new UnsupportedOperationException("Implemented and delegated by jaxrs layer");
	}

	@Override
	protected MediaType getMediaTypeForRead(UriInfo uri, HttpHeaders headers)
	{
		if (uri.getQueryParameters().containsKey(Constants.PARAM_FORMAT))
			return super.getMediaTypeForRead(uri, headers);
		else
			return getMediaType(headers);
	}

	@Override
	protected MediaType getMediaTypeForVRead(UriInfo uri, HttpHeaders headers)
	{
		if (uri.getQueryParameters().containsKey(Constants.PARAM_FORMAT))
			return super.getMediaTypeForVRead(uri, headers);
		else
			return getMediaType(headers);
	}

	private MediaType getMediaType(HttpHeaders headers)
	{
		List<MediaType> types = headers.getAcceptableMediaTypes();
		return types == null ? null : types.get(0);
	}
}
