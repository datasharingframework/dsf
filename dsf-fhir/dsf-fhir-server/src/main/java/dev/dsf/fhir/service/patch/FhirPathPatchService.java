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
package dev.dsf.fhir.service.patch;

import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;

/**
 * Applies a <a href="https://www.hl7.org/fhir/R4/fhirpatch.html">FHIRPath Patch</a> (a {@link Parameters} resource
 * containing one or more <code>operation</code> parameters) to a FHIR resource.
 * <p>
 * Only the FHIRPath Patch format is supported (JSON Patch / XML Patch are intentionally not implemented to keep the DSF
 * dependency footprint minimal). The supported operation types are <code>add</code>, <code>insert</code>,
 * <code>delete</code>, <code>replace</code> and <code>move</code>.
 */
public interface FhirPathPatchService
{
	/**
	 * Applies the given patch to a copy of the given resource. The input resource is not modified.
	 *
	 * @param <R>
	 *            the resource type
	 * @param resource
	 *            the resource to patch, not <code>null</code>
	 * @param patch
	 *            the FHIRPath Patch as a {@link Parameters} resource, not <code>null</code>
	 * @return a patched copy of the given resource, never <code>null</code>
	 * @throws FhirPatchException
	 *             if the patch is syntactically invalid or can not be applied to the given resource
	 */
	<R extends Resource> R apply(R resource, Parameters patch);
}
