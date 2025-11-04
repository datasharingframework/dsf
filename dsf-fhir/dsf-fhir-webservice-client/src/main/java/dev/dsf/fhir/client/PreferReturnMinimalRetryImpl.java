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
package dev.dsf.fhir.client;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.prefer.PreferReturnType;
import jakarta.ws.rs.core.MediaType;

class PreferReturnMinimalRetryImpl extends AbstractFhirWebserviceClientJerseyWithRetry implements PreferReturnMinimal
{
	PreferReturnMinimalRetryImpl(FhirWebserviceClientJersey delegate, int nTimes, Duration delay)
	{
		super(delegate, nTimes, delay);
	}

	@Override
	public IdType create(Resource resource)
	{
		return retry(() -> delegate.create(PreferReturnType.MINIMAL, resource).getId());
	}

	@Override
	public IdType createConditionaly(Resource resource, String ifNoneExistCriteria)
	{
		return retry(
				() -> delegate.createConditionaly(PreferReturnType.MINIMAL, resource, ifNoneExistCriteria).getId());
	}

	@Override
	public IdType createBinary(InputStream in, MediaType mediaType, String securityContextReference)
	{
		return retry(
				() -> delegate.createBinary(PreferReturnType.MINIMAL, in, mediaType, securityContextReference).getId());
	}

	@Override
	public IdType update(Resource resource)
	{
		return retry(() -> delegate.update(PreferReturnType.MINIMAL, resource).getId());
	}

	@Override
	public IdType updateConditionaly(Resource resource, Map<String, List<String>> criteria)
	{
		return retry(() -> delegate.updateConditionaly(PreferReturnType.MINIMAL, resource, criteria).getId());
	}

	@Override
	public IdType updateBinary(String id, InputStream in, MediaType mediaType, String securityContextReference)
	{
		return retry(() -> delegate.updateBinary(PreferReturnType.MINIMAL, id, in, mediaType, securityContextReference)
				.getId());
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(() -> delegate.postBundle(PreferReturnType.MINIMAL, bundle));
	}
}