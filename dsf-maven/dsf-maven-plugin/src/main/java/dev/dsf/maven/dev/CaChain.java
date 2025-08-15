package dev.dsf.maven.dev;

import java.io.File;
import java.util.List;

public class CaChain
{
	private List<File> targets;

	public List<File> getTargets()
	{
		return targets;
	}

	@Override
	public String toString()
	{
		return "CaChain [" + (targets != null ? "targets=" + targets : "") + "]";
	}
}
