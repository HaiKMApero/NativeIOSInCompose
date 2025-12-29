# System Architecture - NativeIOSInCompose

## Architectural Pattern: Clean Architecture

The codebase follows clean architecture principles with strict separation of concerns across four layers.

## Layer Responsibilities

### 1. Core Layer
**Location:** `shared/src/commonMain/kotlin/core/`

Fundamental utilities used across all layers:

| Component | Responsibility |
|-----------|-----------------|
| `Result<T>` | Type-safe error handling (Ok/Err sealed class) |
| `AppDispatchers` | Testable coroutine dispatcher abstraction |

**Design Principle:** Platform-agnostic, no framework dependencies.

### 2. Domain Layer
**Location:** `shared/src/commonMain/kotlin/domain/`

Business logic and contracts (pure Kotlin, no dependencies):

| Component | Responsibility |
|-----------|-----------------|
| `User` Model | Domain entity (id, name, email) |
| `UserRepository` Interface | Data access contract |
| `GetUsersUseCase` | Orchestrates user fetching logic |

**Design Principle:** Independent of frameworks, databases, UI. Highest level of policy.

### 3. Data Layer
**Location:** `shared/src/commonMain/kotlin/data/`

Concrete implementations of domain interfaces:

| Component | Responsibility |
|-----------|-----------------|
| `UserDto` | API response structure (Kotlinx.serializable) |
| `UsersApi` Interface | HTTP contract |
| `UsersApiImpl` | Ktor HttpClient HTTP implementation |
| `UserMapper` | Converts UserDto → User |
| `UserRepositoryImpl` | Implements UserRepository interface |

**Flow:** API → DTO → Mapper → Repository → Domain Model

**Design Principle:** Framework-specific (Ktor), encapsulates external dependencies.

### 4. Presentation Layer
**Location:** `shared/src/commonMain/kotlin/presentation/`

UI state and ViewModel logic:

| Component | Responsibility |
|-----------|-----------------|
| `UsersSharedViewModel` | Manages UI state, triggers use cases |
| `UsersUiState` | Immutable state snapshot |
| `StateObserver` | KMP bridge for platform-specific observation |

**Design Principle:** Reactive, testable, decoupled from UI frameworks (Compose).

## Dependency Flow

```
┌─────────────────────────┐
│  Presentation Layer     │  (ViewModel, State)
│  ↓ depends on
├─────────────────────────┤
│  Domain Layer           │  (UseCase, Interface)
│  ↓ depends on
├─────────────────────────┤
│  Data Layer             │  (Repository, API)
│  ↓ depends on
├─────────────────────────┤
│  Core Layer             │  (Result, Dispatchers)
│  ↓ depends on
├─────────────────────────┤
│  External Libraries     │  (Ktor, Coroutines, Serialization)
└─────────────────────────┘
```

**Rule:** Dependencies flow inward. Outer layers depend on inner layers, never reverse.

## Component Interactions

### Data Fetching Flow

```
UI (UsersScreen)
    ↓ observes StateFlow
UsersSharedViewModel
    ↓ calls load()
GetUsersUseCase
    ↓ calls
UserRepository (interface)
    ↓ implemented by
UserRepositoryImpl
    ↓ calls
UsersApi (interface)
    ↓ implemented by
UsersApiImpl (Ktor HttpClient)
    ↓ HTTP GET
jsonplaceholder.typicode.com/users
    ↓ response
UserDto[] → UserMapper → User[]
    ↓ wraps in Result
UsersUiState (isLoading=false, users=[...])
    ↓ emitted to StateFlow
UI observes change and recomposes
```

## Multiplatform Strategy

### Platform Separation

**Common Code (androidMain, iosMain NOT needed for logic):**
- All business logic in `commonMain`
- Platform-agnostic implementations

**Platform-Specific Code:**
- **Android:** OkHttp HTTP engine (`androidMain` - Ktor dependency)
- **iOS:** Darwin HTTP engine (`iosMain` - Ktor dependency)
- **DI:** Expect/actual pattern in `SharedModule.kt`

```
SharedModule (expect)
    ├── Android Implementation
    │   └── createHttpClient() → OkHttp engine
    ├── iOS Implementation
    │   └── createHttpClient() → Darwin engine
    └── Common Factory
        └── createUsersVM()
```

### Ktor HTTP Client Configuration

