package dev.dsf.bpe.plugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.api.plugin.BpmnFileAndModel;
import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.dao.ProcessStateDao;

public class BpmnProcessStateChangeServiceImpl implements BpmnProcessStateChangeService, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(BpmnProcessStateChangeServiceImpl.class);

	private final RepositoryService repositoryService;
	private final ProcessStateDao processStateDao;

	private final Set<ProcessIdAndVersion> excluded = new HashSet<>();
	private final Set<ProcessIdAndVersion> retired = new HashSet<>();

	public BpmnProcessStateChangeServiceImpl(RepositoryService repositoryService, ProcessStateDao processStateDao,
			List<ProcessIdAndVersion> excluded, List<ProcessIdAndVersion> retired)
	{
		this.repositoryService = repositoryService;
		this.processStateDao = processStateDao;

		if (excluded != null)
			this.excluded.addAll(excluded);
		if (retired != null)
			this.retired.addAll(retired);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(repositoryService, "repositoryService");
		Objects.requireNonNull(processStateDao, "processStateDao");

		logger.info("Excluded processes: {}", excluded);
		logger.info("Retired processes: {}", retired);
	}

	private Map<ProcessIdAndVersion, ProcessState> getStates()
	{
		try
		{
			return processStateDao.getStates();
		}
		catch (SQLException e)
		{
			logger.debug("Error while retrieving process states from db", e);
			logger.warn("Error while retrieving process states from db: {} - {}", e.getClass().getName(),
					e.getMessage());

			throw new RuntimeException(e);
		}
	}

	@Override
	public List<ProcessStateChangeOutcome> deploySuspendOrActivateProcesses(List<BpmnFileAndModel> models)
	{
		Objects.requireNonNull(models, "models");

		Map<ProcessIdAndVersion, ProcessState> oldProcessStates = getStates();
		Map<ProcessIdAndVersion, ProcessState> newProcessStates = new HashMap<>();

		logger.debug("Deploying process models ...");
		models.forEach(this::deploy);

		Set<ProcessIdAndVersion> loadedProcesses = models.stream().map(BpmnFileAndModel::toProcessIdAndVersion)
				.collect(Collectors.toSet());
		Set<ProcessIdAndVersion> draft = models.stream().filter(BpmnFileAndModel::draft)
				.map(BpmnFileAndModel::toProcessIdAndVersion).collect(Collectors.toSet());

		List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery().list();
		for (ProcessDefinition definition : definitions)
		{
			ProcessIdAndVersion process = ProcessIdAndVersion.fromDefinition(definition);

			ProcessState oldState = oldProcessStates.getOrDefault(process, ProcessState.NEW);

			ProcessState newState = loadedProcesses.contains(process) ? ProcessState.ACTIVE : ProcessState.EXCLUDED;
			if (excluded.contains(process))
				newState = ProcessState.EXCLUDED;
			else if (retired.contains(process))
				newState = ProcessState.RETIRED;
			else if (draft.contains(process))
				newState = ProcessState.DRAFT;

			newProcessStates.put(process, newState);

			logger.debug("Process {} state change: {} -> {}", process.toString(), oldState, newState);

			// NEW -> ACTIVE : - (new process active by default)
			// NEW -> DRAFT : - (new process active by default)
			// NEW -> RETIRED : suspend
			// NEW -> EXCLUDED : suspend
			// ACTIVE -> ACTIVE : -
			// ACTIVE -> DRAFT : -
			// ACTIVE -> RETIRED : suspend
			// ACTIVE -> EXCLUDED : suspend
			// DRAFT -> ACTIVE : -
			// DRAFT -> DRAFT : -
			// DRAFT -> RETIRED : suspend
			// DRAFT -> EXCLUDED : suspend
			// RETIRED -> ACTIVE : activate
			// RETIRED -> DRAFT : activate
			// RETIRED -> RETIRED : -
			// RETIRED -> EXCLUDED : -
			// EXCLUDED -> ACTIVE : activate
			// EXCLUDED -> DRAFT : activate
			// EXCLUDED -> RETIRED : -
			// EXCLUDED -> EXCLUDED : -

			if ((ProcessState.RETIRED.equals(oldState) && ProcessState.ACTIVE.equals(newState))
					|| (ProcessState.RETIRED.equals(oldState) && ProcessState.DRAFT.equals(newState))
					|| (ProcessState.EXCLUDED.equals(oldState) && ProcessState.ACTIVE.equals(newState))
					|| (ProcessState.EXCLUDED.equals(oldState) && ProcessState.DRAFT.equals(newState)))
			{
				logger.debug("Activating process {}", process.toString());
				repositoryService.activateProcessDefinitionById(definition.getId());
			}
			else if ((ProcessState.NEW.equals(oldState) && ProcessState.RETIRED.equals(newState))
					|| (ProcessState.NEW.equals(oldState) && ProcessState.EXCLUDED.equals(newState))
					|| (ProcessState.ACTIVE.equals(oldState) && ProcessState.RETIRED.equals(newState))
					|| (ProcessState.ACTIVE.equals(oldState) && ProcessState.EXCLUDED.equals(newState))
					|| (ProcessState.DRAFT.equals(oldState) && ProcessState.RETIRED.equals(newState))
					|| (ProcessState.DRAFT.equals(oldState) && ProcessState.EXCLUDED.equals(newState)))
			{
				logger.debug("Suspending process {}", process.toString());
				repositoryService.suspendProcessDefinitionById(definition.getId());
			}
		}

		updateStates(newProcessStates);

		logProcessDeploymentStatus();

		return newProcessStates.entrySet().stream()
				.map(e -> new ProcessStateChangeOutcome(e.getKey(),
						oldProcessStates.getOrDefault(e.getKey(), ProcessState.NEW), e.getValue()))
				.collect(Collectors.toList());
	}

	private void updateStates(Map<ProcessIdAndVersion, ProcessState> states)
	{
		try
		{
			processStateDao.updateStates(states);
		}
		catch (SQLException e)
		{
			logger.debug("Error while updating process states in db", e);
			logger.warn("Error while updating process states in db: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	private void logProcessDeploymentStatus()
	{
		Map<String, Deployment> deploymentsById = repositoryService.createDeploymentQuery().orderByDeploymentName()
				.asc().orderByDeploymentTime().desc().list().stream()
				.collect(Collectors.toMap(Deployment::getId, Function.identity()));

		List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
				.orderByProcessDefinitionKey().asc().orderByVersionTag().asc().orderByProcessDefinitionVersion().asc()
				.list();

		// standard for-each loop to produce cleaner log messages
		for (ProcessDefinition def : definitions)
		{
			Deployment dep = deploymentsById.get(def.getDeploymentId());

			if (def.isSuspended())
				logger.debug("Suspended process {}/{} (internal version {}) from {} deployed {}", def.getKey(),
						def.getVersionTag(), def.getVersion(), dep.getSource(), dep.getDeploymentTime());
			else
				logger.info("Active process {}/{} (internal version {}) from {} deployed {}", def.getKey(),
						def.getVersionTag(), def.getVersion(), dep.getSource(), dep.getDeploymentTime());
		}
	}

	private void deploy(BpmnFileAndModel fileAndModel)
	{
		ProcessIdAndVersion processKeyAndVersion = fileAndModel.toProcessIdAndVersion();

		DeploymentBuilder builder = repositoryService.createDeployment().name(processKeyAndVersion.toString())
				.source(fileAndModel.file()).addModelInstance(fileAndModel.file(), fileAndModel.model())
				.enableDuplicateFiltering(true).tenantId(String.valueOf(fileAndModel.processPluginApiVersion()));

		Deployment deployment = builder.deploy();

		logger.debug("Process {} from {}://{} deployed with id {}", processKeyAndVersion.toString(),
				fileAndModel.jar().toString(), fileAndModel.file(), deployment.getId());

		if (fileAndModel.draft())
		{
			List<ProcessDefinition> activeDraftDefinitions = repositoryService.createProcessDefinitionQuery()
					.processDefinitionKey(processKeyAndVersion.getId()).versionTag(processKeyAndVersion.getVersion())
					.orderByDeploymentTime().desc().active().list();

			activeDraftDefinitions.stream().skip(1).forEach(def ->
			{
				logger.debug("Suspending existing draft process definition {} from deployment with id {}",
						processKeyAndVersion.toString(), def.getDeploymentId());
				repositoryService.suspendProcessDefinitionById(def.getId());
			});
		}
	}
}
