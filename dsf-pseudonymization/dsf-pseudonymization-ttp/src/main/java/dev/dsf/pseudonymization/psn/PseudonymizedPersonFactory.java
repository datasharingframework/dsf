package dev.dsf.pseudonymization.psn;

import dev.dsf.pseudonymization.domain.PseudonymizedPerson;
import dev.dsf.pseudonymization.recordlinkage.MatchedPerson;
import dev.dsf.pseudonymization.recordlinkage.Person;

@FunctionalInterface
public interface PseudonymizedPersonFactory<P extends Person, PP extends PseudonymizedPerson>
{
	PP create(MatchedPerson<P> person, String pseudonym);
}
