package dev.dsf.pseudonymization.translation;

import java.util.List;

import dev.dsf.openehr.model.structure.ResultSet;
import dev.dsf.pseudonymization.domain.PseudonymizedPersonWithMdat;

public interface ResultSetTranslatorResearchResultFromMedic
{
	List<PseudonymizedPersonWithMdat> translate(ResultSet resultSet);
}
