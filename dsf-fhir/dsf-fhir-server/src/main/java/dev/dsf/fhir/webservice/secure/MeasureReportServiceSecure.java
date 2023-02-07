package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.MeasureReport;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.MeasureReportDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.webservice.specification.MeasureReportService;

public class MeasureReportServiceSecure
		extends AbstractResourceServiceSecure<MeasureReportDao, MeasureReport, MeasureReportService>
		implements MeasureReportService
{
	public MeasureReportServiceSecure(MeasureReportService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, MeasureReportDao measureReportDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<MeasureReport> authorizationRule,
			ResourceValidator resourceValidator)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				MeasureReport.class, measureReportDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator);
	}
}
