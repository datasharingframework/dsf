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
package dev.dsf.fhir.search;

import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.Resource;

public class PartialResult<R extends Resource>
{
	private final int total;
	private final PageAndCount pageAndCount;
	private final List<R> partialResult;
	private final List<Resource> includes;

	public PartialResult(int total, PageAndCount pageAndCount, List<R> partialResult, List<Resource> includes)
	{
		this.total = total;
		this.pageAndCount = pageAndCount;
		this.partialResult = partialResult;
		this.includes = includes;
	}

	public int getTotal()
	{
		return total;
	}

	public PageAndCount getPageAndCount()
	{
		return pageAndCount;
	}

	public List<R> getPartialResult()
	{
		return Collections.unmodifiableList(partialResult);
	}

	public List<Resource> getIncludes()
	{
		return Collections.unmodifiableList(includes);
	}
}
