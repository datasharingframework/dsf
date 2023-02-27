package dev.dsf.pseudonymization.translation;

import java.util.List;

import dev.dsf.openehr.model.structure.Column;
import dev.dsf.openehr.model.structure.ResultSet;
import dev.dsf.pseudonymization.domain.PersonWithMdat;

public interface ResultSetTranslatorResearchResultToMedic
{
	ResultSet translate(List<Column> columns, List<PersonWithMdat> personsWithMdat);
}
