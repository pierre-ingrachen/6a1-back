package com.takima.backskeleton.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionCorsOriginsValidatorTest {
    private final ApplicationContextRunner prodRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProductionCorsOriginsValidator.class)
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    void startsWithExplicitOrigins() {
        prodRunner.withPropertyValues("app.cors.allowed-origins=https://front.example,https://admin.example")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void refusesMissingOrigins() {
        prodRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void refusesEmptyOrigins() {
        prodRunner.withPropertyValues("app.cors.allowed-origins=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void refusesWildcardOrigin() {
        prodRunner.withPropertyValues("app.cors.allowed-origins=*")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void refusesWildcardAmongOrigins() {
        prodRunner.withPropertyValues("app.cors.allowed-origins=https://front.example,*")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void isInactiveOutsideProduction() {
        new ApplicationContextRunner()
                .withUserConfiguration(ProductionCorsOriginsValidator.class)
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(ProductionCorsOriginsValidator.class));
    }
}
