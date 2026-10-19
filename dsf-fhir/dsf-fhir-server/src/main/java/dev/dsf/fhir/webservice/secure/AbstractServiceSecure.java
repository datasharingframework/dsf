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

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.webservice.base.AbstractDelegatingBasicService;
import dev.dsf.fhir.webservice.base.BasicService;
import jakarta.ws.rs.core.Response;

public abstract class AbstractServiceSecure<S extends BasicService> extends AbstractDelegatingBasicService<S>
{
	protected static final Logger audit = LoggerFactory.getLogger("dsf-audit-logger");

	protected final String serverBase;
	protected final ResponseGenerator responseGenerator;
	protected final ReferenceResolver referenceResolver;

	/**
	 * @param delegate
	 *            not <code>null</code>
	 * @param serverBase
	 *            not <code>null</code>
	 * @param responseGenerator
	 *            not <code>null</code>
	 * @param referenceResolver
	 *            not <code>null</code>
	 */
	public AbstractServiceSecure(S delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver)
	{
		super(delegate);

		this.serverBase = Objects.requireNonNull(serverBase, "serverBase");
		this.referenceResolver = Objects.requireNonNull(referenceResolver, "referenceResolver");
		this.responseGenerator = Objects.requireNonNull(responseGenerator, "responseGenerator");
	}

	protected final Response forbidden(String operation)
	{
		return responseGenerator.forbiddenNotAllowed(operation, currentIdentityProvider.getCurrentIdentity());
	}
}
