package dev.dsf.bpe.v1.variables;

import dev.dsf.bpe.v1.constants.BpmnExecutionVariables;

/**
 * Specifies a communication target for FHIR Task resources.
 *
 * @see BpmnExecutionVariables#TARGET
 * @see Variables#createTarget(String, String, String, String)
 * @see Variables#createTarget(String, String, String)
 * @see Targets
 */
public interface Target
{
	/**
	 * @return not <code>null</code>
	 */
	String getOrganizationIdentifierValue();

	/**
	 * @return not <code>null</code>
	 */
	String getEndpointIdentifierValue();

	/**
	 * @return not <code>null</code>
	 */
	String getEndpointUrl();

	/**
	 * @return may be <code>null</code>
	 */
	String getCorrelationKey();
}