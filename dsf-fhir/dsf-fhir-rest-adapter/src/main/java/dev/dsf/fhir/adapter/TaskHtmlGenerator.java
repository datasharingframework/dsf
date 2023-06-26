package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;

public class TaskHtmlGenerator extends InputHtmlGenerator implements HtmlGenerator<Task>
{
	private static final String CODESYSTEM_BPMN_MESSAGE_MESSAGE_NAME = "message-name";
	private static final String CODESYSTEM_BPMN_MESSAGE_BUSINESS_KEY = "business-key";
	private static final String CODESYSTEM_BPMN_MESSAGE_CORRELATION_KEY = "correlation-key";

	@Override
	public Class<Task> getResourceType()
	{
		return Task.class;
	}

	@Override
	public void writeHtml(String basePath, Task task, OutputStreamWriter out) throws IOException
	{
		boolean draft = Task.TaskStatus.DRAFT.equals(task.getStatus());

		out.write("<div id=\"spinner\" class=\"spinner spinner-disabled\"></div>");
		out.write("<form>\n");
		out.write("<div class=\"row row-info " + getColorClass(task.getStatus(), ELEMENT_TYPE_ROW) + "\">\n");

		out.write("<div>");
		out.write("<svg class=\"info-icon\" id=\"info-icon\" height=\"0.3em\" viewBox=\"0 0 512 512\">");
		out.write("<title>Info</title>\n");
		out.write("<path class=\"" + getColorClass(task.getStatus(), ELEMENT_TYPE_PATH)
				+ "\" d=\"M256 512A256 256 0 1 0 256 0a256 256 0 1 0 0 512zM216 336h24V272H216c-13.3 0-24-10.7-24-24s10.7-24 24-24h48c13.3 0 24 10.7 24 24v88h8c13.3 0 24 10.7 24 24s-10.7 24-24 24H216c-13.3 0-24-10.7-24-24s10.7-24 24-24zm40-208a32 32 0 1 1 0 64 32 32 0 1 1 0-64z\"/>");
		out.write("</svg>");
		out.write("</div>\n");

		String[] taskCanonicalSplit = task.getInstantiatesCanonical().split("\\|");
		String href = basePath + "ActivityDefinition?url=" + taskCanonicalSplit[0] + "&version="
				+ taskCanonicalSplit[1];

		out.write("<div>");
		out.write("<p>\n");
		out.write("This Task resource " + (draft ? "can be used" : "was used")
				+ " to instantiate the following process:");
		out.write("</p>\n");
		out.write("<ul class=\"info-list\">\n");
		out.write(
				"<li><b>URL: <a class=\"info-link info-link-task " + getColorClass(task.getStatus(), ELEMENT_TYPE_LINK)
						+ "\" href=\"" + href + "\">" + taskCanonicalSplit[0] + "</a></b></li>\n");
		out.write("<li><b>Version: <a class=\"info-link info-link-task "
				+ getColorClass(task.getStatus(), ELEMENT_TYPE_LINK) + "\" href=\"" + href + "\">"
				+ taskCanonicalSplit[1] + "</a></b></li>\n");
		out.write("<li><b>Status:</b> " + task.getStatus().getDisplay() + "</li>\n");
		out.write("</ul>\n");
		out.write("</div>\n");
		out.write("</div>\n");

		out.write("<fieldset id=\"form-fieldset\" " + (draft ? "" : "disabled=\"disabled\"") + ">\n");

		out.write("<div class=\"row\" id=\"requester-row\">\n");
		out.write("<label class=\"row-label\" for=\"requester\">requester</label>\n");
		out.write("<input type=\"text\" id=\"requester\" name=\"requester\" disabled=\"disabled\" value=\""
				+ task.getRequester().getIdentifier().getValue() + "\"></input>\n");
		out.write("</div>\n");

		out.write("<div class=\"row\" id=\"recipient-row\">\n");
		out.write("<label class=\"row-label\" for=\"recipient\">recipient</label>\n");
		out.write("<input type=\"text\" id=\"recipient\" name=\"recipient\" disabled=\"disabled\" value=\""
				+ task.getRestriction().getRecipient().stream().findFirst().get().getIdentifier().getValue()
				+ "\"></input>\n");
		out.write("</div>\n");

		String authoredOn = DATE_TIME_FORMAT.format(task.getAuthoredOn());
		out.write("<div class=\"row " + (draft ? "invisible" : "") + "\" id=\"authored-on-row\">\n");
		out.write("<label class=\"row-label\" for=\"authored-on\">authored-on</label>\n");
		out.write("<input type=\"datetime-local\" id=\"authored-on\" name=\"authored-on\" "
				+ (draft ? "placeholder=\"yyyy.MM.dd hh:mm:ss\"" : "value=\"" + authoredOn + "\"") + "></input>\n");
		out.write("</div>\n");

		if (task.hasInput())
		{
			out.write("<section>");
			out.write("<h2 class=\"input-output-header\">Inputs</h2>");

			Map<String, Integer> elementIdIndexMap = new HashMap<>();
			for (Task.ParameterComponent input : task.getInput())
			{
				writeInput(input, elementIdIndexMap, draft, out);
			}

			if (draft)
			{
				out.write("<div class=\"row row-submit\" id=\"submit-row\">\n");
				out.write("<button type=\"button\" id=\"submit\" class=\"submit\" " + "onclick=\"startProcess();\""
						+ ">Start Process</button>\n");
				out.write("</div>\n");
			}

			out.write("</section>");
		}

		if (task.hasOutput())
		{
			out.write("<section>");
			out.write("<h2 class=\"input-output-header\">Outputs</h2>");

			Map<String, Integer> elementIdIndexMap = new HashMap<>();
			for (Task.TaskOutputComponent output : task.getOutput())
			{
				writeOutput(output, elementIdIndexMap, out);
			}

			out.write("</section>");
		}

		out.write("</fieldset>\n");
		out.write("</form>\n");
	}

