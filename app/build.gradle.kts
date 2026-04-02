plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val supabaseUrl = (project.findProperty("SUPABASE_URL") as String?)
    ?: ""
val supabasePublishableKey = (project.findProperty("SUPABASE_PUBLISHABLE_KEY") as String?)
    ?: ""

android {
    namespace = "com.kelasin.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kelasin.app"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabasePublishableKey\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("../kelasin-release.jks")
            storePassword = "kelasin123"
            keyAlias = "kelasin"
            keyPassword = "kelasin123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Core AndroidX
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.8")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // HorizontalPager (swipe between tabs)
    implementation("androidx.compose.foundation:foundation:1.7.8")

    // Image loading (Coil) — for in-app image file preview
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // DataStore Preferences (sesi login)
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // WorkManager (reminder otomatis)
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Supabase
    val supabaseVer = "3.0.2"
    implementation("io.github.jan-tennert.supabase:auth-kt:$supabaseVer")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVer")
    implementation("io.github.jan-tennert.supabase:realtime-kt:$supabaseVer")
    implementation("io.github.jan-tennert.supabase:storage-kt:$supabaseVer")
    implementation("io.ktor:ktor-client-okhttp:3.0.1")
    implementation("io.ktor:ktor-client-websockets:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

}
