/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.webservice.specification;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.webservice.base.BasicService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public interface BasicResourceService<R extends Resource> extends BasicService
{
	/**
	 * standard and conditional create
	 *
	 * @param resource
	 *            not <code>null</code>
	 * @param uri
	 *            not <code>null</code>
	 * @param headers
	 *            not <code>null</code>
	 * @return {@link Response} defined in
	 *         <a href="https://www.hl7.org/fhir/http.html#create">https://www.hl7.org/fhir/http.html#create</a>
	 */
	Response create(R resource, UriInfo uri, HttpHeaders headers);

	/**
	 * read by id
	 *
	 * @param id
	 *            not <code>null</code>
	 * @param uri
	 *            not <code>null</code>
	 * @param headers
	 *            not <code>null</code>
	 * @return {@link Response} defined in
	 *         <a href="https://www.hl7.org/fhir/http.html#read">https://www.hl7.org/fhir/http.html#read</a>
	 */
	Response read(String id, UriInfo uri, HttpHeaders headers);

	/**
	 * read by id and version
	 *
	 * @param id
	 *            not <code>null</code>
	 * @param version
	 *            {@code >0}
	 * @param uri
	 *            not <code>null</code>
	 * @param headers
	 *            not <code>null</code>
	 * @return {@link Response} defined in
	 *         <a href="https://www.hl7.org/fhir/http.html#vread">https://www.hl7.org/fhir/http.html#vread</a>
	 */
	Response vread(String id, long version, UriInfo uri, HttpHeaders headers);

	Response history(UriInfo uri, HttpHeaders headers);

	Response history(String id, UriInfo uri, HttpHeaders headers);

	/**
	 * standard update
	 *
	 * @param id
	 *            not <code>null</code>
	 * @param resource
	 *            not <code>null</code>
	 * @param uri
	 *            not <code>null</code>
	 * @param headers
	 *            not <code>null</code>
	 * @return {@link Response} defined in
	 *         <a href="https://www.hl7.org/fhir/http.html#update">https://www.hl7.org/fhir/http.html#update</a>
	 */
	Response update(String id, R resource, UriInfo uri, HttpHeaders headers);

	/**
	 * conditional update
	 *
	 * @param resource
	 *            not <code>null</code>
	 * @param uri
	 *            not <code>null</code>
	 * @param headers
	 *            not <code>null</code>
	 * @return {@link Response} defined in
	 *         <a href="https://www.hl7.org/fhir/http.html#update">https://www.hl7.org/fhir/http.html#update</a>
	 */
	Response update(R resource, UriInfo uri, HttpHeaders headers);

	/**
	 * standard delete
	 *
	 * @param id
	 *            not <code>null</code>
	 * @param uri
	 *            not <code>null</code>
	 * @param headers
	 *            not <code>null</code>
	 * @return {@link Response} defined in
	 *         <a href="https://www.hl7.org/fhir/http.html#delete">https://www.hl7.org/fhir/http.html#delete</a>
	 */
	Response delete(String id, UriInfo uri, HttpHeaders headers);

	/**
	 * conditional delete
	 *
	 * @param uri
	 *            not <code>null</code>
	 * @param headers
	 *            not <code>null</code>
	 * @return {@link Response} defined in
	 *         <a href="https://www.hl7.org/fhir/http.html#delete">https://www.hl7.org/fhir/http.html#delete</a>
	 */
	Response delete(UriInfo uri, HttpHeaders headers);

	/**
	 * search by request parameter
	 *
	 * @param uri
	 *            not <code>null</code>
	 * @param headers
	 *            not <code>null</code>
	 * @return {@link Response} defined in
	 *         <a href="https://www.hl7.org/fhir/http.html#search">https://www.hl7.org/fhir/http.html#search</a>
	 */
	Response search(UriInfo uri, HttpHeaders headers);

	Response deletePermanently(String deletePath, String id, UriInfo uri, HttpHeaders headers);
}
