package dev.dsf.fhir.dao.command;

import org.hl7.fhir.r4.model.Bundle;

import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.dao.exception.BadBundleException;
import dev.dsf.fhir.prefer.PreferHandlingType;
import dev.dsf.fhir.prefer.PreferReturnType;

public interface CommandFactory
{
	/**
	 * @param bundle
	 *            not <code>null</code>
	 * @param user
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
	CommandList createCommands(Bundle bundle, User user, PreferReturnType returnType, PreferHandlingType handlingType)
			throws BadBundleException;
}