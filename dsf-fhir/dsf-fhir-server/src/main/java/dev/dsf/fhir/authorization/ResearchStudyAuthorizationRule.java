package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.PractitionerRoleDao;
import dev.dsf.fhir.dao.ResearchStudyDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.service.ReferenceResolver;

public class ResearchStudyAuthorizationRule extends AbstractMetaTagAuthorizationRule<ResearchStudy, ResearchStudyDao>
{
	private static final Logger logger = LoggerFactory.getLogger(ResearchStudyAuthorizationRule.class);

	private static final String DSF_RESEARCH_STUDY = "http://dsf.dev/fhir/StructureDefinition/research-study";
	private static final String RESEARCH_STUDY_IDENTIFIER = "http://dsf.dev/sid/research-study-identifier";
	private static final String RESEARCH_STUDY_IDENTIFIER_PATTERN_STRING = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
	private static final Pattern RESEARCH_STUDY_IDENTIFIER_PATTERN = Pattern
			.compile(RESEARCH_STUDY_IDENTIFIER_PATTERN_STRING);
	private static final String PARTICIPATING_MEDIC_EXTENSION_URL = "http://dsf.dev/fhir/StructureDefinition/extension-participating-medic";
	private static final String PARTICIPATING_TTP_EXTENSION_URL = "http://dsf.dev/fhir/StructureDefinition/extension-participating-ttp";

