package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class EndpointProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(EndpointProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-endpoint-1.0.0.xml"), List.of("dsf-read-access-tag-1.0.0.xml", "urn_ietf_bcp_13.xml"),
			List.of("dsf-read-access-tag-1.0.0.xml", "valueset-mimetypes.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testEndpointProfileValid() throws Exception
	{
		Endpoint endpoint = new Endpoint();
		endpoint.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/endpoint");
		endpoint.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		endpoint.getIdentifierFirstRep().setSystem("http://dsf.dev/sid/endpoint-identifier").setValue("fhir.test.com");
		endpoint.setStatus(EndpointStatus.ACTIVE);
		endpoint.getConnectionType().setSystem("http://terminology.hl7.org/CodeSystem/endpoint-connection-type")
				.setCode("hl7-fhir-rest");
		endpoint.getManagingOrganization().setReference("Organization/" + UUID.randomUUID().toString());
		endpoint.getPayloadTypeFirstRep().getCodingFirstRep().setSystem("http://hl7.org/fhir/resource-types")
				.setCode("Task");
		endpoint.addPayloadMimeTypeElement().setSystem("urn:ietf:bcp:13").setValue(Constants.CT_FHIR_XML_NEW);
		endpoint.addPayloadMimeTypeElement().setSystem("urn:ietf:bcp:13").setValue(Constants.CT_FHIR_JSON_NEW);
		endpoint.setAddress("https://fhir.test.com/fhir");

		ValidationResult result = resourceValidator.validate(endpoint);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}
}
