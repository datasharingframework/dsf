/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectFalse;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.constants.NamingSystems;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class QuestionnaireTestAnswerCheck extends AbstractTest implements ServiceTask
{
	private static final Logger logger = LoggerFactory.getLogger(QuestionnaireTestAnswerCheck.class);

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
		executeTests(api, variables);
	}

	@PluginTest
	public void checkQuestionnaireResponse(ProcessPluginApi api) throws Exception
	{
		Bundle resultBundle = api.getDsfClientProvider().getLocalDsfClient().search(QuestionnaireResponse.class,
				Map.of("status", List.of(QuestionnaireResponseStatus.AMENDED.toCode())));

		expectNotNull(resultBundle);
		expectSame(1, resultBundle.getTotal());
		expectSame(1, resultBundle.getEntry().size());

		BundleEntryComponent e = resultBundle.getEntryFirstRep();
		expectNotNull(e);
		expectTrue(e.hasResource());
		expectTrue(e.getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse qr = (QuestionnaireResponse) e.getResource();

		expectTrue(qr.hasAuthored());
		expectTrue(qr.hasAuthor());
		expectTrue(qr.getAuthor().hasIdentifier());

		if (type != null)
		{
			expectSame(NamingSystems.PractitionerIdentifier.SID, qr.getAuthor().getIdentifier().getSystem());
			expectSame("dic-user@test.org", qr.getAuthor().getIdentifier().getValue());
		}
		else
		{
			expectSame(NamingSystems.OrganizationIdentifier.SID, qr.getAuthor().getIdentifier().getSystem());
			expectSame("Test_Organization", qr.getAuthor().getIdentifier().getValue());
		}

		qr.getItem().forEach(item ->
		{
			switch (item.getLinkId())
			{
				case "string-example" -> test(item, new StringType("string-example answer"));

				case "text-example" -> test(item, new StringType("text-example answer"));

				case "integer-example" -> test(item, new IntegerType(666));

				case "decimal-example" -> test(item, new DecimalType(Math.PI));

				case "date-example" -> test(item, new DateType(new Date()));

				case "time-example" -> test(item, new TimeType("11:55:00"));

				case "date-time-example" -> test(item, new DateTimeType(new Date(), TemporalPrecisionEnum.MONTH));

				// TODO potential bug, QuestionnaireResponse has "url-example" item with UriType not UrlType answer
				case "url-example" -> test(item, new UriType("http://test.com/foo"));

				case "reference-example" -> test(item,
						new Reference()
								.setIdentifier(new Identifier().setSystem("http://dsf.dev/sid/organization-identifier")
										.setValue("External_Test_Organization")));

				case "boolean-example" -> test(item, new BooleanType(true));

				case "choice-example" ->
					test(item, new Coding().setSystem("http://example.org/fhir/CodeSystem/name").setCode("code"));

				case "quantity-example" -> test(item, new Quantity().setValue(0).setUnit("unit"));
			}
		});

		if (type != null)
		{
			expectFalse(read(api, qr.getIdElement(), "uac-user"));
			expectTrue(read(api, qr.getIdElement(), "dic-user"));
		}
	}

	private void test(QuestionnaireResponseItemComponent item, Type expected)
	{
		Type value = item.getAnswerFirstRep().getValue();

		expectNotNull(value);
		expectSame(expected.getClass(), value.getClass());

		switch (value)
		{
			case DateType d -> expectSame(0, DateUtils.truncatedCompareTo(((DateType) expected).getValue(),
					d.getValue(), Calendar.DAY_OF_MONTH));

			case DateTimeType d -> expectSame(0,
					DateUtils.truncatedCompareTo(((DateTimeType) expected).getValue(), d.getValue(), Calendar.MONTH));

			case PrimitiveType<?> p -> expectSame(((PrimitiveType<?>) expected).getValue(), p.getValue());

			case Reference r -> {
				expectTrue(r.hasIdentifier());
				expectNotNull(r.getIdentifier());
				expectTrue(r.getIdentifier().hasSystem());
				expectTrue(r.getIdentifier().hasValue());
				expectSame(((Reference) expected).getIdentifier().getSystem(), r.getIdentifier().getSystem());
				expectSame(((Reference) expected).getIdentifier().getValue(), r.getIdentifier().getValue());

			}

			case Coding c -> {
				expectTrue(c.hasSystem());
				expectSame(((Coding) expected).getSystem(), c.getSystem());
				expectTrue(c.hasCode());
				expectSame(((Coding) expected).getCode(), c.getCode());
			}

			case Quantity q -> {
				expectTrue(q.hasValue());
				expectSame(((Quantity) expected).getValue(), q.getValue());
				expectTrue(q.hasUnit());
				expectSame(((Quantity) expected).getUnit(), q.getUnit());
			}

			default ->
				throw new IllegalArgumentException("Value of type " + value.getClass().getName() + " not supported");
		}
	}

	private boolean read(ProcessPluginApi api, IdType id, String clientId)
	{
		Optional<IGenericClient> oClient = api.getFhirClientProvider().getClient(clientId);

		expectTrue(oClient.isPresent());

		IGenericClient client = oClient.get();

		try
		{
			client.read().resource(QuestionnaireResponse.class).withId(id).execute();
			return true;
		}
		catch (BaseServerResponseException e)
		{
			logger.info("QuestionnaireResponse read, status {}, {} : {}", e.getStatusCode(), e.getClass().getName(),
					e.getMessage());

			return false;
		}
	}
}
