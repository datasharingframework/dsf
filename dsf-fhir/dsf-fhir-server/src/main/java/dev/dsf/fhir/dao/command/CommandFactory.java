package dev.dsf.fhir.dao.command;

import org.hl7.fhir.r4.model.Bundle;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.exception.BadBundleException;
import dev.dsf.fhir.prefer.PreferHandlingType;
import dev.dsf.fhir.prefer.PreferReturnType;

public interface CommandFactory
{
	/**
	 * @param bundle
	 *            not <code>null</code>
	 * @param identity
	 *            not <code>null</code>
	 * @param returnType
	 *            not <code>null</code>
	 * @param handlingType
	 *            not <code>null</code>
	 *
	 * @return {@link CommandList} with individual commands from each entry contained in the bundle
	 * @throws BadBundleException
	 *             if the bundle could not be processed because of wrong bundle type or other errors
	 */
	default CommandList createCommands(Bundle bundle, Identity identity, PreferReturnType returnType,
			PreferHandlingType handlingType) throws BadBundleException
	{
		return createCommands(bundle, identity, returnType, handlingType, true);
	}

	/**
	 * @param bundle
	 *            not <code>null</code>
	 * @param identity
	 *            not <code>null</code>
	 * @param returnType
	 *            not <code>null</code>
	 * @param handlingType
	 *            not <code>null</code>
	 * @param enableValidation
	 *            set to <code>false</code> to disable FHIR resource validation, useful for initial data loader with
	 *            internal bundle
	 *
	 * @return {@link CommandList} with individual commands from each entry contained in the bundle
	 * @throws BadBundleException
	 *             if the bundle could not be processed because of wrong bundle type or other errors
	 */
	CommandList createCommands(Bundle bundle, Identity identity, PreferReturnType returnType,
			PreferHandlingType handlingType, boolean enableValidation);
}