**Common Configuration (all platforms):**
```kotlin
HttpTimeout {
    requestTimeoutMillis = 30_000
    connectTimeoutMillis = 10_000
    socketTimeoutMillis = 30_000
}
ContentNegotiation {
    json(Json { ignoreUnknownKeys = true })
}
```

**Platform Engines:**
- Android: `ktor-client-okhttp` (okhttp3 native)
- iOS: `ktor-client-darwin` (URLSession native)

## State Management

### StateFlow Pattern

**Why StateFlow?**
- Multiplatform support (coroutines for all platforms)
- Hot observable (always has current value)
- Easy to test (collect flows in tests)
- Compatible with Compose

**ViewModel Lifecycle:**
```kotlin
class UsersSharedViewModel(useCase, dispatchers) {
    // Scope tied to ViewModel lifetime
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    // Emit state changes
    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow() // Read-only exposure

    // Cleanup
    fun clear() { scope.cancel() }
}
```

**State Evolution:**
```
Initial: UsersUiState(isLoading=true)
    ↓
load() triggered:
    _state = copy(isLoading=true, errorMessage=null)
    ↓
API call in IO dispatcher:
    Result.Ok → _state = copy(isLoading=false, users=[...])
    Result.Err → _state = copy(isLoading=false, errorMessage="...")
    ↓
UI observes and recomposes
```

## Error Handling Strategy

### Result Type

```kotlin
sealed class Result<out T> {
    data class Ok<T>(val value: T) : Result<T>()
    data class Err(val message: String, val throwable: Throwable?) : Result<Nothing>()
}
```

**Advantages:**
- Type-safe (compiler enforces handling both branches)
- No null checking (explicit success/failure)
- Throwable preserved for logging
- Composable with other Results

**Usage Pattern:**
```kotlin
val result = getUsers() // Returns Result<List<User>>
when (result) {
    is Result.Ok -> { /* use result.value */ }
    is Result.Err -> { /* use result.message */ }
}
```

## Testing Architecture

### Testability Considerations

**AppDispatchers Injection:**
```kotlin
// Production
AppDispatchers(Dispatchers.Main, Dispatchers.Default)

// Testing
AppDispatchers(Dispatchers.Unconfined, Dispatchers.Unconfined)
```

**Mock-Friendly Interfaces:**
- UsersApi (can mock HTTP responses)
- UserRepository (can mock data access)
- GetUsersUseCase (can inject mock repository)

**StateFlow Testing:**
```kotlin
val viewModel = UsersSharedViewModel(mockUseCase, testDispatchers)
val state = viewModel.state.value
assertEquals(expectedState, state)
```

## Performance Considerations

### Memory Management

1. **SupervisorJob** - Prevents scope cancellation on errors
2. **StateFlow** - Reference equality for state comparison (optimization)
3. **Mapper Pattern** - DTO to domain conversion (separate concerns)

### Network Optimization

1. **Timeouts** - Prevents hanging requests
2. **JSON Parsing** - Kotlinx.serialization (compile-time generation)
3. **Platform Engines** - Native HTTP (OkHttp/URLSession)

## Security Considerations

1. **No Hardcoded Secrets** - Base URL injectable via SharedModule
2. **HTTPS Only** - URL scheme configurable (defaults to https)
3. **Timeout Protection** - Prevents connection exhaustion
4. **JSON Validation** - Unknown keys ignored (graceful degradation)

## Extension Points

### Adding New Features

1. **New Domain Model:**
   - Add to `domain/model/`
   - Create interface in `domain/repo/`

2. **New API Endpoint:**
   - Add method to `UsersApi` interface
   - Implement in `UsersApiImpl`
   - Add DTO in `data/dto/`

3. **New ViewModel:**
   - Create in `presentation/`
   - Use AppDispatchers
   - Expose StateFlow

4. **Platform-Specific Feature:**
   - Use expect/actual pattern
   - Isolate in `commonMain`, `androidMain`, `iosMain`

## Deployment Architecture

### Build Targets

| Target | Framework | Engine |
|--------|-----------|--------|
| Android | Gradle AAB/APK | OkHttp |
| iOS | XCFramework | Darwin/URLSession |

**Framework Configuration:**
- iOS: Static framework (isStatic = true)
- Targets: arm64, simulator (arm64 + x86_64)

### Version Management

- Kotlin: 2.0+
- Gradle: 8.0+
- Android: API 24+
- iOS: 13.0+
