import Foundation
import ComposeApp

@MainActor
final class UsersObservableViewModel: ObservableObject {
    @Published private(set) var state: UsersUiState

    private let sharedVM: UsersSharedViewModel

    init(sharedVM: UsersSharedViewModel) {
        self.sharedVM = sharedVM
        self.state = sharedVM.state.value as! UsersUiState
    }

    func load() {
        sharedVM.load()
        // Poll for state updates (simple approach)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.refreshState()
        }
    }

    private func refreshState() {
        state = sharedVM.state.value as! UsersUiState
        // Continue polling while loading
        if state.isLoading {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
                self?.refreshState()
            }
        }
    }

    deinit {
        sharedVM.clear()
    }
}
