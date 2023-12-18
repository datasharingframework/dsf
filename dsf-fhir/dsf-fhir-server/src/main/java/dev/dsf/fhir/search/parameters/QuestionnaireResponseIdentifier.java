package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;
import dev.dsf.fhir.search.parameters.basic.AbstractSingleIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/QuestionnaireResponse-identifier", type = SearchParamType.TOKEN, documentation = "The unique identifier for the questionnaire response")
public class QuestionnaireResponseIdentifier extends AbstractSingleIdentifierParameter<QuestionnaireResponse>
{
	public QuestionnaireResponseIdentifier()
	{
		super(QuestionnaireResponse.class, "questionnaire_response",
				singleMatcher(QuestionnaireResponse::hasIdentifier, QuestionnaireResponse::getIdentifier));
	}
}
