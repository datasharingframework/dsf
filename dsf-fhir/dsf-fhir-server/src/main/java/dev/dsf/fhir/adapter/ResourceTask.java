package dev.dsf.fhir.adapter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.annotation.DatatypeDef;

public class ResourceTask extends AbstractResource<Task>
{
	private static final Logger logger = LoggerFactory.getLogger(ResourceTask.class);

	private static final String CODESYSTEM_DSF_BPMN_MESSAGE = "http://dsf.dev/fhir/CodeSystem/bpmn-message";
	private static final String CODESYSTEM_DSF_BPMN_MESSAGE_VALUE_MESSAGE_NAME = "message-name";
	private static final String CODESYSTEM_DSF_BPMN_MESSAGE_VALUE_BUSINESS_KEY = "business-key";
	private static final String CODESYSTEM_DSF_BPMN_MESSAGE_VALUE_CORRELATION_KEY = "correlation-key";

	private static final List<String> FILTERD_INPUTS = List.of(CODESYSTEM_DSF_BPMN_MESSAGE_VALUE_MESSAGE_NAME,
			CODESYSTEM_DSF_BPMN_MESSAGE_VALUE_BUSINESS_KEY, CODESYSTEM_DSF_BPMN_MESSAGE_VALUE_CORRELATION_KEY);

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private record Element(String process, String messageName, String businessKey, String correlationKey,
			ElementSystemValue requester, ElementSystemValue recipient, String authoredOn, List<InputItem> input,
			List<OutputItem> output)
	{
	}

	private record InputItem(String id, String type, String label, String labelTitle, String fhirType,
			String stringValue, ElementSystemValue systemValueValue, Boolean booleanValue)
	{
	}

	private record OutputItem(String id, String type, String label, String labelTitle, String stringValue,
			ElementSystemValue systemValueValue, Boolean booleanValue, List<ExtensionItem> extension)
	{
	}

	private record ExtensionItem(String id, String type, String url, String stringValue,
			ElementSystemValue systemValueValue, Boolean booleanValue)
	{
	}

	public ResourceTask()
	{
		super(Task.class, ActiveOrStatus.status(Task::hasStatusElement, Task::getStatusElement));
	}

	@Override
	protected Element toElement(Task resource)
	{
		String process = resource.hasInstantiatesCanonicalElement()
				&& resource.getInstantiatesCanonicalElement().hasValue()
						? resource.getInstantiatesCanonicalElement().getValue().replace("|", " | ")
						: null;
		String messageName = getFirstInputParameter(resource, CODESYSTEM_DSF_BPMN_MESSAGE,
				CODESYSTEM_DSF_BPMN_MESSAGE_VALUE_MESSAGE_NAME);
		String businessKey = getFirstInputParameter(resource, CODESYSTEM_DSF_BPMN_MESSAGE,
				CODESYSTEM_DSF_BPMN_MESSAGE_VALUE_BUSINESS_KEY);
		String correlationKey = getFirstInputParameter(resource, CODESYSTEM_DSF_BPMN_MESSAGE,
				CODESYSTEM_DSF_BPMN_MESSAGE_VALUE_CORRELATION_KEY);
		ElementSystemValue requester = resource.hasRequester() && resource.getRequester().hasIdentifier()
				? ElementSystemValue.from(resource.getRequester().getIdentifier())
				: null;
		ElementSystemValue recipient = resource.hasRestriction() && resource.getRestriction().hasRecipient()
				&& resource.getRestriction().getRecipient().size() == 1
				&& resource.getRestriction().getRecipientFirstRep().hasIdentifier()
						? ElementSystemValue.from(resource.getRestriction().getRecipientFirstRep().getIdentifier())
						: null;
		String authoredOn = resource.hasAuthoredOnElement() && resource.getAuthoredOnElement().hasValue()
				? formatDateTime(resource.getAuthoredOnElement().getValue())
				: null;

		Map<String, Integer> idCounter = new HashMap<>();
		List<InputItem> input = resource.hasInput()
				? resource.getInput().stream().filter(ParameterComponent::hasType).filter(i -> i.getType().hasCoding())
						.filter(i -> i.getType().getCoding().stream().filter(Coding::hasSystemElement)
								.filter(Coding::hasCodeElement).filter(c -> c.getSystemElement().hasValue())
								.filter(c -> c.getCodeElement().hasValue())
								.anyMatch(c -> !CODESYSTEM_DSF_BPMN_MESSAGE.equals(c.getSystemElement().getValue())
										|| !(CODESYSTEM_DSF_BPMN_MESSAGE.equals(c.getSystemElement().getValue())
												&& FILTERD_INPUTS.contains(c.getCodeElement().getValue()))))
						.map(toInputItem(idCounter)).filter(i -> i != null).toList()
				: List.of();

		List<OutputItem> output = resource.hasOutput()
				? resource.getOutput().stream().filter(TaskOutputComponent::hasType)
						.filter(i -> i.getType().hasCoding())
						.filter(i -> i.getType().getCoding().stream().filter(Coding::hasSystemElement)
								.filter(Coding::hasCodeElement)
								.anyMatch(c -> c.getSystemElement().hasValue() && c.getCodeElement().hasValue()))
						.map(this::toOutputItem).filter(i -> i != null).toList()
				: List.of();

		return new Element(process, messageName, businessKey, correlationKey, requester, recipient, authoredOn, input,
				output);
	}

