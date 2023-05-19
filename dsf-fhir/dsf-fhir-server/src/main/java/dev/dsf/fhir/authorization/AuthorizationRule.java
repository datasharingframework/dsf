package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.util.Optional;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.conf.Identity;

public interface AuthorizationRule<R extends Resource>
{
	Class<R> getResourceType();

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param identity
	 *            not <code>null</code>
	 * @param newResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if create allowed
	 */
	Optional<String> reasonCreateAllowed(Identity identity, R newResource);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param connection
	 *            not <code>null</code>
	 * @param identity
	 *            not <code>null</code>
	 * @param newResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if create allowed
	 */
	Optional<String> reasonCreateAllowed(Connection connection, Identity identity, R newResource);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param identity
	 *            not <code>null</code>
	 * @param existingResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if read allowed
	 */
	Optional<String> reasonReadAllowed(Identity identity, R existingResource);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param connection
	 *            not <code>null</code>
	 * @param identity
	 *            not <code>null</code>
	 * @param existingResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if read allowed
	 */
	Optional<String> reasonReadAllowed(Connection connection, Identity identity, R existingResource);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param identity
	 *            not <code>null</code>
	 * @param oldResource
	 *            not <code>null</code>
	 * @param newResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if update allowed
	 */
	Optional<String> reasonUpdateAllowed(Identity identity, R oldResource, R newResource);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param connection
	 *            not <code>null</code>
	 * @param identity
	 *            not <code>null</code>
	 * @param oldResource
	 *            not <code>null</code>
	 * @param newResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if update allowed
	 */
	Optional<String> reasonUpdateAllowed(Connection connection, Identity identity, R oldResource, R newResource);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param identity
	 *            not <code>null</code>
	 * @param oldResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if delete allowed
	 */
	Optional<String> reasonDeleteAllowed(Identity identity, R oldResource);


	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param connection
	 *            not <code>null</code>
	 * @param identity
	 *            not <code>null</code>
	 * @param oldResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if delete allowed
	 */
	Optional<String> reasonDeleteAllowed(Connection connection, Identity identity, R oldResource);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param identity
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if delete allowed
	 */
	Optional<String> reasonSearchAllowed(Identity identity);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param identity
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if delete allowed
	 */
	Optional<String> reasonHistoryAllowed(Identity identity);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param identity
	 *            not <code>null</code>
	 * @param oldResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if permanent delete allowed
	 */
	Optional<String> reasonPermanentDeleteAllowed(Identity identity, R oldResource);

	/**
	 * Override this method for non default behavior. Default: Not allowed.
	 *
	 * @param connection
	 *            not <code>null</code>
	 * @param identity
	 *            not <code>null</code>
	 * @param oldResource
	 *            not <code>null</code>
	 * @return Reason as String in {@link Optional#of(Object)} if permanent delete allowed
	 */
	Optional<String> reasonPermanentDeleteAllowed(Connection connection, Identity identity, R oldResource);
}