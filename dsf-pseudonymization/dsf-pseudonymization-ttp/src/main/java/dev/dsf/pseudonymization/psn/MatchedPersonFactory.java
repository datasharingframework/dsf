package dev.dsf.pseudonymization.psn;

import java.util.List;

import dev.dsf.pseudonymization.domain.PseudonymizedPerson;
import dev.dsf.pseudonymization.recordlinkage.MatchedPerson;
import dev.dsf.pseudonymization.recordlinkage.MedicId;
import dev.dsf.pseudonymization.recordlinkage.Person;

@FunctionalInterface
public interface MatchedPersonFactory<P extends Person>
{
	MatchedPerson<P> create(PseudonymizedPerson person, List<MedicId> medicIds);
}
