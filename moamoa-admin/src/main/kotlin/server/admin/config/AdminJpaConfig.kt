package server.admin.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan(basePackages = ["server.admin.domain"])
@EnableJpaRepositories(basePackages = ["server.admin.domain"])
class AdminJpaConfig
