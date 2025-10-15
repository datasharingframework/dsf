package dev.dsf.common.auth.conf;

import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

import org.hl7.fhir.r4.model.Coding;
import org.yaml.snakeyaml.Yaml;

import dev.dsf.common.auth.conf.RoleConfig.DsfRoleFactory;

public class RoleConfigReader
{
	public <R extends DsfRole> RoleConfig<R> read(String config, DsfRoleFactory<R> dsfRoleFactory,
			Function<String, Coding> practitionerRoleFactory)
	{
		Objects.requireNonNull(config, "config");
		Objects.requireNonNull(dsfRoleFactory, "dsfRoleFactory");
		Objects.requireNonNull(practitionerRoleFactory, "practitionerRoleFactory");

		Object o = yaml().load(config);
		return new RoleConfig<R>(o, dsfRoleFactory, practitionerRoleFactory);
	}

	public <R extends DsfRole> RoleConfig<R> read(InputStream config, DsfRoleFactory<R> dsfRoleFactory,
			Function<String, Coding> practitionerRoleFactory)
	{
		Objects.requireNonNull(config, "config");
		Objects.requireNonNull(dsfRoleFactory, "dsfRoleFactory");
		Objects.requireNonNull(practitionerRoleFactory, "practitionerRoleFactory");

		Object o = yaml().load(config);
		return new RoleConfig<R>(o, dsfRoleFactory, practitionerRoleFactory);
	}

	protected Yaml yaml()
	{
		return new Yaml();
	}
}
