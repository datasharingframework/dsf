package dev.dsf.fhir.adapter;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.common.buildinfo.BuildInfoReader;
import dev.dsf.fhir.dao.StatisticsDao;
import dev.dsf.fhir.dao.StatisticsDao.Statistics;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.webservice.impl.RootServiceImpl;

public class ResourceOperationOutcome extends AbstractThymeleafContext implements ThymeleafContext, InitializingBean
{
	private record Element(String status, Boolean active, String type, String title, String subtitle, String unit,
			String value, String link)
	{
	}

	private record ByteSize(double value, String unit)
	{
	}

	private static final String[] BYTE_UNITS = { "B", "KiB", "MiB", "GiB", "TiB" };

	private final BuildInfoReader buildInfoReader;
	private final StatisticsDao statisticsDao;
	private final ExceptionHandler exceptionHandler;

	public ResourceOperationOutcome(BuildInfoReader buildInfoReader, StatisticsDao statisticsDao,
			ExceptionHandler exceptionHandler)
	{
		this.buildInfoReader = buildInfoReader;
		this.statisticsDao = statisticsDao;
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(buildInfoReader, "buildInfoReader");
		Objects.requireNonNull(statisticsDao, "statisticsDao");
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
	}

	@Override
	public Class<? extends Resource> getResourceType()
	{
		return OperationOutcome.class;
	}

	@Override
	public boolean isResourceSupported(String requestPathLastElement)
	{
		return false;
	}

	@Override
	public boolean isRootSupported(Resource resource, Principal principal)
	{
		// only show statistics when GET of root URL, TODO move principal logic to authorization rules
		return resource instanceof OperationOutcome o && (boolean) o.getUserData(RootServiceImpl.ROOT_GET)
				&& ((principal instanceof OrganizationIdentity org && org.isLocalIdentity())
						|| (principal instanceof PractitionerIdentity prc && prc.hasPractionerRole("DSF_ADMIN")));
	}

	@Override
	public String getHtmlFragment()
	{
		return "root";
	}

	@Override
	public void setVariables(BiConsumer<String, Object> variables, Resource resource)
	{
		variables.accept("elements", Stream.concat(buildInfoElements(), statisticsElements()).toList());
	}

	private Stream<Element> buildInfoElements()
	{
		return Stream.of(
				new Element(null, null, null, "DSF", "Version", null, buildInfoReader.getProjectVersion(), null),
				new Element(null, null, null, "DSF", "Release Date", null,
						formatDate(buildInfoReader.getBuildDateAsDate()), null));
	}

