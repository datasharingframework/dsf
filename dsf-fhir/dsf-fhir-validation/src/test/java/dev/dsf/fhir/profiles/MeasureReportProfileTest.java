package dev.dsf.fhir.profiles;

import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class MeasureReportProfileTest extends AbstractMetaTagProfileTest<MeasureReport>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(context,
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-measure-report-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected MeasureReport create()
	{
		MeasureReport m = new MeasureReport();
		m.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/measure-report");
		m.setStatus(MeasureReportStatus.COMPLETE);
		m.setType(MeasureReportType.SUMMARY);
		m.setMeasure("http://localhost/fhir/Measure/foo");
		m.getPeriod().setStart(new Date()).setEnd(new Date());

		return m;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}
}
