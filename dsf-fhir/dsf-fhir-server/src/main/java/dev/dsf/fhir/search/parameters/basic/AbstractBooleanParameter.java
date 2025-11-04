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
package dev.dsf.fhir.search.parameters.basic;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Resource;

import com.google.common.base.Objects;

import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;

public abstract class AbstractBooleanParameter<R extends Resource> extends AbstractSearchParameter<R>
{
	private final Predicate<R> hasBoolean;
	private final Function<R, Boolean> getBoolean;

	protected Boolean value;

	public AbstractBooleanParameter(Class<R> resourceType, String parameterName, Predicate<R> hasBoolean,
			Function<R, Boolean> getBoolean)
	{
		super(resourceType, parameterName);

		this.hasBoolean = hasBoolean;
		this.getBoolean = getBoolean;
	}

	@Override
	public void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		if (queryParameterValue != null && !queryParameterValue.isEmpty())
		{
			switch (queryParameterValue)
			{
				case "true":
					value = true;
					break;
				case "false":
					value = false;
					break;
				default:
					value = null;
					errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
							parameterName, queryParameterValue, "true or false expected"));
					break;
			}
		}
	}

	@Override
	public boolean isDefined()
	{
		return value != null;
	}

	@Override
	public String getBundleUriQueryParameterName()
	{
		return parameterName;
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return String.valueOf(value);
	}

	@Override
	protected boolean resourceMatches(R resource)
	{
		return hasBoolean.test(resource) && Objects.equal(getBoolean.apply(resource), value);
	}
}
