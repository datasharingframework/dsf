package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;

public class TaskHtmlGenerator extends InputHtmlGenerator implements HtmlGenerator<Task>
{
	private static final String CODESYSTEM_BPMN = "http://dsf.dev/fhir/CodeSystem/bpmn-message";
	private static final String CODESYSTEM_BPMN_MESSAGE_MESSAGE_NAME = "message-name";
	private static final String CODESYSTEM_BPMN_MESSAGE_BUSINESS_KEY = "business-key";
	private static final String CODESYSTEM_BPMN_MESSAGE_CORRELATION_KEY = "correlation-key";

	@Override
	public Class<Task> getResourceType()
	{
		return Task.class;
	}

	@Override
	public boolean isResourceSupported(URI resourceUri, Resource resource)
	{
		return resource != null && resource instanceof Task;
	}

	@Override
	public void writeHtml(URI resourceUri, Task task, OutputStreamWriter out) throws IOException
	{
		boolean draft = Task.TaskStatus.DRAFT.equals(task.getStatus());

		out.write("<div id=\"spinner\" class=\"spinner spinner-disabled\"></div>");
		out.write("<form status=\"" + (task.getStatus() == null ? "" : task.getStatus().toCode()) + "\">\n");
		out.write("<div class=\"row row-info\">\n");

		out.write("<div>");
		out.write("<svg class=\"info-icon\" id=\"info-icon\" height=\"0.3em\" viewBox=\"0 0 512 512\">");
		out.write("<title>Info</title>\n");
		out.write(
				"<path d=\"M256 512A256 256 0 1 0 256 0a256 256 0 1 0 0 512zM216 336h24V272H216c-13.3 0-24-10.7-24-24s10.7-24 24-24h48c13.3 0 24 10.7 24 24v88h8c13.3 0 24 10.7 24 24s-10.7 24-24 24H216c-13.3 0-24-10.7-24-24s10.7-24 24-24zm40-208a32 32 0 1 1 0 64 32 32 0 1 1 0-64z\"/>");
		out.write("</svg>");
		out.write("</div>\n");

		out.write("<div>");
		out.write("<ul class=\"info-list\">\n");
		out.write("<li><b>ID / Version:</b> " + (task.getIdElement() == null ? "" : task.getIdElement().getIdPart())
				+ " / " + (task.getIdElement() == null ? "" : task.getIdElement().getVersionIdPart()) + "</li>\n");
		out.write("<li><b>Last Updated:</b> " + (task.getMeta().getLastUpdated() == null ? ""
				: DATE_TIME_DISPLAY_FORMAT.format(task.getMeta().getLastUpdated())) + "</li>\n");
		out.write("<li><b>Status:</b> " + (task.getStatus() == null ? "" : task.getStatus().toCode()) + "</li>\n");
		out.write("<li><b>Process:</b> <a href=\"ActivityDefinition?url="
				+ (task.getInstantiatesCanonical() == null ? "" : task.getInstantiatesCanonical()) + "\">"
				+ (task.getInstantiatesCanonical() == null ? ""
						: task.getInstantiatesCanonical().replaceAll("\\|", " | "))
				+ "</a></li>\n");
		out.write("<li><b>Task Profile:</b> " + task.getMeta().getProfile().stream().map(CanonicalType::getValue)
				.map(v -> "<a href=\"StructureDefinition?url=" + v + "\">" + v.replaceAll("\\|", " | ") + "</a>")
				.collect(Collectors.joining(", ")) + "</li>\n");
		getInput(task, isMessageName()).ifPresent(m -> silentWrite(out, "<li><b>Message-Name:</b> " + m + "</li>\n"));
		getInput(task, isBusinessKey()).ifPresent(k -> silentWrite(out, "<li><b>Business-Key:</b> " + k + "</li>\n"));
		getInput(task, isCorrelationKey())
				.ifPresent(k -> silentWrite(out, "<li><b>Correlation-Key:</b> " + k + "</li>\n"));
		out.write("</ul>\n");
		out.write("</div>\n");
		out.write("</div>\n");

		out.write("<fieldset id=\"form-fieldset\"" + (draft ? "" : " disabled") + ">\n");

		out.write("<div class=\"row\" name=\"requester-row\">\n");
		out.write("<label class=\"row-label\">requester</label>\n");
		out.write("<input type=\"text\" name=\"requester\" disabled value=\""
				+ task.getRequester().getIdentifier().getValue() + "\"></input>\n");
		out.write("</div>\n");

		out.write("<div class=\"row\" name=\"recipient-row\">\n");
		out.write("<label class=\"row-label\">recipient</label>\n");
		out.write("<input type=\"text\" name=\"recipient\" disabled value=\""
				+ task.getRestriction().getRecipient().stream().findFirst().get().getIdentifier().getValue()
				+ "\"></input>\n");
		out.write("</div>\n");

		String authoredOn = DATE_TIME_FORMAT.format(task.getAuthoredOn());
		out.write("<div class=\"row " + (draft ? "invisible" : "") + "\" name=\"authored-on-row\">\n");
		out.write("<label class=\"row-label\">authored-on</label>\n");
		out.write("<input type=\"datetime-local\" name=\"authored-on\" "
				+ (draft ? "placeholder=\"yyyy.MM.dd hh:mm:ss\"" : "value=\"" + authoredOn + "\"") + "></input>\n");
		out.write("</div>\n");

		List<ParameterComponent> filteredInputs = task.getInput().stream()
				.filter(isMessageName().negate().and(isBusinessKey().negate()).and(isCorrelationKey().negate()))
				.toList();

		if (filteredInputs.size() > 0)
		{
			out.write("<section id=\"inputs\">");
			out.write("<h2 class=\"input-output-header\">Inputs</h2>");

			Map<String, Integer> elemenIndexMap = new HashMap<>();
			for (ParameterComponent input : filteredInputs)
			{
				writeInput(input, elemenIndexMap, draft, out);
			}

			out.write("</section>");
		}

		if (task.hasOutput())
		{
			out.write("<section id=\"outputs\">");
			out.write("<h2 class=\"input-output-header\">Outputs</h2>");

			Map<String, Integer> elemenIndexMap = new HashMap<>();
			for (TaskOutputComponent output : task.getOutput())
			{
				writeOutput(output, elemenIndexMap, out);
			}

			out.write("</section>");
		}

		if (draft)
		{
			out.write("<div class=\"row row-submit\" name=\"submit-row\">\n");
			out.write("<button type=\"button\" name=\"submit\" class=\"submit\" " + "onclick=\"startProcess();\""
					+ ">Start Process</button>\n");
			out.write("</div>\n");
		}

		out.write("</fieldset>\n");
		out.write("</form>\n");
	}

