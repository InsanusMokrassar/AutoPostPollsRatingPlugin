package com.github.insanusmokrassar.AutoPostPollsRatingPlugin.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

internal suspend fun <T> Flow<T>.first(predicate: suspend (T) -> Boolean): Flow<T> {
    val parentFlow = this
    return object : Flow<T> {
        override suspend fun collect(collector: FlowCollector<T>) {
            try {
                parentFlow.collect { value ->
                    if (predicate(value)) {
                        collector.emit(value)
                        throw FirstElementReachedException()
                    }
                }
            } catch (_: FirstElementReachedException) { }
        }
    }
}

private class FirstElementReachedException : CancellationException("Flow first element is reached, cancelling") {
    // TODO expect/actual
    // override fun fillInStackTrace(): Throwable = this
}