	private String getColorClass(Task.TaskStatus status, String elementType)
	{
		switch (status)
		{
			case DRAFT:
			case REQUESTED:
			{
				if (ELEMENT_TYPE_ROW.equals(elementType))
					return "info-color-draft-requested";
				else if (ELEMENT_TYPE_LINK.equals(elementType))
					return "info-link-draft-requested";
				else if (ELEMENT_TYPE_PATH.equals(elementType))
					return "info-path-draft-requested";
			}
			case INPROGRESS:
			{
				if (ELEMENT_TYPE_ROW.equals(elementType))
					return "info-color-progress";
				else if (ELEMENT_TYPE_LINK.equals(elementType))
					return "info-link-progress";
				else if (ELEMENT_TYPE_PATH.equals(elementType))
					return "info-path-progress";
			}
			case COMPLETED:
			{
				if (ELEMENT_TYPE_ROW.equals(elementType))
					return "info-color-completed";
				else if (ELEMENT_TYPE_LINK.equals(elementType))
					return "info-link-completed";
				else if (ELEMENT_TYPE_PATH.equals(elementType))
					return "info-path-completed";
			}
			case ENTEREDINERROR:
			case REJECTED:
			case CANCELLED:
			case FAILED:
			{
				if (ELEMENT_TYPE_ROW.equals(elementType))
					return "info-color-stopped-failed";
				else if (ELEMENT_TYPE_LINK.equals(elementType))
					return "info-link-stopped-failed";
				else if (ELEMENT_TYPE_PATH.equals(elementType))
					return "info-path-stopped-failed";
			}
			case RECEIVED:
			case ACCEPTED:
			case READY:
			case ONHOLD:
			case NULL:
			default:
				return "";
		}
	}

	private void writeInput(Task.ParameterComponent input, Map<String, Integer> elementIdIndexMap, boolean draft,
			OutputStreamWriter out) throws IOException
	{
		String typeCode = getTypeCode(input);
		boolean display = display(draft, typeCode);

		if (input.hasValue())
		{
			writeInputRow(input.getValue(), input.getExtension(), typeCode, elementIdIndexMap, typeCode, display, draft,
					out);
		}
	}

	private void writeOutput(Task.TaskOutputComponent output, Map<String, Integer> elementIdIndexMap,
			OutputStreamWriter out) throws IOException
	{
		String typeCode = getTypeCode(output);
		if (output.hasValue())
		{
			writeInputRow(output.getValue(), output.getExtension(), typeCode, elementIdIndexMap, typeCode, true, false,
					out);
		}
	}

	private boolean display(boolean draft, String typeCode)
	{
		if (draft)
			return !((CODESYSTEM_BPMN_MESSAGE_MESSAGE_NAME.equals(typeCode)
					|| CODESYSTEM_BPMN_MESSAGE_BUSINESS_KEY.equals(typeCode)
					|| CODESYSTEM_BPMN_MESSAGE_CORRELATION_KEY.equals(typeCode)));
		else
			return true;
	}

	private String getTypeCode(Task.ParameterComponent input)
	{
		return getCode(input.getType());
	}

	private String getTypeCode(Task.TaskOutputComponent output)
	{
		return getCode(output.getType());
	}

	private String getCode(CodeableConcept codeableConcept)
	{
		return codeableConcept.getCoding().stream().findFirst()
				.orElse(new Coding().setCode(UUID.randomUUID().toString())).getCode();
	}
}
