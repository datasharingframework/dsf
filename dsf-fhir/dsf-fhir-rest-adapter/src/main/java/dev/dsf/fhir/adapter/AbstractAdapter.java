package dev.dsf.fhir.adapter;

import java.util.Set;
import java.util.function.Supplier;

import ca.uhn.fhir.parser.IParser;
import jakarta.ws.rs.core.MediaType;

public abstract class AbstractAdapter
{
	public static final String PRETTY = "pretty";
	public static final String SUMMARY = "summary";

	protected IParser getParser(MediaType mediaType, Supplier<IParser> parserFactor)
	{
		/* Parsers are not guaranteed to be thread safe */
		IParser p = parserFactor.get();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);

		if (mediaType != null)
		{
			if ("true".equals(mediaType.getParameters().getOrDefault(PRETTY, "false")))
				p.setPrettyPrint(true);

			switch (mediaType.getParameters().getOrDefault(SUMMARY, "false"))
			{
				case "true" -> p.setSummaryMode(true);
				case "text" -> p.setEncodeElements(Set.of("*.text", "*.id", "*.meta", "*.(mandatory)"));
				case "data" -> p.setSuppressNarratives(true);
			}
		}

		return p;
	}
}
