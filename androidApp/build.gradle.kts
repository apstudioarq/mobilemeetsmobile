import java.util.Properties
import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

fun readSecret(name: String): String {
    return (localProperties.getProperty(name) ?: System.getenv(name) ?: "").trim()
}

fun String.toBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

data class FirebaseAndroidConfig(
    val databaseUrl: String = "",
    val apiKey: String = "",
)

fun readFirebaseAndroidConfig(): FirebaseAndroidConfig {
    val configFile = file("google-services.json")
    if (!configFile.exists()) return FirebaseAndroidConfig()

    val root = JsonSlurper().parse(configFile) as? Map<*, *> ?: return FirebaseAndroidConfig()
    val projectInfo = root["project_info"] as? Map<*, *>
    val matchingClient = (root["client"] as? List<*>)
        ?.filterIsInstance<Map<*, *>>()
        ?.firstOrNull { client ->
            val clientInfo = client["client_info"] as? Map<*, *>
            val androidInfo = clientInfo?.get("android_client_info") as? Map<*, *>
            androidInfo?.get("package_name") == "com.ing.event"
        }
    val apiKey = (matchingClient?.get("api_key") as? List<*>)
        ?.filterIsInstance<Map<*, *>>()
        ?.firstNotNullOfOrNull { it["current_key"] as? String }
        .orEmpty()

    return FirebaseAndroidConfig(
        databaseUrl = projectInfo?.get("firebase_url") as? String ?: "",
        apiKey = apiKey,
    )
}

val supabaseUrl = readSecret("SUPABASE_URL")
val supabaseAnonKey = readSecret("SUPABASE_ANON_KEY")
val firebaseFunctionsUrl = readSecret("FIREBASE_FUNCTIONS_URL")
val firebaseAndroidConfig = readFirebaseAndroidConfig()
val firebaseDatabaseUrl = readSecret("FIREBASE_DATABASE_URL").ifBlank {
    firebaseAndroidConfig.databaseUrl.ifBlank {
        "https://ingtechrating-default-rtdb.europe-west1.firebasedatabase.app"
    }
}
val firebaseConferenceId = readSecret("FIREBASE_CONFERENCE_ID")
val firebaseApiKey = readSecret("FIREBASE_API_KEY").ifBlank { firebaseAndroidConfig.apiKey }

android {
    namespace = "com.ing.event"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ing.event"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", supabaseUrl.toBuildConfigString())
        buildConfigField("String", "SUPABASE_ANON_KEY", supabaseAnonKey.toBuildConfigString())
        buildConfigField("String", "FIREBASE_FUNCTIONS_URL", firebaseFunctionsUrl.toBuildConfigString())
        buildConfigField("String", "FIREBASE_DATABASE_URL", firebaseDatabaseUrl.toBuildConfigString())
        buildConfigField("String", "FIREBASE_CONFERENCE_ID", firebaseConferenceId.toBuildConfigString())
        buildConfigField("String", "FIREBASE_API_KEY", firebaseApiKey.toBuildConfigString())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime.compose)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Coil
    implementation(libs.coil.compose)
}
