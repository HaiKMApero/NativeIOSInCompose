import Foundation
import ComposeApp

enum Injection {
    static func usersSharedVM() -> UsersSharedViewModel {
        SharedModule(baseUrl: "https://jsonplaceholder.typicode.com")
            .provideUsersVM()
    }
}
