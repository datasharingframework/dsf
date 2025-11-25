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
package dev.dsf.fhir.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public interface ReferenceCleaner
{
	/**
	 * Removes literal references, if a conditional reference is also set
	 *
	 * @param <R>
	 *            the resource type
	 * @param resource
	 *            the resource to clean, may be <code>null</code>
	 * @return null if given resource is null, cleaned up resource (same instance)
	 */
	<R extends Resource> R cleanLiteralReferences(R resource);

	/**
	 * Removes embedded resources from references within {@link Bundle} entries
	 *
	 * @param <R>
	 *            the resource type
	 * @param resource
	 *            the resource to clean, may be <code>null</code>
	 * @return null if given resource is null, cleaned up resource (same instance)
	 */
	<R extends Resource> R cleanReferenceResourcesIfBundle(R resource);
}
