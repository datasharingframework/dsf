package dev.dsf.maven.dev;

import java.io.File;

public class Template
{
	private File source;
	private File target;

	public File getSource()
	{
		return source;
	}

	public File getTarget()
	{
		return target;
	}

	@Override
	public String toString()
	{
		return "Template [" + (source != null ? "source=" + source + ", " : "")
				+ (target != null ? "target=" + target : "") + "]";
	}
}
