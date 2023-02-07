package dev.dsf.pseudonymization.translation;

import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import dev.dsf.openehr.model.datatypes.StringRowElement;
import dev.dsf.openehr.model.structure.Column;
import dev.dsf.openehr.model.structure.ResultSet;
import dev.dsf.openehr.model.structure.RowElement;
import dev.dsf.pseudonymization.domain.PersonWithMdat;
import dev.dsf.pseudonymization.domain.impl.MedicIdImpl;
import dev.dsf.pseudonymization.domain.impl.OpenEhrMdatContainer;
import dev.dsf.pseudonymization.domain.impl.PersonImpl;
import dev.dsf.pseudonymization.openehr.Constants;
import dev.dsf.pseudonymization.recordlinkage.MedicId;

public class ResultSetTranslatorFromMedicWithRbfImpl implements ResultSetTranslatorFromMedicWithRbf
{
	@Override
	public List<PersonWithMdat> translate(String organization, ResultSet resultSet)
	{
		int medicIdColumnIndex = getMedicIdColumnIndex(resultSet.getColumns());
		int rbfColumnIndex = getRbfColumnIndex(resultSet.getColumns());

		if (medicIdColumnIndex < 0)
			throw new IllegalArgumentException("Missing MeDIC id column with name '" + Constants.MEDICID_COLUMN_NAME
					+ "' and path '" + Constants.MEDICID_COLUMN_PATH + "'");
		if (rbfColumnIndex < 0)
			throw new IllegalArgumentException("Missing RBF column with name '" + Constants.RBF_COLUMN_NAME
					+ "' and path '" + Constants.RBF_COLUMN_PATH + "'");

		return resultSet.getRows().parallelStream()
				.map(toPersonWithMdat(organization, medicIdColumnIndex, rbfColumnIndex)).collect(Collectors.toList());
	}

	private Function<List<RowElement>, PersonWithMdat> toPersonWithMdat(String organization, int medicIdColumnIndex,
			int rbfColumnIndex)
	{
		return rowElements ->
		{
			MedicId medicId = getMedicId(organization, rowElements.get(medicIdColumnIndex));
			BitSet recordBloomFilter = getRecordBloomFilter(rowElements.get(rbfColumnIndex));
			List<RowElement> elements = new ArrayList<>();
			for (int i = 0; i < rowElements.size(); i++)
				if (i != medicIdColumnIndex && i != rbfColumnIndex)
					elements.add(rowElements.get(i));

			return new PersonImpl(medicId, recordBloomFilter, new OpenEhrMdatContainer(elements));
		};
	}

	private MedicId getMedicId(String organization, RowElement rowElement)
	{
		if (rowElement instanceof StringRowElement)
			return new MedicIdImpl(organization, ((StringRowElement) rowElement).getValue());
		else
			throw new IllegalArgumentException("RowElement of type " + StringRowElement.class.getName()
					+ " expected but got " + rowElement.getClass().getName());
	}

	private BitSet getRecordBloomFilter(RowElement rowElement)
	{
		if (rowElement instanceof StringRowElement)
		{
			String rbfString = ((StringRowElement) rowElement).getValue();
			byte[] rbfBytes = Base64.getDecoder().decode(rbfString);
			return BitSet.valueOf(rbfBytes);
		}
		else
			throw new IllegalArgumentException("RowElement of type " + StringRowElement.class.getName()
					+ " expected, but got " + rowElement.getClass().getName());
	}

	private int getMedicIdColumnIndex(List<Column> columns)
	{
		for (int i = 0; i < columns.size(); i++)
			if (isMedicIdColumn().test(columns.get(i)))
				return i;

		return -1;
	}

	private Predicate<? super Column> isMedicIdColumn()
	{
		return column -> Constants.MEDICID_COLUMN_NAME.equals(column.getName())
				&& Constants.MEDICID_COLUMN_PATH.equals(column.getPath());
	}

	private int getRbfColumnIndex(List<Column> columns)
	{
		for (int i = 0; i < columns.size(); i++)
			if (isRbfColumn().test(columns.get(i)))
				return i;

		return -1;
	}

	private Predicate<? super Column> isRbfColumn()
	{
		return column -> Constants.RBF_COLUMN_NAME.equals(column.getName())
				&& Constants.RBF_COLUMN_PATH.equals(column.getPath());
	}
}
