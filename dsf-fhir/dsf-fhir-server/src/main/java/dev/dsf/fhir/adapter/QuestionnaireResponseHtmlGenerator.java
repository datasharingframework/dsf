package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

public class QuestionnaireResponseHtmlGenerator extends InputHtmlGenerator
		implements HtmlGenerator<QuestionnaireResponse>
{
	private static final String CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY = "business-key";
	private static final String CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID = "user-task-id";

	@Override
	public Class<QuestionnaireResponse> getResourceType()
	{
		return QuestionnaireResponse.class;
	}

	@Override
	public boolean isResourceSupported(URI resourceUri, Resource resource)
	{
		return resource != null && resource instanceof QuestionnaireResponse;
	}

	@Override
	public void writeHtml(URI resourceUri, QuestionnaireResponse questionnaireResponse, OutputStreamWriter out)
			throws IOException
	{
		final boolean completed = QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
				.equals(questionnaireResponse.getStatus());

		out.write("<div id=\"spinner\" class=\"spinner spinner-disabled\"></div>");

		out.write("<form status=\""
				+ (questionnaireResponse.getStatus() == null ? "" : questionnaireResponse.getStatus().toCode())
				+ "\">\n");

		out.write("<div class=\"row row-info\">\n");
		out.write("<div>");
		out.write("<svg class=\"info-icon\" id=\"info-icon\" height=\"0.5em\" viewBox=\"0 0 512 512\">");
		out.write("<title>Info</title>\n");
		out.write(
				"<path d=\"M256 512A256 256 0 1 0 256 0a256 256 0 1 0 0 512zM216 336h24V272H216c-13.3 0-24-10.7-24-24s10.7-24 24-24h48c13.3 0 24 10.7 24 24v88h8c13.3 0 24 10.7 24 24s-10.7 24-24 24H216c-13.3 0-24-10.7-24-24s10.7-24 24-24zm40-208a32 32 0 1 1 0 64 32 32 0 1 1 0-64z\"/>");
		out.write("</svg>");
		out.write("</div>\n");
		out.write("<div>");
		out.write("<ul class=\"info-list\">\n");
		out.write("<li><b>ID / Version:</b> "
				+ (questionnaireResponse.getIdElement() == null ? "" : questionnaireResponse.getIdElement().getIdPart())
				+ " / " + (questionnaireResponse.getIdElement() == null ? ""
						: questionnaireResponse.getIdElement().getVersionIdPart())
				+ "</li>\n");
		out.write("<li><b>Last Updated:</b> "
				+ (questionnaireResponse.getMeta().getLastUpdated() == null ? ""
						: DATE_TIME_DISPLAY_FORMAT.format(questionnaireResponse.getMeta().getLastUpdated()))
				+ "</li>\n");
		out.write("<li><b>Status:</b> "
				+ (questionnaireResponse.getStatus() == null ? "" : questionnaireResponse.getStatus().toCode())
				+ "</li>\n");
		out.write("<li><b>Questionnaire:</b> <a href=\"Questionnaire?url="
				+ (questionnaireResponse.getQuestionnaire() == null ? "" : questionnaireResponse.getQuestionnaire())
				+ "\">" + (questionnaireResponse.getQuestionnaire() == null ? ""
						: questionnaireResponse.getQuestionnaire().replaceAll("\\|", " | "))
				+ "</a></li>\n");
		out.write("<li><b>Business-Key:</b> " + getProcessInstanceId(questionnaireResponse) + "</li>\n");
		out.write("</ul>\n");
		out.write("</div>\n");
		out.write("</div>\n");

		out.write("<fieldset id=\"form-fieldset\"" + (completed ? " disabled" : "") + ">\n");

		Map<String, Integer> elemenIndexMap = new HashMap<>();
		for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : questionnaireResponse.getItem())
		{
			writeRow(item, elemenIndexMap, completed, out);
		}

		if (QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS.equals(questionnaireResponse.getStatus()))
		{
			out.write("<div class=\"row row-submit\" name=\"submit-row\">\n");
			out.write(
					"<button id=\"complete-questionnaire-response\" type=\"button\" name=\"submit\" class=\"submit\">Submit</button>\n");
			out.write("</div>\n");
		}

		out.write("</fieldset>\n");
		out.write("</form>\n");
	}

	private String getProcessInstanceId(QuestionnaireResponse questionnaireResponse)
	{
		return questionnaireResponse.getItem().stream()
				.filter(i -> CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY.equals(i.getLinkId()))
				.flatMap(i -> i.getAnswer().stream()).map(a -> ((StringType) a.getValue()).getValue()).findFirst()
				.orElse("unknown");
	}

	private void writeRow(QuestionnaireResponse.QuestionnaireResponseItemComponent item,
			Map<String, Integer> elemenIndexMap, boolean completed, OutputStreamWriter out) throws IOException
	{
		String linkId = item.getLinkId();
		String text = item.getText();
		boolean display = display(linkId);
		boolean writable = !completed;

		if (item.hasAnswer())
			writeInputRow(item.getAnswerFirstRep().getValue(), Collections.emptyList(), linkId, elemenIndexMap, text,
					display, writable, out);
		else
			writeDisplayRow(text, linkId, display, out);
	}

	private boolean display(String linkId)
	{
		return !(CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY.equals(linkId)
				|| CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID.equals(linkId));
	}
}
