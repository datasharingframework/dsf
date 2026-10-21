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

import java.util.Objects;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.prefer.PreferReturnType;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ValidationRules;

public abstract class AbstractCommandWithResource<R extends Resource, D extends ResourceDao<R>> extends AbstractCommand
		implements Command
{
	protected final R resource;
	protected final D dao;
	protected final ExceptionHandler exceptionHandler;
	protected final ParameterConverter parameterConverter;
	protected final ValidationRules validationRules;
	protected final ReferencesHelper<R> referencesHelper;

	/**
	 * @param transactionPriority
	 * @param index
	 * @param identity
	 *            not <code>null</code>
	 * @param returnType
	 *            not <code>null</code>
	 * @param bundle
	 *            not <code>null</code>
	 * @param entry
	 *            not <code>null</code>
	 * @param serverBase
	 *            not <code>null</code>
	 * @param authorizationHelper
	 *            not <code>null</code>
	 * @param resource
	 *            not <code>null</code>
	 * @param dao
	 *            not <code>null</code>
	 * @param exceptionHandler
	 *            not <code>null</code>
	 * @param parameterConverter
	 *            not <code>null</code>
	 * @param responseGenerator
	 *            not <code>null</code>
	 * @param referenceExtractor
	 *            not <code>null</code>
	 * @param referenceResolver
	 *            not <code>null</code>
	 */
	public AbstractCommandWithResource(int transactionPriority, int index, Identity identity,
			PreferReturnType returnType, Bundle bundle, BundleEntryComponent entry, String serverBase,
			AuthorizationHelper authorizationHelper, R resource, D dao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, ValidationRules validationRules, ResponseGenerator responseGenerator,
			ReferenceExtractor referenceExtractor, ReferenceResolver referenceResolver)
	{
		super(transactionPriority, index, identity, returnType, bundle, entry, serverBase, authorizationHelper);

		this.resource = Objects.requireNonNull(resource, "resource");
		this.dao = Objects.requireNonNull(dao, "dao");
		this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		this.parameterConverter = Objects.requireNonNull(parameterConverter, "parameterConverter");
		this.validationRules = Objects.requireNonNull(validationRules, "validationRules");

		referencesHelper = new ReferencesHelperImpl<>(index, resource, serverBase, referenceExtractor,
				referenceResolver, responseGenerator);
	}

	@Override
	public String getResourceTypeName()
	{
		return resource.getResourceType().name();
	}
}
