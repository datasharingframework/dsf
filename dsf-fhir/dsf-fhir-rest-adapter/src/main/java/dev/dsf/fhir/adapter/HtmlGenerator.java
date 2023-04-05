package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.hl7.fhir.r4.model.BaseResource;

public interface HtmlGenerator<R extends BaseResource>
{
	/**
	 * @return the resource type supported by this generator
	 */
	Class<R> getResourceType();

	/**
	 * @param basePath
	 *            the applications base base, e.g. /fhir/
	 * @param resource
	 *            the resource, not <code>null</code>
	 * @param out
	 *            the outputStreamWriter, not <code>null</code>
	 * @throws IOException
	 */
	void writeHtml(String basePath, R resource, OutputStreamWriter out) throws IOException;
}
