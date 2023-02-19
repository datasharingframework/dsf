package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.QuestionnaireResponse;

import dev.dsf.fhir.webservice.specification.QuestionnaireResponseService;
import jakarta.ws.rs.Path;

@Path(QuestionnaireResponseServiceJaxrs.PATH)
public class QuestionnaireResponseServiceJaxrs
		extends AbstractResourceServiceJaxrs<QuestionnaireResponse, QuestionnaireResponseService>
		implements QuestionnaireResponseService
{
	public static final String PATH = "QuestionnaireResponse";

	public QuestionnaireResponseServiceJaxrs(QuestionnaireResponseService delegate)
	{
		super(delegate);
	}
}
