package dev.dsf.fhir.adapter;

import java.util.regex.Pattern;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;

public class SearchSetMeasureReport extends AbstractSearchSet<MeasureReport>
{
	private static final String MEASURE_URL_PATTERN_STRING = "((http|https):\\/\\/([A-Za-z0-9\\-\\\\\\.\\:\\%\\$]*\\/)+)?Measure\\/[A-Za-z0-9\\-\\.]{1,64}(\\/_history\\/[A-Za-z0-9\\-\\.]{1,64})?";
	private static final Pattern MEASURE_URL_PATTERN = Pattern.compile(MEASURE_URL_PATTERN_STRING);

	private record Row(ElementId id, String status, String type, String date, ElementId measure, String lastUpdated)
	{
	}

	public SearchSetMeasureReport(int defaultPageCount)
	{
		super(defaultPageCount, MeasureReport.class);
	}

	@Override
	protected Row toRow(ElementId id, MeasureReport resource)
	{
		String status = getEnumeration(resource, MeasureReport::hasStatusElement, MeasureReport::getStatusElement);
		String type = getEnumeration(resource, MeasureReport::hasTypeElement, MeasureReport::getTypeElement);
		String date = getDateTime(resource, MeasureReport::hasDateElement, MeasureReport::getDateElement);

		String measureCanonical = resource.hasMeasureElement() && resource.getMeasureElement().hasValue()
				? resource.getMeasureElement().getValue()
				: null;
		ElementId measure = measureCanonical != null && MEASURE_URL_PATTERN.matcher(measureCanonical).matches()
				? ElementId.from(new IdType(measureCanonical))
				: null;

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, type, date, measure, lastUpdated);
	}
}
