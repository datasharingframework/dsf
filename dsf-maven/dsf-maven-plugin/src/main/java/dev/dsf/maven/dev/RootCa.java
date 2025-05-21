package dev.dsf.maven.dev;

import java.io.File;
import java.util.List;

public class RootCa
{
	private List<File> targets;

	public List<File> getTargets()
	{
		return targets;
	}

	@Override
	public String toString()
	{
		return "RootCa [" + (targets != null ? "targets=" + targets : "") + "]";
	}
}
