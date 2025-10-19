import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinSerialization) // Usando alias
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ibc.procrastinapp"
    compileSdk = 36



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
        targetSdk = 36
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
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "ENABLE_LOGS", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
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
        }
    }

}

dependencies {

    // --- Core y Lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // --- Compose ---
    // El BOM gestiona las versiones de las librerías de Compose.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose) // Alias estandarizado

    // UI
    implementation(libs.androidx.compose.ui) // Única implementación, alias correcto
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Material
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core.android)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // El BOM también aplica a las dependencias de test
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

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
    // Mockito
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.mockito.kotlin) // Duplicado, pero agrupado por claridad
    // MockK
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk.android)
    // Otros
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.test) // Duplicado
    testImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.androidx.core.testing) // Duplicado
    testImplementation(libs.robolectric)
}
