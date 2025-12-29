package data.api

import core.Result
import data.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.serialization.SerializationException

class UsersApiImpl(
    private val client: HttpClient,
    private val baseUrl: String
) : UsersApi {

    override suspend fun fetchUsers(): Result<List<UserDto>> = try {
        val url = "$baseUrl/users"
        val res: List<UserDto> = client.get(url) {
            accept(ContentType.Application.Json)
        }.body()
        Result.Ok(res)
    } catch (t: Throwable) {
        val userMessage = when (t) {
            is HttpRequestTimeoutException -> "Request timed out. Check connection."
            is SerializationException -> "Data format error."
            else -> "Network error. Check connection."
        }
        Result.Err(userMessage, null)
    }
}
