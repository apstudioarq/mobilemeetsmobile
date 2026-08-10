import SwiftUI
import shared

@main
struct MobileMeetsMobileApp: App {
    init() {
        let info = Bundle.main.infoDictionary
        let firebaseFunctionsUrl = (info?["FIREBASE_FUNCTIONS_URL"] as? String) ?? ""
        let supabaseUrl = (info?["SUPABASE_URL"] as? String) ?? ""
        let supabaseAnonKey = (info?["SUPABASE_ANON_KEY"] as? String) ?? ""

        if !firebaseFunctionsUrl.isEmpty {
            BackendConfig.shared.configureFirebaseFunctions(functionsBaseUrl: firebaseFunctionsUrl)
        } else {
            BackendConfig.shared.configureSupabase(
                projectUrl: supabaseUrl,
                anonKey: supabaseAnonKey
            )
        }

        // Initialize Koin for iOS
        KoinInit.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
        }
    }
}
