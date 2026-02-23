package server.cache

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import server.shared.cache.CacheMemory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@Component
@Suppress("UNCHECKED_CAST")
internal class ResilientCacheMethodInvoker(
    @Qualifier("redisCacheMemory")
    redisCacheMemory: CacheMemory,
    @Qualifier("caffeineCacheMemory")
    caffeineCacheMemory: CacheMemory,
) {
    private val methodsByTargetAndSignature: Map<Class<*>, Map<MethodSignatureKey, Method>>

    init {
        methodsByTargetAndSignature = listOf(redisCacheMemory, caffeineCacheMemory)
            .map { it.javaClass }
            .distinct()
            .associateWith { targetClass ->
                targetClass.methods.associateBy { method ->
                    MethodSignatureKey.of(method)
                }
            }
    }

    fun invokeAsMono(
        target: CacheMemory,
        method: Method,
        args: Array<Any?>,
        originalException: Throwable? = null,
    ): Mono<Any?> {
        val targetMethod = resolveMethod(target, method)
        return try {
            val result = targetMethod.invoke(target, *args)
            result as? Mono<Any?>
                ?: Mono.justOrEmpty(result)
        } catch (exception: Throwable) {
            val fallbackException = (exception as? InvocationTargetException)?.targetException
                ?: exception
            if (originalException != null) {
                fallbackException.addSuppressed(originalException)
            }
            Mono.error(fallbackException)
        }
    }

    private fun resolveMethod(target: CacheMemory, sourceMethod: Method): Method {
        val signature = MethodSignatureKey.of(sourceMethod)
        return methodsByTargetAndSignature[target.javaClass]?.get(signature) ?: sourceMethod
    }

    private data class MethodSignatureKey(
        val methodName: String,
        val parameterTypes: List<Class<*>>,
    ) {
        companion object {
            fun of(method: Method): MethodSignatureKey {
                return MethodSignatureKey(
                    methodName = method.name,
                    parameterTypes = method.parameterTypes.toList(),
                )
            }
        }
    }
}
