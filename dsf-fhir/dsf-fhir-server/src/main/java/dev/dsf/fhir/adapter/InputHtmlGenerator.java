package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;

public abstract class InputHtmlGenerator
{
	protected static final String ELEMENT_TYPE_ROW = "row";
	protected static final String ELEMENT_TYPE_LINK = "link";
	protected static final String ELEMENT_TYPE_PATH = "path";
	protected static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	protected static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	protected static final SimpleDateFormat DATE_TIME_DISPLAY_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	protected void writeDisplayRow(String text, String elementName, boolean display, OutputStreamWriter out)
			throws IOException
	{
		out.write("<div class=\"row row-display" + (display ? "" : " invisible") + "\" name=\"" + elementName
				+ "-display-row\">\n");
		out.write("<p class=\"p-display\">" + text + "</label>\n");
		out.write("</div>\n");
	}

	protected void writeInputRow(Type type, List<Extension> extensions, String elementName,
			Map<String, Integer> elemenIndexMap, String elementLabel, boolean display, boolean writable,
			OutputStreamWriter out) throws IOException
	{
		int elementIndex = getElementIndex(elementName, elemenIndexMap);

		out.write("<div class=\"row" + (display ? "" : " invisible") + "\" name=\"" + elementName
				+ "-input-row\" index=\"" + elementIndex + "\">\n");

		writeInputLabel(elementLabel, elementIndex, () -> "", out);
		writeInputField(type, elementName, elementIndex, writable, out);
		writeInputExtensionFields(extensions, elementName, elementIndex, writable, 0, out);

		out.write("<ul class=\"error-list-not-visible\" name=\"" + elementName + "-error\" index=\"" + elementIndex
				+ "\">\n");
		out.write("</ul>\n");
		out.write("</div>\n");
	}

	protected void writeInputLabel(String elementLabel, int elementIndex, Supplier<String> additionalClasses,
			OutputStreamWriter out) throws IOException
	{

		out.write("<label class=\"row-label " + additionalClasses.get() + "\" index=\"" + elementIndex + "\">"
				+ elementLabel + "</label>\n");
	}

	protected void writeInputExtensionFields(List<Extension> extensions, String elementName, int elementIndex,
			boolean writable, int depth, OutputStreamWriter out) throws IOException
	{
		for (Extension extension : extensions)
		{
			String extensionelementName = elementName + "-" + extension.getUrl();
			out.write("<div class=\"" + (depth == 0 ? "row-extension-0" : "row-extension") + "\" name=\""
					+ extensionelementName + "-extension-row\" index=\"" + elementIndex + "\">\n");

			String extensionElementLabel = "Extension: " + extension.getUrl();
			if (extension.hasValue())
			{
				writeInputLabel(extensionElementLabel, elementIndex, () -> "", out);
				writeInputField(extension.getValue(), extensionelementName, elementIndex, writable, out);
			}
			else
			{
				writeInputLabel(extensionElementLabel, elementIndex, () -> "row-label-extension-no-value", out);
			}

			if (extension.hasExtension())
			{
				writeInputExtensionFields(extension.getExtension(), extensionelementName, elementIndex, writable,
						++depth, out);
			}
			out.write("</div>\n");
		}
	}

