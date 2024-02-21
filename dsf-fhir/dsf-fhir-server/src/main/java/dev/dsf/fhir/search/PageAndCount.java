package dev.dsf.fhir.search;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PageAndCount
{
	private static final PageAndCount EXISTS = new PageAndCount(0, 0, 0);
	private static final PageAndCount SINGLE = new PageAndCount(1, 1, 1);

	public static PageAndCount single()
	{
		return SINGLE;
	}

	public static PageAndCount exists()
	{
		return EXISTS;
	}

	public static PageAndCount from(Map<String, List<String>> queryParameters, int defaultPageCount)
	{
		Integer page = getFirstInt(queryParameters, SearchQuery.PARAMETER_PAGE);
		Integer count = getFirstInt(queryParameters, SearchQuery.PARAMETER_COUNT);

		return new PageAndCount(page, count, defaultPageCount);
	}

	private static Integer getFirstInt(Map<String, List<String>> queryParameters, String key)
	{
		List<String> values = queryParameters.getOrDefault(key, Collections.emptyList());
		if (values.isEmpty())
			return null;
		else
		{
			// TODO control flow by exception
			try
			{
				return Integer.valueOf(values.get(0));
			}
			catch (NumberFormatException e)
			{
				return null;
			}
		}
	}

	/**
	 * @param page
	 * @param count
	 * @return
	 * @throws ArithmeticException
	 *             if <code>page * count > Integer.MAX_VALUE</code>
	 * @see Math#multiplyExact(int, int)
	 */
	public static PageAndCount from(int page, int count)
	{
		Math.multiplyExact(page, count); // throws ArithmeticException

		return new PageAndCount(page, count, count);
	}


	private final int page;
	private final int count;

	private PageAndCount(Integer page, Integer count, int defaultPageCount)
	{
		int effectivePage = page == null ? 1 : page < 0 ? 0 : page;
		int effectiveCount = count == null ? defaultPageCount : count < 0 ? 0 : count;

		// Bundle.total is an unsigned integer, so we can't access more resources than Integer.MAX_VALUE
		long testOverflow = (long) effectivePage * (long) effectiveCount;
		if (testOverflow > Integer.MAX_VALUE)
		{
			effectivePage = 1;
			effectiveCount = defaultPageCount;
		}

		this.page = effectivePage;
		this.count = effectiveCount;
	}

	public String getSql()
	{
		return " LIMIT " + count + (page > 1 ? " OFFSET " + getOffset() : "");
	}

	private int getOffset()
	{
		return (page - 1) * count;
	}

	public boolean isCountOnly(int total)
	{
		return page < 1 || count < 1 || page > getLastPage(total);
	}

	public int getPage()
	{
		return page;
	}

	public int getCount()
	{
		return count;
	}

	public boolean isLastPage(int total)
	{
		return page >= getLastPage(total);
	}

	public int getLastPage(int total)
	{
		return count < 1 ? 0 : (int) Math.ceil((double) total / count);
	}
}