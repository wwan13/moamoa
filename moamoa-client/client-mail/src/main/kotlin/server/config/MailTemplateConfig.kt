package server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

@Configuration
class MailTemplateConfig {

    @Bean
    fun emailTemplateResolver(): ClassLoaderTemplateResolver {
        return ClassLoaderTemplateResolver().apply {
            prefix = "templates/mail/"
            suffix = ".html"
            characterEncoding = "UTF-8"
            setTemplateMode("HTML")
            isCacheable = false
        }
    }

    @Bean
    fun emailTemplateEngine(
        emailTemplateResolver: ClassLoaderTemplateResolver
    ) = SpringTemplateEngine().apply {
        setTemplateResolver(emailTemplateResolver)
    }
}