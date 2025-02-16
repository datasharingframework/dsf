package dev.dsf.common.auth.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleConfig
{
	private static final Logger logger = LoggerFactory.getLogger(RoleConfig.class);

	private static final String PROPERTY_THUMBPRINT = "thumbprint";
	private static final String PROPERTY_EMAIL = "email";
	private static final String PROPERTY_TOKEN_ROLE = "token-role";
	private static final String PROPERTY_TOKEN_GROUP = "token-group";
	private static final String PROPERTY_DSF_ROLE = "dsf-role";
	private static final String PROPERTY_PRACTITIONER_ROLE = "practitioner-role";

	private static final List<String> PROPERTIES = List.of(PROPERTY_THUMBPRINT, PROPERTY_EMAIL, PROPERTY_TOKEN_ROLE,
			PROPERTY_TOKEN_GROUP, PROPERTY_DSF_ROLE, PROPERTY_PRACTITIONER_ROLE);

	private static final String THUMBPRINT_PATTERN_STRING = "^[a-f0-9]{128}$";
	private static final Pattern THUMBPRINT_PATTERN = Pattern.compile(THUMBPRINT_PATTERN_STRING);

	private static final String EMAIL_PATTERN_STRING = "^[\\w!#$%&'*+/=?`{\\|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{\\|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_PATTERN_STRING);

	public static final class Mapping
	{
		private final String name;

		private final List<String> thumbprints = new ArrayList<>();
		private final List<String> emails = new ArrayList<>();
		private final List<String> tokenRoles = new ArrayList<>();
		private final List<String> tokenGroups = new ArrayList<>();

		private final List<DsfRole> dsfRoles = new ArrayList<>();
		private final List<Coding> practitionerRoles = new ArrayList<>();

		public Mapping(String name, List<String> thumbprints, List<String> emails, List<String> tokenRoles,
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
		if (config != null && config instanceof List<?> l)
		{
			l.forEach(mapping ->
			{
				if (mapping != null && mapping instanceof Map<?, ?> m)
				{
					m.forEach((mappingKey, mappingValues) ->
					{
						if (mappingKey != null && mappingKey instanceof String && mappingValues != null
								&& mappingValues instanceof Map<?, ?> v)
						{
							List<String> thumbprints = null, emails = null, tokenRoles = null, tokenGroups = null;
							List<DsfRole> dsfRoles = null;
							List<Coding> practitionerRoles = null;

							for (Entry<?, ?> property : v.entrySet())
							{
								if (property.getKey() != null && property.getKey() instanceof String key)
								{
									switch (key)
									{
										case PROPERTY_THUMBPRINT:
											thumbprints = getValues(property.getValue()).stream().map(value ->
											{
												if (value == null || value.isBlank())
												{
													logger.warn("Ignoring empty of blank thumbprint in rule '{}'",
															mappingKey);
													return null;
												}
												else if (!THUMBPRINT_PATTERN.matcher(value.trim()).matches())
												{
													logger.warn(
															"Malformed thumbprint: '{}' not matching {}, ignoring value in rule '{}'",
															value, THUMBPRINT_PATTERN_STRING, mappingKey);
													return null;
												}
												else
													return value.trim();
											}).filter(g -> g != null).toList();
											break;

										case PROPERTY_EMAIL:
											emails = getValues(property.getValue()).stream().map(value ->
											{
												if (value == null || value.isBlank())
												{
													logger.warn("Ignoring empty of blank email in rule '{}'",
															mappingKey);
													return null;
												}
												else if (!EMAIL_PATTERN.matcher(value.trim()).matches())
												{
													logger.warn(
															"Malformed email: '{}' not matching {}, ignoring value in rule '{}'",
															value, EMAIL_PATTERN_STRING, mappingKey);
													return null;
												}
												else
													return value.trim();
											}).filter(g -> g != null).toList();
											break;

										case PROPERTY_TOKEN_ROLE:
											tokenRoles = getValues(property.getValue()).stream().map(value ->
											{
												if (value == null || value.isBlank())
												{
													logger.warn("Ignoring empty of blank token-role in rule '{}'",
															mappingKey);
													return null;
												}
												else
													return value.trim();
											}).filter(g -> g != null).toList();
											break;

										case PROPERTY_TOKEN_GROUP:
											tokenGroups = getValues(property.getValue()).stream().map(value ->
											{
												if (value == null || value.isBlank())
												{
													logger.warn("Ignoring empty of blank token-group in rule '{}'",
															mappingKey);
													return null;
												}
												else
													return value.trim();
											}).filter(g -> g != null).toList();
											break;

										case PROPERTY_DSF_ROLE:
											dsfRoles = getValues(property.getValue()).stream().map(value ->
											{
												if (value == null || value.isBlank())
												{
													logger.warn("Ignoring empty of blank dsf-role in rule '{}'",
															mappingKey);
													return null;
												}

												DsfRole dsfRole = dsfRoleFactory.apply(value.trim());
												if (dsfRole == null)
													logger.warn("Unknown dsf-role '{}', ignoring value in rule '{}'",
															value, mappingKey);

												return dsfRole;
											}).filter(r -> r != null).toList();
											break;

										case PROPERTY_PRACTITIONER_ROLE:
											practitionerRoles = getValues(property.getValue()).stream().map(value ->
											{
												if (value == null || value.isBlank())
												{
													logger.warn(
															"Ignoring empty of blank practitioner-role in rule '{}'",
															mappingKey);
													return null;
												}

												Coding coding = practitionerRoleFactory.apply(value.trim());
												if (coding == null)
													logger.warn(
															"Unknown practitioner-role '{}', ignoring value in rule '{}'",
															value, mappingKey);

												return coding;
											}).filter(r -> r != null).toList();
											break;

										default:
											logger.warn(
													"Unknown role config property '{}' expected one of {}, ignoring property in rule '{}'",
													property.getKey(), PROPERTIES, mappingKey);
									}
								}
							}

							entries.add(new Mapping((String) mappingKey, thumbprints, emails, tokenRoles, tokenGroups,
									dsfRoles, practitionerRoles));
						}
						else if (mappingKey != null && mappingKey instanceof String
								&& (mappingValues == null || !(mappingValues instanceof Map)))
						{
							logger.warn("Ignoring invalid rule '{}', no value specified or value not map", mappingKey);
						}
						else
							logger.warn("Ignoring invalid rule '{}'", Objects.toString(mappingKey));
					});
				}
				else
					logger.warn("Ignoring invalud rule '{}'", mapping);
			});
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> getValues(Object o)
	{
		return switch (o)
		{
			case String s -> List.of(s);
			case @SuppressWarnings("rawtypes") List l -> l;
			default -> List.of();
		};
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
