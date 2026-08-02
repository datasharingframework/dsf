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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Before;
import org.junit.Test;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.CurrentIdentityProvider;
import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.webservice.specification.RootService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

public class RootServiceSecureTest
{
	private static final String SERVER_BASE = "https://dsf.test/fhir";

	private final RootService delegate = mock(RootService.class);

	@SuppressWarnings("unchecked")
	private final AuthorizationRule<Resource> authorizationRule = mock(AuthorizationRule.class);
	private final CurrentIdentityProvider currentIdentityProvider = mock(CurrentIdentityProvider.class);

	private RootServiceSecure rootServiceSecure;

	@Before
	public void before() throws Exception
	{
		rootServiceSecure = new RootServiceSecure(delegate, SERVER_BASE, new ResponseGenerator(SERVER_BASE),
				mock(ReferenceResolver.class), authorizationRule);
		rootServiceSecure.afterPropertiesSet();

		rootServiceSecure.setCurrentIdentityProvider(currentIdentityProvider);

		Identity identity = mock(Identity.class);
		when(identity.getName()).thenReturn("Test Identity");
		when(currentIdentityProvider.getCurrentIdentity()).thenReturn(identity);
	}

	@Test
	public void getAllowedForAll() throws Exception
	{
		when(delegate.root(any(), any())).thenReturn(mock(Response.class));

		rootServiceSecure.root(mock(UriInfo.class), mock(HttpHeaders.class));

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void handleBundleAllowedBatch()
	{
		when(delegate.handleBundle(any(), any(), any())).thenReturn(mock(Response.class));

		Bundle b = new Bundle();
		b.setType(BundleType.BATCH);

		rootServiceSecure.handleBundle(b, mock(UriInfo.class), mock(HttpHeaders.class));

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void handleBundleAllowedTransaction()
	{
		when(delegate.handleBundle(any(), any(), any())).thenReturn(mock(Response.class));

		Bundle b = new Bundle();
		b.setType(BundleType.TRANSACTION);

		rootServiceSecure.handleBundle(b, mock(UriInfo.class), mock(HttpHeaders.class));

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void expectForbiddenHandleBundleNotBatchNotTransaction()
	{
		EnumSet<BundleType> forbiddenTypes = EnumSet.complementOf(EnumSet.of(BundleType.BATCH, BundleType.TRANSACTION));
		for (BundleType type : forbiddenTypes)
		{
			Bundle b = new Bundle();
			b.setType(type);

			Response response = rootServiceSecure.handleBundle(b, mock(UriInfo.class), mock(HttpHeaders.class));
			assertEquals(Status.FORBIDDEN, response.getStatusInfo());
			assertTrue(response.hasEntity());
			assertEquals(OperationOutcome.class, response.getEntity().getClass());

		}

		verifyNoInteractions(authorizationRule);
	}

	@Test
	public void historyMustEnforceHistoryAuthorization()
	{
		when(delegate.history(any(), any())).thenReturn(mock(Response.class));

		rootServiceSecure.history(mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonHistoryAllowed(any());
	}

	@Test
	public void expectOkHistoryAllowed() throws Exception
	{
		when(authorizationRule.reasonHistoryAllowed(any())).thenReturn(Optional.of("Test Reason"));

		Response responseOk = mock(Response.class);
		when(responseOk.getStatusInfo()).thenReturn(Status.OK);
		when(responseOk.getStatus()).thenReturn(Status.OK.getStatusCode());
		when(delegate.history(any(), any())).thenReturn(responseOk);

		Response response = rootServiceSecure.history(mock(UriInfo.class), mock(HttpHeaders.class));
		assertEquals(Status.OK, response.getStatusInfo());

		verify(authorizationRule).reasonHistoryAllowed(any());
	}
}
