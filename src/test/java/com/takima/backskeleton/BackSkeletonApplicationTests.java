package com.takima.backskeleton;

import com.takima.backskeleton.support.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(PostgresTestConfiguration.class)
class BackSkeletonApplicationTests {

	@Test
	void contextLoads() {
	}

}
