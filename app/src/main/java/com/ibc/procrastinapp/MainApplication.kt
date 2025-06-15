/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Punto de entrada principal de la aplicación.
 *
 * Esta clase extiende `Application` y se encarga de inicializar los componentes globales
 * necesarios para el funcionamiento de la app, como el sistema de inyección de dependencias.
 *
 * Funcionalidades principales:
 *
 * ✅ Inicializa Koin (Dependency Injection):
 * - Registra los módulos de la aplicación: `appModule`, `dataModule`, `chatAIModule`, `viewModelModule`.
 * - Proporciona el contexto de la aplicación a Koin.
 * - Habilita el logger para depuración de inyección.
 *
 * ✅ Define una extensión global de `Context` para acceder a `DataStore<Preferences>`:
 * - Usa `preferencesDataStore` con el nombre "messages_preferences".
 * - Esta instancia puede utilizarse en cualquier parte de la app para guardar/cargar preferencias.
 *
 * Esta clase se declara en el `AndroidManifest.xml` como `android:name=".MainApplication"`.
 */

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "messages_preferences")

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializar Koin
        startKoin {

            // Habilita el sistema de logs de Koin para depuración (nivel INFO por defecto)
            androidLogger()

            // Proporciona el Contexto de la aplicación a Koin (necesario para instancias dependientes de Android)
            androidContext(this@MainApplication)

            // Módulos koin definidos en AppModule.kt
            modules(listOf(
                appModule,
                dataModule,
                chatAIModule,
                viewModelModule
            ))
        }
    }
}