	private String getFirstInputParameter(Task task, String system, String code)
	{
		return task.getInput().stream().filter(ParameterComponent::hasType).filter(c -> c.getType().hasCoding())
				.filter(c -> c.getType().getCoding().stream()
						.anyMatch(co -> Objects.equals(system, co.getSystem()) && Objects.equals(code, co.getCode())))
				.filter(ParameterComponent::hasValue).map(ParameterComponent::getValue)
				.filter(v -> v instanceof StringType).map(v -> (StringType) v).map(StringType::getValue).findFirst()
				.orElse(null);
	}

	private Function<ParameterComponent, InputItem> toInputItem(Map<String, Integer> idCounter)
	{
		return i ->
		{
			String id = i.hasType() && i.getType().hasCoding() ? i.getType().getCoding().stream()
					.filter(Coding::hasSystemElement).filter(c -> c.getSystemElement().hasValue())
					.filter(Coding::hasCodeElement).filter(c -> c.getCodeElement().hasValue()).findFirst()
					.map(c -> c.getSystemElement().getValue() + "|" + c.getCodeElement().getValue()).orElse(null)
					: null;
			String labelTitle = id;

			if (idCounter.containsKey(id))
			{
				int count = idCounter.get(id) + 1;
				idCounter.put(id, count);
				id = id + '|' + count;
			}
			else
				idCounter.put(id, 0);

			String label = i.hasType() && i.getType().hasCoding() ? i.getType().getCoding().stream()
					.filter(Coding::hasSystemElement).filter(c -> c.getSystemElement().hasValue())
					.filter(Coding::hasCodeElement).filter(c -> c.getCodeElement().hasValue()).findFirst()
					.map(c -> c.getCodeElement().getValue()).orElse(null) : null;

			return toItem(id, label, labelTitle, i.getValue());
		};
	}

	private InputItem toItem(String id, String label, String labelTitle, Type typedValue)
	{
		String type = getHtmlInputType(typedValue);
		String fhirType = getFhirType(typedValue);
		String stringValue = getStringValue(typedValue);
		ElementSystemValue systemValueValue = getSystemValueValue(typedValue);
		Boolean booleanValue = getBooleanValue(typedValue);

		if (stringValue == null && systemValueValue == null && booleanValue == null)
			logger.warn("Output parameter with {} value, not supported", fhirType);

		return new InputItem(id, type, label, labelTitle, fhirType, stringValue, systemValueValue, booleanValue);
	}

	private OutputItem toOutputItem(TaskOutputComponent o)
	{
		String labelTitle = o.hasType() && o.getType().hasCoding()
				? o.getType().getCoding().stream().filter(Coding::hasSystemElement)
						.filter(c -> c.getSystemElement().hasValue()).filter(Coding::hasCodeElement)
						.filter(c -> c.getCodeElement().hasValue()).findFirst()
						.map(c -> c.getSystemElement().getValue() + "|" + c.getCodeElement().getValue()).orElse(null)
				: null;
		String label = o.hasType() && o.getType().hasCoding() ? o.getType().getCoding().stream()
				.filter(Coding::hasSystemElement).filter(c -> c.getSystemElement().hasValue())
				.filter(Coding::hasCodeElement).filter(c -> c.getCodeElement().hasValue()).findFirst()
				.map(c -> c.getCodeElement().getValue()).orElse(null) : null;

		List<ExtensionItem> extension = o.hasExtension() ? toExtensionItems(o.getExtension()) : List.of();

		return toOutputItem(label, labelTitle, o.getValue(), extension);
	}

	private OutputItem toOutputItem(String label, String labelTitle, Type typedValue, List<ExtensionItem> extension)
	{
		String type = getHtmlInputType(typedValue);
		String stringValue = getStringValue(typedValue);
		ElementSystemValue systemValueValue = getSystemValueValue(typedValue);
		Boolean booleanValue = getBooleanValue(typedValue);

		if (stringValue == null && systemValueValue == null && booleanValue == null)
			logger.warn("Output parameter with {} value, not supported",
					typedValue.getClass().getAnnotation(DatatypeDef.class).name());

		return new OutputItem(UUID.randomUUID().toString(), type, label, labelTitle, stringValue, systemValueValue,
				booleanValue, extension);
	}

