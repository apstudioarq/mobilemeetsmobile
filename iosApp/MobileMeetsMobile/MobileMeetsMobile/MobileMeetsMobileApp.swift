import Foundation
import SwiftUI
import shared

@main
struct MobileMeetsMobileApp: App {
    init() {
        let info = Bundle.main.infoDictionary
        let firebaseInfo: [String: Any]
        if let url = Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist"),
           let data = try? Data(contentsOf: url),
           let values = try? PropertyListSerialization.propertyList(
               from: data,
               options: [],
               format: nil
           ) as? [String: Any] {
            firebaseInfo = values
        } else {
            firebaseInfo = [:]
        }

        func firebaseValue(infoKey: String, plistKey: String) -> String {
            let override = ((info?[infoKey] as? String) ?? "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if !override.isEmpty && !override.hasPrefix("$(") {
                return override
            }
            return ((firebaseInfo[plistKey] as? String) ?? "")
                .trimmingCharacters(in: .whitespacesAndNewlines)
        }

        let firebaseFunctionsUrl = (info?["FIREBASE_FUNCTIONS_URL"] as? String) ?? ""
        let firebaseDatabaseUrl = firebaseValue(
            infoKey: "FIREBASE_DATABASE_URL",
            plistKey: "DATABASE_URL"
        )
        let firebaseConferenceId = (info?["FIREBASE_CONFERENCE_ID"] as? String) ?? ""
        let firebaseApiKey = firebaseValue(infoKey: "FIREBASE_API_KEY", plistKey: "API_KEY")
        let supabaseUrl = (info?["SUPABASE_URL"] as? String) ?? ""
        let supabaseAnonKey = (info?["SUPABASE_ANON_KEY"] as? String) ?? ""
        let effectiveFirebaseDatabaseUrl: String
        if firebaseDatabaseUrl.isEmpty {
            effectiveFirebaseDatabaseUrl = "https://ingtechrating-default-rtdb.europe-west1.firebasedatabase.app"
        } else {
            effectiveFirebaseDatabaseUrl = firebaseDatabaseUrl
        }

        if !effectiveFirebaseDatabaseUrl.isEmpty {
            BackendConfig.shared.configureFirebaseRealtimeDatabase(
                databaseUrl: effectiveFirebaseDatabaseUrl,
                conferenceId: firebaseConferenceId,
                apiKey: firebaseApiKey
            )
        } else if !firebaseFunctionsUrl.isEmpty {
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
        }
    }
}
