package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;
import dev.dsf.fhir.search.parameters.basic.TokenSearchType;

@SearchParameterDefinition(name = BinaryContentType.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Binary-contentType", type = SearchParamType.TOKEN, documentation = "The MIME type of the actual binary content")
public class BinaryContentType extends AbstractTokenParameter<Binary>
{
	public static final String PARAMETER_NAME = "contentType";
	public static final String RESOURCE_COLUMN = "binary_json";

	private CodeType contentType;

	public BinaryContentType()
	{
		super(PARAMETER_NAME);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (valueAndType != null && valueAndType.type == TokenSearchType.CODE)
			contentType = toContentType(valueAndType.codeValue);
	}

	private CodeType toContentType(String contentType)
	{
		if (contentType == null || contentType.isBlank())
			return null;

		return new CodeType(contentType);
	}

	@Override
	public boolean isDefined()
	{
		return super.isDefined() && contentType != null;
	}

	@Override
	public String getFilterQuery()
	{
		return RESOURCE_COLUMN + "->>'contentType' " + (valueAndType.negated ? "<>" : "=") + " ?";
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
		statement.setString(parameterIndex, contentType.getValue());
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return contentType.getValue();
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!isDefined())
			throw notDefined();

		if (!(resource instanceof Binary))
			return false;

		if (valueAndType.negated)
			return !((Binary) resource).getContentType().equals(contentType.getValue());
		else
			return ((Binary) resource).getContentType().equals(contentType.getValue());
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return RESOURCE_COLUMN + "->>'contentType'" + sortDirectionWithSpacePrefix;
	}
}
