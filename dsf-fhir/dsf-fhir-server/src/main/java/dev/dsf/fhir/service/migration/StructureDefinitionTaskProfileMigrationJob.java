package dev.dsf.fhir.service.migration;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.SnapshotGenerator.SnapshotWithValidationMessages;

public class StructureDefinitionTaskProfileMigrationJob implements MigrationJob, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(StructureDefinitionTaskProfileMigrationJob.class);

	private static final String P_STRUCTURE_DEFINITION = "http://dsf.dev/fhir/StructureDefinition/structure-definition";

	private static final String BD_TASK = "http://dsf.dev/fhir/StructureDefinition/task";
	private static final String BD_TASK_BASE = "http://dsf.dev/fhir/StructureDefinition/task-base";

	private final StructureDefinitionDao dao;
	private final StructureDefinitionDao snapshotDao;
	private final SnapshotGenerator snapshotGenerator;
	private final ExceptionHandler exceptionHandler;
	private final EventHandler eventHandler;
	private final EventGenerator eventGenerator;

	/**
	 * @param dao
	 *            not <code>null</code>
	 * @param snapshotDao
	 *            not <code>null</code>
	 * @param snapshotGenerator
	 *            not <code>null</code>
	 * @param exceptionHandler
	 *            not <code>null</code>
	 * @param eventHandler
	 *            not <code>null</code>
	 * @param eventGenerator
	 *            not <code>null</code>
	 */
	public StructureDefinitionTaskProfileMigrationJob(StructureDefinitionDao dao, StructureDefinitionDao snapshotDao,
			SnapshotGenerator snapshotGenerator, ExceptionHandler exceptionHandler, EventHandler eventHandler,
			EventGenerator eventGenerator)
	{
		this.dao = dao;
		this.snapshotDao = snapshotDao;
		this.snapshotGenerator = snapshotGenerator;
		this.exceptionHandler = exceptionHandler;
		this.eventHandler = eventHandler;
		this.eventGenerator = eventGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dao, "dao");
		Objects.requireNonNull(snapshotDao, "snapshotDao");
		Objects.requireNonNull(snapshotGenerator, "snapshotGenerator");
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		Objects.requireNonNull(eventHandler, "eventHandler");
		Objects.requireNonNull(eventGenerator, "eventGenerator");
	}

	@Override
	public void execute() throws Exception
	{
		try (Connection connection = dao.newReadWriteTransaction())
		{
			List<StructureDefinition> sds = dao.readAllByBaseDefinitionWithTransaction(connection, BD_TASK_BASE);

			for (StructureDefinition sd : sds)
			{
				logger.info("Updating StructureDefinition {}|{}", sd.getUrl(), sd.getVersion());

				sd.setBaseDefinition(BD_TASK);

				removeVersionsFromDsfValueSetBindings(sd);

				List<CanonicalType> oldProfiles = sd.getMeta().getProfile();
				List<CanonicalType> newProfiles = Stream
						.concat(Stream.of(P_STRUCTURE_DEFINITION),
								oldProfiles.stream().filter(CanonicalType::hasValue).map(CanonicalType::getValue))
						.distinct().map(CanonicalType::new).toList();

				sd.getMeta().setProfile(newProfiles);

				StructureDefinition updated = dao.update(sd);

				try
				{
					logger.info("Generating new snapshot for StructureDefinition {}|{}", updated.getUrl(),
							updated.getVersion());

					SnapshotWithValidationMessages s = snapshotGenerator.generateSnapshot(updated);
					if (s != null && s.getSnapshot() != null && s.getMessages().isEmpty())
					{
						exceptionHandler.catchAndLogSqlAndResourceNotFoundException("StructureDefinition",
								() -> snapshotDao.update(s.getSnapshot()));
					}

					eventHandler.handleEvent(eventGenerator.newResourceUpdatedEvent(updated));
				}
				catch (Exception e)
				{
					logger.debug("Error while generating snapshot for StructureDefinition with id {}",
							updated.getIdElement().getIdPart(), e);
					logger.warn("Error while generating snapshot for StructureDefinition with id {}: {} - {}",
							updated.getIdElement().getIdPart(), e.getClass().getName(), e.getMessage());
				}
			}
		}
	}

	private void removeVersionsFromDsfValueSetBindings(StructureDefinition sd)
	{
		sd.getDifferential().getElement().stream().filter(ElementDefinition::hasBinding)
				.map(ElementDefinition::getBinding).filter(ElementDefinitionBindingComponent::hasValueSet).forEach(b ->
				{
					String newValueSet = switch (b.getValueSet())
					{
						case "http://dsf.dev/fhir/CodeSystem/bpmn-message|1.0.0", "http://dsf.dev/fhir/CodeSystem/bpmn-message|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/bpmn-message";
						case "http://dsf.dev/fhir/CodeSystem/organization-role|1.0.0", "http://dsf.dev/fhir/CodeSystem/organization-role|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/organization-role";
						case "http://dsf.dev/fhir/CodeSystem/practitioner-role|1.0.0", "http://dsf.dev/fhir/CodeSystem/practitioner-role|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/practitioner-role";
						case "http://dsf.dev/fhir/CodeSystem/process-authorization|1.0.0", "http://dsf.dev/fhir/CodeSystem/process-authorization|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/process-authorization";
						case "http://dsf.dev/fhir/CodeSystem/read-access-tag|1.0.0", "http://dsf.dev/fhir/CodeSystem/read-access-tag|2.0.0" -> "http://dsf.dev/fhir/CodeSystem/read-access-tag";
						default -> b.getValueSet();
					};

					b.setValueSet(newValueSet);
				});
	}
}