	protected void writeInputField(Type type, String elementName, int elementIndex, boolean writable,
			OutputStreamWriter out) throws IOException
	{
		if (type != null)
		{
			if (type instanceof StringType)
			{
				String value = ((StringType) type).getValue();
				writeInputFieldValueInput("text", value, elementName, elementIndex, writable, out);
			}
			else if (type instanceof IntegerType)
			{
				String value = String.valueOf(((IntegerType) type).getValue());
				writeInputFieldValueInput("number", value, elementName, elementIndex, writable, out);
			}
			else if (type instanceof DecimalType)
			{
				String value = String.valueOf(((DecimalType) type).getValue());
				writeInputFieldValueInput("number", value, elementName, elementIndex, writable, out);
			}
			else if (type instanceof DateType)
			{
				Date value = ((DateType) type).getValue();
				String date = DATE_FORMAT.format(value);
				writeInputFieldValueInput("date", date, elementName, elementIndex, writable, out);
			}
			else if (type instanceof TimeType)
			{
				String value = ((TimeType) type).getValue();
				writeInputFieldValueInput("time", value, elementName, elementIndex, writable, out);
			}
			else if (type instanceof DateTimeType)
			{
				Date value = ((DateTimeType) type).getValue();
				String dateTime = DATE_TIME_FORMAT.format(value);
				writeInputFieldValueInput("datetime-local", dateTime, elementName, elementIndex, writable, out);
			}
			else if (type instanceof InstantType)
			{
				Date value = ((InstantType) type).getValue();
				String dateTime = DATE_TIME_FORMAT.format(value);
				writeInputFieldValueInput("datetime-local", dateTime, elementName, elementIndex, writable, out);
			}
			else if (type instanceof UriType)
			{
				String value = ((UriType) type).getValue();
				writeInputFieldValueInput("url", value, elementName, elementIndex, writable, out);
			}
			else if (type instanceof Reference reference)
			{
				if (reference.hasReference())
				{
					String value = reference.getReference();
					writeInputFieldValueInput("url", value, elementName, elementIndex, writable, out);
				}
				else if (reference.hasIdentifier())
				{
					Identifier identifier = reference.getIdentifier();
					writeInputFieldSystemCodeInput(identifier.getSystem(), identifier.getValue(), elementName,
							elementIndex, writable, out);
				}
			}
			else if (type instanceof Identifier identifier)
			{
				writeInputFieldSystemCodeInput(identifier.getSystem(), identifier.getValue(), elementName, elementIndex,
						writable, out);
			}
			else if (type instanceof Coding coding)
			{
				writeInputFieldSystemCodeInput(coding.getSystem(), coding.getCode(), elementName, elementIndex,
						writable, out);
			}
			else if (type instanceof BooleanType)
			{
				boolean valueIsTrue = ((BooleanType) type).getValue();

				out.write("<div class=\"input-group\">\n");
				out.write("<label class=\"radio\"><input type=\"radio\" name=\"" + elementName + "\" index=\""
						+ elementIndex + "\" value=\"true\"" + ((valueIsTrue && !writable) ? " checked" : "")
						+ "/>Yes</label>\n");
				out.write("<label class=\"radio\"><input type=\"radio\" name=\"" + elementName + "\" " + "\" index=\""
						+ elementIndex + "\" value=\"false\"" + ((!valueIsTrue && !writable) ? " checked" : "")
						+ "/>No</label>\n");
				out.write("</div>\n");
			}
			else
			{
				throw new RuntimeException("Answer type '" + type.getClass().getName()
						+ "' in QuestionnaireResponse.item is not supported");
			}
		}
	}

	private void writeInputFieldValueInput(String type, String value, String elementName, int elementIndex,
			boolean writable, OutputStreamWriter out) throws IOException
	{
		out.write("<div class=\"input-group\">\n");
		writeInput(type, value, elementName, elementIndex, Optional.empty(), writable, out);
		writePlaceholderButton(elementName, value, writable, out);
		out.write("</div>\n");
	}

	private void writeInputFieldSystemCodeInput(String system, String code, String elementName, int elementIndex,
			boolean writable, OutputStreamWriter out) throws IOException
	{
		out.write("<div class=\"input-group\">\n");
		writeInput("url", system, elementName + "-system", elementIndex, Optional.empty(), writable, out);
		writePlaceholderButton(elementName + "-system", system, writable, out);
		out.write("</div>\n");

		out.write("<div class=\"input-group\">\n");
		writeInput("text", code, elementName + "-code", elementIndex, Optional.of("identifier-coding-code"), writable,
				out);
		writePlaceholderButton(elementName + "-code", code, writable, out);
		out.write("</div>\n");
	}

	private void writeInput(String type, String value, String elementName, int elementIndex, Optional<String> classes,
			boolean writable, OutputStreamWriter out) throws IOException
	{
		out.write("<input type=\"" + type + "\"" + (classes.map(c -> " class=\"" + c + "\"").orElse("")) + " name=\""
				+ elementName + "\" index=\"" + elementIndex + "\" "
				+ (writable ? "placeholder=\"" + value + "\"" : "value=\"" + value + "\"") + "></input>\n");
	}

	private void writePlaceholderButton(String elementName, String value, boolean writable, OutputStreamWriter out)
			throws IOException
	{
		if (writable)
		{
			out.write("<svg class=\"input-group-svg\" height=\"22\" width=\"22\" viewBox=\"0 -960 960 960\" "
					+ "onclick=\"insertPlaceholderInValue(this.parentElement, '" + elementName + "', '" + value
					+ "')\">\n");
			out.write("<title>Use placeholder value</title>\n");
			out.write(
					"<path d=\"M140-160q-24 0-42-18t-18-42v-169h60v169h680v-520H140v171H80v-171q0-24 18-42t42-18h680q24 0 42 18t18 42v520q0 24-18 42t-42 18H140Zm319-143-43-43 103-103H80v-60h439L416-612l43-43 176 176-176 176Z\"/>\n");
			out.write("</svg>\n");
		}
	}

	private int getElementIndex(String elementName, Map<String, Integer> elementIndexMap)
	{
		if (elementIndexMap.containsKey(elementName))
		{
			int index = elementIndexMap.get(elementName) + 1;
			elementIndexMap.put(elementName, index);
			return index;
		}
		else
		{
			elementIndexMap.put(elementName, 0);
			return 0;
		}
	}
}
