# Phase 02: iOS SwiftUI Navigation

## Context Links

- [Main Plan](./plan.md)
- [Phase 01: Compose Navigation](./phase-01-compose-navigation.md)
- [Code Standards](/docs/code-standards.md)

## Overview

Implement native SwiftUI navigation using NavigationStack (iOS 16+) or NavigationView (iOS 13+ fallback). Add UserDetailView for displaying user info.

## Key Insights

1. **Native SwiftUI nav preferred** - Better performance, native back gesture, iOS conventions
2. **NavigationStack vs NavigationView** - Use NavigationStack for iOS 16+; NavigationView deprecated but needed for iOS 13-15 compat
3. **User accessible from Shared** - ComposeApp.User accessible in Swift
4. **List already exists** - Just wrap with NavigationStack, add NavigationLink

## Requirements

### Functional
- FR1: Wrap UsersView List in NavigationStack
- FR2: List items navigate to detail via NavigationLink
- FR3: UserDetailView shows name and email
- FR4: Native back button/swipe works

### Non-Functional
- NFR1: Support iOS 13+ (NavigationView fallback if needed)
- NFR2: Follow Swift naming conventions (PascalCase types, camelCase properties)

## Architecture

### Navigation Flow

```
UsersView
  |-- NavigationStack {
        List(users) { user in
          NavigationLink(value: user) {
            // Row content
          }
        }
        .navigationDestination(for: User.self) { user in
          UserDetailView(user: user)
        }
      }
```

### iOS 13 Fallback Pattern

```swift
NavigationView {
    List(users) { user in
        NavigationLink(destination: UserDetailView(user: user)) {
            // Row content
        }
    }
}
```

## Related Code Files

| File | Purpose | Changes |
|------|---------|---------|
| `iosApp/iosApp/Users/UsersView.swift` | User list | Wrap with NavigationStack, add NavigationLink |
| `iosApp/iosApp/Users/UserDetailView.swift` | Detail view | NEW FILE - display name/email |

## Implementation Steps

### Step 1: Create UserDetailView.swift

Location: `iosApp/iosApp/Users/UserDetailView.swift`

```swift
import SwiftUI
import ComposeApp

struct UserDetailView: View {
    let user: User

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(user.name)
                .font(.largeTitle)
                .fontWeight(.bold)

            Text(user.email)
                .font(.body)
                .foregroundColor(.secondary)

            Spacer()
        }
        .padding()
        .navigationTitle("User Details")
        .navigationBarTitleDisplayMode(.inline)
    }
}
```

### Step 2: Update UsersView.swift with NavigationStack

```swift
import SwiftUI
import ComposeApp

struct UsersView: View {
    @StateObject var vm = UsersObservableViewModel(sharedVM: Injection.usersSharedVM())

    var body: some View {
        NavigationStack {
            Group {
                if vm.state.isLoading {
                    ProgressView("Loading...")
                } else if let err = vm.state.errorMessage {
                    VStack(spacing: 16) {
                        Text("Error: \(err)")
                            .foregroundColor(.red)
                        Button("Retry") {
                            vm.load()
                        }
                    }
                } else {
                    List(vm.state.users, id: \.id) { user in
                        NavigationLink(value: user) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(user.name)
                                    .font(.headline)
                                Text(user.email)
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    .navigationDestination(for: User.self) { user in
                        UserDetailView(user: user)
                    }
                }
            }
            .navigationTitle("Users")
            .onAppear {
                vm.load()
            }
        }
    }
}
```

### Step 3: Make User Hashable (if needed)

Check if ComposeApp.User already conforms to Hashable. If not, extend:

```swift
extension User: Hashable {
    public static func == (lhs: User, rhs: User) -> Bool {
        lhs.id == rhs.id
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}
```

### Step 4: iOS 13 Compatibility (Optional)

If iOS 13-15 support needed, use availability check:

```swift
var body: some View {
    if #available(iOS 16.0, *) {
        NavigationStack {
            // iOS 16+ implementation
        }
    } else {
        NavigationView {
            // Fallback implementation
        }
        .navigationViewStyle(.stack)
    }
}
```

## Todo List

- [ ] Create UserDetailView.swift file
- [ ] Implement UserDetailView with name/email display
- [ ] Wrap UsersView List in NavigationStack
- [ ] Add NavigationLink to list items
- [ ] Add navigationDestination for User
- [ ] Add navigationTitle to views
- [ ] Extend User with Hashable if needed
- [ ] Test navigation forward/back
- [ ] Test native back swipe gesture
- [ ] Verify on iOS simulator

## Success Criteria

- [ ] List appears with "Users" navigation title
- [ ] Tapping user shows detail screen
- [ ] Detail shows correct name and email
- [ ] Back button appears and works
- [ ] Swipe-back gesture works
- [ ] List state preserved after returning
- [ ] No memory leaks (ObservableObject lifecycle correct)

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| User not Hashable | Med | Low | Add Hashable extension |
| iOS 13 compat | Low | Med | NavigationView fallback |
| ComposeApp import issues | Low | Med | Verify framework exported |

## Security Considerations

- User data already visible in list (no new exposure)
- No sensitive data in navigation state
- Native SwiftUI nav handles state securely

## Testing Notes

1. Build iOS app in Xcode
2. Run on simulator (iPhone 14+ recommended for iOS 16+)
3. Test list load -> tap item -> verify detail -> back
4. Test swipe-back gesture
5. Test retry on error state

## Next Steps

After completion:
1. Verify both platforms have consistent UX
2. Consider adding pull-to-refresh (future)
3. Consider animation polish (future)
