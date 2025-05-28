package dev.dsf.bpe.v2.variables;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import dev.dsf.bpe.v2.constants.BpmnExecutionVariables;

/**
 * Specifies a list of communication targets for FHIR Task resources.
 *
 * @see BpmnExecutionVariables#TARGETS
 * @see Variables#createTargets(List)
 * @see Variables#createTargets(Target...)
 * @see Target
 */
public interface Targets
{
	/**
	 * @return not <code>null</code>
	 */
	List<Target> getEntries();

	/**
	 * Removes targets base on the given {@link Target}s endpoint identifier value.
	 *
	 * @param target
	 * @return new {@link Targets} object
	 * @see Target#getEndpointIdentifierValue()
	 */
	Targets removeByEndpointIdentifierValue(Target target);

	/**
	 * Removes targets base on the given endpoint identifier value.
	 *
	 * @param targetEndpointIdentifierValue
	 * @return new {@link Targets} object
	 */
	Targets removeByEndpointIdentifierValue(String targetEndpointIdentifierValue);

	/**
	 * Removes targets base on the given endpoint identifier values.
	 *
	 * @param targetEndpointIdentifierValues
	 * @return new {@link Targets} object
	 */
	Targets removeAllByEndpointIdentifierValue(Collection<String> targetEndpointIdentifierValues);

	/**
	 * @return <code>true</code> if the entries list is empty
	 */
	boolean isEmpty();

	/**
	 * @return number of target entries
	 */
	int size();

	/**
	 * @return {@link Optional} with the first element of the target entries, or {@link Optional#empty()} if
	 *         {@link Targets#isEmpty()}
	 */
	Optional<Target> getFirst();
}