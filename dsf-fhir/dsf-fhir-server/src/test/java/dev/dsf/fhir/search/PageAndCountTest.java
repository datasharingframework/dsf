package dev.dsf.fhir.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class PageAndCountTest
{
	private static final int DEFAULT_PAGE_COUNT = 20;

	@Test
	public void testSingle() throws Exception
	{
		PageAndCount pC = PageAndCount.single();
		assertNotNull(pC);
		assertEquals(1, pC.getCount());
		assertEquals(1, pC.getPage());
		assertEquals(" LIMIT 1", pC.getSql());

		assertTrue(pC.isLastPage(0));
		assertTrue(pC.isLastPage(1));
		assertFalse(pC.isLastPage(2));
		assertFalse(pC.isLastPage(Integer.MAX_VALUE));

		assertEquals(0, pC.getLastPage(0));
		assertEquals(1, pC.getLastPage(1));
		assertEquals(2, pC.getLastPage(2));
		assertEquals(Integer.MAX_VALUE, pC.getLastPage(Integer.MAX_VALUE));

		assertTrue(pC.isCountOnly(0));
		assertFalse(pC.isCountOnly(1));
		assertFalse(pC.isCountOnly(2));
		assertFalse(pC.isCountOnly(Integer.MAX_VALUE));
	}

	@Test
	public void testExists() throws Exception
	{
		PageAndCount pC = PageAndCount.exists();
		assertNotNull(pC);
		assertEquals(0, pC.getCount());
		assertEquals(0, pC.getPage());
		assertEquals(" LIMIT 0", pC.getSql());

		assertTrue(pC.isLastPage(0));
		assertTrue(pC.isLastPage(1));
		assertTrue(pC.isLastPage(2));
		assertTrue(pC.isLastPage(Integer.MAX_VALUE));

		assertEquals(0, pC.getLastPage(0));
		assertEquals(0, pC.getLastPage(1));
		assertEquals(0, pC.getLastPage(2));
		assertEquals(0, pC.getLastPage(Integer.MAX_VALUE));

		assertTrue(pC.isCountOnly(0));
		assertTrue(pC.isCountOnly(1));
		assertTrue(pC.isCountOnly(2));
		assertTrue(pC.isCountOnly(Integer.MAX_VALUE));
	}

	@Test
	public void testPage1Count0() throws Exception
	{
		PageAndCount pC = PageAndCount.from(1, 0);
		assertNotNull(pC);
		assertEquals(0, pC.getCount());
		assertEquals(1, pC.getPage());
		assertEquals(" LIMIT 0", pC.getSql());

		assertTrue(pC.isLastPage(0));
		assertTrue(pC.isLastPage(1));
		assertTrue(pC.isLastPage(2));
		assertTrue(pC.isLastPage(Integer.MAX_VALUE));

		assertEquals(0, pC.getLastPage(0));
		assertEquals(0, pC.getLastPage(1));
		assertEquals(0, pC.getLastPage(2));
		assertEquals(0, pC.getLastPage(Integer.MAX_VALUE));

		assertTrue(pC.isCountOnly(0));
		assertTrue(pC.isCountOnly(1));
		assertTrue(pC.isCountOnly(2));
		assertTrue(pC.isCountOnly(Integer.MAX_VALUE));
	}

	@Test
	public void testPage1Count20() throws Exception
	{
		PageAndCount pC = PageAndCount.from(1, 20);
		assertNotNull(pC);
		assertEquals(20, pC.getCount());
		assertEquals(1, pC.getPage());
		assertEquals(" LIMIT 20", pC.getSql());

		assertTrue(pC.isLastPage(0));
		assertTrue(pC.isLastPage(1));
		assertTrue(pC.isLastPage(2));
		assertTrue(pC.isLastPage(19));
		assertTrue(pC.isLastPage(20));
		assertFalse(pC.isLastPage(21));
		assertFalse(pC.isLastPage(Integer.MAX_VALUE));

		assertEquals(0, pC.getLastPage(0));
		assertEquals(1, pC.getLastPage(1));
		assertEquals(1, pC.getLastPage(2));
		assertEquals(1, pC.getLastPage(19));
		assertEquals(1, pC.getLastPage(20));
		assertEquals(2, pC.getLastPage(21));
		assertEquals(107374183, pC.getLastPage(Integer.MAX_VALUE));

		assertTrue(pC.isCountOnly(0));
		assertFalse(pC.isCountOnly(1));
		assertFalse(pC.isCountOnly(2));
		assertFalse(pC.isCountOnly(19));
		assertFalse(pC.isCountOnly(20));
		assertFalse(pC.isCountOnly(21));
		assertFalse(pC.isCountOnly(Integer.MAX_VALUE));
	}

	@Test
	public void testPage2Count20() throws Exception
	{
		PageAndCount pC = PageAndCount.from(2, 20);
		assertNotNull(pC);
		assertEquals(20, pC.getCount());
		assertEquals(2, pC.getPage());
		assertEquals(" LIMIT 20 OFFSET 20", pC.getSql());

		assertTrue(pC.isLastPage(0));
		assertTrue(pC.isLastPage(1));
		assertTrue(pC.isLastPage(2));
		assertTrue(pC.isLastPage(19));
		assertTrue(pC.isLastPage(20));
		assertTrue(pC.isLastPage(21));
		assertTrue(pC.isLastPage(39));
		assertTrue(pC.isLastPage(40));
		assertFalse(pC.isLastPage(41));
		assertFalse(pC.isLastPage(Integer.MAX_VALUE));

		assertEquals(0, pC.getLastPage(0));
		assertEquals(1, pC.getLastPage(1));
		assertEquals(1, pC.getLastPage(2));
		assertEquals(1, pC.getLastPage(19));
		assertEquals(1, pC.getLastPage(20));
		assertEquals(2, pC.getLastPage(21));
		assertEquals(2, pC.getLastPage(39));
		assertEquals(2, pC.getLastPage(40));
		assertEquals(3, pC.getLastPage(41));
		assertEquals(107374183, pC.getLastPage(Integer.MAX_VALUE));

		assertTrue(pC.isCountOnly(0));
		assertTrue(pC.isCountOnly(1));
		assertTrue(pC.isCountOnly(2));
		assertTrue(pC.isCountOnly(19));
		assertTrue(pC.isCountOnly(20));
		assertFalse(pC.isCountOnly(21));
		assertFalse(pC.isCountOnly(39));
		assertFalse(pC.isCountOnly(40));
		assertFalse(pC.isCountOnly(41));
		assertFalse(pC.isCountOnly(Integer.MAX_VALUE));
	}

	@Test
	public void testFromQueryParameters() throws Exception
	{
		PageAndCount pC_m1_m1 = PageAndCount.from(Map.of("_page", List.of("-1"), "_count", List.of("-1")),
				DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_m1_m1, 0, 0);
		PageAndCount pC_0_0 = PageAndCount.from(Map.of("_page", List.of("0"), "_count", List.of("0")),
				DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_0_0, 0, 0);
		PageAndCount pC_1_1 = PageAndCount.from(Map.of("_page", List.of("1"), "_count", List.of("1")),
				DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_1_1, 1, 1);
		PageAndCount pC_2_2 = PageAndCount.from(Map.of("_page", List.of("2"), "_count", List.of("2")),
				DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_2_2, 2, 2);
		PageAndCount pC_1_19 = PageAndCount.from(Map.of("_page", List.of("1"), "_count", List.of("19")),
				DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_1_19, 1, 19);
		PageAndCount pC_1_20 = PageAndCount.from(Map.of("_page", List.of("1"), "_count", List.of("20")),
				DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_1_20, 1, 20);
		PageAndCount pC_1_21 = PageAndCount.from(Map.of("_page", List.of("1"), "_count", List.of("21")),
				DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_1_21, 1, 21);

		PageAndCount pC_m1_ = PageAndCount.from(Map.of("_page", List.of("-1")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_m1_, 0, DEFAULT_PAGE_COUNT);
		PageAndCount pC_0_ = PageAndCount.from(Map.of("_page", List.of("0")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_0_, 0, DEFAULT_PAGE_COUNT);
		PageAndCount pC_1_ = PageAndCount.from(Map.of("_page", List.of("1")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_1_, 1, DEFAULT_PAGE_COUNT);
		PageAndCount pC_2_ = PageAndCount.from(Map.of("_page", List.of("2")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_2_, 2, DEFAULT_PAGE_COUNT);

		PageAndCount pC__m1 = PageAndCount.from(Map.of("_count", List.of("-1")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC__m1, 1, 0);
		PageAndCount pC__0 = PageAndCount.from(Map.of("_count", List.of("0")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC__0, 1, 0);
		PageAndCount pC__1 = PageAndCount.from(Map.of("_count", List.of("1")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC__1, 1, 1);
		PageAndCount pC__2 = PageAndCount.from(Map.of("_count", List.of("2")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC__2, 1, 2);
		PageAndCount pC__19 = PageAndCount.from(Map.of("_count", List.of("19")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC__19, 1, 19);
		PageAndCount pC__20 = PageAndCount.from(Map.of("_count", List.of("20")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC__20, 1, 20);
		PageAndCount pC__21 = PageAndCount.from(Map.of("_count", List.of("21")), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC__21, 1, 21);

		PageAndCount pC_max_max = PageAndCount.from(Map.of("_page", List.of(String.valueOf(Integer.MAX_VALUE)),
				"_count", List.of(String.valueOf(Integer.MAX_VALUE))), DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_max_max, 1, DEFAULT_PAGE_COUNT);

		PageAndCount pC_s_30 = PageAndCount.from(Map.of("_page", List.of("foo"), "_count", List.of("30")),
				DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_s_30, 1, 30);
		PageAndCount pC_1_s = PageAndCount.from(Map.of("_page", List.of("2"), "_count", List.of("bar")),
				DEFAULT_PAGE_COUNT);
		assertPageAndCount(pC_1_s, 2, DEFAULT_PAGE_COUNT);
	}

	private void assertPageAndCount(PageAndCount pC, int expectedPage, int expectedCount)
	{
		assertNotNull(pC);
		assertEquals("page", expectedPage, pC.getPage());
		assertEquals("count", expectedCount, pC.getCount());
	}
}
