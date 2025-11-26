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

import java.util.List;
import java.util.regex.Pattern;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;

public class ResourceMeasureReport extends AbstractResource<MeasureReport>
{
	private static final String MEASURE_URL_PATTERN_STRING = "((http|https):\\/\\/([A-Za-z0-9\\-\\\\\\.\\:\\%\\$]*\\/)+)?Measure\\/[A-Za-z0-9\\-\\.]{1,64}(\\/_history\\/[A-Za-z0-9\\-\\.]{1,64})?";
	private static final Pattern MEASURE_URL_PATTERN = Pattern.compile(MEASURE_URL_PATTERN_STRING);

	private final String serverBaseUrl;

	private record Element(List<ElementSystemValue> identifier, String type, ElementId measure, String date,
			String groupPopulationCount)
	{
	}

	public ResourceMeasureReport(String serverBaseUrl)
	{
		super(MeasureReport.class,
				ActiveOrStatus.status(MeasureReport::hasStatusElement, MeasureReport::getStatusElement));

		this.serverBaseUrl = serverBaseUrl;
	}

	@Override
	protected Element toElement(MeasureReport resource)
	{
		List<ElementSystemValue> identifier = getIdentifiers(resource, MeasureReport::hasIdentifier,
				MeasureReport::getIdentifier);
		String type = getEnumeration(resource, MeasureReport::hasTypeElement, MeasureReport::getTypeElement);
		ElementId measure = getMeasure(resource);
		String date = getDateTime(resource, MeasureReport::hasDateElement, MeasureReport::getDateElement);
		String groupPopulationCount = getGroupPopulationCount(resource);

		return new Element(identifier, type, measure, date, groupPopulationCount);
	}

	private ElementId getMeasure(MeasureReport resource)
	{
		if (!resource.hasMeasureElement() || !resource.getMeasureElement().hasValue())
			return null;

		String measureCanonical = resource.getMeasureElement().getValue();
		if (measureCanonical != null && MEASURE_URL_PATTERN.matcher(measureCanonical).matches())
		{
			IdType measureId = new IdType(measureCanonical);
			if (!measureId.hasBaseUrl() || serverBaseUrl.equals(measureId.getBaseUrl()))
				return ElementId.from(measureId);
			else
				return ElementId.from(measureId.getValue(), "Measure", measureId.getValue());
		}
		else
			return null;
	}

	private String getGroupPopulationCount(MeasureReport resource)
	{
		if (!resource.hasGroup())
			return null;

		List<String> values = resource.getGroup().stream().filter(MeasureReportGroupComponent::hasPopulation)
				.map(MeasureReportGroupComponent::getPopulation).flatMap(List::stream)
				.filter(MeasureReportGroupPopulationComponent::hasCode)
				.filter(MeasureReportGroupPopulationComponent::hasCountElement)
				.filter(g -> g.getCode().hasCoding() && g.getCode().getCoding().stream().filter(Coding::hasSystem)
						.filter(Coding::hasCode)
						.filter(c -> "http://terminology.hl7.org/CodeSystem/measure-population".equals(c.getSystem())
								&& "initial-population".equals(c.getCode()))
						.count() >= 1)
				.map(MeasureReportGroupPopulationComponent::getCountElement).filter(IntegerType::hasValue)
				.map(IntegerType::getValue).map(String::valueOf).toList();
		if (values.size() != 1)
			return null;
		else
			return values.get(0);
	}
}