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
package dev.dsf.common.auth.logging;

import java.io.IOException;
import java.security.Principal;

import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;

@ConstrainedTo(RuntimeType.SERVER)
@PreMatching
public abstract class AbstractUserLogger implements ContainerRequestFilter, ContainerResponseFilter
{
	@Override
	public final void filter(ContainerRequestContext requestContext) throws IOException
	{
		Principal principal = requestContext.getSecurityContext().getUserPrincipal();

		before(principal);

		if (principal instanceof OrganizationIdentity organization)
			before(organization);
		else if (principal instanceof PractitionerIdentity practitioner)
			before(practitioner);
	}

	protected abstract void before(OrganizationIdentity organization);

	protected abstract void before(PractitionerIdentity practitioner);

	protected abstract void before(Principal userPrincipal);

	@Override
	public final void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException
	{
		after();
	}

	protected void after()
	{
	}
}
