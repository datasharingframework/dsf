package dev.dsf.pseudonymization.domain.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.dsf.openehr.model.structure.RowElement;
import dev.dsf.pseudonymization.domain.MdatContainer;

public class OpenEhrMdatContainer implements MdatContainer
{
	private final List<RowElement> elements = new ArrayList<>();

	@JsonCreator
	public OpenEhrMdatContainer(@JsonProperty("elements") Collection<? extends RowElement> elements)
	{
		if (elements != null)
			this.elements.addAll(elements);
	}

	public List<RowElement> getElements()
	{
		return Collections.unmodifiableList(elements);
	}
}
