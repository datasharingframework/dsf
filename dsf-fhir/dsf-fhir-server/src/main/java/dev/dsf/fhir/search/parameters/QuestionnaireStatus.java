package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Questionnaire;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStatusParameter;

@SearchParameterDefinition(name = AbstractStatusParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Questionnaire-status", type = SearchParamType.TOKEN, documentation = "The current status of the questionnaire")
public class QuestionnaireStatus extends AbstractStatusParameter<Questionnaire>
{
	public static final String RESOURCE_COLUMN = "questionnaire";

	public QuestionnaireStatus()
	{
		super(RESOURCE_COLUMN, Questionnaire.class);
	}
}
