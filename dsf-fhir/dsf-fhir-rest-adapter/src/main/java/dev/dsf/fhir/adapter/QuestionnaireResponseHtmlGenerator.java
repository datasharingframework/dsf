package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
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
	public void writeHtml(String basePath, QuestionnaireResponse questionnaireResponse, OutputStreamWriter out)
			throws IOException
	{
		boolean completed = QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
				.equals(questionnaireResponse.getStatus());

		out.write("<div id=\"spinner\" class=\"spinner spinner-disabled\"></div>");
		out.write("<form>\n");
		out.write("<div class=\"row row-info " + getColorClass(questionnaireResponse.getStatus(), ELEMENT_TYPE_ROW)
				+ "\">\n");

		out.write("<div>");
		out.write("<svg class=\"info-icon\" id=\"info-icon\" height=\"0.5em\" viewBox=\"0 0 512 512\">");
		out.write("<title>Info</title>\n");
		out.write("<path class=\"" + getColorClass(questionnaireResponse.getStatus(), ELEMENT_TYPE_PATH)
				+ "\" d=\"M256 512A256 256 0 1 0 256 0a256 256 0 1 0 0 512zM216 336h24V272H216c-13.3 0-24-10.7-24-24s10.7-24 24-24h48c13.3 0 24 10.7 24 24v88h8c13.3 0 24 10.7 24 24s-10.7 24-24 24H216c-13.3 0-24-10.7-24-24s10.7-24 24-24zm40-208a32 32 0 1 1 0 64 32 32 0 1 1 0-64z\"/>");
		out.write("</svg>");
		out.write("</div>\n");

		String urlVersion = questionnaireResponse.getQuestionnaire();
		String[] urlVersionSplit = urlVersion.split("\\|");
		String href = basePath + "Questionnaire?url=" + urlVersionSplit[0] + "&version=" + urlVersionSplit[1];

		out.write("<div>");
		out.write("<p>\n");
		out.write("This QuestionnaireResponse answers the Questionnaire:</br><b><a class=\"info-link "
				+ getColorClass(questionnaireResponse.getStatus(), ELEMENT_TYPE_LINK) + "\" href=\"" + href + "\">"
				+ urlVersion + "</b></a>");
		out.write("</p>\n");
		out.write("<ul class=\"info-list\">\n");
		out.write("<li><b>State:</b> " + questionnaireResponse.getStatus().getDisplay() + "</li>\n");
		out.write("<li><b>Process instance-id:</b> " + getProcessInstanceId(questionnaireResponse) + "</li>\n");

		String lastUpdated = DATE_TIME_DISPLAY_FORMAT.format(questionnaireResponse.getMeta().getLastUpdated());
		if (completed)
		{
			out.write("<li><b>Completion date:</b> " + lastUpdated + "</li>\n");
		}
		else
		{
			out.write("<li><b>Creation date:</b> " + lastUpdated + "</li>\n");
		}

		out.write("</ul>\n");
		out.write("</div>\n");
		out.write("</div>\n");

		out.write("<fieldset id=\"form-fieldset\" " + (completed ? "disabled=\"disabled\"" : "") + ">\n");

		for (QuestionnaireResponse.QuestionnaireResponseItemComponent item : questionnaireResponse.getItem())
		{
			writeRow(item, completed, out);
		}

		if (QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS.equals(questionnaireResponse.getStatus()))
		{
			out.write("<div class=\"row row-submit\" id=\"submit-row\">\n");
			out.write(
					"<button type=\"button\" id=\"submit\" class=\"submit\" onclick=\"completeQuestionnaireResponse();\">Submit</button>\n");
			out.write("</div>\n");
		}

		out.write("</fieldset>\n");
		out.write("</form>\n");
	}

	private String getColorClass(QuestionnaireResponse.QuestionnaireResponseStatus status, String elementType)
	{
		switch (status)
		{
			case INPROGRESS:
				if (ELEMENT_TYPE_ROW.equals(elementType))
					return "info-color-progress";
				else if (ELEMENT_TYPE_LINK.equals(elementType))
					return "info-link-progress";
				else if (ELEMENT_TYPE_PATH.equals(elementType))
					return "info-path-progress";
			case COMPLETED:
				if (ELEMENT_TYPE_ROW.equals(elementType))
					return "info-color-completed";
				else if (ELEMENT_TYPE_LINK.equals(elementType))
					return "info-link-completed";
				else if (ELEMENT_TYPE_PATH.equals(elementType))
					return "info-path-completed";
			case STOPPED:
			case ENTEREDINERROR:
				if (ELEMENT_TYPE_ROW.equals(elementType))
					return "info-color-stopped-failed";
				else if (ELEMENT_TYPE_LINK.equals(elementType))
					return "info-link-stopped-failed";
				else if (ELEMENT_TYPE_PATH.equals(elementType))
					return "info-path-stopped-failed";
			case AMENDED:
			case NULL:
			default:
				return "";
		}
	}

	private String getProcessInstanceId(QuestionnaireResponse questionnaireResponse)
	{
		return questionnaireResponse.getItem().stream()
				.filter(i -> CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY.equals(i.getLinkId()))
				.flatMap(i -> i.getAnswer().stream()).map(a -> ((StringType) a.getValue()).getValue()).findFirst()
				.orElse("unknown");
	}

	private void writeRow(QuestionnaireResponse.QuestionnaireResponseItemComponent item, boolean completed,
			OutputStreamWriter out) throws IOException
	{
		String linkId = item.getLinkId();
		String text = item.getText();
		boolean display = display(linkId);
		boolean writable = !completed;

		if (item.hasAnswer())
			writeInputRow(item.getAnswerFirstRep().getValue(), linkId, text, display, writable, out);
		else
			writeDisplayRow(text, linkId, display, out);
	}

	private boolean display(String linkId)
	{
		return !(CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_BUSINESS_KEY.equals(linkId)
				|| CODESYSTEM_DSF_BPMN_USER_TASK_VALUE_USER_TASK_ID.equals(linkId));
	}
}
