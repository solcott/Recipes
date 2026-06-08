import UIKit
import SwiftUI
import RecipeApp

struct ComposeView: UIViewControllerRepresentable {
    let deepLinkUrl: String?

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(deepLinkUrl: deepLinkUrl)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var deepLinkUrl: String? = nil

    var body: some View {
        ComposeView(deepLinkUrl: deepLinkUrl)
            .ignoresSafeArea()
    }
}

#Preview {
    ContentView()
}
