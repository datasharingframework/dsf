package dev.dsf.fhir.client;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;

import jakarta.ws.rs.core.MediaType;

public interface BasicFhirWebserviceClient extends PreferReturnResource
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

	Bundle search(Class<? extends Resource> resourceType, Map<String, List<String>> parameters);

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
