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

import org.hl7.fhir.r4.model.Binary;

import dev.dsf.fhir.webservice.RangeRequest;

public class SearchSetBinary extends AbstractSearchSet<Binary>
{
	private static final String[] UNITS = { "Byte", "KiB", "MiB", "GiB", "TiB" };
	private static final long UNIT = 1024;

	private record Row(ElementId id, String contentType, ElementId securityContext, String dataSize, String lastUpdated)
	{
	}

	public SearchSetBinary(int defaultPageCount)
	{
		super(defaultPageCount, Binary.class);
	}

	@Override
	protected Row toRow(ElementId id, Binary resource)
	{
		String contentType = resource.hasContentTypeElement() && resource.getContentTypeElement().hasValue()
				? resource.getContentTypeElement().getValue()
				: "";

		ElementId securityContext = ElementId.from(resource, Binary::hasSecurityContext, Binary::getSecurityContext,
				true);

		String dataSize = resource.hasDataElement() ? toDataSize(resource) : "";

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, contentType, securityContext, dataSize, lastUpdated);
	}

	private String toDataSize(Binary resource)
	{
		long dataSize = (long) resource.getUserData(RangeRequest.USER_DATA_VALUE_DATA_SIZE);

		if (dataSize < 0)
			throw new IllegalArgumentException("bytes < 0");

		double value = dataSize;
		int unitIndex = 0;

		while (value >= UNIT && unitIndex < UNITS.length - 1)
		{
			value /= UNIT;
			unitIndex++;
		}

		if (value == (long) value)
			return String.format("%d %s", (long) value, UNITS[unitIndex]);
		else
			return String.format("%.2f %s", value, UNITS[unitIndex]);
	}
}
