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
    compileSdk = 35



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
        targetSdk = 35
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Iconos de material3 (ExpandMore, ExpandLess)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.material.icons.core.android)
    implementation(libs.androidx.material.icons.extended)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // AIClient dependencies
    implementation(libs.ui) // Versión actual de Compose
    //implementation(libs.androidx.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor) // Para depurar las solicitudes HTTP

    // Koin dependencies
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // DataStore dependencies
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json) //If you need to serialize to json

    // For function "extractJSON"
    //implementation(libs.gson)
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation(libs.gson)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    // kapt("androidx.room:room-compiler:2.5.2")
    // ksp(libs.room.compiler)
    // add("ksp", libs.room.compiler)  // ¡Prueba con add("ksp", ...)
    ksp(libs.room.compiler)  // ¡Esta es la forma correcta de usar KSP con Kotlin DSL!

    // Mockito (tests para Room
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.android)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.junit) // or newer version if available
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Kotlin stdlib for test code:
    //androidTestImplementation(libs.kotlin.stdlib.jdk8)

    // Kotlin coroutines test library
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Compose Navigation dependencies
    implementation(libs.androidx.navigation.compose)

    // WorkManager dependencies
    implementation(libs.androidx.work.runtime.ktx)

    // Dependencias para testing
    testImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.androidx.core.testing)

    // Si también necesitas la librería de corrutinas para testing
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Robolectric sirve para testing de UI
    testImplementation(libs.robolectric)

    // Para los tests de Mockito
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.mockito.kotlin)

    // MockK para testing
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk.android)

// Para las reglas de arquitectura usadas en los tests (InstantTaskExecutorRule)
    testImplementation(libs.androidx.core.testing)

    // Splash screen video
    // ExoPlayer (Media3)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // Navegación Compose
    implementation(libs.androidx.navigation.compose)

}