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

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameterError;

public abstract class AbstractCanonicalUrlParameter<R extends Resource> extends AbstractSearchParameter<R>
{
	protected enum UriSearchType
	{
		PRECISE(""), BELOW(":below"); // TODO, ABOVE(":above");

		public final String modifier;

		UriSearchType(String modifier)
		{
			this.modifier = modifier;
		}
	}

	public static List<String> getNameModifiers()
	{
		return List.of(UriSearchType.BELOW.modifier);
	}

	protected static class CanonicalUrlAndSearchType
	{
		public final String url;
		public final String version;
		public final UriSearchType type;

		private CanonicalUrlAndSearchType(String url, String version, UriSearchType type)
		{
			this.url = url;
			this.version = version;
			this.type = type;
		}
	}

	protected CanonicalUrlAndSearchType valueAndType;

	public AbstractCanonicalUrlParameter(Class<R> resourceType, String parameterName)
	{
		super(resourceType, parameterName);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		if ((parameterName + UriSearchType.PRECISE.modifier).equals(queryParameterName))
			valueAndType = toValueAndType(queryParameterValue, UriSearchType.PRECISE);
		else if ((parameterName + UriSearchType.BELOW.modifier).equals(queryParameterName))
			valueAndType = toValueAndType(queryParameterValue, UriSearchType.BELOW);
		// TODO
		// else if ((parameterName + UriSearchType.ABOVE.modifier).equals(queryParameterName))
		// valueAndType = toValueAndType(queryParameterValue, UriSearchType.ABOVE);
	}

	private CanonicalUrlAndSearchType toValueAndType(String value, UriSearchType type)
	{
		if (value != null && !value.isBlank())
		{
			String[] split = value.split("[|]");
			if (split.length == 1)
				return new CanonicalUrlAndSearchType(split[0], null, type);
			else if (split.length == 2)
				return new CanonicalUrlAndSearchType(split[0], split[1], type);
		}

		return null;
	}

	@Override
	public boolean isDefined()
	{
		return valueAndType != null;
	}

	protected boolean hasVersion()
	{
		return isDefined() && valueAndType.version != null;
	}

	@Override
	public String getBundleUriQueryParameterName()
	{
		return parameterName + valueAndType.type.modifier;
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return valueAndType.url + (hasVersion() ? "|" + valueAndType.version : "");
	}
}
