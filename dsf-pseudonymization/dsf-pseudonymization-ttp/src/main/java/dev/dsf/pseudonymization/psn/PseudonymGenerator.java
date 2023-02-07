package dev.dsf.pseudonymization.psn;

import java.util.Collection;
import java.util.List;

import dev.dsf.pseudonymization.domain.PseudonymizedPerson;
import dev.dsf.pseudonymization.recordlinkage.MatchedPerson;
import dev.dsf.pseudonymization.recordlinkage.Person;

public interface PseudonymGenerator<P extends Person, R extends PseudonymizedPerson>
{
	List<R> createPseudonymsAndShuffle(Collection<? extends MatchedPerson<P>> persons);
}
