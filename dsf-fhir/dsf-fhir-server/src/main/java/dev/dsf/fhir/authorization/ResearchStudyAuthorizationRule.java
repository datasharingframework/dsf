package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ResearchStudy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.ResearchStudyDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
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

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("ResearchStudy is missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	@Override
	protected boolean resourceExists(Connection connection, ResearchStudy newResource)
	{
		// no unique criteria for ResearchStudy
		return false;
	}

	@Override
	protected boolean modificationsOk(Connection connection, ResearchStudy oldResource, ResearchStudy newResource)
	{
		// no unique criteria for ResearchStudy
		return true;
	}
}
