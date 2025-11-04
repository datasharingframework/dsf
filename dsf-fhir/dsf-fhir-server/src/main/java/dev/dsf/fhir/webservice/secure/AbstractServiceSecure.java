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
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.webservice.base.AbstractDelegatingBasicService;
import dev.dsf.fhir.webservice.base.BasicService;
import jakarta.ws.rs.core.Response;

public abstract class AbstractServiceSecure<S extends BasicService> extends AbstractDelegatingBasicService<S>
		implements InitializingBean
{
	protected static final Logger audit = LoggerFactory.getLogger("dsf-audit-logger");

	protected final String serverBase;
	protected final ResponseGenerator responseGenerator;
	protected final ReferenceResolver referenceResolver;

	public AbstractServiceSecure(S delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver)
	{
		super(delegate);

		this.serverBase = serverBase;
		this.referenceResolver = referenceResolver;
		this.responseGenerator = responseGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(serverBase, "serverBase");
		Objects.requireNonNull(responseGenerator, "responseGenerator");
		Objects.requireNonNull(referenceResolver, "referenceResolver");
	}

	protected final Response forbidden(String operation)
	{
		return responseGenerator.forbiddenNotAllowed(operation, currentIdentityProvider.getCurrentIdentity());
	}
}
