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
package dev.dsf.bpe.v2.client.dsf;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

public record PreferReturn<R extends Resource>(IdType id, R resource, OperationOutcome operationOutcome)
{
	public static <R extends Resource> PreferReturn<R> minimal(String location)
	{
		return new PreferReturn<>(new IdType(location), null, null);
	}

	public static <R extends Resource> PreferReturn<R> resource(R resource)
	{
		return new PreferReturn<>(null, resource, null);
	}

	public static <R extends Resource> PreferReturn<R> outcome(OperationOutcome operationOutcome)
	{
		return new PreferReturn<>(null, null, operationOutcome);
	}
}
