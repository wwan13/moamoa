package server.resilient

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import server.shared.lock.KeyedLock
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@Component
@Suppress("UNCHECKED_CAST")
internal class ResilientKeyedLockMethodInvoker(
    @Qualifier("redissonLock")
    redissonLock: KeyedLock,
    @Qualifier("coroutineMutexLock")
    coroutineMutexLock: KeyedLock,
) {
    private val methodsByTargetAndSignature: Map<Class<*>, Map<MethodSignatureKey, Method>>

    init {
        methodsByTargetAndSignature = listOf(redissonLock, coroutineMutexLock)
            .map { it.javaClass }
            .distinct()
            .associateWith { targetClass ->
                targetClass.methods.associateBy { method ->
                    MethodSignatureKey.of(method)
                }
            }
    }

    fun invoke(target: KeyedLock, method: Method, args: Array<Any?>): Any? {
        val targetMethod = resolveMethod(target, method)
        return try {
            targetMethod.invoke(target, *args)
        } catch (ex: InvocationTargetException) {
            throw ex.targetException ?: ex
        }
    }

    fun invokeAsMono(
        target: KeyedLock,
        method: Method,
        args: Array<Any?>,
        originalException: Throwable,
    ): Mono<Any?> {
        return try {
            val result = invoke(target, method, args)
            when (result) {
                is Mono<*> -> result as Mono<Any?>
                else -> Mono.justOrEmpty(result)
            }
        } catch (fallbackException: Throwable) {
            fallbackException.addSuppressed(originalException)
            Mono.error(fallbackException)
        }
    }

    private fun resolveMethod(target: KeyedLock, sourceMethod: Method): Method {
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
