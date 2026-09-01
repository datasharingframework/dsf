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

/**
 * Thrown when a FHIRPath Patch is syntactically invalid or can not be applied to the target resource. Callers are
 * expected to translate this into an HTTP <code>400 Bad Request</code> with an <code>OperationOutcome</code>.
 */
public class FhirPatchException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public FhirPatchException(String message)
	{
		super(message);
	}

	public FhirPatchException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