	private Stream<Element> statisticsElements()
	{
		Statistics statistics = exceptionHandler.catchAndLogSqlExceptionAndIfReturn(() -> statisticsDao.getStatistics(),
				() -> null);

		if (statistics == null)
			return Stream.of();

		ByteSize databaseSize = formatBytes(statistics.databaseSize());
		ByteSize binariesSize = formatBytes(statistics.binariesSize());

		String minusOneDay = DateTimeFormatter.ISO_OFFSET_DATE_TIME
				.format(OffsetDateTime.now().minusDays(1).withNano(0));
		String minusThirtyDays = DateTimeFormatter.ISO_OFFSET_DATE_TIME
				.format(OffsetDateTime.now().minusDays(30).withNano(0));

		return Stream.of(
				new Element(null, null, null, "DSF", "Database Size", databaseSize.unit(),
						String.format("%.2f", databaseSize.value()), null),
				new Element(null, null, null, "DSF", "Binaries Size", binariesSize.unit(),
						String.format("%.2f", binariesSize.value()), null),
				new Element("active", null, ResourceType.ActivityDefinition.name(),
						ResourceType.ActivityDefinition.name(), null, null,
						String.valueOf(statistics.activityDefinitions()),
						ResourceType.ActivityDefinition.name() + "?_sort=status,url,version"),
				new Element(null, null, ResourceType.Binary.name(), ResourceType.Binary.name(), null, null,
						String.valueOf(statistics.binaries()), ResourceType.Binary.name()),
				new Element(null, null, ResourceType.DocumentReference.name(), ResourceType.DocumentReference.name(),
						null, null, String.valueOf(statistics.documentReferences()),
						ResourceType.DocumentReference.name()),
				new Element("active", null, ResourceType.Endpoint.name(), ResourceType.Endpoint.name(), "active", null,
						String.valueOf(statistics.endpoints()), ResourceType.Endpoint.name() + "?status=active"),
				new Element(null, null, ResourceType.Library.name(), ResourceType.Library.name(), null, null,
						String.valueOf(statistics.libraries()), ResourceType.Library.name()),
				new Element(null, null, ResourceType.Measure.name(), ResourceType.Measure.name(), null, null,
						String.valueOf(statistics.measures()), ResourceType.Measure.name()),
				new Element(null, null, ResourceType.MeasureReport.name(), ResourceType.MeasureReport.name(), null,
						null, String.valueOf(statistics.measureReports()), ResourceType.MeasureReport.name()),
				new Element(null, true, ResourceType.Organization.name(), ResourceType.Organization.name(),
						"member, active", null, String.valueOf(statistics.organizationsMember()),
						ResourceType.Organization.name()
								+ "?_profile=http://dsf.dev/fhir/StructureDefinition/organization"),
				new Element(null, true, ResourceType.Organization.name(), ResourceType.Organization.name(),
						"parent, active", null, String.valueOf(statistics.organizationsParent()),
						ResourceType.Organization.name()
								+ "?_profile=http://dsf.dev/fhir/StructureDefinition/organization-parent"),
				new Element(null, true, ResourceType.OrganizationAffiliation.name(),
						ResourceType.OrganizationAffiliation.name(), "active", null,
						String.valueOf(statistics.organizationAffiliations()),
						ResourceType.OrganizationAffiliation + "?active=true"),
				new Element("in-progress", null, ResourceType.QuestionnaireResponse.name(),
						ResourceType.QuestionnaireResponse.name(), "in-progress", "24h",
						String.valueOf(statistics.questionnaireResponsesInProgress24h()),
						ResourceType.QuestionnaireResponse.name() + "?status=in-progress&_lastUpdated=ge"
								+ minusOneDay),
				new Element("amended", null, ResourceType.QuestionnaireResponse.name(),
						ResourceType.QuestionnaireResponse.name(), "amended", "24h",
						String.valueOf(statistics.questionnaireResponsesAmended24h()),
						ResourceType.QuestionnaireResponse.name() + "?status=amended&_lastUpdated=ge" + minusOneDay),
				new Element("in-progress", null, ResourceType.QuestionnaireResponse.name(),
						ResourceType.QuestionnaireResponse.name(), "in-progress", "30d",
						String.valueOf(statistics.questionnaireResponsesInProgress30d()),
						ResourceType.QuestionnaireResponse.name() + "?status=in-progress&_lastUpdated=ge"
								+ minusThirtyDays),
				new Element("amended", null, ResourceType.QuestionnaireResponse.name(),
						ResourceType.QuestionnaireResponse.name(), "amended", "30d",
						String.valueOf(statistics.questionnaireResponsesAmended30d()),
						ResourceType.QuestionnaireResponse.name() + "?status=amended&_lastUpdated=ge"
								+ minusThirtyDays),
				new Element("in-progress", null, ResourceType.QuestionnaireResponse.name(),
						ResourceType.QuestionnaireResponse.name(), "in-progress", null,
						String.valueOf(statistics.questionnaireResponsesInProgress()),
						ResourceType.QuestionnaireResponse.name() + "?status=in-progress"),
				new Element("amended", null, ResourceType.QuestionnaireResponse.name(),
						ResourceType.QuestionnaireResponse.name(), "amended", null,
						String.valueOf(statistics.questionnaireResponsesAmended()),
						ResourceType.QuestionnaireResponse.name() + "?status=amended"),
				new Element("draft", null, ResourceType.Task.name(), ResourceType.Task.name(), "draft", null,
						String.valueOf(statistics.tasksDraft()),
						ResourceType.Task.name() + "?status=draft&_sort=_profile,identifier"),
				new Element("in-progress", null, ResourceType.Task.name(), ResourceType.Task.name(), "in-progress",
						"24h", String.valueOf(statistics.tasksInProgress24h()),
						ResourceType.Task.name() + "?status=in-progress&_lastUpdated=ge" + minusOneDay),
				new Element("completed", null, ResourceType.Task.name(), ResourceType.Task.name(), "completed", "24h",
						String.valueOf(statistics.tasksCompleted24h()),
						ResourceType.Task.name() + "?status=completed&_lastUpdated=ge" + minusOneDay),
				new Element("failed", null, ResourceType.Task.name(), ResourceType.Task.name(), "failed", "24h",
						String.valueOf(statistics.tasksFailed24h()),
						ResourceType.Task.name() + "?status=failed&_lastUpdated=ge" + minusOneDay),
				new Element("in-progress", null, ResourceType.Task.name(), ResourceType.Task.name(), "in-progress",
						"30d", String.valueOf(statistics.tasksInProgress30d()),
						ResourceType.Task.name() + "?status=in-progress&_lastUpdated=ge" + minusThirtyDays),
				new Element("completed", null, ResourceType.Task.name(), ResourceType.Task.name(), "completed", "30d",
						String.valueOf(statistics.tasksCompleted30d()),
						ResourceType.Task.name() + "?status=completed&_lastUpdated=ge" + minusThirtyDays),
				new Element("failed", null, ResourceType.Task.name(), ResourceType.Task.name(), "failed", "30d",
						String.valueOf(statistics.tasksFailed30d()),
						ResourceType.Task.name() + "?status=failed&_lastUpdated=ge" + minusThirtyDays),
				new Element("in-progress", null, ResourceType.Task.name(), ResourceType.Task.name(), "in-progress",
						null, String.valueOf(statistics.tasksInProgress()),
						ResourceType.Task.name() + "?status=in-progress"),
				new Element("completed", null, ResourceType.Task.name(), ResourceType.Task.name(), "completed", null,
						String.valueOf(statistics.tasksCompleted()), ResourceType.Task.name() + "?status=completed"),
				new Element("failed", null, ResourceType.Task.name(), ResourceType.Task.name(), "failed", null,
						String.valueOf(statistics.tasksFailed()), ResourceType.Task.name() + "?status=failed"));
	}

	private ByteSize formatBytes(long bytes)
	{
		if (bytes < 0)
			return new ByteSize(0, "B");

		double value = bytes;
		int unitIndex = 0;

		while (value >= 1024 && unitIndex < BYTE_UNITS.length - 1)
		{
			value /= 1024;
			unitIndex++;
		}

		return new ByteSize(value, BYTE_UNITS[unitIndex]);
	}

}
