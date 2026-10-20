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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.hl7.fhir.r4.model.Binary;
import org.junit.Test;

import dev.dsf.fhir.dao.BinaryDao;
import dev.dsf.fhir.webservice.specification.BinaryService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

public class BinaryServiceSecureTest extends AbstractResourceServiceSecureTest<Binary, BinaryService, BinaryDao>
{
	public BinaryServiceSecureTest()
	{
		super(Binary.class, BinaryService.class, BinaryDao.class, Binary::new, BinaryServiceSecure::new);
	}

	@Test
	public void readHeadMustEnforceReadAuthorization()
	{
		when(delegate.readHead(anyString(), any(), any())).thenReturn(responseOkWithResourceIdVersion);

		resourceServiceSecure.readHead("some-id", mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test
	public void vreadHeadMustEnforceReadAuthorization()
	{
		when(delegate.vreadHead(anyString(), anyLong(), any(), any())).thenReturn(responseOkWithResourceIdVersion);

		resourceServiceSecure.vreadHead("some-id", 1, mock(UriInfo.class), mock(HttpHeaders.class));

		verify(authorizationRule).reasonReadAllowed(any(), any());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void expectUnsupportedOperationExceptionCreateInputStream() throws Exception
	{
		resourceServiceSecure.create(mock(InputStream.class), mock(UriInfo.class), mock(HttpHeaders.class));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void expectUnsupportedOperationExceptionUpdateInputStream() throws Exception
	{
		resourceServiceSecure.update("some-id", mock(InputStream.class), mock(UriInfo.class), mock(HttpHeaders.class));
	}
}
