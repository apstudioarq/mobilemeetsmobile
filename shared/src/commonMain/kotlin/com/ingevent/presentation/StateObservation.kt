package com.ingevent.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class StateObservation internal constructor(
    private val job: Job,
) {
    fun cancel() {
        job.cancel()
    }
}

internal fun <T> StateFlow<T>.observeIn(
    scope: CoroutineScope,
    onStateChanged: (T) -> Unit,
): StateObservation {
    return StateObservation(
        scope.launch {
            collect { state -> onStateChanged(state) }
        },
    )
}
