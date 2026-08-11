import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinSerialization) // Usando alias
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ibc.procrastinapp"
    compileSdk = 37

    val openAiKey: String by lazy {
        val props = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { props.load(it) }
        }

        props["OPENAI_API_KEY"]?.toString()
            ?: throw GradleException("Falta OPENAI_API_KEY en local.properties")
    }


    defaultConfig {
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKey\"")
        applicationId = "com.ibc.procrastinapp"
        minSdk = 26
        @Suppress("AndroidTargetSdkEdit")
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "ENABLE_LOGS", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("Boolean", "ENABLE_LOGS", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
            pickFirsts += "dispatcher.jar"
        }
    }

}

dependencies {

    // --- Core y Lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // --- Compose ---
    // El BOM gestiona las versiones de las librerías de Compose.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose) // Alias estandarizado

    // UI
    implementation(libs.androidx.compose.ui) // Única implementación, alias correcto
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)

    // Material
    implementation(libs.compose.material3)
    implementation(libs.androidx.material.icons.core.android)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // El BOM también aplica a las dependencias de test
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // --- AI, Redes y Serialización ---
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)

    // --- Koin (Inyección de dependencias) ---
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // --- DataStore ---
    implementation(libs.androidx.datastore.preferences)

    // --- Room (Base de datos) ---
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // --- WorkManager ---
    implementation(libs.androidx.work.runtime.ktx)

    // --- Media3 (ExoPlayer) ---
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // --- Librerías de Testing Adicionales ---
    // Unit Testing
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    // Instrumented Testing
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.core.testing)
}
