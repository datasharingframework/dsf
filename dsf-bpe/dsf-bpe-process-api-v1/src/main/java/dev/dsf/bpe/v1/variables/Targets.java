package dev.dsf.bpe.v1.variables;

import java.util.Collection;
import java.util.List;

import org.camunda.bpm.model.cmmn.impl.instance.TargetImpl;

public interface Targets
{
	List<Target> getEntries();

	/**
	 * Removes targets base on the given {@link Target}s endpoint identifier value.
	 *
	 * @param target
	 * @return new {@link Targets} object
	 * @see TargetImpl#getEndpointIdentifierValue()
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

	boolean isEmpty();
}