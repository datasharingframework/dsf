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
package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.jdbc.LargeObjectManager;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.prefer.PreferReturnType;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.service.ResourceReference;
import dev.dsf.fhir.validation.ValidationRules;
import jakarta.ws.rs.WebApplicationException;

public class CheckReferencesCommand<R extends Resource, D extends ResourceDao<R>>
		extends AbstractCommandWithResource<R, D> implements Command
{
	private final HTTPVerb verb;
	private final ValidationRules validationRules;

	public CheckReferencesCommand(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper, R resource,
			HTTPVerb verb, D dao, ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			ResponseGenerator responseGenerator, ReferenceExtractor referenceExtractor,
			ReferenceResolver referenceResolver, ValidationRules validationRules)
	{
		super(4, index, identity, returnType, bundle, entry, serverBase, authorizationHelper, resource, dao,
				exceptionHandler, parameterConverter, responseGenerator, referenceExtractor, referenceResolver);

		this.verb = verb;
		this.validationRules = validationRules;
	}

	@Override
	public void execute(Map<String, IdType> idTranslationTable, LargeObjectManager largeObjectManager,
			Connection connection, ValidationHelper validationHelper) throws SQLException, WebApplicationException
	{
		referencesHelper.checkReferences(idTranslationTable, connection, this::checkReferenceAfterUpdate);
	}

	private boolean checkReferenceAfterUpdate(ResourceReference ref)
	{
		if (HTTPVerb.PUT.equals(verb))
		{
			// version -1 as we are checking against the pre-update resource
			return validationRules.checkReferenceAfterUpdate(resource, ref, v -> v - 1);
		}

		return true;
	}

	@Override
	public String getResourceTypeName()
	{
		throw new UnsupportedOperationException();
	}
}
