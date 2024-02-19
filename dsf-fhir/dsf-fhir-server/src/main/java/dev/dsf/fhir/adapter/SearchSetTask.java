package dev.dsf.fhir.adapter;

import java.util.regex.Matcher;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;

public class SearchSetTask extends AbstractSearchSet<Task>
{
	private record Row(ElementId id, String status, String processDomain, String processName, String processVersion,
			String messageName, String requester, String businessKeyOrIdentifier, String lastUpdated)
	{
	}

	public SearchSetTask(int defaultPageCount)
	{
		super(defaultPageCount, Task.class);
	}

	@Override
	protected Row toRow(ElementId id, Task resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String processDomain = "", processName = "", processVersion = "";
		if (resource.getInstantiatesCanonical() != null && !resource.getInstantiatesCanonical().isBlank())
		{
			Matcher matcher = INSTANTIATES_CANONICAL_PATTERN.matcher(resource.getInstantiatesCanonical());
			if (matcher.matches())
			{
				processDomain = matcher.group("domain");
				processName = matcher.group("processName");
				processVersion = matcher.group("processVersion");
			}
		}

		String messageName = resource.getInput().stream()
				.filter(isStringParam(CODE_SYSTEM_BPMN_MESSAGE, CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME)).findFirst()
				.map(c -> ((StringType) c.getValue()).getValue()).orElse("");

		String requester = resource.hasRequester() && resource.getRequester().hasIdentifier()
				&& resource.getRequester().getIdentifier().hasValue()
						? resource.getRequester().getIdentifier().getValue()
						: "";

		String businessKeyOrIdentifier;
		if (TaskStatus.DRAFT.equals(resource.getStatus()))
		{
			businessKeyOrIdentifier = resource.getIdentifier().stream()
					.filter(i -> i.hasSystem() && NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem()) && i.hasValue())
					.map(Identifier::getValue).findFirst().map(v ->
					{
						String[] parts = v.split("/");
						return parts.length > 0 ? parts[parts.length - 1] : "";
					}).orElse("");
		}
		else
		{
			businessKeyOrIdentifier = resource.getInput().stream()
					.filter(isStringParam(CODE_SYSTEM_BPMN_MESSAGE, CODE_SYSTEM_BPMN_MESSAGE_BUSINESS_KEY)).findFirst()
					.map(c -> ((StringType) c.getValue()).getValue()).orElse("");
		}

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, processDomain, processName, processVersion, messageName, requester,
				businessKeyOrIdentifier, lastUpdated);
	}
}
