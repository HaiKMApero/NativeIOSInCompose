package di

import io.ktor.client.engine.okhttp.OkHttp
import presentation.users.UsersSharedViewModel

actual class SharedModule actual constructor(private val baseUrl: String) {
    actual fun provideUsersVM(): UsersSharedViewModel {
        val client = createHttpClient(OkHttp.create())
        return createUsersVM(client, baseUrl)
    }
}
