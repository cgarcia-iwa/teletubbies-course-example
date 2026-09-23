package com.teletubbies.course;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class PostgresContainerBaseTest {

  @ServiceConnection
  protected static final PostgreSQLContainer<?> postgresContainer =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine")).withReuse(true);

  static {
    postgresContainer.start();
  }
}
