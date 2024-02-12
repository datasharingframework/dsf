package dev.dsf.fhir.adapter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;

public class ResourceQuestionnaireResponse extends AbstractResource<QuestionnaireResponse>
{
	private static final Logger logger = LoggerFactory.getLogger(ResourceQuestionnaireResponse.class);

	private static final String CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY = "business-key";
	private static final String CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID = "user-task-id";

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private record Element(String questionnaire, String businessKey, String userTaskId, List<Item> item)
	{
	}

	private record Item(boolean show, String id, String type, String label, String fhirType, String stringValue,
			ElementSystemValue systemValueValue, Boolean booleanValue)
	{
	}

	public ResourceQuestionnaireResponse()
	{
		super(QuestionnaireResponse.class, ActiveOrStatus.status(QuestionnaireResponse::hasStatusElement,
				QuestionnaireResponse::getStatusElement));
	}

	@Override
	protected void doSetAdditionalVariables(BiConsumer<String, Object> variables, QuestionnaireResponse resource)
	{
		variables.accept("form", true);
	}

	@Override
	protected Element toElement(QuestionnaireResponse resource)
	{
		String questionnaire = resource.hasQuestionnaireElement() && resource.getQuestionnaireElement().hasValue()
				? resource.getQuestionnaireElement().getValue().replace("|", " | ")
				: null;
		String businessKey = getStringValue(resource, CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY);
		String userTaskId = getStringValue(resource, CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID);

		List<Item> item = resource.hasItem() ? resource.getItem().stream().map(this::toItem).toList() : null;

		return new Element(questionnaire, businessKey, userTaskId, item);
	}

	private String getStringValue(QuestionnaireResponse resource, String linkId)
	{
		return resource.hasItem()
				? resource.getItem().stream().filter(QuestionnaireResponseItemComponent::hasLinkIdElement)
						.filter(i -> i.getLinkIdElement().hasValue())
						.filter(i -> linkId.equals(i.getLinkIdElement().getValue()))
						.filter(QuestionnaireResponseItemComponent::hasAnswer).filter(i -> i.getAnswer().size() == 1)
						.map(QuestionnaireResponseItemComponent::getAnswer).flatMap(List::stream)
						.filter(QuestionnaireResponseItemAnswerComponent::hasValue)
						.map(QuestionnaireResponseItemAnswerComponent::getValue).filter(v -> v instanceof StringType)
						.map(v -> (StringType) v).filter(s -> s.hasValue()).map(StringType::getValue).findFirst()
						.orElse(null)
				: null;
	}

	private Item toItem(QuestionnaireResponseItemComponent i)
	{
		boolean show = i.hasLinkIdElement() && i.getLinkIdElement().hasValue()
				&& !CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY.equals(i.getLinkIdElement().getValue())
				&& !CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID.equals(i.getLinkIdElement().getValue());
		String text = i.hasTextElement() && i.getTextElement().hasValue() ? i.getTextElement().getValue() : null;
		String linkId = i.hasLinkIdElement() && i.getLinkIdElement().hasValue() ? i.getLinkIdElement().getValue()
				: null;

		if (i.hasAnswer() && i.getAnswer().size() == 1)
			return toItem(show, linkId, text, i.getAnswerFirstRep().getValue());
		else
			return new Item(show, linkId, null, text, null, null, null, null);
	}

	private Item toItem(boolean show, String id, String label, Type typedValue)
	{
		String fhirType = typedValue.getClass().getAnnotation(DatatypeDef.class).name();

		if (typedValue instanceof BooleanType b)
			return new Item(show, id, "boolean", label, fhirType, null, null, b.hasValue() ? b.getValue() : null);
		else if (typedValue instanceof DecimalType d)
			return new Item(show, id, "number", label, fhirType, d.hasValue() ? String.valueOf(d.getValue()) : null,
					null, null);
		else if (typedValue instanceof IntegerType i)
			return new Item(show, id, "number", label, fhirType, i.hasValue() ? String.valueOf(i.getValue()) : null,
					null, null);
		else if (typedValue instanceof DateType d)
			return new Item(show, id, "date", label, fhirType, d.hasValue() ? format(d.getValue(), DATE_FORMAT) : null,
					null, null);
		else if (typedValue instanceof DateTimeType dt)
			return new Item(show, id, "datetime-local", label, fhirType,
					dt.hasValue() ? format(dt.getValue(), DATE_TIME_FORMAT) : null, null, null);
		else if (typedValue instanceof TimeType t)
			return new Item(show, id, "time", label, fhirType, t.hasValue() ? t.getValue() : null, null, null);
		else if (typedValue instanceof StringType s)
			return new Item(show, id, "text", label, fhirType, s.hasValue() ? s.getValue() : null, null, null);
		else if (typedValue instanceof UriType u)
			return new Item(show, id, "url", label, fhirType, u.hasValue() ? u.getValue() : null, null, null);
		// else if (typedValue instanceof Attachment a)
		// return TODO
		else if (typedValue instanceof Coding c)
			return new Item(show, id, "coding", label, fhirType, null, ElementSystemValue.from(c), null);
		// else if(typedValue instanceof Quantity q)
		// return TODO
		else if (typedValue instanceof Reference r)
		{
			if (r.hasReferenceElement())
				return new Item(show, id, "url", label, fhirType + ".reference",
						r.getReferenceElement().hasValue() ? r.getReferenceElement().getValue() : null, null, null);
			else if (r.hasIdentifier())
				return new Item(show, id, "identifier", label, fhirType + ".identifier", null,
						ElementSystemValue.from(r.getIdentifier()), null);
		}

		logger.warn("Element of type {}, not supported", fhirType);
		return null;
	}
}