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
package dev.dsf.fhir.webservice;

public interface RangeRequest
{
	String USER_DATA_VALUE_RANGE_REQUEST = "range-request";
	String USER_DATA_VALUE_DATA_SIZE = "data-size";

	String RANGE_HEADER = "Range";
	String RANGE_HEADER_LC = RANGE_HEADER.toLowerCase();
	String IF_RANGE_HEADER = "If-Range";
	String IF_RANGE_HEADER_LC = "If-Range".toLowerCase();

	String CONTENT_RANGE_HEADER = "Content-Range";
	String ACCEPT_RANGES_HEADER = "Accept-Ranges";
	String ACCEPT_RANGES_HEADER_VALUE = "bytes";

	long getStart(long dataSize);

	long getRequestedLength(long dataSize);

	boolean isRangeSatisfiable(long dataSize);

	boolean isRangeNotDefined();

	String createContentRangeHeaderValue(long dataSize);

	String createRangeHeaderValue(long dataSize);
}