	private void silentWrite(OutputStreamWriter out, String value)
	{
		try
		{
			out.write(value);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Optional<String> getInput(Task task, Predicate<ParameterComponent> paramMatches)
	{
		if (task == null || paramMatches == null)
			return Optional.empty();

		return task.getInput().stream().filter(paramMatches).filter(i -> i.getValue() instanceof StringType)
				.map(i -> ((StringType) i.getValue()).getValue()).findFirst();
	}

	private Predicate<ParameterComponent> isMessageName()
	{
		return param -> param != null && param.getType().getCoding().stream().anyMatch(
				c -> CODESYSTEM_BPMN.equals(c.getSystem()) && CODESYSTEM_BPMN_MESSAGE_MESSAGE_NAME.equals(c.getCode()));
	}

	private Predicate<ParameterComponent> isBusinessKey()
	{
		return param -> param != null && param.getType().getCoding().stream().anyMatch(
				c -> CODESYSTEM_BPMN.equals(c.getSystem()) && CODESYSTEM_BPMN_MESSAGE_BUSINESS_KEY.equals(c.getCode()));
	}

	private Predicate<ParameterComponent> isCorrelationKey()
	{
		return param -> param != null
				&& param.getType().getCoding().stream().anyMatch(c -> CODESYSTEM_BPMN.equals(c.getSystem())
						&& CODESYSTEM_BPMN_MESSAGE_CORRELATION_KEY.equals(c.getCode()));
	}

	private void writeInput(Task.ParameterComponent input, Map<String, Integer> elemenIndexMap, boolean draft,
			OutputStreamWriter out) throws IOException
	{
		String typeCode = getCode(input.getType());
		boolean display = display(draft, typeCode);

		if (input.hasValue())
		{
			writeInputRow(input.getValue(), input.getExtension(), typeCode, elemenIndexMap, typeCode, display, draft,
					out);
		}
	}

	private void writeOutput(Task.TaskOutputComponent output, Map<String, Integer> elemenIndexMap,
			OutputStreamWriter out) throws IOException
	{
		String typeCode = getCode(output.getType());
		if (output.hasValue())
		{
			writeInputRow(output.getValue(), output.getExtension(), typeCode, elemenIndexMap, typeCode, true, false,
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

	private String getCode(CodeableConcept codeableConcept)
	{
		return codeableConcept.getCoding().stream().findFirst()
				.orElse(new Coding().setCode(UUID.randomUUID().toString())).getCode();
	}
}
