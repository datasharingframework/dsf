package dev.dsf.bpe.v1.variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TargetsImpl implements Targets
{
	private final List<TargetImpl> entries = new ArrayList<>();

	@JsonCreator
	public TargetsImpl(@JsonProperty("entries") List<? extends TargetImpl> targets)
	{
		if (targets != null)
			this.entries.addAll(targets);
	}

	@JsonProperty("entries")
	@Override
	public List<Target> getEntries()
	{
		return Collections.unmodifiableList(entries);
	}

	@Override
	public Targets removeByEndpointIdentifierValue(Target target)
	{
		if (target == null)
			return new TargetsImpl(entries);

		return removeByEndpointIdentifierValue(target.getEndpointIdentifierValue());
	}

	@Override
	public Targets removeByEndpointIdentifierValue(String targetEndpointIdentifierValue)
	{
		if (targetEndpointIdentifierValue == null)
			return new TargetsImpl(entries);

		return new TargetsImpl(
				entries.stream().filter(t -> !targetEndpointIdentifierValue.equals(t.getEndpointIdentifierValue()))
						.collect(Collectors.toList()));
	}

	@Override
	public Targets removeAllByEndpointIdentifierValue(Collection<String> targetEndpointIdentifierValues)
	{
		if (targetEndpointIdentifierValues == null || targetEndpointIdentifierValues.isEmpty())
			return new TargetsImpl(entries);

		return new TargetsImpl(
				entries.stream().filter(t -> !targetEndpointIdentifierValues.contains(t.getEndpointIdentifierValue()))
						.collect(Collectors.toList()));
	}

	@JsonIgnore
	@Override
	public boolean isEmpty()
	{
		return entries.isEmpty();
	}

	@Override
	public String toString()
	{
		return "TargetsImpl [entries=" + entries + "]";
	}
}
