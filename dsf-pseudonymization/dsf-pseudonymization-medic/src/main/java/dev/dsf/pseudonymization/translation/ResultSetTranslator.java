package dev.dsf.pseudonymization.translation;

import dev.dsf.openehr.model.structure.ResultSet;

public interface ResultSetTranslator
{
	ResultSet translate(ResultSet resultSet);
}
