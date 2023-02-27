package dev.dsf.pseudonymization.translation;

import java.util.List;

import dev.dsf.openehr.model.structure.ResultSet;
import dev.dsf.pseudonymization.domain.PersonWithMdat;

public interface ResultSetTranslatorFromMedic
{
	List<PersonWithMdat> translate(String organization, ResultSet resultSet);
}
