/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
