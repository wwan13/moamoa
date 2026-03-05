package server.core.global.config

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@ConfigurationPropertiesScan(basePackages = ["server"])
@EnableScheduling
class CoreApiConfig
