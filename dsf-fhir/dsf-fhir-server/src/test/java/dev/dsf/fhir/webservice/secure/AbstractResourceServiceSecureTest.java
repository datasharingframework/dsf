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
package dev.dsf.fhir.webservice.secure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Before;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.CurrentIdentityProvider;
import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.BasicResourceService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

public abstract class AbstractResourceServiceSecureTest<R extends Resource, S extends BasicResourceService<R>, D extends ResourceDao<R>>
{
	@FunctionalInterface
	public interface ResourceServiceSecureFactory<R extends Resource, S extends BasicResourceService<R>, D extends ResourceDao<R>>
	{
		S create(S delegate, String serverBase, ResponseGenerator responseGenerator,
				ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
				ReferenceExtractor referenceExtractor, D binaryDao, ExceptionHandler exceptionHandler,
				ParameterConverter parameterConverter, AuthorizationRule<R> authorizationRule,
				ResourceValidator resourceValidator, ValidationRules validationRules,
				DefaultProfileProvider defaultProfileProvider);
	}

	protected static final String SERVER_BASE = "https://dsf.test/fhir";
	private static final String PERMANENT_DELETE_PATH = "$permanent-delete";

	protected static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

	protected final Class<R> resourceClass;
	protected final Class<S> serviceClass;
	protected final Class<D> daoClass;
	protected final Supplier<R> resourceSupplier;
	protected final ResourceServiceSecureFactory<R, S, D> resourceServiceSecureFactory;

	protected final ValidationRules validationRules = new ValidationRules(SERVER_BASE);
	protected final ResponseGenerator responseGenerator = new ResponseGenerator(SERVER_BASE);
	protected final ExceptionHandler exceptionHandler = new ExceptionHandler(responseGenerator);
	protected final ResourceValidator resourceValidator = mock(ResourceValidator.class);
	protected final ReferenceCleaner referenceCleaner = mock(ReferenceCleaner.class);
	@SuppressWarnings("unchecked")
	protected final AuthorizationRule<R> authorizationRule = mock(AuthorizationRule.class);
	protected final CurrentIdentityProvider currentIdentityProvider = mock(CurrentIdentityProvider.class);
	protected final Response responseOkWithResourceIdVersion = mock(Response.class);
	protected final Response responseForbiddenWithOperationOutcome = mock(Response.class);
	protected final Response responseNotModifiedWithResourceIdVersion = mock(Response.class);
	protected final Response responsePreconditionFailedWithResourceIdVersion = mock(Response.class);

	protected final S delegate;
	protected final D dao;

	protected S resourceServiceSecure;

	public AbstractResourceServiceSecureTest(Class<R> resourceClass, Class<S> serviceClass, Class<D> daoClass,
			Supplier<R> resourceSupplier, ResourceServiceSecureFactory<R, S, D> resourceServiceSecureFactory)
	{
		this.resourceClass = resourceClass;
		this.serviceClass = serviceClass;
		this.daoClass = daoClass;
		this.resourceSupplier = resourceSupplier;
		this.resourceServiceSecureFactory = resourceServiceSecureFactory;

		delegate = mock(serviceClass);
		dao = mock(daoClass);
	}

