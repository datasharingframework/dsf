package dev.dsf.fhir.search;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class SearchQueryRevIncludeParameterFactory
{
	private final Supplier<SearchQueryRevIncludeParameter> revIncludeSupplier;
	private final List<String> revIncludeParameterValues = new ArrayList<>();

	/**
	 * @param revIncludeSupplier
	 *            may be <code>null</code>, not <code>null</code> if param <b>revIncludeParameterValues</b> not
	 *            <code>null</code>
	 * @param revIncludeParameterValues
	 *            may be <code>null</code>, not <code>null</code> if param <b>revIncludeSupplier</b> not
	 *            <code>null</code>
	 */
	public SearchQueryRevIncludeParameterFactory(Supplier<SearchQueryRevIncludeParameter> revIncludeSupplier,
			List<String> revIncludeParameterValues)
	{
		this.revIncludeSupplier = revIncludeSupplier;
		if (revIncludeParameterValues != null)
			this.revIncludeParameterValues.addAll(revIncludeParameterValues);

		if (revIncludeSupplier != null ^ revIncludeParameterValues != null)
			throw new IllegalArgumentException(
					"includeSupplier and includeParameterValues must both be null or not null");
	}

	public Stream<String> getRevIncludeParameterValues()
	{
		return revIncludeParameterValues.stream();
	}

	public boolean isIncludeParameter()
	{
		return revIncludeSupplier != null && revIncludeParameterValues != null;
	}

	public SearchQueryRevIncludeParameter createQueryRevIncludeParameter()
	{
		return revIncludeSupplier.get();
	}
}
