package presentation.users

import core.AppDispatchers
import core.Result
import domain.usecase.GetUsersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("UsersSharedViewModel")
class UsersSharedViewModel(
    private val getUsers: GetUsersUseCase,
    private val dispatchers: AppDispatchers
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    private val _state = MutableStateFlow(UsersUiState(isLoading = true))
    val state: StateFlow<UsersUiState> = _state.asStateFlow()

    fun load() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val result = withContext(dispatchers.io) { getUsers() }
            _state.value = when (result) {
                is Result.Ok -> UsersUiState(isLoading = false, users = result.value)
                is Result.Err -> UsersUiState(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun clear() {
        scope.cancel()
    }
}
