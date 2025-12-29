package domain.usecase

import domain.repo.UserRepository

class GetUsersUseCase(
    private val repo: UserRepository
) {
    suspend operator fun invoke() = repo.getUsers()
}
