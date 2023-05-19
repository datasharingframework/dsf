package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.ResearchStudy;

import dev.dsf.fhir.webservice.specification.ResearchStudyService;
import jakarta.ws.rs.Path;

@Path(ResearchStudyServiceJaxrs.PATH)
public class ResearchStudyServiceJaxrs extends AbstractResourceServiceJaxrs<ResearchStudy, ResearchStudyService>
		implements ResearchStudyService
{
	public static final String PATH = "ResearchStudy";

	public ResearchStudyServiceJaxrs(ResearchStudyService delegate)
	{
		super(delegate);
	}
}
