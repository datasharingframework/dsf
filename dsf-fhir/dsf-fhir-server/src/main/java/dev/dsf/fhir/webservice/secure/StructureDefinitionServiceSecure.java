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

import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StructureDefinition;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.StructureDefinitionService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class StructureDefinitionServiceSecure
		extends AbstractResourceServiceSecure<StructureDefinitionDao, StructureDefinition, StructureDefinitionService>
		implements StructureDefinitionService
{
	public StructureDefinitionServiceSecure(StructureDefinitionService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, StructureDefinitionDao structureDefinitionDao,
			ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			AuthorizationRule<StructureDefinition> authorizationRule, ResourceValidator resourceValidator,
			ValidationRules validationRules, DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				StructureDefinition.class, structureDefinitionDao, exceptionHandler, parameterConverter,
				authorizationRule, resourceValidator, validationRules, defaultProfileProvider);
	}

	@Override
	public Response postSnapshotNew(String snapshotPath, Parameters parameters, UriInfo uri, HttpHeaders headers)
	{
		return delegate.postSnapshotNew(snapshotPath, parameters, uri, headers);
	}

	@Override
	public Response getSnapshotNew(String snapshotPath, UriInfo uri, HttpHeaders headers)
	{
		return delegate.getSnapshotNew(snapshotPath, uri, headers);
	}

	@Override
	public Response postSnapshotExisting(String snapshotPath, String id, UriInfo uri, HttpHeaders headers)
	{
		return delegate.postSnapshotExisting(snapshotPath, id, uri, headers);
	}

	@Override
	public Response getSnapshotExisting(String snapshotPath, String id, UriInfo uri, HttpHeaders headers)
	{
		return delegate.getSnapshotExisting(snapshotPath, id, uri, headers);
	}
}
