package domain.repo

import core.Result
import domain.model.User

interface UserRepository {
    suspend fun getUsers(): Result<List<User>>
}
