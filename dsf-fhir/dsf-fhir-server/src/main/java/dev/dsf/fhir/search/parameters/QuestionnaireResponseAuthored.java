package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = QuestionnaireResponseAuthored.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/QuestionnaireRespone-authored", type = SearchParamType.DATE, documentation = "When the questionnaire response was last changed")
public class QuestionnaireResponseAuthored extends AbstractDateTimeParameter<QuestionnaireResponse>
{
	public static final String PARAMETER_NAME = "authored";

	public QuestionnaireResponseAuthored()
	{
		super(PARAMETER_NAME, "questionnaire_response->>'authored'");
	}
}
