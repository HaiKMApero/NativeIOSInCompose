package di

import core.AppDispatchers
import data.api.UsersApi
import data.api.UsersApiImpl
import data.repo.UserRepositoryImpl
import domain.repo.UserRepository
import domain.usecase.GetUsersUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import presentation.users.UsersSharedViewModel

expect class SharedModule(baseUrl: String) {
    fun provideUsersVM(): UsersSharedViewModel
}

internal fun createHttpClient(engine: HttpClientEngine): HttpClient =
    HttpClient(engine) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }

internal fun createUsersVM(client: HttpClient, baseUrl: String): UsersSharedViewModel {
    val dispatchers = AppDispatchers()
    val api: UsersApi = UsersApiImpl(client, baseUrl)
    val repo: UserRepository = UserRepositoryImpl(api)
    val useCase = GetUsersUseCase(repo)
    return UsersSharedViewModel(useCase, dispatchers)
}
