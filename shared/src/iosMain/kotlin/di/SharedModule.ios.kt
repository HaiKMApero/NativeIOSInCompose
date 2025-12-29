package di

import io.ktor.client.engine.darwin.Darwin
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName
import presentation.users.UsersSharedViewModel

@OptIn(ExperimentalObjCName::class)
@ObjCName("SharedModule")
actual class SharedModule actual constructor(private val baseUrl: String) {
    actual fun provideUsersVM(): UsersSharedViewModel {
        val client = createHttpClient(Darwin.create())
        return createUsersVM(client, baseUrl)
    }
}
