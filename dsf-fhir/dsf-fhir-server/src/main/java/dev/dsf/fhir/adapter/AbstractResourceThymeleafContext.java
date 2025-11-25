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

import java.util.Objects;
import java.util.function.BiConsumer;

import org.hl7.fhir.r4.model.Resource;

abstract class AbstractResourceThymeleafContext<R extends Resource> extends AbstractThymeleafContext
{
	private final Class<R> resourceType;
	private final String htmlFragment;

	protected AbstractResourceThymeleafContext(Class<R> resourceType, String htmlFragment)
	{
		this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
		this.htmlFragment = Objects.requireNonNull(htmlFragment, "htmlFragment");
	}

	@Override
	public Class<R> getResourceType()
	{
		return resourceType;
	}

	@Override
	public String getHtmlFragment()
	{
		return htmlFragment;
	}

	@Override
	public final void setVariables(BiConsumer<String, Object> variables, Resource resource)
	{
		if (resourceType.isInstance(resource))
			doSetVariables(variables, resourceType.cast(resource));
		else
			throw new IllegalStateException("Unsupported resource of type " + resource.getClass().getName()
					+ ", expected " + resourceType.getName());
	}

	protected abstract void doSetVariables(BiConsumer<String, Object> variables, R resource);
}