	public ResearchStudyAuthorizationRule(DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter)
	{
		super(ResearchStudy.class, daoProvider, serverBase, referenceResolver, organizationProvider, readAccessHelper,
				parameterConverter);
	}

	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity identity,
			ResearchStudy newResource)
	{
		return newResourceOk(connection, identity, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity,
			ResearchStudy newResource)
	{
		return newResourceOk(connection, identity, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, Identity identity, ResearchStudy newResource)
	{
		List<String> errors = new ArrayList<String>();

		if (newResource.getMeta().hasProfile(DSF_RESEARCH_STUDY))
		{
			if (newResource.hasIdentifier())
			{
				if (newResource.getIdentifier().stream()
						.filter(i -> i.hasSystem() && i.hasValue() && RESEARCH_STUDY_IDENTIFIER.equals(i.getSystem())
								&& RESEARCH_STUDY_IDENTIFIER_PATTERN.matcher(i.getValue()).matches())
						.count() != 1)
				{
					errors.add("ResearchStudy.identifier one with system '" + RESEARCH_STUDY_IDENTIFIER
							+ "' and non empty value matching " + RESEARCH_STUDY_IDENTIFIER_PATTERN_STRING
							+ " expected");
				}
			}
			else
			{
				errors.add("ResearchStudy.identifier missing");
			}

			if (getParticipatingMedicReferences(newResource).count() >= 0)
			{
				if (!organizationsResolvable(connection, identity,
						"ResearchStudy.extension(url:http://dsf.dev/fhir/StructureDefinition/extension-participating-medic)",
						getParticipatingMedicReferences(newResource)).allMatch(t -> t))
				{
					errors.add(
							"ResearchStudy.extension(url:http://dsf.dev/fhir/StructureDefinition/extension-participating-medic) one or more participating-medic Organizations not resolved");
				}
			}
			else
			{
				errors.add(
						"ResearchStudy.extension(url:http://dsf.dev/fhir/StructureDefinition/extension-participating-medic) one or more participating-medic Organization references missing");
			}

			Optional<Reference> participatingTtpReference = getParticipatingTtpReference(newResource);
			if (participatingTtpReference.isPresent())
			{
				if (!organizationResolvable(connection, identity,
						"ResearchStudy.extension(url:http://dsf.dev/fhir/StructureDefinition/extension-participating-ttp)",
						participatingTtpReference.get()))
				{
					errors.add(
							"ResearchStudy.extension(url:http://dsf.dev/fhir/StructureDefinition/extension-participating-ttp) participating-ttp Organization not resolved");
				}
			}
			else
			{
				errors.add(
						"ResearchStudy.extension(url:http://dsf.dev/fhir/StructureDefinition/extension-participating-ttp) participating-ttp Organization reference missing");
			}

			if (newResource.getEnrollment().size() >= 0)
			{
				if (!enrollmentsResolvable(connection, identity, "ResearchStudy.enrollment",
						newResource.getEnrollment().stream()).allMatch(t -> t))
				{
					errors.add("ResearchStudy.enrollment one or more Groups not resolved");
				}
			}
			else
			{
				errors.add("ResearchStudy.enrollment one or more Group references missing");
			}

			// TODO: hasPrincipalInvestigator check is only optional for feasibility requests. For full data sharing
			// processes, the field is mandatory and should lead to a validation error if not supplied.
			if (newResource.hasPrincipalInvestigator())
			{
				Optional<Resource> practitioner = resolvePractitioner(connection, identity,
						"ResearchStudy.principalInvestigator", newResource.getPrincipalInvestigator());
				if (practitioner.isPresent() && practitioner.get() instanceof Practitioner
						&& ((Practitioner) practitioner.get()).getActive())
				{
					if (!practitionerRoleExists(connection, identity, practitioner.get().getIdElement()))
					{
						errors.add(
								"ResearchStudy.principalInvestigator corresponding PractitionerRole.practitioner not found");
					}
				}
				else
				{
					errors.add(
							"ResearchStudy.principalInvestigator not resolved or not instance of Practitioner or not active");
				}
			}
		}

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("ResearchStudy is missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	private Stream<Reference> getParticipatingMedicReferences(ResearchStudy resource)
	{
		return resource.getExtensionsByUrl(PARTICIPATING_MEDIC_EXTENSION_URL).stream().map(e -> e.getValue())
				.filter(t -> t instanceof Reference).map(t -> ((Reference) t));
	}

	private Optional<Reference> getParticipatingTtpReference(ResearchStudy resource)
	{
		return Optional.ofNullable(resource.getExtensionByUrl(PARTICIPATING_TTP_EXTENSION_URL)).map(e -> e.getValue())
				.filter(t -> t instanceof Reference).map(t -> ((Reference) t));
	}

	private Stream<Boolean> organizationsResolvable(Connection connection, Identity identity, String referenceLocation,
			Stream<Reference> references)
	{
		return references.map(r -> organizationResolvable(connection, identity, referenceLocation, r));
	}

	private boolean organizationResolvable(Connection connection, Identity identity, String referenceLocation,
			Reference reference)
	{
		var ref = createIfLiteralInternalOrLogicalReference(referenceLocation, reference, Organization.class);
		return resolveReference(connection, identity, ref).isPresent();
	}

	private Stream<Boolean> enrollmentsResolvable(Connection connection, Identity identity, String referenceLocation,
			Stream<Reference> references)
	{
		return references.map(r -> enrollmentResolvable(connection, identity, referenceLocation, r));
	}

	private boolean enrollmentResolvable(Connection connection, Identity identity, String referenceLocation,
			Reference reference)
	{
		var ref = createIfLiteralInternalOrLogicalReference(referenceLocation, reference, Group.class);
		return resolveReference(connection, identity, ref).isPresent();
	}

	private Optional<Resource> resolvePractitioner(Connection connection, Identity identity, String referenceLocation,
			Reference reference)
	{
		var ref = createIfLiteralInternalOrLogicalReference(referenceLocation, reference, Practitioner.class);
		return resolveReference(connection, identity, ref);
	}

	private boolean practitionerRoleExists(Connection connection, Identity identity, IdType practitionerId)
	{
		Map<String, List<String>> queryParameters = Map.of("practitioner",
				Collections.singletonList("Practitioner/" + practitionerId.getIdPart()), "active",
				Collections.singletonList("true"));
		PractitionerRoleDao dao = daoProvider.getPractitionerRoleDao();
		SearchQuery<PractitionerRole> query = dao.createSearchQuery(identity, 0, 0)
				.configureParameters(queryParameters);

		if (!query.getUnsupportedQueryParameters(queryParameters).isEmpty())
			return false;

		try
		{
			return dao.searchWithTransaction(connection, query).getTotal() == 1;
		}
		catch (SQLException e)
		{
			logger.warn("Error while searching for PractitionerRole", e);
			return false;
		}
	}

	@Override
	protected boolean resourceExists(Connection connection, ResearchStudy newResource)
	{
		if (newResource.getMeta().hasProfile(DSF_RESEARCH_STUDY))
		{
			String identifierValue = newResource.getIdentifier().stream()
					.filter(i -> i.hasSystem() && i.hasValue() && RESEARCH_STUDY_IDENTIFIER.equals(i.getSystem()))
					.map(i -> i.getValue()).findFirst().orElseThrow();

			return researchStudyWithIdentifierExists(connection, identifierValue);
		}
		else
			// no unique criteria if not a DSF ResearchStudy
			return false;
	}

	private boolean researchStudyWithIdentifierExists(Connection connection, String identifierValue)
	{
		Map<String, List<String>> queryParameters = Map.of("identifier",
				Collections.singletonList(RESEARCH_STUDY_IDENTIFIER + "|" + identifierValue));
		ResearchStudyDao dao = getDao();
		SearchQuery<ResearchStudy> query = dao.createSearchQueryWithoutUserFilter(0, 0)
				.configureParameters(queryParameters);

		if (!query.getUnsupportedQueryParameters(queryParameters).isEmpty())
			return false;

		try
		{
			PartialResult<ResearchStudy> result = dao.searchWithTransaction(connection, query);
			return result.getTotal() >= 1;
		}
		catch (SQLException e)
		{
			logger.warn("Error while searching for ResearchStudy with identifier", e);
			return false;
		}
	}

	@Override
	protected boolean modificationsOk(Connection connection, ResearchStudy oldResource, ResearchStudy newResource)
	{
		String oldIdentifierValue = oldResource.getIdentifier().stream()
				.filter(i -> RESEARCH_STUDY_IDENTIFIER.equals(i.getSystem())).map(i -> i.getValue()).findFirst()
				.orElseThrow();

		String newIdentifierValue = newResource.getIdentifier().stream()
				.filter(i -> RESEARCH_STUDY_IDENTIFIER.equals(i.getSystem())).map(i -> i.getValue()).findFirst()
				.orElseThrow();

		return oldIdentifierValue.equals(newIdentifierValue);
	}
}
