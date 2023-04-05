package dev.dsf.fhir.resources;

import java.util.Objects;

import org.hl7.fhir.r4.model.Questionnaire;

public class QuestionnaireResource extends AbstractResource
{
	private QuestionnaireResource(String questionnaireFileName)
	{
		super(Questionnaire.class, questionnaireFileName);
	}

	public static QuestionnaireResource file(String questionnaireFileName)
	{
		return new QuestionnaireResource(Objects.requireNonNull(questionnaireFileName, "questionnaireFileName"));
	}
}
