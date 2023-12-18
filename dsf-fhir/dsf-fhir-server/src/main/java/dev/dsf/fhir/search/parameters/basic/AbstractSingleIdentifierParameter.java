package dev.dsf.fhir.search.parameters.basic;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.BiPredicate;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

public class AbstractSingleIdentifierParameter<R extends Resource> extends AbstractIdentifierParameter<R>
{
	public AbstractSingleIdentifierParameter(Class<R> resourceType, String resourceColumn,
			BiPredicate<TokenValueAndSearchType, R> identifierMatches)
	{
		super(resourceType, resourceColumn, identifierMatches);
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM -> resourceColumn + "->'identifier' = ?::jsonb";
			case CODE_AND_NO_SYSTEM_PROPERTY -> resourceColumn + "->'identifier'->>'value' = ? AND NOT ("
					+ resourceColumn + "->'identifier' ?? 'system')";
		};
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM -> resourceColumn + "->'identifier' <> ?::jsonb";
			case CODE_AND_NO_SYSTEM_PROPERTY ->
				resourceColumn + "->'identifier'->>'value' <> ? OR (" + resourceColumn + "->'identifier' ?? 'system')";
		};
	}

	@Override
	public int getSqlParameterCount()
	{
		return 1;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator) throws SQLException
	{
		switch (valueAndType.type)
		{
			case CODE:
				statement.setString(parameterIndex, "{\"value\": \"" + valueAndType.codeValue + "\"}");
				return;

			case CODE_AND_SYSTEM:
				statement.setString(parameterIndex, "{\"value\": \"" + valueAndType.codeValue + "\", \"system\": \""
						+ valueAndType.systemValue + "\"}");
				return;

			case CODE_AND_NO_SYSTEM_PROPERTY:
				statement.setString(parameterIndex, valueAndType.codeValue);
				return;

			case SYSTEM:
				statement.setString(parameterIndex, "{\"system\": \"" + valueAndType.systemValue + "\"}");
				return;
		}
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(" + resourceColumn + "->'identifier'->>'system')::text || (" + resourceColumn
				+ "->'identifier'->>'value')::text" + sortDirectionWithSpacePrefix;
	}
}
