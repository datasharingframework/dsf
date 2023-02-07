package dev.dsf.pseudonymization.translation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.dsf.openehr.model.datatypes.StringRowElement;
import dev.dsf.openehr.model.structure.Column;
import dev.dsf.openehr.model.structure.Meta;
import dev.dsf.openehr.model.structure.ResultSet;
import dev.dsf.openehr.model.structure.RowElement;
import dev.dsf.pseudonymization.domain.MdatContainer;
import dev.dsf.pseudonymization.domain.PersonWithMdat;
import dev.dsf.pseudonymization.domain.impl.OpenEhrMdatContainer;
import dev.dsf.pseudonymization.openehr.Constants;

public class ResultSetTranslatorResearchResultToMedicImpl implements ResultSetTranslatorResearchResultToMedic
{
	@Override
	public ResultSet translate(List<Column> columns, List<PersonWithMdat> personsWithMdat)
	{
		Meta newMeta = createMeta();
		List<Column> newColumns = createColumns(columns);
		return new ResultSet(newMeta, "", "", newColumns,
				personsWithMdat.parallelStream().map(toRows()).collect(Collectors.toList()));
	}

	private Meta createMeta()
	{
		return new Meta("", "", "", LocalDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "", "");
	}

	private List<Column> createColumns(List<Column> columns)
	{
		return Stream
				.concat(columns.stream().map(toNewColumn()),
						Stream.of(new Column(Constants.MEDICID_COLUMN_NAME, Constants.MEDICID_COLUMN_PATH)))
				.collect(Collectors.toList());
	}

	private Function<Column, Column> toNewColumn()
	{
		return c -> new Column(c.getName(), c.getPath());
	}

	private Function<PersonWithMdat, List<RowElement>> toRows()
	{
		return person ->
		{
			MdatContainer mdatContainer = person.getMdatContainer();
			if (mdatContainer instanceof OpenEhrMdatContainer)
			{
				String medicId = person.getMedicId().getValue();
				return Stream.concat(((OpenEhrMdatContainer) mdatContainer).getElements().stream(),
						Stream.of(new StringRowElement(medicId))).collect(Collectors.toList());
			}
			else
				throw new IllegalArgumentException("MdatContainer of type " + OpenEhrMdatContainer.class.getName()
						+ " expected, but got " + mdatContainer.getClass().getName());
		};
	}
}
