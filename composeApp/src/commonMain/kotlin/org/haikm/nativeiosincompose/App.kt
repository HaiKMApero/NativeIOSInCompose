package org.haikm.nativeiosincompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import di.SharedModule
import domain.model.User
import org.haikm.nativeiosincompose.users.UserDetailScreen
import org.haikm.nativeiosincompose.users.UsersScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val BASE_URL = "https://jsonplaceholder.typicode.com"

/** Navigation state for the app */
sealed class Screen {
    data object UserList : Screen()
    data class UserDetail(val user: User) : Screen()
}

@Composable
@Preview
fun App() {
    val sharedModule = remember { SharedModule(BASE_URL) }
    val usersVm = remember { sharedModule.provideUsersVM() }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.UserList) }

    DisposableEffect(Unit) {
        onDispose { usersVm.clear() }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
        ) {
            when (val screen = currentScreen) {
                is Screen.UserList -> UsersScreen(
                    vm = usersVm,
                    onUserClick = { user -> currentScreen = Screen.UserDetail(user) }
                )
                is Screen.UserDetail -> UserDetailScreen(
                    user = screen.user,
                    onBack = { currentScreen = Screen.UserList }
                )
            }
        }
    }
}
