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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UrlType;
import org.junit.Test;

import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.webservice.specification.StructureDefinitionService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

public class StructureDefinitionServiceSecureTest extends
		AbstractResourceServiceSecureTest<StructureDefinition, StructureDefinitionService, StructureDefinitionDao>
{
	private static final String SNAPSHOT_PATH = "$snapshot";

	public StructureDefinitionServiceSecureTest()
	{
		super(StructureDefinition.class, StructureDefinitionService.class, StructureDefinitionDao.class,
				StructureDefinition::new, StructureDefinitionServiceSecure::new);
	}

	@Test
	public void getSnapshotExistingMustEnforceReadAuthorization()
	{
		when(delegate.getSnapshotExisting(anyString(), anyString(), any(), any()))
				.thenReturn(responseOkWithResourceIdVersion);

		resourceServiceSecure.getSnapshotExisting(SNAPSHOT_PATH, "some-id", mock(UriInfo.class),
				mock(HttpHeaders.class));

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void postSnapshotExistingMustEnforceReadAuthorization()
	{
		when(delegate.postSnapshotExisting(anyString(), anyString(), any(), any()))
				.thenReturn(responseOkWithResourceIdVersion);

		resourceServiceSecure.postSnapshotExisting(SNAPSHOT_PATH, "some-id", mock(UriInfo.class),
				mock(HttpHeaders.class));

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void getSnapshotNewMustEnforceReadAuthorization()
	{
		when(delegate.getSnapshotNew(anyString(), any(), any())).thenReturn(responseOkWithResourceIdVersion);

		resourceServiceSecure.getSnapshotNew(SNAPSHOT_PATH, mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void postSnapshotNewMustEnforceReadAuthorizationIfInvokedWithUrlParameter()
	{
		Parameters parameters = mock(Parameters.class);
		when(parameters.getParameter("url")).thenReturn(new ParametersParameterComponent().setValue(new UrlType()));

		when(delegate.postSnapshotNew(anyString(), eq(parameters), any(), any()))
				.thenReturn(responseOkWithResourceIdVersion);

		resourceServiceSecure.postSnapshotNew(SNAPSHOT_PATH, parameters, mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void postSnapshotNewMustNotEnforceReadAuthorizationIfInvokedWithResourceParameter()
	{
		Parameters parameters = mock(Parameters.class);
		when(parameters.getParameter("resource"))
				.thenReturn(new ParametersParameterComponent().setResource(new StructureDefinition()));

		when(delegate.postSnapshotNew(anyString(), eq(parameters), any(), any()))
				.thenReturn(responseOkWithResourceIdVersion);

		resourceServiceSecure.postSnapshotNew(SNAPSHOT_PATH, parameters, mock(UriInfo.class), mock(HttpHeaders.class));

		verifyNoInteractions(authorizationRule);
	}
}
