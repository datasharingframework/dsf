package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;

import org.hl7.fhir.r4.model.Resource;

public interface HtmlGenerator<R extends Resource>
{
	/**
	 * @return the resource type supported by this generator
	 */
	Class<R> getResourceType();

	/**
	 * @param basePath
	 *            the applications base base, e.g. /fhir/
	 * @param resourceUri
	 *            not <code>null</code>
	 * @param resource
	 *            the resource, not <code>null</code>
	 * @param out
	 *            the outputStreamWriter, not <code>null</code>
	 * @throws IOException
	 */
	void writeHtml(String basePath, URI resourceUri, R resource, OutputStreamWriter out) throws IOException;

	/**
	 * @param basePath
	 *            the applications base base, e.g. /fhir/
	 * @param resourceUri
	 *            not <code>null</code>
	 * @param resource
	 *            not <code>null</code>
	 * @return <code>true</code> if this HtmlGenerator supports the given <b>resource</b> for the given <b>uri</b>
	 */
	boolean isResourceSupported(String basePath, URI resourceUri, Resource resource);
}
