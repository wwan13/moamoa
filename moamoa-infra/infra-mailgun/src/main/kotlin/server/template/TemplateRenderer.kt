package server.template

import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Component
internal class TemplateRenderer(
    private val templateEngine: SpringTemplateEngine,
) {

    fun render(path: String, args: Map<String, Any>): String {
        val templateContext = Context().apply {
            args.forEach { (key, value) ->
                setVariable(key, value)
            }
        }

        return templateEngine.process(path, templateContext)
    }
}