package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.IncludeParameterDefinition;
import dev.dsf.fhir.search.IncludeParts;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractCanonicalReferenceParameter;

@IncludeParameterDefinition(resourceType = QuestionnaireResponse.class, parameterName = QuestionnaireResponseQuestionnaire.PARAMETER_NAME, targetResourceTypes = Questionnaire.class)
@SearchParameterDefinition(name = QuestionnaireResponseQuestionnaire.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/QuestionnaireResponse-questionnaire", type = SearchParamType.REFERENCE, documentation = "The questionnaire the answers are provided for")
public class QuestionnaireResponseQuestionnaire extends AbstractCanonicalReferenceParameter<QuestionnaireResponse>
{
	private static final String RESOURCE_TYPE_NAME = "QuestionnaireResponse";
	public static final String PARAMETER_NAME = "questionnaire";
	private static final String TARGET_RESOURCE_TYPE_NAME = "Questionnaire";

	public static List<String> getIncludeParameterValues()
	{
		return List.of(RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME,
				RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + TARGET_RESOURCE_TYPE_NAME);
	}

	public QuestionnaireResponseQuestionnaire()
	{
		super(QuestionnaireResponse.class, PARAMETER_NAME, TARGET_RESOURCE_TYPE_NAME);
	}

	@Override
	public boolean isDefined()
	{
		return super.isDefined() && ReferenceSearchType.URL.equals(valueAndType.type);
	}

	@Override
	public String getFilterQuery()
	{
		return "(questionnaire_response->>'questionnaire' LIKE (? || '%'))";
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
		statement.setString(parameterIndex, valueAndType.url);
	}

	@Override
	protected void doResolveReferencesForMatching(QuestionnaireResponse resource, DaoProvider daoProvider)
			throws SQLException
	{
		// Nothing to do for questionnaires
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!(resource instanceof QuestionnaireResponse))
			return false;

		QuestionnaireResponse qr = (QuestionnaireResponse) resource;

		return qr.getQuestionnaire().equals(valueAndType.url);
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(SELECT string_agg(canonical, ' ') FROM questionnaire_response->'questionnaire' AS canonical)";
	}

	@Override
	protected String getIncludeSql(IncludeParts includeParts)
	{
		if (includeParts.matches(RESOURCE_TYPE_NAME, PARAMETER_NAME, TARGET_RESOURCE_TYPE_NAME))
			return "(SELECT json_agg(questionnaire) FROM current_questionnaires WHERE (questionnaire->>'url' = split_part((questionnaire_response->>'questionnaire'), '|', 1) AND questionnaire->>'version' = split_part((questionnaire_response->>'questionnaire'), '|', 2)) OR (questionnaire->>'url' = split_part((questionnaire_response->>'questionnaire'), '|', 1) AND split_part((questionnaire_response->>'questionnaire'), '|', 2) = 'null') OR (questionnaire->>'url' = questionnaire_response->>'questionnaire' AND (questionnaire->'version') is null)) AS questionnaire";
		else
			return null;
	}

	@Override
	protected void modifyIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for questionnaires
	}
}
