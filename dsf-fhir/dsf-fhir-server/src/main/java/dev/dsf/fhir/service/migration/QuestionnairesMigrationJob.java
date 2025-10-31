package dev.dsf.fhir.service.migration;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.dao.QuestionnaireDao;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.event.EventHandler;

public class QuestionnairesMigrationJob implements MigrationJob, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(QuestionnairesMigrationJob.class);

	private static final String P_QUESTIONNAIRE = "http://dsf.dev/fhir/StructureDefinition/questionnaire";
	private static final String P_QUESTIONNAIRE_1_5_0 = "http://dsf.dev/fhir/StructureDefinition/questionnaire|1.5.0";
	private static final String P_QUESTIONNAIRE_1_0_0 = "http://dsf.dev/fhir/StructureDefinition/questionnaire|1.0.0";

	private final QuestionnaireDao dao;
	private final EventHandler eventHandler;
	private final EventGenerator eventGenerator;

	/**
	 * @param dao
	 *            not <code>null</code>
	 * @param eventHandler
	 *            not <code>null</code>
	 * @param eventGenerator
	 *            not <code>null</code>
	 */
	public QuestionnairesMigrationJob(QuestionnaireDao dao, EventHandler eventHandler, EventGenerator eventGenerator)
	{
		this.dao = dao;
		this.eventHandler = eventHandler;
		this.eventGenerator = eventGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dao, "dao");
		Objects.requireNonNull(eventHandler, "eventHandler");
		Objects.requireNonNull(eventGenerator, "eventGenerator");
	}

	@Override
	public void execute() throws Exception
	{
		try (Connection connection = dao.newReadWriteTransaction())
		{
			List<Questionnaire> qs150 = dao.readAllByProfileWithTransaction(connection, P_QUESTIONNAIRE_1_5_0);
			for (Questionnaire q : qs150)
			{
				updateProfile(q, P_QUESTIONNAIRE_1_5_0);

				logger.info("Updating Questionnaire {}|{}", q.getUrl(), q.getVersion());
				Questionnaire updated = dao.update(q);

				eventHandler.handleEvent(eventGenerator.newResourceUpdatedEvent(updated));
			}

			List<Questionnaire> qs100 = dao.readAllByProfileWithTransaction(connection, P_QUESTIONNAIRE_1_0_0);
			for (Questionnaire q : qs100)
			{
				setRequired(q.getItem());

				updateProfile(q, P_QUESTIONNAIRE_1_0_0);

				logger.info("Updating Questionnaire {}|{}", q.getUrl(), q.getVersion());
				Questionnaire updated = dao.update(q);

				eventHandler.handleEvent(eventGenerator.newResourceUpdatedEvent(updated));
			}
		}
	}

	private void setRequired(List<QuestionnaireItemComponent> items)
	{
		items.stream().filter(QuestionnaireItemComponent::hasLinkId).filter(QuestionnaireItemComponent::hasType)
				.filter(i -> !QuestionnaireItemType.DISPLAY.equals(i.getType())).forEach(i ->
				{
					switch (i.getLinkId())
					{
						case "business-key", "user-task-id" -> i.setRequired(true);
						default -> i.setRequired(false);
					}

					if (i.hasItem())
						setRequired(i.getItem());
				});
	}

	private void updateProfile(Questionnaire q, String oldProfile)
	{
		List<String> oldProfiles = q.getMeta().getProfile().stream().filter(CanonicalType::hasValue)
				.map(CanonicalType::getValue).toList();

		List<CanonicalType> newProfiles = Stream
				.concat(oldProfiles.stream().filter(p -> !oldProfile.equals(p)), Stream.of(P_QUESTIONNAIRE)).distinct()
				.map(CanonicalType::new).toList();

		q.getMeta().setProfile(newProfiles);
	}
}
