package dev.dsf.bpe.dao;

import java.sql.SQLException;
import java.util.Map;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.plugin.ProcessState;

public interface ProcessStateDao
{
	void updateStates(Map<ProcessIdAndVersion, ProcessState> states) throws SQLException;

	Map<ProcessIdAndVersion, ProcessState> getStates() throws SQLException;
}
