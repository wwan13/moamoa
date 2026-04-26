package server.template.mail

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

private object MailTemplateMapCache {

    private val propsByClass =
        MailTemplate::class.sealedSubclasses.associateWith { kClass ->
            kClass.memberProperties
                .filter { it.name != "target" }
                .onEach { it.isAccessible = true }
        }

    fun toMap(template: MailTemplate): Map<String, Any> {
        val props = propsByClass[template::class]
            ?: throw IllegalStateException("캐시에 없는 MailTemplate 타입: ${template::class.qualifiedName}")

        return props.associate { prop ->
            val value = prop.getter.call(template)
                ?: throw IllegalStateException(
                    "MailTemplate ${template::class.simpleName}.${prop.name} 는 null일 수 없습니다."
                )
            prop.name to value
        }
    }
}

fun MailTemplate.toTemplateArgs(): Map<String, Any> =
    MailTemplateMapCache.toMap(this)