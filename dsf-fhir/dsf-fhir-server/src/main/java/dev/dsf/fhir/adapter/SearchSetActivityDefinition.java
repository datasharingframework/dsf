package dev.dsf.fhir.adapter;

import java.util.List;
import java.util.regex.Matcher;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.StringType;

public class SearchSetActivityDefinition extends AbstractSearchSet<ActivityDefinition>
{
	private record Row(ElementId id, String status, String title, String processDomain, String processName,
			String processVersion, String messageNames, String lastUpdated)
	{
	}

	public SearchSetActivityDefinition(int defaultPageCount)
	{
		super(defaultPageCount, ActivityDefinition.class);
	}

	@Override
	protected Row toRow(ElementId id, ActivityDefinition resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String title = resource.hasTitleElement() && resource.getTitleElement().hasValue()
				? resource.getTitleElement().getValue()
				: "";

		String processDomain = "", processName = "", processVersion = "";
		if (resource.getUrl() != null && !resource.getUrl().isBlank())
		{
			Matcher matcher = INSTANTIATES_CANONICAL_PATTERN
					.matcher(resource.getUrl() + (resource.hasVersion() ? "|" + resource.getVersion() : ""));
			if (matcher.matches())
			{
				processDomain = matcher.group("domain");
				processName = matcher.group("processName");
				processVersion = matcher.group("processVersion");
			}
		}

		List<String> messageNames = resource.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION).stream()
				.flatMap(e -> e.getExtensionsByUrl(EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME).stream())
				.filter(e -> e.getValue() instanceof StringType).map(e -> ((StringType) e.getValue()).getValue())
				.toList();

		String combinedMessageNames = (messageNames.size() > 2)
				? String.join(", ", messageNames.subList(0, 2)) + ", ..."
				: String.join(", ", messageNames);

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, title, processDomain, processName, processVersion, combinedMessageNames,
				lastUpdated);
	}
}
