package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;

@SearchParameterDefinition(name = DocumentReferenceIdentifier.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/clinical-identifier", type = SearchParamType.TOKEN, documentation = "Identifies this document reference across multiple systems")
public class DocumentReferenceIdentifier extends AbstractTokenParameter<DocumentReference>
{
	public static final String PARAMETER_NAME = "identifier";

	public DocumentReferenceIdentifier()
	{
		super(DocumentReference.class, PARAMETER_NAME);
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE ->
				"(document_reference->'identifier' @> ?::jsonb OR document_reference->'masterIdentifier'->>'value' = ?)";
			case CODE_AND_SYSTEM ->
				"(document_reference->'identifier' @> ?::jsonb OR (document_reference->'masterIdentifier'->>'value' = ? AND document_reference->'masterIdentifier'->>'system' = ?))";
			case SYSTEM ->
				"(document_reference->'identifier' @> ?::jsonb OR document_reference->'masterIdentifier'->>'system' = ?)";
			case CODE_AND_NO_SYSTEM_PROPERTY -> "(SELECT count(*) FROM ("
					+ "SELECT identifier FROM jsonb_array_elements(document_reference->'identifier') AS identifier "
					+ "UNION SELECT document_reference->'masterIdentifier') AS document_reference_identifiers "
					+ "WHERE identifier->>'value' = ? AND NOT (identifier ?? 'system')" + ") > 0";
		};
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE ->
				"NOT (document_reference->'identifier' @> ?::jsonb OR document_reference->'masterIdentifier'->>'value' = ?)";
			case CODE_AND_SYSTEM ->
				"NOT (document_reference->'identifier' @> ?::jsonb OR (document_reference->'masterIdentifier'->>'value' = ? AND document_reference->'masterIdentifier'->>'system' = ?))";
			case SYSTEM ->
				"NOT (document_reference->'identifier' @> ?::jsonb OR document_reference->'masterIdentifier'->>'system' = ?)";
			case CODE_AND_NO_SYSTEM_PROPERTY -> "(SELECT count(*) FROM ("
					+ "SELECT identifier FROM jsonb_array_elements(document_reference->'identifier') AS identifier "
					+ "UNION SELECT document_reference->'masterIdentifier') AS document_reference_identifiers "
					+ "WHERE identifier->>'value' <> ? OR (identifier ?? 'system')" + ") > 0";
		};
	}

	@Override
	public int getSqlParameterCount()
	{
		return switch (valueAndType.type)
		{
			case CODE -> 2;
			case CODE_AND_SYSTEM -> 3;
			case SYSTEM -> 2;
			case CODE_AND_NO_SYSTEM_PROPERTY -> 1;
		};
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator) throws SQLException
	{
		switch (valueAndType.type)
		{
			case CODE:
				if (subqueryParameterIndex == 1)
					statement.setString(parameterIndex, "[{\"value\": \"" + valueAndType.codeValue + "\"}]");
				else if (subqueryParameterIndex == 2)
					statement.setString(parameterIndex, valueAndType.codeValue);
				return;

			case CODE_AND_SYSTEM:
				if (subqueryParameterIndex == 1)
					statement.setString(parameterIndex, "[{\"value\": \"" + valueAndType.codeValue
							+ "\", \"system\": \"" + valueAndType.systemValue + "\"}]");
				else if (subqueryParameterIndex == 2)
					statement.setString(parameterIndex, valueAndType.codeValue);
				else if (subqueryParameterIndex == 3)
					statement.setString(parameterIndex, valueAndType.systemValue);
				return;

			case SYSTEM:
				if (subqueryParameterIndex == 1)
					statement.setString(parameterIndex, "[{\"system\": \"" + valueAndType.systemValue + "\"}]");
				else if (subqueryParameterIndex == 2)
					statement.setString(parameterIndex, valueAndType.systemValue);
				return;

			case CODE_AND_NO_SYSTEM_PROPERTY:
				statement.setString(parameterIndex, valueAndType.codeValue);
				return;
		}
	}

	@Override
	protected boolean resourceMatches(DocumentReference resource)
	{
		return identifierMatches(resource) || masterIdentifierMatches(resource);
	}

	private boolean identifierMatches(DocumentReference r)
	{
		return r.hasIdentifier()
				&& r.getIdentifier().stream().anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType));
	}

	private boolean masterIdentifierMatches(DocumentReference r)
	{
		return r.hasMasterIdentifier()
				&& AbstractIdentifierParameter.identifierMatches(valueAndType, r.getMasterIdentifier());
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(SELECT string_agg((identifier->>'system')::text || (identifier->>'value')::text, ' ') "
				+ "FROM (SELECT identifier FROM jsonb_array_elements(document_reference->'identifier') identifier "
				+ "UNION SELECT document_reference->'masterIdentifier') AS document_reference_identifier)"
				+ sortDirectionWithSpacePrefix;
	}
}
