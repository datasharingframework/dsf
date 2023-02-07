package dev.dsf.pseudonymization.psn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.dsf.pseudonymization.recordlinkage.MedicId;

class PseudonymWithPadding
{
	@JsonProperty
	final List<MedicId> medicIds = new ArrayList<>();

	@JsonProperty
	final String padding;

	PseudonymWithPadding(int paddingLength, Collection<? extends MedicId> medicIds)
	{
		this(IntStream.range(0, paddingLength).mapToObj(i -> " ").collect(Collectors.joining()), medicIds);
	}

	@JsonCreator
	PseudonymWithPadding(@JsonProperty("padding") String padding,
			@JsonProperty("medicIds") Collection<? extends MedicId> medicIds)
	{
		this.padding = padding;
		if (medicIds != null)
			this.medicIds.addAll(medicIds);
	}
}