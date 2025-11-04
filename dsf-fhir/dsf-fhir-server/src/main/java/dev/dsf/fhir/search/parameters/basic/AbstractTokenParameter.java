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
import java.util.Objects;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameterError;

public abstract class AbstractTokenParameter<R extends Resource> extends AbstractSearchParameter<R>
{
	public static List<String> getNameModifiers()
	{
		return List.of(TokenValueAndSearchType.NOT);
	}

	protected TokenValueAndSearchType valueAndType;

	public AbstractTokenParameter(Class<R> resourceType, String parameterName)
	{
		super(resourceType, parameterName);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		valueAndType = TokenValueAndSearchType.fromParamValue(parameterName, queryParameterName, queryParameterValue);
	}

	@Override
	public boolean isDefined()
	{
		return valueAndType != null;
	}

	@Override
	public final String getFilterQuery()
	{
		return valueAndType.negated ? getNegatedFilterQuery() : getPositiveFilterQuery();
	}

	protected abstract String getNegatedFilterQuery();

	protected abstract String getPositiveFilterQuery();

	@Override
	public String getBundleUriQueryParameterName()
	{
		return valueAndType.negated ? parameterName + TokenValueAndSearchType.NOT : parameterName;
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return switch (valueAndType.type)
		{
			case CODE -> valueAndType.codeValue;
			case CODE_AND_SYSTEM -> valueAndType.systemValue + "|" + valueAndType.codeValue;
			case CODE_AND_NO_SYSTEM_PROPERTY -> "|" + valueAndType.codeValue;
			case SYSTEM -> valueAndType.systemValue + "|";
		};
	}

	protected boolean codingMatches(List<CodeableConcept> codes)
	{
		return codes.stream().filter(CodeableConcept::hasCoding).map(CodeableConcept::getCoding).flatMap(List::stream)
				.filter(Coding::hasCode).anyMatch(codingMatches(valueAndType));
	}

	private Predicate<Coding> codingMatches(TokenValueAndSearchType valueAndType)
	{
		return coding -> valueAndType.negated ^ switch (valueAndType.type)
		{
			case CODE -> coding.hasCode() && Objects.equals(valueAndType.codeValue, coding.getCode());

			case CODE_AND_SYSTEM -> coding.hasCode() && Objects.equals(valueAndType.codeValue, coding.getCode())
					&& coding.hasSystem() && Objects.equals(valueAndType.systemValue, coding.getSystem());

			case CODE_AND_NO_SYSTEM_PROPERTY ->
				coding.hasCode() && Objects.equals(valueAndType.codeValue, coding.getCode()) && !coding.hasSystem();

			case SYSTEM -> coding.hasSystem() && Objects.equals(valueAndType.systemValue, coding.getSystem());

			default -> false;
		};
	}
}
