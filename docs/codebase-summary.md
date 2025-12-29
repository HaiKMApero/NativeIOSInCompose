# Codebase Summary - NativeIOSInCompose

## Project Overview

KMP (Kotlin Multiplatform) production template for Android/iOS applications using Compose Multiplatform. Implements clean architecture with reactive state management.

**Tech Stack:**
- Kotlin Multiplatform Mobile (KMM)
- Compose Multiplatform (UI)
- Ktor HTTP Client (networking)
- Kotlin Serialization (JSON)
- Coroutines + StateFlow (state management)
- Dependency Injection (manual DI pattern)

## Architecture

### Clean Architecture Layers

```
┌─────────────────────┐
│   Presentation      │ (UI, ViewModels, State)
├─────────────────────┤
│   Domain            │ (UseCases, Models, Interfaces)
├─────────────────────┤
│   Data              │ (API, Repositories, Mappers, DTOs)
├─────────────────────┤
│   Core              │ (Result type, Dispatchers)
└─────────────────────┘
```

### Project Structure

```
shared/
├── src/commonMain/kotlin/
│   ├── core/
│   │   ├── Result.kt          # Sealed class for error handling
│   │   └── AppDispatchers.kt  # Coroutine dispatcher abstraction
│   ├── domain/
│   │   ├── model/
│   │   │   └── User.kt        # Domain entity (id, name, email)
│   │   ├── repo/
│   │   │   └── UserRepository.kt # Repository interface
│   │   └── usecase/
│   │       └── GetUsersUseCase.kt # Fetch users use case
│   ├── data/
│   │   ├── api/
│   │   │   ├── UsersApi.kt      # API interface
│   │   │   └── UsersApiImpl.kt   # Ktor HTTP client impl
│   │   ├── dto/
│   │   │   └── UserDto.kt       # Serializable API response DTO
│   │   ├── mapper/
│   │   │   └── UserMapper.kt    # DTO to Domain mapping
│   │   └── repo/
│   │       └── UserRepositoryImpl.kt # Repository implementation
│   ├── presentation/
│   │   ├── users/
│   │   │   ├── UsersSharedViewModel.kt # Shared ViewModel (StateFlow)
│   │   │   └── UsersUiState.kt        # UI state data class
│   │   └── bridge/
│   │       └── StateObserver.kt       # KMP state observation bridge
│   └── di/
│       └── SharedModule.kt            # DI container (expect/actual)
├── src/androidMain/kotlin/
│   └── di/SharedModule.kt             # Android DI implementation
└── src/iosMain/kotlin/
    └── di/SharedModule.kt             # iOS DI implementation

composeApp/
├── src/commonMain/kotlin/
│   └── org/haikm/nativeiosincompose/
│       ├── App.kt                     # Main app composable
│       └── users/
│           └── UsersScreen.kt         # Users list UI
└── [platform-specific app modules]
```

## Key Components

### Core Layer

**Result.kt** - Sealed class for type-safe error handling:
```kotlin
sealed class Result<out T> {
    data class Ok<T>(val value: T)
    data class Err(val message: String, val throwable: Throwable?)
}
```

**AppDispatchers.kt** - Abstraction for coroutine dispatchers enabling easy testing.

### Domain Layer

**User Model** - Domain entity with fields: id, name, email

**UserRepository Interface** - Contract for data access:
```kotlin
suspend fun getUsers(): Result<List<User>>
```

**GetUsersUseCase** - Business logic orchestrator that calls repository.

### Data Layer

**UsersApi Interface** - HTTP contract
```kotlin
suspend fun fetchUsers(): Result<List<UserDto>>
```

**UsersApiImpl** - Ktor HttpClient implementation with:
- Timeout configuration (30s request, 10s connect, 30s socket)
- Content negotiation with Kotlin JSON serialization
- Error handling wrapped in Result type

**UserDto** - Kotlinx.serialization data class mirroring API response

**UserMapper** - Converts UserDto to User domain model

**UserRepositoryImpl** - Implements UserRepository using UsersApi

### Presentation Layer

**UsersSharedViewModel** - Multiplatform ViewModel:
- Manages loading state, user list, and error messages
- Uses MutableStateFlow for reactive state
- SupervisorJob + CoroutineScope for lifecycle management
- Methods: load(), clear()

**UsersUiState** - Data class:
```kotlin
data class UsersUiState(
    val isLoading: Boolean,
    val users: List<User>,
    val errorMessage: String?
)
```

**StateObserver Bridge** - KMP pattern for platform-specific state observation.

### DI Layer

**SharedModule** - Expect/actual pattern for platform-specific initialization:
- HTTP client creation (Android: OkHttp, iOS: Darwin engine)
- Factory functions for API, Repository, UseCase, ViewModel
- All dependencies wired in createUsersVM()

## Networking

**Ktor Configuration:**
- Core: `io.ktor.client.core`
- Engine: `okhttp` (Android), `darwin` (iOS)
- Content Negotiation: JSON serialization
- Timeouts: 30s request/socket, 10s connect

## State Management

**StateFlow Pattern:**
- Single MutableStateFlow per ViewModel
- Exposed as read-only StateFlow
- Coroutine scope tied to ViewModel lifecycle
- SupervisorJob prevents scope cancellation on child failures

## UI Integration

**UsersScreen Composable** - Observes ViewModels StateFlow and displays:
- Loading indicator
- Users list
- Error messages

## Build Configuration

**Gradle Dependencies:**
```gradle
kotlin-multiplatform
android-library
kotlin-serialization
kotlinx-coroutines-core
ktor-client (core + platform engines)
ktor-serialization-json
```

**Targets:**
- Android: API 24+, JVM 11
- iOS: arm64 + simulator, static framework

## Development Guidelines

1. **Error Handling** - Always use Result<T> for operations that can fail
2. **Dispatchers** - Inject AppDispatchers for testable coroutine management
3. **State** - Use StateFlow in ViewModels, expose as immutable StateFlow
4. **Layers** - Keep domain layer pure, data/presentation can change
5. **DI** - Use SharedModule factory functions, platform-specific engines via expect/actual

## Next Steps

- Add unit tests with MockK
- Implement platform-specific features (permissions, sensors)
- Add error retry logic
- Implement pagination for users list
- Add cache layer using SQLite/Realm
