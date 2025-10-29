package dev.dsf.fhir.service;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

public interface DefaultProfileProvider
{
	/**
	 * Set the default profile if non of the {@link #getSupportedDefaultProfiles(ResourceType)} are already set.<br>
	 * <br>
	 * Does nothing if the given resource is <code>null</code>
	 *
	 * @param resource
	 *            may be <code>null</code>
	 */
	void setDefaultProfile(Resource resource);

	/**
	 * @param resourceType
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if no default profile for the given <b>resourceTyp</b> or <b>resourceTyp</b>
	 *         <code>null</code>
	 */
	Optional<String> getDefaultProfile(ResourceType resourceType);

	/**
	 * @param resourceType
	 *            may be <code>null</code>
	 * @return all supported default profiles, only the default profile or default profile and secondary default
	 *         profiles, empty if <b>resourceType</b> is <code>null</code>
	 */
	List<String> getSupportedDefaultProfiles(ResourceType resourceType);

	/**
	 * @param resourceType
	 *            may be <code>null</code>
	 * @return all supported default profiles except the default profile, empty if <b>resourceType</b> is
	 *         <code>null</code>
	 */
	List<String> getSecondaryDefaultProfiles(ResourceType resourceType);
}
