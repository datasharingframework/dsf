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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SearchQueryParameterOrGroupTest
{
	@Mock
	private SearchQueryParameter<Resource> firstParameter;

	@Mock
	private SearchQueryParameter<Resource> secondParameter;

	@Mock
	private SearchQueryParameter<Resource> undefinedParameter;

	@Mock
	private Resource resource;

	@Mock
	private PreparedStatement statement;

	private Supplier<SearchQueryParameter<Resource>> supplier;

	private SearchQueryParameterOrGroup<Resource> orGroup;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp()
	{
		supplier = mock(Supplier.class);

		orGroup = new SearchQueryParameterOrGroup<>(supplier);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void configureShouldCreateOneParameterPerNonBlankOrValue()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		List<SearchQueryParameterError> errors = new ArrayList<>();

		orGroup.configure(errors, "name", "foo,bar");

		assertEquals(2, orGroup.getSearchParameters().size());
		assertSame(firstParameter, orGroup.getSearchParameters().get(0));
		assertSame(secondParameter, orGroup.getSearchParameters().get(1));

		verify(firstParameter).configure(errors, "name", "foo");
		verify(secondParameter).configure(errors, "name", "bar");
		verify(supplier, times(2)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void configureShouldCreateOneParameterForSingleValueWithNoComma()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		List<SearchQueryParameterError> errors = new ArrayList<>();

		orGroup.configure(errors, "name", "foo");

		assertEquals(1, orGroup.getSearchParameters().size());
		assertSame(firstParameter, orGroup.getSearchParameters().get(0));

		verify(firstParameter).configure(errors, "name", "foo");
		verify(supplier, times(1)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void configureShouldIgnoreBlankValues()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		List<SearchQueryParameterError> errors = new ArrayList<>();

		orGroup.configure(errors, "name", "foo,,   ,bar,");

		assertEquals(2, orGroup.getSearchParameters().size());
		assertSame(firstParameter, orGroup.getSearchParameters().get(0));
		assertSame(secondParameter, orGroup.getSearchParameters().get(1));

		verify(firstParameter).configure(errors, "name", "foo");
		verify(secondParameter).configure(errors, "name", "bar");
		verify(supplier, times(2)).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void configureShouldTreatEscapedCommaAsPartOfValue()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		List<SearchQueryParameterError> errors = new ArrayList<>();

		orGroup.configure(errors, "name", "foo\\,bar,baz");

		verify(firstParameter).configure(errors, "name", "foo,bar");
		verify(secondParameter).configure(errors, "name", "baz");

		assertEquals(2, orGroup.getSearchParameters().size());
	}

	@Test
	public void configureShouldTreatBackslashBeforeNonCommaAsLiteralBackslash()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(supplier.get()).thenReturn(firstParameter);

		List<SearchQueryParameterError> errors = new ArrayList<>();

		orGroup.configure(errors, "name", "foo\\x");

		verify(firstParameter).configure(errors, "name", "foo\\x");
	}

	@Test
	public void configureShouldHandleTrailingBackslash()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(supplier.get()).thenReturn(firstParameter);

		List<SearchQueryParameterError> errors = new ArrayList<>();

		orGroup.configure(errors, "name", "foo\\");

		verify(firstParameter).configure(errors, "name", "foo\\");
	}

	@Test
	public void configureShouldReturnThis()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(supplier.get()).thenReturn(firstParameter);

		List<SearchQueryParameterError> errors = new ArrayList<>();

		SearchQueryParameter<Resource> result = orGroup.configure(errors, "name", "foo");

		assertSame(orGroup, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void matchesShouldReturnTrueWhenAnyDefinedParameterMatches()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(false);
		when(secondParameter.isDefined()).thenReturn(true);
		when(secondParameter.matches(resource)).thenReturn(true);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertTrue(orGroup.matches(resource));

		verify(firstParameter, never()).matches(any());
		verify(secondParameter).matches(resource);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void matchesShouldReturnFalseWhenNoDefinedParameterMatches()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(true);
		when(secondParameter.isDefined()).thenReturn(true);
		when(firstParameter.matches(resource)).thenReturn(false);
		when(secondParameter.matches(resource)).thenReturn(false);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertFalse(orGroup.matches(resource));

		verify(firstParameter).matches(resource);
		verify(secondParameter).matches(resource);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void matchesShouldNotEvaluateUndefinedParameters()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(false);
		when(secondParameter.isDefined()).thenReturn(true);
		when(secondParameter.matches(resource)).thenReturn(false);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertFalse(orGroup.matches(resource));

		verify(firstParameter, never()).matches(any());
		verify(secondParameter).matches(resource);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void isDefinedShouldReturnTrueWhenAtLeastOneParameterIsDefined()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(false);
		when(secondParameter.isDefined()).thenReturn(true);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertTrue(orGroup.isDefined());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void isDefinedShouldReturnFalseWhenNoParameterIsDefined()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(false);
		when(secondParameter.isDefined()).thenReturn(false);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertFalse(orGroup.isDefined());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getFilterQueryShouldReturnEmptyStringWhenNoParameterIsDefined()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(false);
		when(secondParameter.isDefined()).thenReturn(false);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertEquals("", orGroup.getFilterQuery());

		verify(firstParameter, never()).getFilterQuery();
		verify(secondParameter, never()).getFilterQuery();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getFilterQueryShouldReturnSingleFilterWithoutParentheses()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(true);
		when(secondParameter.isDefined()).thenReturn(false);
		when(firstParameter.getFilterQuery()).thenReturn("name = ?");

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertEquals("name = ?", orGroup.getFilterQuery());

		verify(firstParameter).getFilterQuery();
		verify(secondParameter, never()).getFilterQuery();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getFilterQueryShouldJoinMultipleFiltersWithOr()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(true);
		when(secondParameter.isDefined()).thenReturn(true);
		when(firstParameter.getFilterQuery()).thenReturn("name = ?");
		when(secondParameter.getFilterQuery()).thenReturn("name = ?");

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertEquals("(name = ? OR name = ?)", orGroup.getFilterQuery());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getSqlParameterCountShouldReturnSumOfChildCounts()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.getSqlParameterCount()).thenReturn(2);
		when(secondParameter.getSqlParameterCount()).thenReturn(3);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertEquals(5, orGroup.getSqlParameterCount());

		verify(firstParameter).getSqlParameterCount();
		verify(secondParameter).getSqlParameterCount();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void modifyStatementShouldDelegateToFirstParameter() throws Exception
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.getSqlParameterCount()).thenReturn(2);
		when(secondParameter.getSqlParameterCount()).thenReturn(3);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");
		orGroup.getSqlParameterCount();

		orGroup.modifyStatement(7, 1, statement, null, null);

		verify(firstParameter).modifyStatement(eq(7), eq(1), same(statement), isNull(), isNull());

		verify(secondParameter, never()).modifyStatement(anyInt(), anyInt(), any(), any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void modifyStatementShouldRouteToSecondParameterAndAdjustIndex() throws Exception
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.getSqlParameterCount()).thenReturn(2);
		when(secondParameter.getSqlParameterCount()).thenReturn(3);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");
		orGroup.getSqlParameterCount();

		// First parameter owns indexes 1-2.
		// Therefore subqueryParameterIndex 3 belongs to the second parameter
		// and is translated to index 1 within that parameter.
		orGroup.modifyStatement(7, 3, statement, null, null);

		verify(secondParameter).modifyStatement(eq(7), eq(1), same(statement), isNull(), isNull());

		verify(firstParameter, never()).modifyStatement(anyInt(), anyInt(), any(), any(), any());
	}

	@Test(expected = IllegalStateException.class)
	@SuppressWarnings("unchecked")
	public void modifyStatementShouldRejectInvalidSubqueryParameterIndex() throws Exception
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.getSqlParameterCount()).thenReturn(2);
		when(secondParameter.getSqlParameterCount()).thenReturn(3);

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");
		orGroup.getSqlParameterCount();

		orGroup.modifyStatement(7, 6, statement, null, null);
	}

	@Test
	public void getBundleUriQueryParameterNameShouldDelegateToFirstParameter()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(firstParameter.getBundleUriQueryParameterName()).thenReturn("name");

		when(supplier.get()).thenReturn(firstParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo");

		assertEquals("name", orGroup.getBundleUriQueryParameterName());

		verify(firstParameter).getBundleUriQueryParameterName();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getBundleUriQueryParameterValueShouldJoinDefinedValuesAndEscapeCommas()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(true);
		when(secondParameter.isDefined()).thenReturn(true);

		when(firstParameter.getBundleUriQueryParameterValue()).thenReturn("foo,bar");
		when(secondParameter.getBundleUriQueryParameterValue()).thenReturn("baz");

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo\\,bar,baz");

		assertEquals("foo\\,bar,baz", orGroup.getBundleUriQueryParameterValue());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getBundleUriQueryParameterValueShouldIgnoreUndefinedParameters()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(secondParameter.configure(any(), any(), any())).thenReturn(secondParameter);
		when(firstParameter.isDefined()).thenReturn(false);
		when(secondParameter.isDefined()).thenReturn(true);

		when(secondParameter.getBundleUriQueryParameterValue()).thenReturn("baz");

		when(supplier.get()).thenReturn(firstParameter, secondParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo,bar");

		assertEquals("baz", orGroup.getBundleUriQueryParameterValue());

		verify(firstParameter, never()).getBundleUriQueryParameterValue();
		verify(secondParameter).getBundleUriQueryParameterValue();
	}

	@Test
	public void getSearchParametersShouldReturnAnUnmodifiableCopy()
	{
		when(firstParameter.configure(any(), any(), any())).thenReturn(firstParameter);
		when(supplier.get()).thenReturn(firstParameter);

		orGroup.configure(new ArrayList<>(), "name", "foo");

		List<SearchQueryParameter<Resource>> parameters = orGroup.getSearchParameters();

		assertEquals(1, parameters.size());
		assertSame(firstParameter, parameters.get(0));

		try
		{
			parameters.add(secondParameter);
			fail("Expected returned list to be unmodifiable");
		}
		catch (UnsupportedOperationException expected)
		{
			// Expected.
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void configureSortShouldThrowUnsupportedOperationException()
	{
		orGroup.configureSort(new ArrayList<>(), "name");
	}

	@Test
	public void methodsSouldThrowIllegalStateExceptionIfOrGroupNotConfigured() throws Exception
	{
		assertThrows(IllegalStateException.class, () -> orGroup.isDefined());
		assertThrows(IllegalStateException.class, () -> orGroup.getFilterQuery());
		assertThrows(IllegalStateException.class, () -> orGroup.getSqlParameterCount());
		assertThrows(IllegalStateException.class, () -> orGroup.modifyStatement(0, 0, statement, null, null));
		assertThrows(IllegalStateException.class, () -> orGroup.getBundleUriQueryParameterName());
		assertThrows(IllegalStateException.class, () -> orGroup.getBundleUriQueryParameterValue());
		assertThrows(IllegalStateException.class, () -> orGroup.getSearchParameters());
	}
}