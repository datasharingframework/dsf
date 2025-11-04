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
package dev.dsf.fhir.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import dev.dsf.fhir.search.PageAndCount;

public class History
{
	private final int total;
	private final PageAndCount pageAndCount;
	private final List<HistoryEntry> entries = new ArrayList<>();

	public History(int total, PageAndCount pageAndCount, Collection<? extends HistoryEntry> entries)
	{
		this.total = total;
		this.pageAndCount = pageAndCount;
		if (entries != null)
			this.entries.addAll(entries);
	}

	public int getTotal()
	{
		return total;
	}

	public PageAndCount getPageAndCount()
	{
		return pageAndCount;
	}

	public List<HistoryEntry> getEntries()
	{
		return Collections.unmodifiableList(entries);
	}

}