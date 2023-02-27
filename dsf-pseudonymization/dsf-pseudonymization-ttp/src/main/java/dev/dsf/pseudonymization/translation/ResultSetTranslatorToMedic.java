package dev.dsf.pseudonymization.translation;

import java.util.List;

import dev.dsf.openehr.model.structure.Column;
import dev.dsf.openehr.model.structure.Meta;
import dev.dsf.openehr.model.structure.ResultSet;
import dev.dsf.pseudonymization.domain.PseudonymizedPersonWithMdat;

public interface ResultSetTranslatorToMedic
{
	ResultSet translate(Meta meta, List<Column> columns, List<PseudonymizedPersonWithMdat> pseudonymsWithMdat);
}
