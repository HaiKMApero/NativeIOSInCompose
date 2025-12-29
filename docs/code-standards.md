# Code Standards - NativeIOSInCompose

## Overview

Code standards for KMP production template. Ensures consistency, maintainability, and quality across Android/iOS platforms.

## Kotlin Code Style

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `UsersViewModel`, `UserRepository` |
| Functions | camelCase | `getUsers()`, `fetchUsersFromApi()` |
| Variables | camelCase | `userId`, `isLoading` |
| Constants | UPPER_SNAKE_CASE | `REQUEST_TIMEOUT_MS`, `BASE_URL` |
| Properties | camelCase | `state`, `dispatchers` |
| Packages | lowercase.dot | `domain.repo`, `data.api` |

### File Organization

**Single responsibility per file:**
```
UserRepository.kt        // Interface only
UserRepositoryImpl.kt     // Implementation only
User.kt                  // Single entity
UserDto.kt              // Single DTO
UserMapper.kt           // Mapping logic only
```

**File naming = Public class name:**
```kotlin
// File: UserRepository.kt
interface UserRepository { ... }

// File: UserRepositoryImpl.kt
class UserRepositoryImpl(...) : UserRepository { ... }
```

## Architecture Layer Standards

### Core Layer (`core/`)

**Purpose:** Shared utilities, zero framework dependencies

**Rules:**
1. No external library imports (except Kotlin stdlib + Coroutines)
2. Platform-agnostic implementations
3. Reusable across all layers

**Example Structure:**
```kotlin
package core

// ✅ Good
sealed class Result<out T> {
    data class Ok<T>(val value: T) : Result<T>()
    data class Err(val message: String) : Result<Nothing>()
}

// ❌ Bad (framework dependency)
class ApiResult(val httpCode: Int) // Don't reference HTTP details here
```

### Domain Layer (`domain/`)

**Purpose:** Business logic, interfaces, entities

**Rules:**
1. No framework imports (Ktor, Room, etc.)
2. Only depends on core layer
3. Interfaces define contracts for data access
4. Pure Kotlin, highly testable

**Model Standards:**
```kotlin
// ✅ Good - immutable, data class
data class User(
    val id: Long,
    val name: String,
    val email: String
)

// ❌ Bad - mutable, missing documentation
class User {
    var id: Long? = null
    var name: String? = null
}
```

**UseCase Standards:**
```kotlin
// ✅ Good - single responsibility, testable
class GetUsersUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): Result<List<User>> =
        repository.getUsers()
}

// ❌ Bad - multiple responsibilities
class UserUseCase(private val repo: UserRepository) {
    fun getUsers() { ... }
    fun saveUser() { ... }
    fun deleteUser() { ... }
}
```

**Interface Standards:**
```kotlin
// ✅ Good - clear contract
interface UserRepository {
    suspend fun getUsers(): Result<List<User>>
}

// ❌ Bad - mixed concerns
interface UserDataAccess {
    fun getUsers(): List<User>?
    fun cacheUsers(users: List<User>)
    fun clearCache()
}
```

### Data Layer (`data/`)

**Purpose:** Implementations, API clients, mappers, repositories

**Rules:**
1. Implements domain interfaces
2. Encapsulates framework-specific code
3. Handles data transformations (DTO → Domain)
4. Contains only business-related data classes

**DTO Standards:**
```kotlin
// ✅ Good - matches API response, serializable
@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String
)

// ❌ Bad - extra fields, unclear naming
@Serializable
data class UserResponse(
    val userId: Long,
    val fullName: String,
    val contactEmail: String,
    val internalId: String  // Not from API
)
```

**Mapper Standards:**
```kotlin
// ✅ Good - explicit, single purpose
object UserMapper {
    fun dtoToDomain(dto: UserDto): User =
        User(id = dto.id, name = dto.name, email = dto.email)
}

// ❌ Bad - implicit, hidden logic
fun UserDto.toUser(): User {
    val (firstName, lastName) = name.split(" ")
    return User(firstName = firstName, lastName = lastName)
}
```

**Repository Implementation Standards:**
```kotlin
// ✅ Good - wraps errors, delegates to use case
class UserRepositoryImpl(private val api: UsersApi) : UserRepository {
    override suspend fun getUsers(): Result<List<User>> =
        api.fetchUsers().mapCatching { dtos ->
            dtos.map { UserMapper.dtoToDomain(it) }
        }
}

// ❌ Bad - throws exceptions, no error handling
class UserRepositoryImpl(private val api: UsersApi) : UserRepository {
    override suspend fun getUsers(): List<User> =
        api.fetchUsers().map { it.toUser() }
}
```

### Presentation Layer (`presentation/`)

