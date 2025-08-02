package dev.dsf.fhir.adapter;

import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ResourceLibrary extends AbstractMetdataResource<Library>
{
	private static final Logger logger = LoggerFactory.getLogger(ResourceLibrary.class);

	private static final String CONTENT_TYPE_CQL = "text/cql";
	private static final String CONTENT_TYPE_STRUCTURED_QUERY = "application/json";
	private static final List<String> CONTENT_TYPES_SUPPORTED = List.of(CONTENT_TYPE_CQL,
			CONTENT_TYPE_STRUCTURED_QUERY);

	private record Element(String subtitle, String description, List<String> type, List<ContentElement> content)
	{
	}

	private record ContentElement(String data, String contentType)
	{
		static ContentElement from(Attachment attachment, Function<String, String> prettyPrint)
		{
			String data = new String(attachment.getData(), Charset.defaultCharset());
			String pretty = prettyPrint.apply(data);
			return new ContentElement(pretty, attachment.getContentType());
		}
	}

	public ResourceLibrary()
	{
		super(Library.class);
	}

	@Override
	protected Element toElement(Library resource)
	{
		String subtitle = getString(resource, Library::hasSubtitleElement, Library::getSubtitleElement);
		String description = getString(resource, Library::hasDescriptionElement, Library::getDescriptionElement);
		List<String> type = resource.hasType() && resource.getType().hasCoding()
				? resource.getType().getCoding().stream().filter(Coding::hasSystemElement)
						.filter(Coding::hasCodeElement).filter(c -> c.getSystemElement().hasValue())
						.filter(c -> c.getCodeElement().hasValue())
						.map(c -> c.getSystemElement().getValue() + " | " + c.getCodeElement().getValue()).toList()
				: null;

		List<ContentElement> contents = resource.getContent().stream().filter(Attachment::hasData)
				.filter(a -> CONTENT_TYPES_SUPPORTED.contains(a.getContentType()))
				.map(a -> ContentElement.from(a, getPrettyPrintFunction(a.getContentType()))).toList();

		return new Element(subtitle, description, type, contents);
	}

	private Function<String, String> getPrettyPrintFunction(String contentType)
	{
		if (CONTENT_TYPE_STRUCTURED_QUERY.equals(contentType))
			return this::prettyPrintJson;

		return (input) -> input;
	}

	private String prettyPrintJson(String toFormat)
	{
		try
		{
			ObjectMapper mapper = new ObjectMapper();
			Object json = mapper.readValue(toFormat, Object.class);
			ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
			return writer.writeValueAsString(json);
		}
		catch (JsonProcessingException e)
		{
			logger.warn("Could not format JSON string, returning string unformatted");
			return toFormat;
		}
	}
}
