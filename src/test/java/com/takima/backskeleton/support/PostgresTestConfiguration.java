package com.takima.backskeleton.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {
    private static final String INIT_DIRECTORY = "/docker-entrypoint-initdb.d/";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withCopyFileToContainer(MountableFile.forHostPath("initdb/01_schema.sql"), INIT_DIRECTORY + "01_schema.sql")
                .withCopyFileToContainer(MountableFile.forClasspathResource("db/02_notes_columns.sql"), INIT_DIRECTORY + "02_notes_columns.sql")
                .withCopyFileToContainer(MountableFile.forClasspathResource("db/03_test_data.sql"), INIT_DIRECTORY + "03_test_data.sql");
    }
}
