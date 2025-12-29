package data.repo

import core.Result
import data.api.UsersApi
import data.mapper.toDomain
import domain.model.User
import domain.repo.UserRepository

class UserRepositoryImpl(
    private val api: UsersApi
) : UserRepository {

    override suspend fun getUsers(): Result<List<User>> =
        when (val r = api.fetchUsers()) {
            is Result.Ok -> Result.Ok(r.value.mapNotNull { it.toDomain() })
            is Result.Err -> r
        }
}
