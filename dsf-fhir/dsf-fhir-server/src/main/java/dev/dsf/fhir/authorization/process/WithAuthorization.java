package dev.dsf.fhir.authorization.process;

import org.hl7.fhir.r4.model.Coding;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.PractitionerIdentity;

public interface WithAuthorization
{
	Coding getProcessAuthorizationCode();

	boolean matches(Coding processAuthorizationCode);

	String getPractitionerRoleSystem();

	String getPractitionerRoleCode();

	default boolean needsPractitionerRole()
	{
		return getPractitionerRoleSystem() != null && getPractitionerRoleCode() != null;
	}

	default boolean hasPractitionerRole(Identity identity)
	{
		return identity instanceof PractitionerIdentity p
				&& p.getPractionerRoles().stream().anyMatch(c -> getPractitionerRoleSystem().equals(c.getSystem())
						&& getPractitionerRoleCode().equals(c.getCode()));
	}


	default boolean practitionerRoleMatches(Coding coding)
	{
		return coding != null && coding.hasSystem() && coding.hasCode()
				&& getPractitionerRoleSystem().equals(coding.getSystem())
				&& getPractitionerRoleCode().equals(coding.getCode());
	}
}
