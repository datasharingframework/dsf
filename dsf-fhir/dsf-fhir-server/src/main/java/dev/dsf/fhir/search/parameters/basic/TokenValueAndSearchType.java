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

public class TokenValueAndSearchType
{
	public static final String NOT = ":not";

	public final String systemValue;
	public final String codeValue;
	public final TokenSearchType type;
	public final boolean negated;

	private TokenValueAndSearchType(String systemValue, String codeValue, TokenSearchType type, boolean negated)
	{
		this.systemValue = systemValue;
		this.codeValue = codeValue;
		this.type = type;
		this.negated = negated;
	}

	/**
	 * @param parameterName
	 *            not <code>null</code>, not blank
	 * @param queryParameterName
	 *            not <code>null</code>, not blank
	 * @param queryParameterValue
	 *            not <code>null</code>, not blank
	 */
	public static TokenValueAndSearchType fromParamValue(String parameterName, String queryParameterName,
			String queryParameterValue)
	{
		boolean negated = (parameterName + NOT).equals(queryParameterName);

		if (queryParameterValue.indexOf('|') == -1)
			return new TokenValueAndSearchType(null, queryParameterValue, TokenSearchType.CODE, negated);
		else if (queryParameterValue.charAt(0) == '|')
			return new TokenValueAndSearchType(null, queryParameterValue.substring(1),
					TokenSearchType.CODE_AND_NO_SYSTEM_PROPERTY, negated);
		else if (queryParameterValue.charAt(queryParameterValue.length() - 1) == '|')
			return new TokenValueAndSearchType(queryParameterValue.substring(0, queryParameterValue.length() - 1), null,
					TokenSearchType.SYSTEM, negated);
		else
		{
			String[] splitAtPipe = queryParameterValue.split("[|]");
			return new TokenValueAndSearchType(splitAtPipe[0], splitAtPipe[1], TokenSearchType.CODE_AND_SYSTEM,
					negated);
		}
	}
}