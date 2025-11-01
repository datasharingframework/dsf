package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectFalse;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UrlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.constants.CodeSystems;
import dev.dsf.bpe.v2.constants.NamingSystems;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class QuestionnaireTestAnswer extends AbstractTest implements ServiceTask
{
	private static final Logger logger = LoggerFactory.getLogger(QuestionnaireTestAnswer.class);

	private String type;

	/**
	 * @param type
	 * @deprecated only for BPMN field injection
	 */
	@Deprecated
	public void setType(String type)
	{
		this.type = type;
	}

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables, TO_ERROR_BOUNDARY_EVENT);
	}

	@PluginTest
	public void updateQuestionnaireResponse(ProcessPluginApi api) throws Exception
	{
		Bundle resultBundle = null;
		for (int i = 0; i < 5; i++)
		{
			// Sleep to wait for QuestionnaireResponse to have been created with status in-progress
			logger.info("sleeping ...");
			Thread.sleep(Duration.ofMillis(250));

			logger.info("searching ...");
			resultBundle = api.getDsfClientProvider().getLocalDsfClient().search(QuestionnaireResponse.class,
					Map.of("status", List.of(QuestionnaireResponseStatus.INPROGRESS.toCode())));

			if (resultBundle != null && resultBundle.getTotal() == 1)
				break;
		}

		expectNotNull(resultBundle);
		expectSame(1, resultBundle.getTotal());
		expectSame(1, resultBundle.getEntry().size());

		BundleEntryComponent entry = resultBundle.getEntryFirstRep();
		expectNotNull(entry);
		expectTrue(entry.hasResource());
		expectTrue(entry.getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse qr = (QuestionnaireResponse) entry.getResource();

		expectTrue(qr.hasAuthor());
		expectTrue(qr.getAuthor().hasIdentifier());
		expectSame("http://dsf.dev/sid/organization-identifier", qr.getAuthor().getIdentifier().getSystem());
		expectSame("Test_Organization", qr.getAuthor().getIdentifier().getValue());

		if (type != null)
		{
			expectTrue(
					qr.hasExtension("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization"));
			Extension authExt = qr
					.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");

			if ("role".equals(type))
			{
				expectTrue(authExt.hasExtension("practitioner-role"));
				List<Extension> roleExts = authExt.getExtensionsByUrl("practitioner-role");
				expectNotNull(roleExts);
				expectSame(1, roleExts.size());

				Extension roleExt = roleExts.get(0);
				expectTrue(roleExt.hasValue());
				expectSame(Coding.class, roleExt.getValue().getClass());

				Coding role = (Coding) roleExt.getValue();
				expectSame(CodeSystems.PractitionerRole.SYSTEM, role.getSystem());
				expectSame(CodeSystems.PractitionerRole.Codes.DIC_USER, role.getCode());
			}
			else if ("identifier".equals(type))
			{
				expectTrue(authExt.hasExtension("practitioner"));
				List<Extension> idExts = authExt.getExtensionsByUrl("practitioner");
				expectNotNull(idExts);
				expectSame(1, idExts.size());

				Extension idExt = idExts.get(0);
				expectTrue(idExt.hasValue());
				expectSame(Identifier.class, idExt.getValue().getClass());

				Identifier id = (Identifier) idExt.getValue();
				expectSame(NamingSystems.PractitionerIdentifier.SID, id.getSystem());
				expectSame("dic-user@test.org", id.getValue());
			}
			else if ("identifiers".equals(type))
			{
				expectTrue(authExt.hasExtension("practitioner"));
				List<Extension> idExts = authExt.getExtensionsByUrl("practitioner");
				expectNotNull(idExts);
				expectSame(3, idExts.size());

				idExts.stream().filter(Extension::hasValue).filter(e -> e.getValue() instanceof Identifier)
						.map(e -> (Identifier) e.getValue()).map(Identifier::getSystem)
						.allMatch(NamingSystems.PractitionerIdentifier.SID::equals);

				List<String> values = idExts.stream().filter(Extension::hasValue)
						.filter(e -> e.getValue() instanceof Identifier).map(e -> (Identifier) e.getValue())
						.map(Identifier::getValue).toList();
				expectSame(3, values.size());

				expectTrue(values.contains("dic-user@test.org"));
				expectTrue(values.contains("foo@invalid"));
				expectTrue(values.contains("bar@invalid"));
			}
		}
		else
			expectFalse(
					qr.hasExtension("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization"));

		qr.setAuthored(new Date());
		qr.setStatus(QuestionnaireResponseStatus.COMPLETED);
		qr.getItem().forEach(item ->
		{
			switch (item.getLinkId())
			{
				case "string-example" -> set(item, new StringType("string-example answer"));

				case "text-example" -> set(item, new StringType("text-example answer"));

				case "integer-example" -> set(item, new IntegerType(666));

				case "decimal-example" -> set(item, new DecimalType(Math.PI));

				case "date-example" -> set(item, new DateType(new Date()));

				case "time-example" -> set(item, new TimeType("11:55:00"));

				case "date-time-example" -> set(item, new DateTimeType(new Date(), TemporalPrecisionEnum.MONTH));

				case "url-example" -> set(item, new UrlType("http://test.com/foo"));

				case "reference-example" -> set(item,
						new Reference()
								.setIdentifier(new Identifier().setSystem("http://dsf.dev/sid/organization-identifier")
										.setValue("External_Test_Organization")));

				case "boolean-example" -> set(item, new BooleanType(true));

				case "choice-example" ->
					set(item, new Coding().setSystem("http://example.org/fhir/CodeSystem/name").setCode("code"));

				case "quantity-example" -> set(item, new Quantity().setValue(0).setUnit("unit"));
			}
		});

		if (type != null)
		{
			expectFalse(update(api, qr, "uac-user", "uac-user@test.org"));
			expectTrue(update(api, qr, "dic-user", "dic-user@test.org"));
		}
		else
			api.getDsfClientProvider().getLocalDsfClient().update(qr);
	}

	private void set(QuestionnaireResponseItemComponent item, Type value)
	{
		item.getAnswerFirstRep().setValue(value);
	}

	private boolean update(ProcessPluginApi api, QuestionnaireResponse qr, String clientId, String identifierValue)
	{
		qr.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name())
				.setIdentifier(NamingSystems.PractitionerIdentifier.withValue(identifierValue));

		Optional<IGenericClient> oClient = api.getFhirClientProvider().getClient(clientId);

		expectTrue(oClient.isPresent());

		IGenericClient client = oClient.get();
		try
		{

			MethodOutcome outcome = client.update().resource(qr).execute();
			expectNotNull(outcome);

			return outcome.getResponseStatusCode() == 200;
		}
		catch (BaseServerResponseException e)
		{
			logger.info("QuestionnaireResponse update, status {}, {} : {}", e.getStatusCode(), e.getClass().getName(),
					e.getMessage());

			return false;
		}
	}
}
