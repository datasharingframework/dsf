package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Questionnaire;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = QuestionnaireDate.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Questionnaire-date", type = SearchParamType.DATE, documentation = "The questionnaire publication date")
public class QuestionnaireDate extends AbstractDateTimeParameter<Questionnaire>
{
	public static final String PARAMETER_NAME = "date";

	public QuestionnaireDate()
	{
		super(Questionnaire.class, PARAMETER_NAME, "questionnaire->>'date'",
				fromDateTime(Questionnaire::hasDateElement, Questionnaire::getDateElement));
	}
}
