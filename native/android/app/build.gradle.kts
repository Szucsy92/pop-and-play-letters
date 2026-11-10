plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "hu.gyerekbillentyuzet"
    compileSdk = 35
    defaultConfig {
        applicationId = "hu.gyerekbillentyuzet"
        minSdk = 26
        targetSdk = 35
        // A CI felülírja a verziót (APP_VERSION_CODE / APP_VERSION_NAME); helyben marad az alapérték.
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION_NAME") ?: "1.0.0"
    }
    // Aláírás csak akkor, ha a CI megadja a kulcstárat (környezeti változókból, soha nem a forrásból).
    val keystorePath = System.getenv("ANDROID_KEYSTORE_FILE")
    signingConfigs {
        if (!keystorePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "playfulkeypad"
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-text")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
