package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Identifier;
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

	protected void writeDisplayRow(String text, String elementId, boolean display, OutputStreamWriter out)
			throws IOException
	{
		out.write("<div class=\"row row-display" + (display ? "" : " invisible") + "\" id=\"" + elementId
				+ "-display-row\">\n");
		out.write("<p class=\"p-display\">" + text + "</label>\n");
		out.write("</div>\n");
	}

	protected void writeInputRow(Type type, String elementId, String elementLabel, boolean display, boolean writable,
			OutputStreamWriter out) throws IOException
	{
		out.write("<div class=\"row " + (display ? "" : "invisible") + "\" id=\"" + elementId + "-input-row\">\n");
		out.write("<label class=\"row-label\" for=\"" + elementId + "\">" + elementLabel + "</label>\n");

		writeInputField(type, elementId, writable, out);

		out.write("<ul class=\"error-list-not-visible\" id=\"" + elementId + "-error\">\n");
		out.write("</ul>\n");
		out.write("</div>\n");
	}

	protected void writeInputField(Type type, String elementId, boolean writable, OutputStreamWriter out)
			throws IOException
	{
		if (type != null)
		{
			if (type instanceof StringType)
			{
				String value = ((StringType) type).getValue();
				out.write("<input type=\"text\" id=\"" + elementId + "\" name=\"" + elementId + "\" "
						+ (writable ? "placeholder=\"" + value + "\"" : "value=\"" + value + "\"") + "\"></input>\n");
			}
			else if (type instanceof IntegerType)
			{
				String value = String.valueOf(((IntegerType) type).getValue());
				out.write("<input type=\"number\" id=\"" + elementId + "\" name=\"" + elementId + "\" step=\"1\" "
						+ (writable ? "placeholder=\"" + value + "\"" : "value=\"" + value + "\"") + "></input>\n");
			}
			else if (type instanceof DecimalType)
			{
				String value = String.valueOf(((DecimalType) type).getValue());
				out.write("<input type=\"number\" id=\"" + elementId + "\" name=\"" + elementId + "\" step=\"0.01\" "
						+ (writable ? "placeholder=\"" + value + "\"" : "value=\"" + value + "\"") + "></input>\n");
			}
			else if (type instanceof BooleanType)
			{
				boolean valueIsTrue = ((BooleanType) type).getValue();

				out.write("<div>\n");
				out.write("<label class=\"radio\"><input type=\"radio\" id=\"" + elementId + "\" name=\"" + elementId
						+ "\" value=\"true\" " + ((valueIsTrue) ? "checked" : "") + "/>Yes</label>\n");
				out.write("<label class=\"radio\"><input type=\"radio\" id=\"" + elementId + "\" name=\"" + elementId
						+ "\" value=\"false\" " + ((!valueIsTrue) ? "checked" : "") + "/>No</label>\n");
				out.write("</div>\n");
			}
			else if (type instanceof DateType)
			{
				Date value = ((DateType) type).getValue();
				String date = DATE_FORMAT.format(value);

				out.write("<input type=\"date\" id=\"" + elementId + "\" name=\"" + elementId + "\" "
						+ (writable ? "placeholder=\"yyyy.MM.dd\"" : "value=\"" + date + "\"") + "></input>\n");
			}
			else if (type instanceof TimeType)
			{
				String value = ((TimeType) type).getValue();
				out.write("<input type=\"time\" id=\"" + elementId + "\" name=\"" + elementId + "\" "
						+ (writable ? "placeholder=\"hh:mm:ss\"" : "value=\"" + value + "\"") + "></input>\n");
			}
			else if (type instanceof DateTimeType)
			{
				Date value = ((DateTimeType) type).getValue();
				String dateTime = DATE_TIME_FORMAT.format(value);

				out.write("<input type=\"datetime-local\" id=\"" + elementId + "\" name=\"" + elementId + "\" "
						+ (writable ? "placeholder=\"yyyy.MM.dd hh:mm:ss\"" : "value=\"" + dateTime + "\"")
						+ "></input>\n");
			}
			else if (type instanceof UriType)
			{
				String value = ((UriType) type).getValue();
				out.write("<input type=\"url\" id=\"" + elementId + "\" name=\"" + elementId + "\" "
						+ (writable ? "placeholder=\"" + value + "\"" : "value=\"" + value + "\"") + "></input>\n");
			}
			else if (type instanceof Reference)
			{
				String value = ((Reference) type).getReference();
				out.write("<input type=\"url\" id=\"" + elementId + "\" name=\"" + elementId + "\" "
						+ (writable ? "placeholder=\"" + value + "\"" : "value=\"" + value + "\"") + "></input>\n");
			}
			else if (type instanceof Identifier)
			{
				// TODO
			}
			else if (type instanceof Coding)
			{
				// TODO
			}
			else
			{
				throw new RuntimeException("Answer type '" + type.getClass().getName()
						+ "' in QuestionnaireResponse.item is not supported");
			}
		}
	}
}
