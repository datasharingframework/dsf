package dev.dsf.common.auth.conf;

import java.io.InputStream;
import java.util.function.Function;

import org.yaml.snakeyaml.Yaml;

public class RoleConfigReader
{
	public RoleConfig read(String config, Function<String, Role> roleFactory)
	{
		Object o = yaml().load(config);
		return new RoleConfig(o, roleFactory);
	}

	public RoleConfig read(InputStream config, Function<String, Role> roleFactory)
	{
		Object o = yaml().load(config);
		return new RoleConfig(o, roleFactory);
	}

	protected Yaml yaml()
	{
		return new Yaml();
	}
}
