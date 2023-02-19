package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.Questionnaire;

import dev.dsf.fhir.webservice.specification.QuestionnaireService;
import jakarta.ws.rs.Path;

@Path(QuestionnaireServiceJaxrs.PATH)
public class QuestionnaireServiceJaxrs extends AbstractResourceServiceJaxrs<Questionnaire, QuestionnaireService>
		implements QuestionnaireService
{
	public static final String PATH = "Questionnaire";

	public QuestionnaireServiceJaxrs(QuestionnaireService delegate)
	{
		super(delegate);
	}
}
