package dev.dsf.bpe.dao;

import java.sql.SQLException;
import java.util.Map;

import dev.dsf.bpe.process.ProcessKeyAndVersion;
import dev.dsf.bpe.process.ProcessState;

public interface ProcessStateDao
{
	void updateStates(Map<ProcessKeyAndVersion, ProcessState> states) throws SQLException;

	Map<ProcessKeyAndVersion, ProcessState> getStates() throws SQLException;
}
