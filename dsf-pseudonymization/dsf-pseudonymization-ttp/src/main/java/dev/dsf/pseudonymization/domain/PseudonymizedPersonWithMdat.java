package dev.dsf.pseudonymization.domain;

import java.util.List;

public interface PseudonymizedPersonWithMdat extends PseudonymizedPerson
{
	List<MdatContainer> getMdatContainers();
}
