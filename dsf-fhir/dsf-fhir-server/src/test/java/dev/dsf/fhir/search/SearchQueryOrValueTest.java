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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class SearchQueryOrValueTest
{
	@Test
	public void testSingleValue()
	{
		assertEquals(List.of("male"), SearchQuery.splitValuesForOr("male"));
	}

	@Test
	public void testTwoValues()
	{
		assertEquals(List.of("male", "female"), SearchQuery.splitValuesForOr("male,female"));
	}

	@Test
	public void testThreeValues()
	{
		assertEquals(List.of("a", "b", "c"), SearchQuery.splitValuesForOr("a,b,c"));
	}

	@Test
	public void testTokenValuesWithSystem()
	{
		assertEquals(List.of("http://loinc.org|1234-5", "http://loinc.org|6789-0"),
				SearchQuery.splitValuesForOr("http://loinc.org|1234-5,http://loinc.org|6789-0"));
	}

	@Test
	public void testEscapedCommaIsLiteral()
	{
		assertEquals(List.of("a,b"), SearchQuery.splitValuesForOr("a\\,b"));
	}

	@Test
	public void testEscapedAndSeparatingComma()
	{
		assertEquals(List.of("a,b", "c"), SearchQuery.splitValuesForOr("a\\,b,c"));
	}

	@Test
	public void testOtherBackslashEscapesArePreserved()
	{
		// only \, is unescaped here; \| is left for the parameter type specific parsing
		assertEquals(List.of("a\\|b"), SearchQuery.splitValuesForOr("a\\|b"));
	}

	@Test
	public void testEmptyValue()
	{
		assertEquals(List.of(""), SearchQuery.splitValuesForOr(""));
	}

	@Test
	public void testTrailingComma()
	{
		assertEquals(List.of("a", ""), SearchQuery.splitValuesForOr("a,"));
	}

	@Test
	public void testLeadingComma()
	{
		assertEquals(List.of("", "a"), SearchQuery.splitValuesForOr(",a"));
	}

	@Test
	public void testTrailingBackslash()
	{
		assertEquals(List.of("a\\"), SearchQuery.splitValuesForOr("a\\"));
	}
}
