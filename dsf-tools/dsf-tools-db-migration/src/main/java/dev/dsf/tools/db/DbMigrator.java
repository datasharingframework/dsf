package dev.dsf.tools.db;

import java.io.ByteArrayOutputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Scope;
import liquibase.changelog.ChangeLogParameters;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DatabaseChangelogCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
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

					try (Connection connection = dataSource.getConnection())
					{
						Database database = DatabaseFactory.getInstance()
								.findCorrectDatabaseImplementation(new JdbcConnection(connection));

						ChangeLogParameters changeLogParameters = new ChangeLogParameters(database);
						config.getChangeLogParameters().forEach(changeLogParameters::set);
						ByteArrayOutputStream output = new ByteArrayOutputStream();

						CommandScope updateCommand = new CommandScope(UpdateCommandStep.COMMAND_NAME);
						updateCommand.addArgumentValue(DbUrlConnectionCommandStep.DATABASE_ARG, database);
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
				catch (LiquibaseException e)
				{
					logger.warn("Error while running liquibase: {}", e.getMessage());
					throw new DbMigratorExceptions(e);
				}
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
			else
			{
				logger.error("Error while running liquibase: {}", e.getMessage());
				throw e;
			}
		}
	}
}
