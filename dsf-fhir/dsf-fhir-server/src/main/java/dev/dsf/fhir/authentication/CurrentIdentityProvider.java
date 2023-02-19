package dev.dsf.fhir.authentication;

import dev.dsf.common.auth.Identity;

@FunctionalInterface
public interface CurrentIdentityProvider
{
	Identity getCurrentIdentity();
}
