---
title: "User Detail Navigation"
description: "Add user detail screen navigation to Compose Multiplatform and iOS SwiftUI"
status: pending
priority: P2
effort: 2h
branch: main
tags: [navigation, compose, swiftui, ui-feature]
created: 2025-12-29
---

# User Detail Navigation Implementation Plan

## Overview

Add navigation from users list to user detail screen on both platforms. User taps item -> navigates to detail showing name/email.

## Current State

- **Compose (App.kt)**: Single screen, no navigation, UsersScreen displayed directly
- **iOS (UsersView.swift)**: Single view, List without NavigationStack
- **Shared**: User model exists (id, name, email), no navigation abstraction needed

## Architecture Decision

**Compose**: Callback-based navigation with state in App.kt (KISS - no navigation library)
**iOS**: Native NavigationStack + NavigationLink (SwiftUI standard)

Rationale: Avoid over-engineering. Simple state-based nav for Compose, native nav for iOS.

## Phases

| Phase | Description | Effort | Files |
|-------|-------------|--------|-------|
| 01 | Compose Navigation | 1h | App.kt, UsersScreen.kt, UserDetailScreen.kt |
| 02 | iOS Navigation | 1h | UsersView.swift, UserDetailView.swift |

## Deliverables

1. `UserDetailScreen.kt` - Compose detail screen (name, email display)
2. `UserDetailView.swift` - SwiftUI detail view (name, email display)
3. Updated `App.kt` - Navigation state management
4. Updated `UsersScreen.kt` - Click handler on items
5. Updated `UsersView.swift` - NavigationStack integration

## Success Criteria

- [ ] Compose: Tap user item -> detail screen shows name/email
- [ ] Compose: Back navigation returns to list
- [ ] iOS: Tap user item -> detail screen shows name/email
- [ ] iOS: Native back navigation works
- [ ] Both platforms have consistent UX

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Compose nav complexity | Low | Med | Use simple callback, avoid libs |
| iOS 13 compat | Low | Low | NavigationStack avail iOS 16+, use NavigationView fallback |

## Out of Scope

- Additional user fields (phone, address)
- Edit/delete functionality
- Deep linking
- Animation customization

## Related Documents

- [Phase 01: Compose Navigation](./phase-01-compose-navigation.md)
- [Phase 02: iOS Navigation](./phase-02-ios-navigation.md)
