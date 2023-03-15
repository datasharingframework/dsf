package dev.dsf.fhir.authentication;

import dev.dsf.common.auth.conf.Identity;

@FunctionalInterface
public interface CurrentIdentityProvider
{
	Identity getCurrentIdentity();
}
