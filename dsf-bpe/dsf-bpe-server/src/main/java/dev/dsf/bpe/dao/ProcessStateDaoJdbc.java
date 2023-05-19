package dev.dsf.bpe.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.plugin.ProcessState;

public class ProcessStateDaoJdbc extends AbstractDaoJdbc implements ProcessStateDao
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessStateDaoJdbc.class);

	public ProcessStateDaoJdbc(BasicDataSource dataSource)
	{
		super(dataSource);
	}

	@Override
	public void updateStates(Map<ProcessIdAndVersion, ProcessState> states) throws SQLException
	{
		Objects.requireNonNull(states, "states");

		if (states.isEmpty())
			return;

		try (Connection connection = dataSource.getConnection())
		{
			connection.setReadOnly(false);

			try (PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO process_states (process_key_and_version, state) VALUES (?, ?) ON CONFLICT (process_key_and_version) DO UPDATE SET state = ?"))
			{
				for (Entry<ProcessIdAndVersion, ProcessState> entry : states.entrySet())
				{
					statement.setString(1, entry.getKey().toString());
					statement.setString(2, entry.getValue().name());
					statement.setString(3, entry.getValue().name());

					statement.addBatch();
				}

				logger.trace("Executing query '{}'", statement);
				statement.executeBatch();
			}
		}
	}

	@Override
	public Map<ProcessIdAndVersion, ProcessState> getStates() throws SQLException
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT process_key_and_version, state FROM process_states");
				ResultSet resultSet = statement.executeQuery())
		{
			Map<ProcessIdAndVersion, ProcessState> states = new HashMap<>();
			while (resultSet.next())
			{
				ProcessIdAndVersion processKeyAndVersion = ProcessIdAndVersion.fromString(resultSet.getString(1));
				ProcessState state = ProcessState.valueOf(resultSet.getString(2));

				states.putIfAbsent(processKeyAndVersion, state);
			}
			return states;
		}
	}
}
