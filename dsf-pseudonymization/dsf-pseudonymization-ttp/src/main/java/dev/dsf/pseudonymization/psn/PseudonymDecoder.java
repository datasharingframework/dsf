package dev.dsf.pseudonymization.psn;

import java.util.List;
import java.util.stream.Collectors;

import dev.dsf.pseudonymization.domain.PseudonymizedPerson;
import dev.dsf.pseudonymization.recordlinkage.MatchedPerson;
import dev.dsf.pseudonymization.recordlinkage.Person;

public interface PseudonymDecoder<P extends Person>
{
	MatchedPerson<P> decodePseudonym(PseudonymizedPerson person);

	default List<MatchedPerson<P>> decodePseudonyms(List<PseudonymizedPerson> persons)
	{
		return persons.parallelStream().map(this::decodePseudonym).collect(Collectors.toList());
	}
}
