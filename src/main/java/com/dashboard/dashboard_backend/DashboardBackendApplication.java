package com.dashboard.dashboard_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(
    exclude = { ManagementWebSecurityAutoConfiguration.class },
    scanBasePackages = {"com.dashboard.backend", "com.dashboard.dashboard_backend"}
)
@EnableScheduling
@EntityScan("com.dashboard.backend.entity")
@EnableJpaRepositories("com.dashboard.backend.repository")
@EnableConfigurationProperties

public class DashboardBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DashboardBackendApplication.class, args);
	}

}
