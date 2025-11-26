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

import java.sql.Connection;
import java.util.Optional;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

public interface ReferenceResolver
{
	/**
	 * @param reference
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @return <code>true</code> if the {@link ResourceReference} can be resolved
	 */
	boolean referenceCanBeResolved(ResourceReference reference, Connection connection);

	/**
	 * @param reference
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if the reference could not be resolved
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#LITERAL_INTERNAL},
	 *             {@link ResourceReference.ReferenceType#LITERAL_EXTERNAL},
	 *             {@link ResourceReference.ReferenceType#CONDITIONAL} or
	 *             {@link ResourceReference.ReferenceType#LOGICAL}
	 */
	Optional<Resource> resolveReference(ResourceReference reference, Connection connection);

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @param resourceReference
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if the reference could be resolved
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#LITERAL_INTERNAL}
	 * @see ResourceReference#getType(String)
	 */
	Optional<OperationOutcome> checkLiteralInternalReference(Resource resource, ResourceReference resourceReference,
			Connection connection) throws IllegalArgumentException;

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @param resourceReference
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @param bundleIndex
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if the reference could be resolved
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#LITERAL_INTERNAL}
	 * @see ResourceReference#getType(String)
	 */
	Optional<OperationOutcome> checkLiteralInternalReference(Resource resource, ResourceReference resourceReference,
			Connection connection, Integer bundleIndex) throws IllegalArgumentException;

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @param resourceReference
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if the reference check was @Override successful
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#LITERAL_EXTERNAL}
	 * @see ResourceReference#getType(String)
	 */
	Optional<OperationOutcome> checkLiteralExternalReference(Resource resource, ResourceReference resourceReference)
			throws IllegalArgumentException;

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @param resourceReference
	 *            not <code>null</code>
	 * @param bundleIndex
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if the reference check was successful
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#LITERAL_EXTERNAL}
	 * @see ResourceReference#getType(String)
	 */
	Optional<OperationOutcome> checkLiteralExternalReference(Resource resource, ResourceReference resourceReference,
			Integer bundleIndex) throws IllegalArgumentException;

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @param resourceReference
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @param bundleIndex
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if the reference check was successful
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#CONDITIONAL}
	 * @see ResourceReference#getType(String)
	 */
	Optional<OperationOutcome> checkConditionalReference(Resource resource, ResourceReference resourceReference,
			Connection connection, Integer bundleIndex) throws IllegalArgumentException;

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @param resourceReference
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if the reference check was successful
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#LOGICAL}
	 * @see ResourceReference#getType(String)
	 */
	Optional<OperationOutcome> checkLogicalReference(Resource resource, ResourceReference resourceReference,
			Connection connection) throws IllegalArgumentException;

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @param resourceReference
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @param bundleIndex
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if the reference check was successful
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#LOGICAL}
	 * @see ResourceReference#getType(String)
	 */
	Optional<OperationOutcome> checkLogicalReference(Resource resource, ResourceReference resourceReference,
			Connection connection, Integer bundleIndex) throws IllegalArgumentException;

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @param reference
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if the reference check was successful
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#CANONICAL}
	 * @see ResourceReference#getType(String)
	 */
	Optional<OperationOutcome> checkCanonicalReference(Resource resource, ResourceReference reference,
			Connection connection) throws IllegalArgumentException;

	/**
	 * @param resource
	 *            not <code>null</code>
	 * @param reference
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 * @param bundleIndex
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if the reference check was successful
	 * @throws IllegalArgumentException
	 *             if the reference is not of type {@link ResourceReference.ReferenceType#CANONICAL}
	 * @see ResourceReference#getType(String)
	 */
	Optional<OperationOutcome> checkCanonicalReference(Resource resource, ResourceReference reference,
			Connection connection, Integer bundleIndex) throws IllegalArgumentException;
}
