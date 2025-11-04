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

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

public interface DefaultProfileProvider
{
	/**
	 * Set the default profile if non of the {@link #getSupportedDefaultProfiles(ResourceType)} are already set.<br>
	 * <br>
	 * Does nothing if the given resource is <code>null</code>
	 *
	 * @param resource
	 *            may be <code>null</code>
	 */
	void setDefaultProfile(Resource resource);

	/**
	 * @param resourceType
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if no default profile for the given <b>resourceTyp</b> or <b>resourceTyp</b>
	 *         <code>null</code>
	 */
	Optional<String> getDefaultProfile(ResourceType resourceType);

	/**
	 * @param resourceType
	 *            may be <code>null</code>
	 * @return all supported default profiles, only the default profile or default profile and secondary default
	 *         profiles, empty if <b>resourceType</b> is <code>null</code>
	 */
	List<String> getSupportedDefaultProfiles(ResourceType resourceType);

	/**
	 * @param resourceType
	 *            may be <code>null</code>
	 * @return all supported default profiles except the default profile, empty if <b>resourceType</b> is
	 *         <code>null</code>
	 */
	List<String> getSecondaryDefaultProfiles(ResourceType resourceType);
}
