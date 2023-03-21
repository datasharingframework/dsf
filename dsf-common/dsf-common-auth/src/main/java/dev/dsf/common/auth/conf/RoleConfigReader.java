package dev.dsf.common.auth.conf;

import java.io.InputStream;
import java.util.function.Function;

import org.hl7.fhir.r4.model.Coding;
import org.yaml.snakeyaml.Yaml;

public class RoleConfigReader
{
	public RoleConfig read(String config, Function<String, DsfRole> dsfRoleFactory,
			Function<String, Coding> practitionerRoleFactory)
	{
		Object o = yaml().load(config);
		return new RoleConfig(o, dsfRoleFactory, practitionerRoleFactory);
	}

	public RoleConfig read(InputStream config, Function<String, DsfRole> dsfRoleFactory,
			Function<String, Coding> practitionerRoleFactory)
	{
		Object o = yaml().load(config);
		return new RoleConfig(o, dsfRoleFactory, practitionerRoleFactory);
	}

	protected Yaml yaml()
	{
		return new Yaml();
	}
}
