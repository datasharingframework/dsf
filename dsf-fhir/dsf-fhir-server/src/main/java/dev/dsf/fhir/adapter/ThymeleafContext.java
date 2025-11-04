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
package dev.dsf.fhir.adapter;

import java.security.Principal;
import java.util.function.BiConsumer;

import org.hl7.fhir.r4.model.Resource;

public interface ThymeleafContext
{
	Class<? extends Resource> getResourceType();

	boolean isResourceSupported(String requestPathLastElement);

	default boolean isRootSupported(Resource resource, Principal principal)
	{
		return false;
	}

	String getHtmlFragment();

	void setVariables(BiConsumer<String, Object> variables, Resource resource);
}
