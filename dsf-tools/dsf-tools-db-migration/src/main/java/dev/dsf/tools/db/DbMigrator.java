package dev.dsf.tools.db;

import java.io.ByteArrayOutputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Scope;
import liquibase.changelog.ChangeLogParameters;
import liquibase.command.CommandScope;
import liquibase.command.core.ReleaseLocksCommandStep;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DatabaseChangelogCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.configuration.AbstractMapConfigurationValueProvider;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.ui.LoggerUIService;

public final class DbMigrator
{
	private static final Logger logger = LoggerFactory.getLogger(DbMigrator.class);

	private static final class DbMigratorExceptions extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		DbMigratorExceptions(Throwable cause)
		{
			super(cause);
		}
	}

	private static final class LiquibaseConfigProvider extends AbstractMapConfigurationValueProvider
	{
		static final String LIQUIBASE_CHANGELOGLOCK_WAIT_TIME = "liquibase.changelogLockWaitTimeInMinutes";

		final Map<String, Object> map = new HashMap<>();

		LiquibaseConfigProvider(long changelogLockWaitTimeInMinutes)
		{
			map.put(LIQUIBASE_CHANGELOGLOCK_WAIT_TIME, changelogLockWaitTimeInMinutes);
		}

		@Override
		public int getPrecedence()
		{
			return 350;
		}

		@Override
		protected Map<?, ?> getMap()
		{
			return map;
		}

		@Override
		protected String getSourceDescription()
		{
			return "DSF config";
		}
	}

	private static final Set<String> POSTGRES_TRY_AGAIN_ERROR_MESSAGES = Set.of("the database system is starting up",
			"the database system is not yet accepting connections");

	private final DbMigratorConfig config;

	public DbMigrator(DbMigratorConfig config)
	{
		this.config = config;
	}

	public void migrate()
	{
		try
		{
			Scope.child(Scope.Attr.ui, new LoggerUIService(), () ->
			{
				try (BasicDataSource dataSource = new BasicDataSource())
				{
					dataSource.setDriverClassName(Driver.class.getName());
					dataSource.setUrl(config.getDbUrl());
					dataSource.setUsername(config.getDbLiquibaseUsername());
					dataSource.setPassword(toString(config.getDbLiquibasePassword()));

					LiquibaseConfiguration liquibaseConfiguration = Scope.getCurrentScope()
							.getSingleton(LiquibaseConfiguration.class);
					liquibaseConfiguration
							.registerProvider(new LiquibaseConfigProvider(config.getLiquibaseLockWaitTime()));

					try (Connection connection = dataSource.getConnection())
					{
						Database database = DatabaseFactory.getInstance()
								.findCorrectDatabaseImplementation(new JdbcConnection(connection));

						if (config.forceLiquibaseUnlock())
						{
							ByteArrayOutputStream output = new ByteArrayOutputStream();
							CommandScope unlockCommand = new CommandScope(ReleaseLocksCommandStep.COMMAND_NAME);
							unlockCommand.addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database);
							unlockCommand.setOutput(output);

							logger.warn("Unlocking DB for migration ...");
							unlockCommand.execute();

							Arrays.stream(output.toString().split("[\r\n]+")).filter(row -> !row.isBlank())
									.forEach(row -> logger.debug("{}", row));
							logger.warn("Unlocking DB for migration [Done]");
						}

						ChangeLogParameters changeLogParameters = new ChangeLogParameters(database);
						config.getChangeLogParameters().forEach(changeLogParameters::set);

						ByteArrayOutputStream output = new ByteArrayOutputStream();
						CommandScope updateCommand = new CommandScope(UpdateCommandStep.COMMAND_NAME);
						updateCommand.addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database);
						updateCommand.addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, "db/db.changelog.xml");
						updateCommand.addArgumentValue(UpdateCommandStep.CONTEXTS_ARG, new Contexts().toString());
						updateCommand.addArgumentValue(UpdateCommandStep.LABEL_FILTER_ARG,
								new LabelExpression().getOriginalString());
						updateCommand.addArgumentValue(DatabaseChangelogCommandStep.CHANGELOG_PARAMETERS,
								changeLogParameters);
						updateCommand.setOutput(output);

						logger.info("Executing DB migration ...");
						updateCommand.execute();

						Arrays.stream(output.toString().split("[\r\n]+")).filter(row -> !row.isBlank())
								.forEach(row -> logger.debug("{}", row));
						logger.info("Executing DB migration [Done]");
					}
				}
				catch (SQLException e)
				{
					logger.warn("Error while accessing db: {}", e.getMessage());
					throw new DbMigratorExceptions(e);
				}
				// no special case for LiquibaseException
				catch (Exception e)
				{
					logger.warn("Error while running liquibase: {}", e.getMessage());
					throw new DbMigratorExceptions(e);
				}
			});
		}
		catch (DbMigratorExceptions e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.warn("Error while running liquibase: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private String toString(char[] password)
	{
		return password == null ? null : String.valueOf(password);
	}

	public static void retryOnConnectException(int times, Runnable run)
	{
		if (times <= 0)
			return;

		try
		{
			run.run();
		}
		catch (RuntimeException e)
		{
			Throwable cause = e;
			while (!(cause instanceof ConnectException) && cause.getCause() != null)
				cause = cause.getCause();

			if (cause instanceof ConnectException && times > 1)
			{
				logger.warn("ConnectException: trying again in 5s");
				try
				{
					Thread.sleep(5000);
				}
				catch (InterruptedException e1)
				{
				}
				retryOnConnectException(--times, run);
			}
			else if (cause instanceof UnknownHostException && times > 1)
			{
				logger.warn("UnknownHostException: trying again in 10s");
				try
				{
					Thread.sleep(10_000);
				}
				catch (InterruptedException e1)
				{
				}
				retryOnConnectException(--times, run);
			}
			else if (cause instanceof PSQLException p
					&& POSTGRES_TRY_AGAIN_ERROR_MESSAGES.contains(p.getServerErrorMessage().getMessage()) && times > 1)
			{
				logger.warn("PSQLException ({}): trying again in 5s", p.getServerErrorMessage().getMessage());
				try
				{
					Thread.sleep(10_000);
				}
				catch (InterruptedException e1)
				{
				}
				retryOnConnectException(--times, run);
			}
			else
			{
				logger.error("Error while running liquibase: {}", e.getMessage());
				throw e;
			}
		}
	}
}
