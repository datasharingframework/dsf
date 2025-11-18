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
package dev.dsf.bpe.v2.client.dsf;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;

import jakarta.ws.rs.core.MediaType;

public interface BasicDsfClient extends PreferReturnResource
{
	void delete(Class<? extends Resource> resourceClass, String id);

	void deleteConditionaly(Class<? extends Resource> resourceClass, Map<String, List<String>> criteria);

	void deletePermanently(Class<? extends Resource> resourceClass, String id);

	Resource read(String resourceTypeName, String id);

	/**
	 * @param <R>
	 * @param resourceType
	 *            not <code>null</code>
	 * @param id
	 *            not <code>null</code>
	 * @return
	 */
	<R extends Resource> R read(Class<R> resourceType, String id);

	/**
	 * Uses If-None-Match and If-Modified-Since Headers based on the version and lastUpdated values in <b>oldValue</b>
	 * to check if the resource has been modified.
	 *
	 * @param <R>
	 * @param oldValue
	 *            not <code>null</code>
	 * @return oldValue (same object) if server send 304 - Not Modified, else value returned from server
	 */
	<R extends Resource> R read(R oldValue);

	<R extends Resource> boolean exists(Class<R> resourceType, String id);

	/**
	 * @param id
	 *            not <code>null</code>
	 * @param mediaType
	 *            not <code>null</code>
	 * @return {@link BinaryInputStream} needs to be closed
	 */
	BinaryInputStream readBinary(String id, MediaType mediaType);

	/**
	 * @param id
	 *            not <code>null</code>
	 * @param mediaType
	 *            not <code>null</code>
	 * @param rangeStart
	 *            <code>null</code> if suffix range (<b>rangeEndInclusive</b> <code>&lt;0</code>), else <code>>=0</code>
	 * @param rangeEndInclusive
	 *            <code>null</code> if range from <b>rangeStart</b> to end of file, <code>&lt;0</code> if suffix range
	 *            (<b>rangeStart</b> <code>null</code>), <code>>=rangeStart</code> for range end
	 * @return {@link BinaryInputStream} needs to be closed
	 */
	default BinaryInputStream readBinary(String id, MediaType mediaType, Long rangeStart, Long rangeEndInclusive)
	{
		return readBinary(id, mediaType, rangeStart, rangeEndInclusive, null);
	}

	/**
	 * @param id
	 *            not <code>null</code>
	 * @param mediaType
	 *            not <code>null</code>
	 * @param rangeStart
	 *            <code>null</code> if suffix range (<b>rangeEndInclusive</b> <code>&lt;0</code>), else <code>>=0</code>
	 * @param rangeEndInclusive
	 *            <code>null</code> if range from <b>rangeStart</b> to end of file, <code>&lt;0</code> if suffix range
	 *            (<b>rangeStart</b> <code>null</code>), <code>>=rangeStart</code> for range end
	 * @param additionalHeaders
	 *            may be <code>null</code>, use to set values of headers like "If-Unmodified-Since", "If-Match" and
	 *            "If-Range"
	 * @return {@link BinaryInputStream} needs to be closed
	 */
	BinaryInputStream readBinary(String id, MediaType mediaType, Long rangeStart, Long rangeEndInclusive,
			Map<String, String> additionalHeaders);

	/**
	 * @param resourceTypeName
	 *            not <code>null</code>
	 * @param id
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @return {@link Resource}
	 */
	Resource read(String resourceTypeName, String id, String version);

	<R extends Resource> R read(Class<R> resourceType, String id, String version);

	<R extends Resource> boolean exists(Class<R> resourceType, String id, String version);

	/**
	 * @param id
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @param mediaType
	 *            not <code>null</code>
	 * @return {@link BinaryInputStream} needs to be closed
	 */
	BinaryInputStream readBinary(String id, String version, MediaType mediaType);

	/**
	 * @param id
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @param mediaType
	 *            not <code>null</code>
	 * @param rangeStart
	 *            <code>null</code> if suffix range (<b>rangeEndInclusive</b> <code>&lt;0</code>), else <code>>=0</code>
	 * @param rangeEndInclusive
	 *            <code>null</code> if range from <b>rangeStart</b> to end of file, <code>&lt;0</code> if suffix range
	 *            (<b>rangeStart</b> <code>null</code>), <code>>=rangeStart</code> for range end
	 * @return {@link BinaryInputStream} needs to be closed
	 */
	default BinaryInputStream readBinary(String id, String version, MediaType mediaType, Long rangeStart,
			Long rangeEndInclusive)
	{
		return readBinary(id, version, mediaType, rangeStart, rangeEndInclusive, null);
	}

	/**
	 * @param id
	 *            not <code>null</code>
	 * @param version
	 *            not <code>null</code>
	 * @param mediaType
	 *            not <code>null</code>
	 * @param rangeStart
	 *            <code>null</code> if suffix range (<b>rangeEndInclusive</b> <code>&lt;0</code>), else <code>>=0</code>
	 * @param rangeEndInclusive
	 *            <code>null</code> if range from <b>rangeStart</b> to end of file, <code>&lt;0</code> if suffix range
	 *            (<b>rangeStart</b> <code>null</code>), <code>>=rangeStart</code> for range end
	 * @param additionalHeaders
	 *            may be <code>null</code>, use to set values of headers like "If-Unmodified-Since", "If-Match" and
	 *            "If-Range"
	 * @return {@link BinaryInputStream} needs to be closed
	 */
	BinaryInputStream readBinary(String id, String version, MediaType mediaType, Long rangeStart,
			Long rangeEndInclusive, Map<String, String> additionalHeaders);

	boolean exists(IdType resourceTypeIdVersion);

	/**
	 * @param resourceType
	 *            not <code>null</code>
	 * @param parameters
	 *            may be <code>null</code>
	 * @return
	 */
	Bundle search(Class<? extends Resource> resourceType, Map<String, List<String>> parameters);

	/**
	 * Send "Prefer: handling=strict" header
	 *
	 * @param resourceType
	 *            not <code>null</code>
	 * @param parameters
	 *            may be <code>null</code>
	 * @return search result
	 */
	Bundle searchWithStrictHandling(Class<? extends Resource> resourceType, Map<String, List<String>> parameters);

	CapabilityStatement getConformance();

	StructureDefinition generateSnapshot(String url);

	StructureDefinition generateSnapshot(StructureDefinition differential);

	default Bundle history()
	{
		return history(null);
	}

	default Bundle history(int page, int count)
	{
		return history(null, page, count);
	}

	default Bundle history(Class<? extends Resource> resourceType)
	{
		return history(resourceType, null);
	}

	default Bundle history(Class<? extends Resource> resourceType, int page, int count)
	{
		return history(resourceType, null, page, count);
	}

	default Bundle history(Class<? extends Resource> resourceType, String id)
	{
		return history(resourceType, id, Integer.MIN_VALUE, Integer.MIN_VALUE);
	}

	Bundle history(Class<? extends Resource> resourceType, String id, int page, int count);
}
