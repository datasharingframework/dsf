package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Questionnaire;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Questionnaire-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the questionnaire")
public class QuestionnaireIdentifier extends AbstractIdentifierParameter<Questionnaire>
{
	public QuestionnaireIdentifier()
	{
		super(Questionnaire.class, "questionnaire",
				listMatcher(Questionnaire::hasIdentifier, Questionnaire::getIdentifier));
	}
}
