package presentation.bridge

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StateObserver<T>(
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun observe(flow: StateFlow<T>, onEach: (T) -> Unit) {
        job?.cancel()
        job = scope.launch {
            flow.collect { onEach(it) }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
