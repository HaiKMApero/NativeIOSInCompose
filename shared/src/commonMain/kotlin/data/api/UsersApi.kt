package data.api

import core.Result
import data.dto.UserDto

interface UsersApi {
    suspend fun fetchUsers(): Result<List<UserDto>>
}
