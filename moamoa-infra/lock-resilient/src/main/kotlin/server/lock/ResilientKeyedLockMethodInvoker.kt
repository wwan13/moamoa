package server.lock

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.jvm.javaClass

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

    fun invoke(
        target: KeyedLock,
        method: Method,
        args: Array<Any?>,
        originalException: Throwable,
    ): Any? {
        return try {
            invoke(target, method, args)
        } catch (fallbackException: Throwable) {
            fallbackException.addSuppressed(originalException)
            throw fallbackException
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
