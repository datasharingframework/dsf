package dev.dsf.bpe.dao;

import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractDaoJdbc implements InitializingBean
{
	protected final DataSource dataSource;

	public AbstractDaoJdbc(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dataSource, "dataSource");
	}
}
