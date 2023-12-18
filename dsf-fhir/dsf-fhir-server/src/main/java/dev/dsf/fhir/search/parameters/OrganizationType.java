package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Organization;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;

@SearchParameterDefinition(name = OrganizationType.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Organization-type", type = SearchParamType.TOKEN, documentation = "A code for the type of organization")
public class OrganizationType extends AbstractTokenParameter<Organization>
{
	public static final String PARAMETER_NAME = "type";

	public OrganizationType()
	{
		super(Organization.class, PARAMETER_NAME);
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM ->
				"(SELECT jsonb_agg(coding) FROM jsonb_array_elements(organization->'type') AS type, jsonb_array_elements(type->'coding') AS coding) @> ?::jsonb";
			case CODE_AND_NO_SYSTEM_PROPERTY ->
				"(SELECT COUNT(*) FROM jsonb_array_elements(organization->'type') AS type, jsonb_array_elements(type->'coding') AS coding "
						+ "WHERE coding->>'code' = ? AND NOT (coding ?? 'system')) > 0";
		};
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM ->
				"NOT ((SELECT jsonb_agg(coding) FROM jsonb_array_elements(organization->'type') AS type, jsonb_array_elements(type->'coding') AS coding) @> ?::jsonb)";
			case CODE_AND_NO_SYSTEM_PROPERTY ->
				"(SELECT COUNT(*) FROM jsonb_array_elements(organization->'type') AS type, jsonb_array_elements(type->'coding') AS coding "
						+ "WHERE coding->>'code' <> ? OR (coding ?? 'system')) > 0";
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
				statement.setString(parameterIndex, "[{\"code\": \"" + valueAndType.codeValue + "\"}]");
				return;

			case CODE_AND_SYSTEM:
				statement.setString(parameterIndex, "[{\"code\": \"" + valueAndType.codeValue + "\", \"system\": \""
						+ valueAndType.systemValue + "\"}]");
				return;

			case CODE_AND_NO_SYSTEM_PROPERTY:
				statement.setString(parameterIndex, valueAndType.codeValue);
				return;

			case SYSTEM:
				statement.setString(parameterIndex, "[{\"system\": \"" + valueAndType.systemValue + "\"}]");
				return;
		}
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(SELECT string_agg((coding->>'system')::text || (coding->>'code')::text, ' ') FROM jsonb_array_elements(organization->'type'->'coding') coding)"
				+ sortDirectionWithSpacePrefix;
	}

	@Override
	protected boolean resourceMatches(Organization resource)
	{
		return resource.hasType() && codingMatches(resource.getType());
	}
}