	@Before
	public void before() throws Exception
	{
		resourceServiceSecure = createResourceServiceSecure();
		resourceServiceSecure.setCurrentIdentityProvider(currentIdentityProvider);

		Method afterPropertiesSet = resourceServiceSecure.getClass().getMethod("afterPropertiesSet");
		afterPropertiesSet.invoke(resourceServiceSecure);

		Identity identity = mock(Identity.class);
		when(identity.getName()).thenReturn("Test Identity");
		when(currentIdentityProvider.getCurrentIdentity()).thenReturn(identity);

		when(referenceCleaner.cleanLiteralReferences(any(resourceClass)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		when(responseOkWithResourceIdVersion.getStatusInfo()).thenReturn(Status.OK);
		when(responseOkWithResourceIdVersion.getStatus()).thenReturn(Status.OK.getStatusCode());
		when(responseOkWithResourceIdVersion.hasEntity()).thenReturn(true);
		when(responseOkWithResourceIdVersion.getEntity()).thenReturn(createResourceWithIdAndVersion());

		when(responseForbiddenWithOperationOutcome.getStatusInfo()).thenReturn(Status.FORBIDDEN);
		when(responseForbiddenWithOperationOutcome.getStatus()).thenReturn(Status.FORBIDDEN.getStatusCode());
		when(responseForbiddenWithOperationOutcome.hasEntity()).thenReturn(true);
		when(responseForbiddenWithOperationOutcome.getEntity()).thenReturn(new OperationOutcome());

		when(responseNotModifiedWithResourceIdVersion.getStatusInfo()).thenReturn(Status.NOT_MODIFIED);
		when(responseNotModifiedWithResourceIdVersion.getStatus()).thenReturn(Status.NOT_MODIFIED.getStatusCode());
		when(responseNotModifiedWithResourceIdVersion.hasEntity()).thenReturn(true);
		when(responseNotModifiedWithResourceIdVersion.getEntity()).thenReturn(createResourceWithIdAndVersion());

		when(responsePreconditionFailedWithResourceIdVersion.getStatusInfo()).thenReturn(Status.PRECONDITION_FAILED);
		when(responsePreconditionFailedWithResourceIdVersion.getStatus())
				.thenReturn(Status.PRECONDITION_FAILED.getStatusCode());
		when(responsePreconditionFailedWithResourceIdVersion.hasEntity()).thenReturn(true);
		when(responsePreconditionFailedWithResourceIdVersion.getEntity()).thenReturn(createResourceWithIdAndVersion());
	}

	protected final S createResourceServiceSecure()
	{
		return resourceServiceSecureFactory.create(delegate, SERVER_BASE, responseGenerator,
				mock(ReferenceResolver.class), referenceCleaner, mock(ReferenceExtractor.class), dao, exceptionHandler,
				mock(ParameterConverter.class), authorizationRule, resourceValidator, validationRules,
				mock(DefaultProfileProvider.class));
	}

	protected final R createResource()
	{
		return resourceSupplier.get();
	}

	protected final R createResourceWithIdAndVersion()
	{
		R resource = createResource();
		resource.setIdElement(
				new IdType(resourceClass.getAnnotation(ResourceDef.class).name(), UUID.randomUUID().toString(), "1"));
		resource.getMeta().setVersionId("1");

		return resource;
	}

	@Test
	public void readMustEnforceReadAuthorization()
	{
		when(delegate.read(anyString(), any(), any())).thenReturn(responseOkWithResourceIdVersion);

		resourceServiceSecure.read("some-id", mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void vreadMustEnforceReadAuthorization()
	{
		when(delegate.vread(anyString(), anyLong(), any(), any())).thenReturn(responseOkWithResourceIdVersion);

		resourceServiceSecure.vread("some-id", 1, mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void expectForbiddenReadNotAllowed() throws Exception
	{
		when(delegate.read(anyString(), any(), any())).thenReturn(responseOkWithResourceIdVersion);
		when(authorizationRule.reasonReadAllowed(any(), any())).thenReturn(Optional.empty());

		Response response = resourceServiceSecure.read("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.FORBIDDEN, response.getStatusInfo());
	}

	@Test
	public void expectForbiddenVRreadNotAllowed() throws Exception
	{
		when(delegate.vread(anyString(), anyLong(), any(), any())).thenReturn(responseOkWithResourceIdVersion);
		when(authorizationRule.reasonReadAllowed(any(), any())).thenReturn(Optional.empty());

		Response response = resourceServiceSecure.vread("some-id", 1, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.FORBIDDEN, response.getStatusInfo());
	}

	@Test
	public void expectOkReadAllowedWithStatusOk() throws Exception
	{
		when(delegate.read(anyString(), any(), any())).thenReturn(responseOkWithResourceIdVersion);
		when(authorizationRule.reasonReadAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response response = resourceServiceSecure.read("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(resourceClass, response.getEntity().getClass());

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void expectOkVReadAllowedWithStatusOk() throws Exception
	{
		when(delegate.vread(anyString(), anyLong(), any(), any())).thenReturn(responseOkWithResourceIdVersion);
		when(authorizationRule.reasonReadAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response response = resourceServiceSecure.vread("some-id", 1, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(resourceClass, response.getEntity().getClass());

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void expectNoEntityAndStatusNotModifiedReadAllowedWithStatusNotModified() throws Exception
	{
		when(delegate.read(anyString(), any(), any())).thenReturn(responseNotModifiedWithResourceIdVersion);
		when(authorizationRule.reasonReadAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response response = resourceServiceSecure.read("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.NOT_MODIFIED, response.getStatusInfo());
		assertFalse(response.hasEntity());

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void expectNoEntityAndStatusNotModifiedVReadAllowedWithStatusNotModified() throws Exception
	{
		when(delegate.vread(anyString(), anyLong(), any(), any())).thenReturn(responseNotModifiedWithResourceIdVersion);
		when(authorizationRule.reasonReadAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response response = resourceServiceSecure.vread("some-id", 1, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.NOT_MODIFIED, response.getStatusInfo());
		assertFalse(response.hasEntity());

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void expectNoEntityAndStatusPreconditionFailedReadAllowedWithStatusPreconditionFailed() throws Exception
	{
		when(delegate.read(anyString(), any(), any())).thenReturn(responsePreconditionFailedWithResourceIdVersion);
		when(authorizationRule.reasonReadAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response response = resourceServiceSecure.read("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.PRECONDITION_FAILED, response.getStatusInfo());
		assertFalse(response.hasEntity());

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void expectNoEntityAndStatusPreconditionFailedVReadAllowedWithStatusPreconditionFailed() throws Exception
	{
		when(delegate.vread(anyString(), anyLong(), any(), any()))
				.thenReturn(responsePreconditionFailedWithResourceIdVersion);
		when(authorizationRule.reasonReadAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response response = resourceServiceSecure.vread("some-id", 1, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.PRECONDITION_FAILED, response.getStatusInfo());
		assertFalse(response.hasEntity());

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void expectOperationOutcomeReadNoAuthorizationRuleCallOperationOutcome() throws Exception
	{
		when(delegate.read(anyString(), any(), any())).thenReturn(responseForbiddenWithOperationOutcome);

		Response response = resourceServiceSecure.read("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.FORBIDDEN, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectOperationOutcomeVReadNoAuthorizationRuleCallOperationOutcome() throws Exception
	{
		when(delegate.vread(anyString(), anyLong(), any(), any())).thenReturn(responseForbiddenWithOperationOutcome);

		Response response = resourceServiceSecure.vread("some-id", 1, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.FORBIDDEN, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectForbiddenReadNoAuthorizationRuleCallUnexpectedResourceType() throws Exception
	{
		Response unexpectedResourceTpye = mock(Response.class);
		when(unexpectedResourceTpye.getStatusInfo()).thenReturn(Status.OK);
		when(unexpectedResourceTpye.getStatus()).thenReturn(Status.OK.getStatusCode());
		when(unexpectedResourceTpye.getEntity()).thenReturn("Unexpected Resource Type");
		when(unexpectedResourceTpye.hasEntity()).thenReturn(true);
		when(delegate.read(anyString(), any(), any())).thenReturn(unexpectedResourceTpye);

		Response response = resourceServiceSecure.read("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.FORBIDDEN, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectForbiddenVReadNoAuthorizationRuleCallUnexpectedResourceType() throws Exception
	{
		Response unexpectedResourceTpye = mock(Response.class);
		when(unexpectedResourceTpye.getStatusInfo()).thenReturn(Status.OK);
		when(unexpectedResourceTpye.getStatus()).thenReturn(Status.OK.getStatusCode());
		when(unexpectedResourceTpye.getEntity()).thenReturn("Unexpected Resource Type");
		when(unexpectedResourceTpye.hasEntity()).thenReturn(true);
		when(delegate.vread(anyString(), anyLong(), any(), any())).thenReturn(unexpectedResourceTpye);

		Response response = resourceServiceSecure.vread("some-id", 1, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.FORBIDDEN, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectStatusCodeReadNoAuthorizationRuleCallStatusCodeOnly() throws Exception
	{
		Response statusCodeOnly = mock(Response.class);
		when(statusCodeOnly.getStatusInfo()).thenReturn(Status.PAYMENT_REQUIRED);
		when(statusCodeOnly.getStatus()).thenReturn(Status.PAYMENT_REQUIRED.getStatusCode());

		when(delegate.read(anyString(), any(), any())).thenReturn(statusCodeOnly);

		Response response = resourceServiceSecure.read("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.PAYMENT_REQUIRED, response.getStatusInfo());
		assertFalse(response.hasEntity());

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectStatusCodeVReadNoAuthorizationRuleCallStatusCodeOnly() throws Exception
	{
		Response statusCodeOnly = mock(Response.class);
		when(statusCodeOnly.getStatusInfo()).thenReturn(Status.PAYMENT_REQUIRED);
		when(statusCodeOnly.getStatus()).thenReturn(Status.PAYMENT_REQUIRED.getStatusCode());

		when(delegate.vread(anyString(), anyLong(), any(), any())).thenReturn(statusCodeOnly);

		Response response = resourceServiceSecure.vread("some-id", 1, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.PAYMENT_REQUIRED, response.getStatusInfo());
		assertFalse(response.hasEntity());

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void createMustEnforceCreateAuthorization()
	{
		resourceServiceSecure.create(createResourceWithIdAndVersion(), mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonCreateAllowed(any(), any());
	}

	@Test
	public void expectForbiddenCreateNotAllowed() throws Exception
	{
		when(authorizationRule.reasonCreateAllowed(any(), any())).thenReturn(Optional.empty());

		Response response = resourceServiceSecure.create(createResourceWithIdAndVersion(), mock(UriInfo.class),
				mock(HttpHeaders.class));
		assertEquals(Status.FORBIDDEN, response.getStatusInfo());

		verify(authorizationRule).reasonCreateAllowed(any(), any());
	}

	@Test
	public void expectCreatedCreateAllowed() throws Exception
	{
		R resource = createResourceWithIdAndVersion();

		when(resourceValidator.validate(any())).thenReturn(new ValidationResult(FHIR_CONTEXT, List.of()));
		when(authorizationRule.reasonCreateAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response responseCreated = mock(Response.class);
		when(responseCreated.getStatusInfo()).thenReturn(Status.CREATED);
		when(responseCreated.getStatus()).thenReturn(Status.CREATED.getStatusCode());
		when(responseCreated.getEntity()).thenReturn(resource);
		when(responseCreated.hasEntity()).thenReturn(true);
		when(delegate.create(any(), any(), any())).thenReturn(responseCreated);

		Response response = resourceServiceSecure.create(resource, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.CREATED, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(resourceClass, response.getEntity().getClass());

		verify(resourceValidator).validate(any());
		verify(authorizationRule).reasonCreateAllowed(any(), any());
	}

	@Test
	public void expectForbiddenCreateAllowedNonValidResource() throws Exception
	{
		R resource = createResourceWithIdAndVersion();

		SingleValidationMessage validationMessage = new SingleValidationMessage();
		validationMessage.setSeverity(ResultSeverityEnum.ERROR);

		when(resourceValidator.validate(any()))
				.thenReturn(new ValidationResult(FHIR_CONTEXT, List.of(validationMessage)));
		when(authorizationRule.reasonCreateAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response response = resourceServiceSecure.create(resource, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.FORBIDDEN, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verify(resourceValidator).validate(any());
		verify(authorizationRule).reasonCreateAllowed(any(), any());
	}

	@Test
	public void expectOkCreateAllowedOneExists() throws Exception
	{
		R resource = createResourceWithIdAndVersion();

		when(resourceValidator.validate(any())).thenReturn(new ValidationResult(FHIR_CONTEXT, List.of()));
		when(authorizationRule.reasonCreateAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response responseOk = responseGenerator.oneExists(resource, "Test Criteria");
		when(delegate.create(any(), any(), any())).thenReturn(responseOk);

		Response response = resourceServiceSecure.create(resource, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());

		verify(resourceValidator).validate(any());
		verify(authorizationRule).reasonCreateAllowed(any(), any());
	}

	@Test
	public void expectPreconditionFailedCreateAllowedMultipleExists() throws Exception
	{
		R resource = createResourceWithIdAndVersion();

		when(resourceValidator.validate(any())).thenReturn(new ValidationResult(FHIR_CONTEXT, List.of()));
		when(authorizationRule.reasonCreateAllowed(any(), any())).thenReturn(Optional.of("Test Reason"));

		Response responsePreconditionFailed = responseGenerator
				.multipleExists(resourceClass.getAnnotation(ResourceDef.class).name(), "Test Criteria");
		when(delegate.create(any(), any(), any())).thenReturn(responsePreconditionFailed);

		Response response = resourceServiceSecure.create(resource, mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.PRECONDITION_FAILED, response.getStatusInfo());

		verify(resourceValidator).validate(any());
		verify(authorizationRule).reasonCreateAllowed(any(), any());
	}

	@Test
	public void historyBaseMustEnforceHistoryAuthorization()
	{
		when(delegate.history(any(), any())).thenReturn(mock(Response.class));

		resourceServiceSecure.history(mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonHistoryAllowed(any());
	}

	@Test
	public void historyResourceMustEnforceHistoryAuthorization()
	{
		when(delegate.history(anyString(), any(), any())).thenReturn(mock(Response.class));

		resourceServiceSecure.history("some-id", mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonHistoryAllowed(any());
	}

	@Test
	public void expectOkHistoryBaseAllowed() throws Exception
	{
		when(authorizationRule.reasonHistoryAllowed(any())).thenReturn(Optional.of("Test Reason"));

		Response responseOk = mock(Response.class);
		when(responseOk.getStatusInfo()).thenReturn(Status.OK);
		when(responseOk.getStatus()).thenReturn(Status.OK.getStatusCode());
		when(delegate.history(any(), any())).thenReturn(responseOk);

		Response response = resourceServiceSecure.history(mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());

		verify(authorizationRule).reasonHistoryAllowed(any());
	}

	@Test
	public void expectOkHistoryResourceAllowed() throws Exception
	{
		when(authorizationRule.reasonHistoryAllowed(any())).thenReturn(Optional.of("Test Reason"));

		Response responseOk = mock(Response.class);
		when(responseOk.getStatusInfo()).thenReturn(Status.OK);
		when(responseOk.getStatus()).thenReturn(Status.OK.getStatusCode());
		when(delegate.history(anyString(), any(), any())).thenReturn(responseOk);

		Response response = resourceServiceSecure.history("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());

		verify(authorizationRule).reasonHistoryAllowed(any());
	}

	@Test
	public void expectBadRequesHistoryBaseAllowedInvalidRequest() throws Exception
	{
		when(authorizationRule.reasonHistoryAllowed(any())).thenReturn(Optional.of("Test Reason"));

		Response responseOk = mock(Response.class);
		when(responseOk.getStatusInfo()).thenReturn(Status.BAD_REQUEST);
		when(responseOk.getStatus()).thenReturn(Status.BAD_REQUEST.getStatusCode());
		when(delegate.history(any(), any())).thenReturn(responseOk);

		Response response = resourceServiceSecure.history(mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.BAD_REQUEST, response.getStatusInfo());

		verify(authorizationRule).reasonHistoryAllowed(any());
	}

	@Test
	public void expectBadRequestHistoryResourceAllowedInvalidRequest() throws Exception
	{
		when(authorizationRule.reasonHistoryAllowed(any())).thenReturn(Optional.of("Test Reason"));

		Response responseBadRequest = mock(Response.class);
		when(responseBadRequest.getStatusInfo()).thenReturn(Status.BAD_REQUEST);
		when(responseBadRequest.getStatus()).thenReturn(Status.BAD_REQUEST.getStatusCode());
		when(delegate.history(anyString(), any(), any())).thenReturn(responseBadRequest);

		Response response = resourceServiceSecure.history("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.BAD_REQUEST, response.getStatusInfo());

		verify(authorizationRule).reasonHistoryAllowed(any());
	}

	@Test
	public void updateConditionalFoundResourceNoIdMustEnforceUpdateAuthorization() throws Exception
	{
		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		@SuppressWarnings("unchecked")
		PartialResult<R> partialResult = mock(PartialResult.class);
		when(partialResult.getTotal()).thenReturn(1);
		when(partialResult.getPartialResult()).thenReturn(List.of(createResourceWithIdAndVersion()));
		when(dao.search(any())).thenReturn(partialResult);
		when(delegate.update(any(), any(), any())).thenReturn(mock(Response.class));

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		resourceServiceSecure.update(createResource(), uriInfo, mock(HttpHeaders.class));

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verify(dao).search(any());
		verify(authorizationRule).reasonUpdateAllowed(any(Identity.class), any(resourceClass), any(resourceClass));
	}

	@Test
	public void updateConditionalFoundResourceSameIdMustEnforceUpdateAuthorization() throws Exception
	{
		R existingResource = createResourceWithIdAndVersion();
		R updateResource = createResource();
		updateResource.setIdElement(existingResource.getIdElement().toVersionless());

		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		@SuppressWarnings("unchecked")
		PartialResult<R> partialResult = mock(PartialResult.class);
		when(partialResult.getTotal()).thenReturn(1);
		when(partialResult.getPartialResult()).thenReturn(List.of(existingResource));
		when(dao.search(any())).thenReturn(partialResult);
		when(delegate.update(any(), any(), any())).thenReturn(mock(Response.class));

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		resourceServiceSecure.update(updateResource, uriInfo, mock(HttpHeaders.class));

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verify(dao).search(any());
		verify(authorizationRule).reasonUpdateAllowed(any(Identity.class), any(resourceClass), any(resourceClass));
	}

	@Test
	public void updateConditionalNotFoundMustEnforceCreateAuthorization() throws Exception
	{
		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		@SuppressWarnings("unchecked")
		PartialResult<R> partialResult = mock(PartialResult.class);
		when(partialResult.getTotal()).thenReturn(0);
		when(dao.search(any())).thenReturn(partialResult);
		when(delegate.update(any(), any(), any())).thenReturn(mock(Response.class));

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		resourceServiceSecure.update(createResource(), uriInfo, mock(HttpHeaders.class));

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verify(dao).search(any());
		verify(authorizationRule).reasonCreateAllowed(any(Identity.class), any(resourceClass));
	}

	@Test
	public void expectMethodNotAllowedConditionalUpdateNoAuthorizationRuleCallNoMathcingResourceInDbButResourceHasId()
			throws Exception
	{
		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		@SuppressWarnings("unchecked")
		PartialResult<R> partialResult = mock(PartialResult.class);
		when(partialResult.getTotal()).thenReturn(0);
		when(dao.search(any())).thenReturn(partialResult);
		when(delegate.update(any(), any(), any())).thenReturn(mock(Response.class));

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		Response response = resourceServiceSecure.update(createResourceWithIdAndVersion(), uriInfo,
				mock(HttpHeaders.class));
		assertEquals(Status.METHOD_NOT_ALLOWED, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verify(dao).search(any());
		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectBadRequestConditionalUpdateNoAuthorizationRuleCallFoundResourceDifferentId() throws Exception
	{
		R existingResource = createResourceWithIdAndVersion();
		R updateResource = createResourceWithIdAndVersion();

		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		@SuppressWarnings("unchecked")
		PartialResult<R> partialResult = mock(PartialResult.class);
		when(partialResult.getTotal()).thenReturn(1);
		when(partialResult.getPartialResult()).thenReturn(List.of(existingResource));
		when(dao.search(any())).thenReturn(partialResult);
		when(delegate.update(any(), any(), any())).thenReturn(mock(Response.class));

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		Response response = resourceServiceSecure.update(updateResource, uriInfo, mock(HttpHeaders.class));
		assertEquals(Status.BAD_REQUEST, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verify(dao).search(any());
		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectBadRequestConditionalUpdateNoAuthorizationRuleCallUnsupportedQueryParameter() throws Exception
	{
		R updateResource = createResourceWithIdAndVersion();

		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(searchQuery.getUnsupportedQueryParameters())
				.thenReturn(List.of(new SearchQueryParameterError(SearchQueryParameterErrorType.UNSUPPORTED_PARAMETER,
						"ParameterTestName", "Parameter Test Value")));
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		when(delegate.update(any(), any(), any())).thenReturn(mock(Response.class));

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		try
		{
			resourceServiceSecure.update(updateResource, uriInfo, mock(HttpHeaders.class));
			fail("WebApplicationException expected");
		}
		catch (WebApplicationException e)
		{
			assertEquals(Status.BAD_REQUEST, e.getResponse().getStatusInfo());
			assertTrue(e.getResponse().hasEntity());
			assertEquals(OperationOutcome.class, e.getResponse().getEntity().getClass());
		}

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verifyNoMoreInteractions(dao);
		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectPreconditionFailedConditionalUpdateNoAuthorizationRuleCallFoundResourceDifferentId()
			throws Exception
	{
		R updateResource = createResourceWithIdAndVersion();

		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		@SuppressWarnings("unchecked")
		PartialResult<R> partialResult = mock(PartialResult.class);
		when(partialResult.getTotal()).thenReturn(2);
		when(dao.search(any())).thenReturn(partialResult);
		when(delegate.update(any(), any(), any())).thenReturn(mock(Response.class));

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		Response response = resourceServiceSecure.update(updateResource, uriInfo, mock(HttpHeaders.class));
		assertEquals(Status.PRECONDITION_FAILED, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verify(dao).search(any());
		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void updateMustEnforceUpdateAuthorization() throws Exception
	{
		when(dao.read(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));
		when(delegate.update(anyString(), any(resourceClass), any(), any())).thenReturn(mock(Response.class));

		resourceServiceSecure.update("some-id", createResource(), mock(UriInfo.class), mock(HttpHeaders.class));

		verify(dao).read(any());
		verify(authorizationRule).reasonUpdateAllowed(any(Identity.class), any(resourceClass), any(resourceClass));
	}

	@Test
	public void expectMethodNotAllowedUpdateNoAuthorizationRuleCallResourceNotInDb() throws Exception
	{
		when(dao.read(any())).thenReturn(Optional.empty());

		Response response = resourceServiceSecure.update("some-id", createResource(), mock(UriInfo.class),
				mock(HttpHeaders.class));
		assertEquals(Status.METHOD_NOT_ALLOWED, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verify(dao).read(any());
		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectOkUpdateAllowed() throws Exception
	{
		when(dao.read(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));
		when(authorizationRule.reasonUpdateAllowed(any(), any(resourceClass), any(resourceClass)))
				.thenReturn(Optional.of("Test Reason"));
		when(resourceValidator.validate(any())).thenReturn(new ValidationResult(FHIR_CONTEXT, List.of()));

		when(delegate.update(anyString(), any(resourceClass), any(), any()))
				.thenReturn(responseOkWithResourceIdVersion);

		Response response = resourceServiceSecure.update("some-id", createResource(), mock(UriInfo.class),
				mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());

		verify(dao).read(any());
		verify(authorizationRule).reasonUpdateAllowed(any(Identity.class), any(resourceClass), any(resourceClass));
	}

	@Test(expected = IllegalStateException.class)
	public void expectIllegalStateExceptionUpdateSameResources() throws Exception
	{
		R resource = createResourceWithIdAndVersion();

		when(dao.read(any())).thenReturn(Optional.of(resource));

		try
		{
			resourceServiceSecure.update("some-id", resource, mock(UriInfo.class), mock(HttpHeaders.class));
		}
		finally
		{
			verify(dao).read(any());
			verifyNoInteractions(authorizationRule);
		}
	}

	@Test
	public void expectForbiddenUpdateAllowedDelegateDuplicateResource() throws Exception
	{
		when(dao.read(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));
		when(authorizationRule.reasonUpdateAllowed(any(), any(resourceClass), any(resourceClass)))
				.thenReturn(Optional.of("Test Reason"));
		when(resourceValidator.validate(any())).thenReturn(new ValidationResult(FHIR_CONTEXT, List.of()));

		when(delegate.update(anyString(), any(resourceClass), any(), any()))
				.thenThrow(new WebApplicationException(Response.status(Status.FORBIDDEN).build()));

		try
		{
			resourceServiceSecure.update("some-id", createResource(), mock(UriInfo.class), mock(HttpHeaders.class));
			fail("WebApplicationException expected");
		}
		catch (WebApplicationException e)
		{
			assertEquals(Status.FORBIDDEN, e.getResponse().getStatusInfo());
		}

		verify(dao).read(any());
		verify(authorizationRule).reasonUpdateAllowed(any(Identity.class), any(resourceClass), any(resourceClass));
	}

	@Test
	public void deleteMustEnforceDeleteAuthorization() throws Exception
	{
		when(dao.readIncludingDeleted(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));
		when(delegate.delete(anyString(), any(), any())).thenReturn(mock(Response.class));

		resourceServiceSecure.delete("some-id", mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonDeleteAllowed(any(Identity.class), any(resourceClass));
	}

	@Test
	public void expectNotFoundDeleteNoAuthorizationRuleCallResourceNotInDb() throws Exception
	{
		when(dao.readIncludingDeleted(any())).thenReturn(Optional.empty());

		Response response = resourceServiceSecure.delete("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.NOT_FOUND, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectOkDeleteAllowed() throws Exception
	{
		when(dao.readIncludingDeleted(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));
		when(authorizationRule.reasonDeleteAllowed(any(Identity.class), any(resourceClass)))
				.thenReturn(Optional.of("Test Reason"));

		Response responseOk = mock(Response.class);
		when(responseOk.getStatusInfo()).thenReturn(Status.OK);
		when(responseOk.getStatus()).thenReturn(Status.OK.getStatusCode());

		when(delegate.delete(anyString(), any(), any())).thenReturn(responseOk);

		Response response = resourceServiceSecure.delete("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());

		verify(dao).readIncludingDeleted(any());
		verify(authorizationRule).reasonDeleteAllowed(any(Identity.class), any(resourceClass));
	}

	@Test
	public void expectNotFoundDeleteAllowedNotFoundWhileDeleting() throws Exception
	{
		when(dao.readIncludingDeleted(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));
		when(authorizationRule.reasonDeleteAllowed(any(Identity.class), any(resourceClass)))
				.thenReturn(Optional.of("Test Reason"));

		when(delegate.delete(anyString(), any(), any()))
				.thenThrow(new WebApplicationException(Response.status(Status.NOT_FOUND).build()));

		try
		{
			resourceServiceSecure.delete("some-id", mock(UriInfo.class), mock(HttpHeaders.class));
			fail("WebApplicationException expected");
		}
		catch (WebApplicationException e)
		{
			assertEquals(Status.NOT_FOUND, e.getResponse().getStatusInfo());
		}

		verify(dao).readIncludingDeleted(any());
		verify(authorizationRule).reasonDeleteAllowed(any(Identity.class), any(resourceClass));
	}

	@Test
	public void deleteConditionalMustEnforceDeleteAuthorization() throws Exception
	{
		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		@SuppressWarnings("unchecked")
		PartialResult<R> partialResult = mock(PartialResult.class);
		when(partialResult.getTotal()).thenReturn(1);
		when(partialResult.getPartialResult()).thenReturn(List.of(createResourceWithIdAndVersion()));
		when(dao.search(any())).thenReturn(partialResult);
		when(dao.readIncludingDeleted(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));

		when(delegate.delete(anyString(), any(), any())).thenReturn(mock(Response.class));

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		resourceServiceSecure.delete(uriInfo, mock(HttpHeaders.class));

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verify(dao).search(any());
		verify(authorizationRule).reasonDeleteAllowed(any(Identity.class), any(resourceClass));
	}

	@Test
	public void expectBadRequestConditionalDeleteNoAuthorizationRuleCallUnsupportedQueryParameter() throws Exception
	{
		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(searchQuery.getUnsupportedQueryParameters())
				.thenReturn(List.of(new SearchQueryParameterError(SearchQueryParameterErrorType.UNSUPPORTED_PARAMETER,
						"ParameterTestName", "Parameter Test Value")));
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		Response response = resourceServiceSecure.delete(uriInfo, mock(HttpHeaders.class));
		assertEquals(Status.BAD_REQUEST, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verifyNoMoreInteractions(dao);
		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectPreconditionFailedDeleteAllowedMultipleExists() throws Exception
	{
		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		@SuppressWarnings("unchecked")
		PartialResult<R> partialResult = mock(PartialResult.class);
		when(partialResult.getTotal()).thenReturn(2);
		when(dao.search(any())).thenReturn(partialResult);

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		Response response = resourceServiceSecure.delete(uriInfo, mock(HttpHeaders.class));
		assertEquals(Status.PRECONDITION_FAILED, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verify(dao).search(any());
		verifyNoMoreInteractions(dao);
		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectNoContentDeleteAlloweNoneExists() throws Exception
	{
		@SuppressWarnings("unchecked")
		SearchQuery<R> searchQuery = mock(SearchQuery.class);
		when(dao.createSearchQueryWithoutUserFilter(eq(PageAndCount.single()))).thenReturn(searchQuery);
		@SuppressWarnings("unchecked")
		PartialResult<R> partialResult = mock(PartialResult.class);
		when(partialResult.getTotal()).thenReturn(0);
		when(dao.search(any())).thenReturn(partialResult);

		UriInfo uriInfo = mock(UriInfo.class);
		@SuppressWarnings("unchecked")
		MultivaluedMap<String, String> parameters = mock(MultivaluedMap.class);
		when(uriInfo.getQueryParameters()).thenReturn(parameters);

		Response response = resourceServiceSecure.delete(uriInfo, mock(HttpHeaders.class));
		assertEquals(Status.NO_CONTENT, response.getStatusInfo());

		verify(dao).createSearchQueryWithoutUserFilter(any());
		verify(dao).search(any());
		verifyNoMoreInteractions(dao);
		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void searchMustEnforceSearchAuthorization() throws Exception
	{
		when(delegate.search(any(), any())).thenReturn(mock(Response.class));

		resourceServiceSecure.search(mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonSearchAllowed(any(Identity.class));
	}

	@Test
	public void expectOkSearchAllowed() throws Exception
	{
		when(authorizationRule.reasonSearchAllowed(any(Identity.class))).thenReturn(Optional.of("Test Reason"));
		when(delegate.search(any(), any())).thenReturn(Response.ok().build());

		Response response = resourceServiceSecure.search(mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());

		verify(authorizationRule).reasonSearchAllowed(any(Identity.class));
	}

	@Test
	public void expectBadRequestSearchAllowedUnsupportedParameter() throws Exception
	{
		when(authorizationRule.reasonSearchAllowed(any(Identity.class))).thenReturn(Optional.of("Test Reason"));
		when(delegate.search(any(), any())).thenReturn(Response.status(Status.BAD_REQUEST).build());

		Response response = resourceServiceSecure.search(mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.BAD_REQUEST, response.getStatusInfo());

		verify(authorizationRule).reasonSearchAllowed(any(Identity.class));
	}

	@Test
	public void deletePermanentlyMustEnforcePermanentDeleteAuthorization() throws Exception
	{
		when(dao.readIncludingDeleted(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));
		when(delegate.deletePermanently(anyString(), anyString(), any(), any())).thenReturn(mock(Response.class));

		resourceServiceSecure.deletePermanently(PERMANENT_DELETE_PATH, "some-id", mock(UriInfo.class),
				mock(HttpHeaders.class));

		verify(dao).readIncludingDeleted(any());
		verify(authorizationRule).reasonPermanentDeleteAllowed(any(Identity.class), any(resourceClass));
	}

	@Test
	public void expectNotFoundDeletePermanentlyNoAuthorizationRuleCallResourceNotInDb() throws Exception
	{
		when(dao.readIncludingDeleted(any())).thenReturn(Optional.empty());

		Response response = resourceServiceSecure.deletePermanently(PERMANENT_DELETE_PATH, "some-id",
				mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.NOT_FOUND, response.getStatusInfo());
		assertTrue(response.hasEntity());
		assertEquals(OperationOutcome.class, response.getEntity().getClass());

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectOkDeletePermanentlyAllowed() throws Exception
	{
		when(dao.readIncludingDeleted(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));
		when(authorizationRule.reasonPermanentDeleteAllowed(any(Identity.class), any(resourceClass)))
				.thenReturn(Optional.of("Test Reason"));

		Response responseOk = mock(Response.class);
		when(responseOk.getStatusInfo()).thenReturn(Status.OK);
		when(responseOk.getStatus()).thenReturn(Status.OK.getStatusCode());

		when(delegate.deletePermanently(anyString(), anyString(), any(), any())).thenReturn(responseOk);

		Response response = resourceServiceSecure.deletePermanently(PERMANENT_DELETE_PATH, "some-id",
				mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());

		verify(dao).readIncludingDeleted(any());
		verify(authorizationRule).reasonPermanentDeleteAllowed(any(Identity.class), any(resourceClass));
	}


	@Test
	public void expectNotFoundDeletePermanentlyAllowedNotFoundWhileDeletingPermanently() throws Exception
	{
		when(dao.readIncludingDeleted(any())).thenReturn(Optional.of(createResourceWithIdAndVersion()));
		when(authorizationRule.reasonPermanentDeleteAllowed(any(Identity.class), any(resourceClass)))
				.thenReturn(Optional.of("Test Reason"));

		when(delegate.deletePermanently(anyString(), anyString(), any(), any()))
				.thenThrow(new WebApplicationException(Response.status(Status.NOT_FOUND).build()));

		try
		{
			resourceServiceSecure.deletePermanently(PERMANENT_DELETE_PATH, "some-id", mock(UriInfo.class),
					mock(HttpHeaders.class));
			fail("WebApplicationException expected");
		}
		catch (WebApplicationException e)
		{
			assertEquals(Status.NOT_FOUND, e.getResponse().getStatusInfo());
		}

		verify(dao).readIncludingDeleted(any());
		verify(authorizationRule).reasonPermanentDeleteAllowed(any(Identity.class), any(resourceClass));
	}
}