**Purpose:** ViewModels, UI state, platform bridges

**Rules:**
1. Manage UI state reactively (StateFlow)
2. Delegate business logic to use cases
3. Inject AppDispatchers for testability
4. Expose state immutably to UI

**ViewModel Standards:**
```kotlin
// ✅ Good - immutable state, lifecycle management
class UsersSharedViewModel(
    private val getUsers: GetUsersUseCase,
    private val dispatchers: AppDispatchers
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    private val _state = MutableStateFlow(UsersUiState())
    val state: StateFlow<UsersUiState> = _state.asStateFlow()

    fun load() {
        scope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = withContext(dispatchers.io) { getUsers() }
            _state.value = when (result) {
                is Result.Ok -> _state.value.copy(
                    isLoading = false,
                    users = result.value
                )
                is Result.Err -> _state.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun clear() { scope.cancel() }
}

// ❌ Bad - exposed mutable state, direct API calls
class UserViewModel {
    val users = MutableState<List<User>?>()

    fun load() {
        scope.launch {
            users.value = api.fetchUsers()  // Direct API in ViewModel
        }
    }
}
```

**UI State Standards:**
```kotlin
// ✅ Good - immutable, complete state
data class UsersUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val errorMessage: String? = null
)

// ❌ Bad - mutable, incomplete
class UsersUiState {
    var isLoading: Boolean? = null
    var users: MutableList<User>? = null
}
```

### DI Layer (`di/`)

**Purpose:** Dependency wiring, platform-specific implementations

**Rules:**
1. Use expect/actual for platform differences
2. Factory functions with descriptive names
3. Single responsibility per function
4. Compose dependencies in correct order

**SharedModule Standards:**
```kotlin
// ✅ Good - clear responsibilities, platform-aware
expect class SharedModule(baseUrl: String) {
    fun provideUsersVM(): UsersSharedViewModel
}

internal fun createHttpClient(engine: HttpClientEngine): HttpClient =
    HttpClient(engine) {
        install(HttpTimeout) { /* ... */ }
        install(ContentNegotiation) { /* ... */ }
    }

internal fun createUsersVM(
    client: HttpClient,
    baseUrl: String
): UsersSharedViewModel {
    val dispatchers = AppDispatchers()
    val api: UsersApi = UsersApiImpl(client, baseUrl)
    val repo: UserRepository = UserRepositoryImpl(api)
    val useCase = GetUsersUseCase(repo)
    return UsersSharedViewModel(useCase, dispatchers)
}

// ❌ Bad - God object, tight coupling
class DIContainer {
    private val cache = mutableMapOf<String, Any>()

    fun get(key: String): Any = cache.getOrPut(key) {
        when (key) {
            "users" -> UsersSharedViewModel(...)
            "products" -> ProductViewModel(...)
            "orders" -> OrderViewModel(...)
        }
    }
}
```

## Coroutine Standards

### Dispatcher Usage

```kotlin
// ✅ Good - injected, testable
class ViewModel(private val dispatchers: AppDispatchers) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    fun load() {
        scope.launch {
            val result = withContext(dispatchers.io) {
                fetchData()
            }
        }
    }
}

// ❌ Bad - hardcoded, untestable
class ViewModel {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun load() {
        scope.launch(Dispatchers.IO) {
            fetchData()
        }
    }
}
```

### Scope Lifecycle

```kotlin
// ✅ Good - proper cleanup
class UsersSharedViewModel(...) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher.main)

    fun clear() {
        scope.cancel()  // Cleanup when ViewModel destroyed
    }
}

// ❌ Bad - dangling scopes
class UsersSharedViewModel(...) {
    private val scope = CoroutineScope(Dispatchers.Main)
    // No cleanup → memory leak
}
```

## Error Handling Standards

### Result Type Usage

```kotlin
// ✅ Good - explicit error handling
suspend fun getUsers(): Result<List<User>> =
    try {
        val response = api.fetchUsers()
        Result.Ok(response.map { it.toUser() })
    } catch (e: Exception) {
        Result.Err(message = e.message ?: "Unknown error", throwable = e)
    }

// ❌ Bad - exceptions propagate, unclear failures
suspend fun getUsers(): List<User> {
    return api.fetchUsers().map { it.toUser() }
    // Caller must handle all exceptions
}
```

### Error Messages

```kotlin
// ✅ Good - actionable error messages
Result.Err("Network timeout after 30s - check connection")
Result.Err("Invalid user data: missing email field")

// ❌ Bad - vague messages
Result.Err("Error")
Result.Err("Failed")
```

## API Client Standards

### Ktor Configuration

