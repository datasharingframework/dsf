package dev.dsf.common.auth.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Coding;

public class RoleConfig
{
	public class Mapping
	{
		private final String name;

		private final List<String> thumbprints = new ArrayList<>();
		private final List<String> emails = new ArrayList<>();
		private final List<String> tokenRoles = new ArrayList<>();
		private final List<String> tokenGroups = new ArrayList<>();

		private final List<DsfRole> dsfRoles = new ArrayList<>();
		private final List<Coding> practitionerRoles = new ArrayList<>();

		private Mapping(String name, List<String> thumbprints, List<String> emails, List<String> tokenRoles,
				List<String> tokenGroups, List<DsfRole> dsfRoles, List<Coding> practitionerRoles)
		{
			this.name = name;

			if (thumbprints != null)
				this.thumbprints.addAll(thumbprints);
			if (emails != null)
				this.emails.addAll(emails);
			if (tokenRoles != null)
				this.tokenRoles.addAll(tokenRoles);
			if (tokenGroups != null)
				this.tokenGroups.addAll(tokenGroups);

			if (dsfRoles != null)
				this.dsfRoles.addAll(dsfRoles);
			if (practitionerRoles != null)
				this.practitionerRoles.addAll(practitionerRoles);
		}

		public String getName()
		{
			return name;
		}

		public List<String> getThumbprints()
		{
			return Collections.unmodifiableList(thumbprints);
		}

		public List<String> getEmails()
		{
			return Collections.unmodifiableList(emails);
		}

		public List<String> getTokenRoles()
		{
			return Collections.unmodifiableList(tokenRoles);
		}

		public List<String> getTokenGroups()
		{
			return Collections.unmodifiableList(tokenGroups);
		}

		public List<DsfRole> getDsfRoles()
		{
			return Collections.unmodifiableList(dsfRoles);
		}

		public List<Coding> getPractitionerRoles()
		{
			return Collections.unmodifiableList(practitionerRoles);
		}

		@Override
		public String toString()
		{
			return "[name=" + name + ", thumbprints=" + thumbprints + ", emails=" + emails + ", tokenRoles="
					+ tokenRoles + ", tokenGroups=" + tokenGroups + ", dsfRoles=" + dsfRoles + ", practitionerRoles="
					+ practitionerRoles.stream().map(c -> c.getSystem() + "|" + c.getCode())
							.collect(Collectors.joining(", ", "[", "]"))
					+ "]";
		}
	}

	private final List<Mapping> entries = new ArrayList<>();

	/**
	 * @param config
	 *            parsed yaml
	 * @param dsfRoleFactory
	 *            factory should return <code>null</code> if the given string does not represent a valid role, the role
	 *            needs to exists
	 * @param practitionerRoleFactory
	 *            factory should return <code>null</code> if the given string does not represent a valid code, the code
	 *            or CodeSystem does not need to exist
	 */
	public RoleConfig(Object config, Function<String, DsfRole> dsfRoleFactory,
			Function<String, Coding> practitionerRoleFactory)
	{
		if (config != null && config instanceof List)
		{
			@SuppressWarnings("unchecked")
			List<Object> cList = (List<Object>) config;
			cList.forEach(mapping ->
			{
				if (mapping != null && mapping instanceof Map)
				{
					@SuppressWarnings("unchecked")
					Map<Object, Object> m = (Map<Object, Object>) mapping;
					m.forEach((mappingKey, mappingValues) ->
					{
						if (mappingKey != null && mappingKey instanceof String && mappingValues != null
								&& mappingValues instanceof Map)
						{
							@SuppressWarnings("unchecked")
							Map<Object, Object> properties = (Map<Object, Object>) mappingValues;

							// Map<String, Object>
							List<String> thumbprints = null, emails = null, tokenRoles = null, tokenGroups = null;
							List<DsfRole> dsfRoles = null;
							List<Coding> practitionerRoles = null;
							for (Entry<Object, Object> p : properties.entrySet())
							{
								if (p.getKey() != null && p.getKey() instanceof String)
								{
									switch ((String) p.getKey())
									{
										case "thumbprint":
											thumbprints = getValues(p.getValue());
											break;
										case "email":
											emails = getValues(p.getValue());
											break;
										case "token-role":
											tokenRoles = getValues(p.getValue());
											break;
										case "token-group":
											tokenGroups = getValues(p.getValue());
											break;
										case "dsf-role":
											dsfRoles = getValues(p.getValue()).stream().map(dsfRoleFactory)
													.filter(r -> r != null).toList();
											break;
										case "practitioner-role":
											practitionerRoles = getValues(p.getValue()).stream()
													.map(practitionerRoleFactory).filter(r -> r != null).toList();
											break;
									}
								}
							}
							entries.add(new Mapping((String) mappingKey, thumbprints, emails, tokenRoles, tokenGroups,
									dsfRoles, practitionerRoles));
						}
					});
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> getValues(Object o)
	{
		if (o instanceof String)
			return Collections.singletonList((String) o);
		else if (o instanceof List)
			return ((List<String>) o);
		else
			return Collections.emptyList();
	}

	public List<Mapping> getEntries()
	{
		return Collections.unmodifiableList(entries);
	}

	public List<DsfRole> getDsfRolesForThumbprint(String thumbprint)
	{
		return getDsfRoleFor(Mapping::getThumbprints, thumbprint);
	}

	public List<DsfRole> getDsfRolesForEmail(String email)
	{
		return getDsfRoleFor(Mapping::getEmails, email);
	}

	public List<DsfRole> getDsfRolesForTokenRole(String tokenRole)
	{
		return getDsfRoleFor(Mapping::getTokenRoles, tokenRole);
	}

	public List<DsfRole> getDsfRolesForTokenGroup(String tokenGroup)
	{
		return getDsfRoleFor(Mapping::getTokenGroups, tokenGroup);
	}

	private List<DsfRole> getDsfRoleFor(Function<Mapping, List<String>> values, String value)
	{
		return getEntries().stream().filter(m -> values.apply(m).contains(value)).flatMap(m -> m.getDsfRoles().stream())
				.toList();
	}

	public List<Coding> getPractitionerRolesForThumbprint(String thumbprint)
	{
		return getPractitionerRoleFor(Mapping::getThumbprints, thumbprint);
	}

	public List<Coding> getPractitionerRolesForEmail(String email)
	{
		return getPractitionerRoleFor(Mapping::getEmails, email);
	}

	public List<Coding> getPractitionerRolesForTokenRole(String tokenRole)
	{
		return getPractitionerRoleFor(Mapping::getTokenRoles, tokenRole);
	}

	public List<Coding> getPractitionerRolesForTokenGroup(String tokenGroup)
	{
		return getPractitionerRoleFor(Mapping::getTokenGroups, tokenGroup);
	}

	private List<Coding> getPractitionerRoleFor(Function<Mapping, List<String>> values, String value)
	{
		return getEntries().stream().filter(m -> values.apply(m).contains(value))
				.flatMap(m -> m.getPractitionerRoles().stream()).toList();
	}

	@Override
	public String toString()
	{
		return getEntries().stream().map(Mapping::toString).collect(Collectors.joining(", ", "{", "}"));
	}
}
