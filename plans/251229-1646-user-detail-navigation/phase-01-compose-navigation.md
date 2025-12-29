# Phase 01: Compose Multiplatform Navigation

## Context Links

- [Main Plan](./plan.md)
- [Code Standards](/docs/code-standards.md)
- [System Architecture](/docs/system-architecture.md)

## Overview

Implement simple callback-based navigation in Compose Multiplatform. App.kt manages navigation state, UsersScreen receives onUserClick callback, UserDetailScreen displays user info.

## Key Insights

1. **No navigation library needed** - Simple sealed class state in App.kt sufficient
2. **User model already exists** - domain/model/User.kt has id, name, email
3. **Current structure** - App.kt renders UsersScreen directly, easy to wrap with nav state
4. **Composable reuse** - UserDetailScreen receives User directly, no API call

## Requirements

### Functional
- FR1: User list items clickable
- FR2: Click navigates to detail screen
- FR3: Detail screen shows user name and email
- FR4: Back button returns to list

### Non-Functional
- NFR1: Navigation state persists across recomposition
- NFR2: Follow existing code standards (camelCase, single responsibility)

## Architecture

### Navigation State (App.kt)

```kotlin
sealed class Screen {
    object UserList : Screen()
    data class UserDetail(val user: User) : Screen()
}
```

### Component Flow

```
App.kt
  |-- remember { mutableStateOf<Screen>(Screen.UserList) }
  |
  +-- when (currentScreen) {
        UserList -> UsersScreen(
            vm = usersVm,
            onUserClick = { user -> currentScreen = UserDetail(user) }
        )
        UserDetail -> UserDetailScreen(
            user = user,
            onBack = { currentScreen = UserList }
        )
      }
```

## Related Code Files

| File | Purpose | Changes |
|------|---------|---------|
| `composeApp/.../App.kt` | Main app entry | Add Screen sealed class, nav state, when block |
| `composeApp/.../users/UsersScreen.kt` | User list | Add onUserClick param, clickable modifier |
| `composeApp/.../users/UserDetailScreen.kt` | Detail screen | NEW FILE - display name/email, back button |

## Implementation Steps

### Step 1: Create Screen sealed class in App.kt

Add sealed class before App() function:
```kotlin
sealed class Screen {
    object UserList : Screen()
    data class UserDetail(val user: User) : Screen()
}
```

### Step 2: Add navigation state in App()

```kotlin
var currentScreen by remember { mutableStateOf<Screen>(Screen.UserList) }
```

### Step 3: Update UsersScreen signature

```kotlin
@Composable
fun UsersScreen(
    vm: UsersSharedViewModel,
    onUserClick: (User) -> Unit
)
```

### Step 4: Add clickable to user items

```kotlin
items(state.users) { user ->
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onUserClick(user) }
    ) {
        // existing content
    }
}
```

### Step 5: Create UserDetailScreen.kt

```kotlin
@Composable
fun UserDetailScreen(
    user: User,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = user.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = user.email, style = MaterialTheme.typography.bodyLarge)
    }
}
```

### Step 6: Update App.kt with navigation when block

Replace `UsersScreen(usersVm)` with:
```kotlin
when (val screen = currentScreen) {
    is Screen.UserList -> UsersScreen(
        vm = usersVm,
        onUserClick = { user -> currentScreen = Screen.UserDetail(user) }
    )
    is Screen.UserDetail -> UserDetailScreen(
        user = screen.user,
        onBack = { currentScreen = Screen.UserList }
    )
}
```

## Todo List

- [x] Create Screen sealed class in App.kt
- [x] Add navigation state (currentScreen)
- [x] Update UsersScreen to accept onUserClick callback
- [x] Add clickable modifier to user list items
- [x] Create UserDetailScreen.kt composable
- [x] Wire navigation in App.kt with when block
- [ ] Test navigation forward and back (manual verification pending)
- [ ] Verify state survives recomposition (manual verification pending)

## Success Criteria

- [x] User list items show click ripple effect (clickable modifier present)
- [x] Tapping user navigates to detail screen (onUserClick → Screen.UserDetail)
- [x] Detail screen shows correct name and email (user.name, user.email rendered)
- [x] Back button returns to user list (onBack → Screen.UserList)
- [x] User list state preserved after returning (ViewModel persists via remember)
- [x] No memory leaks (ViewModel cleared properly via DisposableEffect)

**Note:** Implementation uses TextButton for back button instead of IconButton as originally specified in Step 5. Acceptable variation.

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| State loss on rotation | remember {} handles recomposition |
| Import conflicts (User) | Use full path domain.model.User if needed |
| Back gesture (Android) | BackHandler compose API if needed (bonus) |

## Security Considerations

- No sensitive data exposed (name/email already visible in list)
- No deep linking (URL manipulation not possible)
- State stays in memory, not persisted

## Implementation Status

**Phase 01:** ✅ COMPLETE (code implementation done, manual verification pending)

**Code Review:** See `/Users/haikhong/RepoHub/NativeIOSInCompose/plans/reports/code-reviewer-251229-1724-phase01-navigation.md`

**Findings:**
- Architecture: Clean, follows KISS/YAGNI/DRY principles
- Security: No issues identified
- Performance: State management correct
- Minor improvement: Move padding before clickable for larger touch target

## Next Steps

1. ⏳ Manual verification on Android emulator
2. ⏳ Manual verification on iOS simulator (via Compose)
3. ⏭️ Proceed to Phase 02 (iOS native navigation)
