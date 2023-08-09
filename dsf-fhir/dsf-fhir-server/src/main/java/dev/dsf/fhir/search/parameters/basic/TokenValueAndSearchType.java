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