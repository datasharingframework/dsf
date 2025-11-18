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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;

import jakarta.ws.rs.core.MediaType;

class PreferReturnMinimalRetryImpl extends AbstractDsfClientJerseyWithRetry implements PreferReturnMinimal
{
	PreferReturnMinimalRetryImpl(DsfClientJersey delegate, int nTimes, DelayStrategy delayStrategy)
	{
		super(delegate, nTimes, delayStrategy);
	}

	@Override
	public IdType create(Resource resource)
	{
		return retry(() -> delegate.create(PreferReturnType.MINIMAL, Resource.class, resource).id());
	}

	@Override
	public IdType createConditionaly(Resource resource, String ifNoneExistCriteria)
	{
		return retry(() -> delegate
				.createConditionaly(PreferReturnType.MINIMAL, Resource.class, resource, ifNoneExistCriteria).id());
	}

	@Override
	public IdType createBinary(InputStream in, MediaType mediaType, String securityContextReference)
	{
		return retry(
				() -> delegate.createBinary(PreferReturnType.MINIMAL, in, mediaType, securityContextReference).id());
	}

	@Override
	public IdType update(Resource resource)
	{
		return retry(() -> delegate.update(PreferReturnType.MINIMAL, Resource.class, resource).id());
	}

	@Override
	public IdType updateConditionaly(Resource resource, Map<String, List<String>> criteria)
	{
		return retry(
				() -> delegate.updateConditionaly(PreferReturnType.MINIMAL, Resource.class, resource, criteria).id());
	}

	@Override
	public IdType updateBinary(String id, InputStream in, MediaType mediaType, String securityContextReference)
	{
		return retry(() -> delegate.updateBinary(PreferReturnType.MINIMAL, id, in, mediaType, securityContextReference)
				.id());
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(() -> delegate.postBundle(PreferReturnType.MINIMAL, bundle));
	}

	@Override
	public IdType operation(String operationName, Parameters parameters)
	{
		return retry(
				() -> delegate.operation(PreferReturnType.MINIMAL, operationName, parameters, Resource.class).id());
	}

	@Override
	public <T extends Resource> IdType operation(Class<T> resourceType, String operationName, Parameters parameters)
	{
		return retry(() -> delegate
				.operation(PreferReturnType.MINIMAL, resourceType, operationName, parameters, Resource.class).id());
	}

	@Override
	public <T extends Resource> IdType operation(Class<T> resourceType, String id, String operationName,
			Parameters parameters)
	{
		return retry(() -> delegate
				.operation(PreferReturnType.MINIMAL, resourceType, id, operationName, parameters, Resource.class).id());
	}

	@Override
	public <T extends Resource> IdType operation(Class<T> resourceType, String id, String version, String operationName,
			Parameters parameters)
	{
		return retry(() -> delegate.operation(PreferReturnType.MINIMAL, resourceType, id, version, operationName,
				parameters, Resource.class).id());
	}
}