```kotlin
// ✅ Good - timeouts, error handling, serialization
val client = HttpClient(engine) {
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

// ❌ Bad - no timeouts, default settings
val client = HttpClient(engine)
```

### HTTP Methods

```kotlin
// ✅ Good - proper HTTP verb, error wrapping
suspend fun fetchUsers(): Result<List<UserDto>> =
    try {
        val response: List<UserDto> = client.get(urlString = "$baseUrl/users")
        Result.Ok(response)
    } catch (e: Exception) {
        Result.Err(e.message ?: "Fetch failed", e)
    }

// ❌ Bad - wrong verb, unhandled exceptions
suspend fun fetchUsers(): List<UserDto> =
    client.post(urlString = "$baseUrl/users")  // POST for reading!
```

## Serialization Standards

### @Serializable Annotation

```kotlin
// ✅ Good - explicit serialization, null handling
@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String
)

// ❌ Bad - implicit fields, unclear behavior
@Serializable
data class UserDto(
    val userId: Long? = null,
    val userName: String = "",
    val userEmail: String = ""
)
```

## Documentation Standards

### KDoc Comments

```kotlin
// ✅ Good - clear intent, examples
/**
 * Fetches users from the remote API.
 *
 * @return Result containing list of users on success, error message on failure
 * @throws NetworkException if network unavailable (wrapped in Result.Err)
 *
 * Example:
 * ```
 * when (val result = repository.getUsers()) {
 *     is Result.Ok -> println(result.value)
 *     is Result.Err -> println(result.message)
 * }
 * ```
 */
suspend fun getUsers(): Result<List<User>>

// ❌ Bad - no documentation
suspend fun getUsers(): Result<List<User>>
```

### Function Comments

```kotlin
// ✅ Good - explains why, not what
// Inject dispatchers to allow testing with unconfined dispatcher
class ViewModel(private val dispatchers: AppDispatchers) { }

// ❌ Bad - obvious comments
// Create a new ViewModel with dispatcher
class ViewModel(private val dispatchers: AppDispatchers) { }
```

## Testing Standards

### Test File Organization

```
shared/src/commonTest/kotlin/
├── core/
│   └── ResultTest.kt
├── domain/
│   └── GetUsersUseCaseTest.kt
├── data/
│   ├── UserMapperTest.kt
│   └── UserRepositoryImplTest.kt
└── presentation/
    └── UsersViewModelTest.kt
```

### Mock-Friendly Code

```kotlin
// ✅ Good - interface injection enables mocking
class ViewModel(private val useCase: GetUsersUseCase) { }

// Test: pass mock use case
val mockUseCase = mockk<GetUsersUseCase>()
val vm = ViewModel(mockUseCase)

// ❌ Bad - hard to test
class ViewModel {
    private val useCase = GetUsersUseCase(realRepository)
}
```

## Import Organization

```kotlin
// ✅ Good - grouped by source
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable

import io.ktor.client.HttpClient

import core.AppDispatchers
import domain.repo.UserRepository

// ❌ Bad - random order
import io.ktor.client.HttpClient
import core.AppDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import domain.repo.UserRepository
```

## Gradle Build Standards

### Dependency Management

```gradle
// ✅ Good - version catalogs
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

dependencies {
    implementation(libs.ktor.client.core)
}

// ❌ Bad - hardcoded versions
plugins {
    id 'org.jetbrains.kotlin.multiplatform' version '1.9.0'
}

dependencies {
    implementation 'io.ktor:ktor-client-core:2.3.0'
}
```

## Code Review Checklist

- [ ] Follows naming conventions (PascalCase, camelCase, UPPER_SNAKE_CASE)
- [ ] Single responsibility per file
- [ ] No layer boundary violations
- [ ] Result<T> used for fallible operations
- [ ] AppDispatchers injected (not hardcoded)
- [ ] StateFlow exposed as immutable
- [ ] Error messages are actionable
- [ ] Expect/actual pattern used for platform code
- [ ] No hardcoded URLs, timeouts, or secrets
- [ ] KDoc comments for public APIs
- [ ] Tests included for domain/data layers
- [ ] No println/Log calls without proper logging framework

## Common Violations & Fixes

| Violation | Fix |
|-----------|-----|
| ViewModel directly calls HttpClient | Inject UseCase instead |
| UI directly calls Repository | Go through UseCase layer |
| Hardcoded dispatcher | Inject AppDispatchers |
| Mutable StateFlow exposed | Use `.asStateFlow()` |
| Exception throwing in layers | Wrap in Result<T> |
| No timeout configuration | Configure in HttpClient |
| Ignoring serialization errors | Use ignoreUnknownKeys in Json config |
| No lifecycle management | Add scope.cancel() in clear() |

