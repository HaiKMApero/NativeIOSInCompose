package data.mapper

import data.dto.UserDto
import domain.model.User

private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

fun UserDto.toDomain(): User? {
    if (id <= 0) return null
    if (name.isBlank() || name.length > 255) return null
    if (!email.matches(EMAIL_REGEX)) return null

    return User(
        id = id,
        name = name.trim().take(255),
        email = email.lowercase().trim()
    )
}
