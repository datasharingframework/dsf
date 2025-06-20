package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Subscription;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.SubscriptionDao;
import dev.dsf.fhir.search.filter.SubscriptionIdentityFilter;
import dev.dsf.fhir.search.parameters.SubscriptionCriteria;
import dev.dsf.fhir.search.parameters.SubscriptionPayload;
import dev.dsf.fhir.search.parameters.SubscriptionStatus;
import dev.dsf.fhir.search.parameters.SubscriptionType;

public class SubscriptionDaoJdbc extends AbstractResourceDaoJdbc<Subscription> implements SubscriptionDao
{
	public SubscriptionDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Subscription.class, "subscriptions", "subscription",
				"subscription_id", SubscriptionIdentityFilter::new,
				List.of(factory(SubscriptionCriteria.PARAMETER_NAME, SubscriptionCriteria::new,
						SubscriptionCriteria.getNameModifiers()),
						factory(SubscriptionPayload.PARAMETER_NAME, SubscriptionPayload::new,
								SubscriptionPayload.getNameModifiers()),
						factory(SubscriptionStatus.PARAMETER_NAME, SubscriptionStatus::new,
								SubscriptionStatus.getNameModifiers()),
						factory(SubscriptionType.PARAMETER_NAME, SubscriptionType::new,
								SubscriptionType.getNameModifiers())),
				List.of());
	}

	@Override
	protected Subscription copy(Subscription resource)
	{
		return resource.copy();
	}

	@Override
	public List<Subscription> readByStatus(Subscription.SubscriptionStatus status) throws SQLException
	{
		if (status == null)
			return Collections.emptyList();

		try (Connection connection = getDataSource().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT subscription FROM current_subscriptions WHERE subscription->>'status' = ?"))
		{
			statement.setString(1, status.toCode());

			try (ResultSet result = statement.executeQuery())
			{
				List<Subscription> all = new ArrayList<>();

				while (result.next())
					all.add(getResource(result, 1));

				return all;
			}
		}
	}

	@Override
	public boolean existsByCriteriaChannelTypeAndChannelPayload(String criteria, String channelType,
			String channelPayload) throws SQLException
	{
		try (Connection connection = getDataSource().getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT count(*) FROM current_subscriptions WHERE "
								+ "subscription->>'criteria' = ? AND subscription->'channel'->>'type' = ? AND "
								+ (channelPayload == null ? "NOT subscription->'channel' ?? 'payload'"
										: "subscription->'channel'->>'payload' = ?")))
		{
			statement.setString(1, criteria);
			statement.setString(2, channelType);

			if (channelPayload != null)
				statement.setString(3, channelPayload);

			try (ResultSet result = statement.executeQuery())
			{
				return result.next() && result.getInt(1) > 0;
			}
		}
	}
}
