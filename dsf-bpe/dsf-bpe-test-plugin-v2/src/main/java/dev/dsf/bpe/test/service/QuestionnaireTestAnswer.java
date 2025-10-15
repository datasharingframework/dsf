package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UrlType;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.constants.CodeSystems;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class QuestionnaireTestAnswer extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		// Sleep to wait for QuestionnaireResponse to have been created with status in-progress
		Thread.sleep(Duration.ofMillis(500));

		executeTests(api, variables);
	}

	@PluginTest
	public void updateQuestionnaireResponse(ProcessPluginApi api) throws Exception
	{
		Bundle resultBundle = api.getDsfClientProvider().getLocalDsfClient().search(QuestionnaireResponse.class,
				Map.of("status", List.of(QuestionnaireResponseStatus.INPROGRESS.toCode())));

		expectNotNull(resultBundle);
		expectSame(1, resultBundle.getTotal());
		expectSame(1, resultBundle.getEntry().size());

		BundleEntryComponent e = resultBundle.getEntryFirstRep();
		expectNotNull(e);
		expectTrue(e.hasResource());
		expectTrue(e.getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse qr = (QuestionnaireResponse) e.getResource();

		expectTrue(qr.hasAuthor());
		expectTrue(qr.getAuthor().hasIdentifier());
		expectSame("http://dsf.dev/sid/organization-identifier", qr.getAuthor().getIdentifier().getSystem());
		expectSame("Test_Organization", qr.getAuthor().getIdentifier().getValue());

		expectTrue(qr.hasExtension("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization"));
		Extension authExt = qr
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");

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
			}
		});
		api.getDsfClientProvider().getLocalDsfClient().update(qr);
	}

	private void set(QuestionnaireResponseItemComponent item, Type value)
	{
		item.getAnswerFirstRep().setValue(value);
	}
}
