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
package dev.dsf.fhir.authorization;

import org.hl7.fhir.r4.model.Endpoint;

public class EndpointAuthorizationRuleTest extends AbstractMetaTagAuthorizationRuleTest<Endpoint>
{
	@Override
	protected EndpointAuthorizationRule createRule()
	{
		return new EndpointAuthorizationRule(daoProvider, SERVER_BASE, referenceResolver, organizationProvider,
				readAccessHelper, parameterConverter);
	}

	@Override
	protected Class<Endpoint> expectedResourceType()
	{
		return Endpoint.class;
	}

	@Override
	protected Endpoint newResource()
	{
		Endpoint resource = new Endpoint();
		resource.setId("Endpoint/d1e2f3a4-b5c6-4d7e-8f90-123456789abc/_history/1");
		return resource;
	}
}
