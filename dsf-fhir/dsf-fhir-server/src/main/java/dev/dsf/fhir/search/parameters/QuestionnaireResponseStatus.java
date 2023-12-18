package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;
import dev.dsf.fhir.search.parameters.basic.TokenSearchType;

@SearchParameterDefinition(name = QuestionnaireResponseStatus.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/QuestionnaireResponse-status", type = SearchParamType.TOKEN, documentation = "The status of the questionnaire response")
public class QuestionnaireResponseStatus extends AbstractTokenParameter<QuestionnaireResponse>
{
	public static final String PARAMETER_NAME = "status";

	private QuestionnaireResponse.QuestionnaireResponseStatus status;

	public QuestionnaireResponseStatus()
	{
		super(QuestionnaireResponse.class, PARAMETER_NAME);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (valueAndType != null && valueAndType.type == TokenSearchType.CODE)
			status = toStatus(errors, valueAndType.codeValue, queryParameterValue);
	}

	private QuestionnaireResponse.QuestionnaireResponseStatus toStatus(List<? super SearchQueryParameterError> errors,
			String status, String queryParameterValue)
	{
		if (status == null || status.isBlank())
			return null;

		try
		{
			return QuestionnaireResponse.QuestionnaireResponseStatus.fromCode(status);
		}
		catch (FHIRException e)
		{
			errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE, parameterName,
					queryParameterValue, e));
			return null;
		}
	}

	@Override
	public boolean isDefined()
	{
		return super.isDefined() && status != null;
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return "questionnaire_response->>'status' = ?";
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return "questionnaire_response->>'status' <> ?";
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
		statement.setString(parameterIndex, status.toCode());
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return status.toCode();
	}

	@Override
	protected boolean resourceMatches(QuestionnaireResponse resource)
	{
		return valueAndType.negated ^ (resource.hasStatus() && Objects.equals(resource.getStatus(), status));
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "questionnaire_response->>'status'" + sortDirectionWithSpacePrefix;
	}
}
