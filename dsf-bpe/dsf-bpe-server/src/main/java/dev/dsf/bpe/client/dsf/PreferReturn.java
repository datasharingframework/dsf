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
package dev.dsf.bpe.client.dsf;

import java.net.URI;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

public class PreferReturn
{
	private final IdType id;
	private final Resource resource;
	private final OperationOutcome operationOutcome;

	private PreferReturn(IdType id, Resource resource, OperationOutcome operationOutcome)
	{
		this.id = id;
		this.resource = resource;
		this.operationOutcome = operationOutcome;
	}

	public static PreferReturn minimal(URI location)
	{
		return new PreferReturn(new IdType(location.toString()), null, null);
	}

	public static PreferReturn resource(Resource resource)
	{
		return new PreferReturn(null, resource, null);
	}

	public static PreferReturn outcome(OperationOutcome operationOutcome)
	{
		return new PreferReturn(null, null, operationOutcome);
	}

	public IdType getId()
	{
		return id;
	}

	public Resource getResource()
	{
		return resource;
	}

	public OperationOutcome getOperationOutcome()
	{
		return operationOutcome;
	}
}
