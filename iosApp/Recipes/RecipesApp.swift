import SwiftUI

@main
struct RecipesApp: App {
    @State private var deepLinkUrl: String? = nil

    var body: some Scene {
        WindowGroup {
            // .id(deepLinkUrl) forces the entire Compose view to be recreated when a URL
            // arrives — this ensures makeUIViewController is called with the new URL even
            // when the app is already running.
            ContentView(deepLinkUrl: deepLinkUrl)
                .id(deepLinkUrl)
                .onOpenURL { url in
                    deepLinkUrl = url.absoluteString
                }
        }
    }
}
