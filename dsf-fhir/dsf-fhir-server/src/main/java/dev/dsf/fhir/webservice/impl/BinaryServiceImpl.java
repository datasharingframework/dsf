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
package dev.dsf.fhir.webservice.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
import dev.dsf.fhir.webservice.RangeRequest;
import dev.dsf.fhir.webservice.RangeRequestImpl;
import dev.dsf.fhir.webservice.specification.BinaryService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

public class BinaryServiceImpl extends AbstractResourceServiceImpl<BinaryDao, Binary> implements BinaryService
{
	private static final String[] FHIR_MEDIA_TYPES = { Constants.CT_FHIR_XML_NEW, Constants.CT_FHIR_JSON_NEW,
			Constants.CT_FHIR_XML, Constants.CT_FHIR_JSON };

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
	public Response read(String id, UriInfo uri, HttpHeaders headers)
	{
		Optional<Binary> read;
		if (!isValidFhirRequest(uri, headers))
		{
			RangeRequest rangeRequest = RangeRequestImpl.fromHeaders((a, b) -> getHeaderString(headers, a, b));
			read = exceptionHandler.handleSqlAndResourceDeletedException(serverBase, resourceTypeName,
					() -> dao.read(parameterConverter.toUuid(resourceTypeName, id), rangeRequest));
		}
		else
		{
			read = exceptionHandler.handleSqlAndResourceDeletedException(serverBase, resourceTypeName,
					() -> dao.read(parameterConverter.toUuid(resourceTypeName, id)));
		}

		return createReadResponse(uri, headers, read);
	}

	@Override
	public Response vread(String id, long version, UriInfo uri, HttpHeaders headers)
	{
		Optional<Binary> read;
		if (!isValidFhirRequest(uri, headers))
		{
			RangeRequest rangeRequest = RangeRequestImpl.fromHeaders((a, b) -> getHeaderString(headers, a, b));
			read = exceptionHandler.handleSqlAndResourceDeletedException(serverBase, resourceTypeName,
					() -> dao.readVersion(parameterConverter.toUuid(resourceTypeName, id), version, rangeRequest));
		}
		else
		{
			read = exceptionHandler.handleSqlAndResourceDeletedException(serverBase, resourceTypeName,
					() -> dao.readVersion(parameterConverter.toUuid(resourceTypeName, id), version));
		}

		return createReadResponse(uri, headers, read);
	}

	private boolean isValidFhirRequest(UriInfo uri, HttpHeaders headers)
	{
		// _format parameter override present and valid
		if (uri.getQueryParameters().containsKey(Constants.PARAM_FORMAT))
		{
			parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers);
			return true;
		}
		else
		{
			List<MediaType> types = headers.getAcceptableMediaTypes();
			MediaType accept = types == null ? null : types.get(0);

			// accept header is FHIR mime-type
			return Arrays.stream(FHIR_MEDIA_TYPES).anyMatch(f -> f.equals(accept.toString()));
		}
	}

	@Override
	public Response readHead(String id, UriInfo uri, HttpHeaders headers)
	{
		Optional<Binary> read = exceptionHandler.handleSqlAndResourceDeletedException(serverBase, resourceTypeName,
				() -> dao.read(parameterConverter.toUuid(resourceTypeName, id)));

		return createReadResponse(uri, headers, read);
	}

	@Override
	public Response vreadHead(String id, long version, UriInfo uri, HttpHeaders headers)
	{
		Optional<Binary> read = exceptionHandler.handleSqlAndResourceDeletedException(serverBase, resourceTypeName,
				() -> dao.readVersion(parameterConverter.toUuid(resourceTypeName, id), version));

		return createReadResponse(uri, headers, read);
	}

	@Override
	protected boolean isSpecialCase(UriInfo uri, HttpHeaders headers, Binary resource)
	{
		Optional<String> ifRange = getHeaderString(headers, RangeRequest.IF_RANGE_HEADER,
				RangeRequest.IF_RANGE_HEADER_LC);

		// not conform to rfc9110 as we are evaluating against a weak ETag here
		if (ifRange.filter(v -> v.startsWith("W/")).isPresent())
		{
			return ifRange.flatMap(parameterConverter::toEntityTag).flatMap(parameterConverter::toVersion)
					.map(v -> !v.equals(resource.getIdElement().getVersionIdPartAsLong())).orElse(false);
		}
		else if (ifRange.filter(v -> !v.startsWith("W/")).isPresent())
		{
			return ifRange.flatMap(this::toDate)
					.map(d -> !equalsWithSecondsPrecision(d, resource.getMeta().getLastUpdated())).orElse(false);
		}
		else
			return false;
	}

	@Override
	protected Response createSpecialCaseResponse(UriInfo uri, HttpHeaders headers, Binary resource)
	{
		resource.setUserData(RangeRequest.USER_DATA_VALUE_RANGE_REQUEST, null);
		return responseGenerator.response(Status.OK, resource, getMediaTypeForRead(uri, headers)).build();
	}

	@Override
	public Response update(String id, InputStream in, UriInfo uri, HttpHeaders headers)
	{
		throw new UnsupportedOperationException("Implemented and delegated by jaxrs layer");
	}

	@Override
	protected MediaType getMediaTypeForRead(UriInfo uri, HttpHeaders headers)
	{
		return parameterConverter.getMediaTypeIfSupported(uri, headers).orElseGet(() -> getMediaType(headers));
	}

	private MediaType getMediaType(HttpHeaders headers)
	{
		List<MediaType> types = headers.getAcceptableMediaTypes();
		return types == null ? null : types.get(0);
	}

	@Override
	public Response deletePermanently(String deletePath, String id, UriInfo uri, HttpHeaders headers)
	{
		Response response = super.deletePermanently(deletePath, id, uri, headers);

		dao.executeLargeObjectUnlink();

		return response;
	}
}
