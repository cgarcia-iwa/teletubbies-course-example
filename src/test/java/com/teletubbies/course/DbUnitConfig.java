package com.teletubbies.course;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import javax.sql.DataSource;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbUnitConfig {

  @Bean
  public IDataTypeFactory dataTypeFactory() {
    return new PostgresqlDataTypeFactory();
  }

  @Bean
  public DatabaseDataSourceConnectionFactoryBean dbUnitDatabaseConnection(
      final DataSource dataSource, final IDataTypeFactory dataTypeFactory) {
    final DatabaseConfigBean databaseConfigBean = new DatabaseConfigBean();
    databaseConfigBean.setDatatypeFactory(dataTypeFactory);
    final DatabaseDataSourceConnectionFactoryBean connectionFactory =
        new DatabaseDataSourceConnectionFactoryBean(dataSource);
    connectionFactory.setDatabaseConfig(databaseConfigBean);
    return connectionFactory;
  }
}
