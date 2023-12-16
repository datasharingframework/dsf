package dev.dsf.fhir.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.parameters.SearchQuerySortParameter;

public final class SearchQueryParameterFactory<R extends Resource>
{
	private final String name;
	private final List<String> nameModifiers = new ArrayList<>();
	private final Supplier<SearchQueryParameter<R>> supplier;
	private final Supplier<SearchQueryIncludeParameter<R>> includeSupplier;
	private final List<String> includeParameterValues = new ArrayList<>();

	/**
	 * @param name
	 *            not <code>null</code>
	 * @param supplier
	 *            not <code>null</code>
	 */
	public SearchQueryParameterFactory(String name, Supplier<SearchQueryParameter<R>> supplier)
	{
		this(name, supplier, null);
	}

	/**
	 * @param name
	 *            not <code>null</code>
	 * @param supplier
	 *            not <code>null</code>
	 * @param nameModifiers
	 *            may be <code>null</code>
	 */
	public SearchQueryParameterFactory(String name, Supplier<SearchQueryParameter<R>> supplier,
			List<String> nameModifiers)
	{
		this(name, supplier, nameModifiers, null, null);
	}

	/**
	 * @param name
	 *            not <code>null</code>
	 * @param supplier
	 *            not <code>null</code>
	 * @param nameModifiers
	 *            may be <code>null</code>
	 * @param includeSupplier
	 *            may be <code>null</code>, not <code>null</code> if param <b>includeParameterValues</b> not
	 *            <code>null</code>
	 * @param includeParameterValues
	 *            may be <code>null</code>, not <code>null</code> if param <b>includeSupplier</b> not <code>null</code>
	 */
	public SearchQueryParameterFactory(String name, Supplier<SearchQueryParameter<R>> supplier,
			List<String> nameModifiers, Supplier<SearchQueryIncludeParameter<R>> includeSupplier,
			List<String> includeParameterValues)
	{
		this.name = Objects.requireNonNull(name, "name");
		this.supplier = Objects.requireNonNull(supplier, "supplier");

		if (nameModifiers != null)
			this.nameModifiers.addAll(nameModifiers);

		this.includeSupplier = includeSupplier;
		if (includeParameterValues != null)
			this.includeParameterValues.addAll(includeParameterValues);

		if (includeSupplier != null ^ includeParameterValues != null)
			throw new IllegalArgumentException(
					"includeSupplier and includeParameterValues must both be null or not null");
	}

	public String getName()
	{
		return name;
	}

	public Stream<String> getNameAndModifiedNames()
	{
		return Stream.concat(Stream.of(name), nameModifiers.stream().map(m -> name + m));
	}

	public Stream<String> getSortNames()
	{
		return Stream.of(name, "+" + name, "-" + name);
	}

	public Stream<String> getIncludeParameterValues()
	{
		return includeParameterValues.stream();
	}

	public SearchQueryParameter<R> createQueryParameter()
	{
		return supplier.get();
	}

	public SearchQuerySortParameter createQuerySortParameter()
	{
		return supplier.get();
	}

	public boolean isIncludeParameter()
	{
		return includeSupplier != null && includeParameterValues != null;
	}

	public SearchQueryIncludeParameter<R> createQueryIncludeParameter()
	{
		return includeSupplier.get();
	}
}