	private String getHtmlInputType(Type typedValue)
	{
		if (typedValue instanceof BooleanType)
			return "boolean";
		else if (typedValue instanceof DecimalType)
			return "number";
		else if (typedValue instanceof IntegerType)
			return "number";
		else if (typedValue instanceof DateType)
			return "date";
		else if (typedValue instanceof DateTimeType)
			return "datetime-local";
		else if (typedValue instanceof TimeType)
			return "time";
		else if (typedValue instanceof InstantType)
			return "datetime-local";
		else if (typedValue instanceof StringType)
			return "text";
		else if (typedValue instanceof UriType)
			return "url";
		else if (typedValue instanceof Coding)
			return "coding";
		else if (typedValue instanceof Identifier)
			return "identifier";
		else if (typedValue instanceof Reference r && r.hasReferenceElement())
			return "url";
		else if (typedValue instanceof Reference r && r.hasIdentifier())
			return "identifier";
		else
			return null;
	}

	private String getFhirType(Type typedValue)
	{
		String type = typedValue.getClass().getAnnotation(DatatypeDef.class).name();

		if (typedValue instanceof Reference r && r.hasReferenceElement())
			return type + ".reference";
		else if (typedValue instanceof Reference r && r.hasIdentifier())
			return type + ".identifier";
		else
			return type;
	}

	private String getStringValue(Type typedValue)
	{
		if (typedValue instanceof DecimalType d)
			return d.hasValue() ? String.valueOf(d.getValue()) : null;
		else if (typedValue instanceof IntegerType i)
			return i.hasValue() ? String.valueOf(i.getValue()) : null;
		else if (typedValue instanceof DateType d)
			return d.hasValue() ? format(d.getValue(), DATE_FORMAT) : null;
		else if (typedValue instanceof DateTimeType dt)
			// TODO format datetime based on precision
			return dt.hasValue() ? format(dt.getValue(), DATE_TIME_FORMAT) : null;
		else if (typedValue instanceof TimeType t)
			return t.hasValue() ? t.getValue() : null;
		else if (typedValue instanceof InstantType i)
			return i.hasValue() ? format(i.getValue(), DATE_TIME_FORMAT) : null;
		else if (typedValue instanceof StringType s)
			return s.hasValue() ? s.getValue() : null;
		else if (typedValue instanceof UriType u)
			return u.hasValue() ? u.getValue() : null;
		else if (typedValue instanceof Reference r && r.hasReferenceElement())
			return r.getReferenceElement().hasValue() ? r.getReferenceElement().getValue() : null;
		else
			return null;
	}

	private ElementSystemValue getSystemValueValue(Type typedValue)
	{
		if (typedValue instanceof Coding c)
			return ElementSystemValue.from(c);
		else if (typedValue instanceof Identifier i)
			return ElementSystemValue.from(i);
		else if (typedValue instanceof Reference r && r.hasIdentifier())
			return ElementSystemValue.from(r.getIdentifier());
		else
			return null;
	}

	private Boolean getBooleanValue(Type typedValue)
	{
		if (typedValue instanceof BooleanType b)
			return b.hasValue() ? b.getValue() : null;
		else
			return null;
	}

	private List<ExtensionItem> toExtensionItems(List<Extension> extensions)
	{
		List<ExtensionItem> items = new ArrayList<>();
		extensions.forEach(e -> addExtensionItem(null, e, items));
		return items;
	}

	private void addExtensionItem(String baseUrl, Extension extension, List<ExtensionItem> items)
	{
		String url = Stream.of(baseUrl, getUri(extension, Extension::hasUrlElement, Extension::getUrlElement))
				.filter(s -> s != null).collect(Collectors.joining(" | "));

		String type = extension.hasValue() ? getHtmlInputType(extension.getValue()) : null;
		String stringValue = extension.hasValue() ? getStringValue(extension.getValue()) : null;
		ElementSystemValue systemValueValue = extension.hasValue() ? getSystemValueValue(extension.getValue()) : null;
		Boolean booleanValue = extension.hasValue() ? getBooleanValue(extension.getValue()) : null;

		if (stringValue != null || systemValueValue != null || booleanValue != null)
			items.add(new ExtensionItem(UUID.randomUUID().toString(), type, url, stringValue, systemValueValue,
					booleanValue));

		if (extension.hasExtension())
			extension.getExtension().forEach(e -> addExtensionItem(url, e, items));
	}
}