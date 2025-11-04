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
package dev.dsf.fhir.exception;

import java.util.Objects;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.parser.DataFormatException;
import dev.dsf.fhir.help.ResponseGenerator;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DataFormatExceptionHandler implements ExceptionMapper<DataFormatException>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(DataFormatExceptionHandler.class);

	private final ResponseGenerator responseGenerator;

	public DataFormatExceptionHandler(ResponseGenerator responseGenerator)
	{
		this.responseGenerator = responseGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(responseGenerator, "responseGenerator");
	}

	@Override
	public Response toResponse(DataFormatException exception)
	{
		logger.warn("Error while parsing resource: {}, returning OperationOutcome with status 403 Forbidden",
				exception.getMessage());

		OperationOutcome outcome = responseGenerator.createOutcome(IssueSeverity.ERROR, IssueType.STRUCTURE,
				"Unable to parse resource");
		return Response.status(Status.FORBIDDEN).entity(outcome).build();
	}
}
