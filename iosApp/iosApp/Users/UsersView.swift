import SwiftUI
import ComposeApp

struct UsersView: View {
    @StateObject var vm = UsersObservableViewModel(sharedVM: Injection.usersSharedVM())

    var body: some View {
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
        }
        .onAppear {
            vm.load()
        }
    }
}
