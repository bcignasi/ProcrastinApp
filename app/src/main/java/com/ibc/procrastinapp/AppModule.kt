/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp

//import androidx.privacysandbox.tools.core.generator.build

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room

import com.ibc.procrastinapp.data.ai.AIService
import com.ibc.procrastinapp.data.alarm.AlarmScheduler
import com.ibc.procrastinapp.data.alarm.AlarmSchedulerImpl
import com.ibc.procrastinapp.data.local.AppDatabase
import com.ibc.procrastinapp.data.repository.TaskRepository
import com.ibc.procrastinapp.data.service.ChatAIService
import com.ibc.procrastinapp.data.service.MessageStorage
import com.ibc.procrastinapp.data.service.MessageStorageImpl
import com.ibc.procrastinapp.data.service.TaskJsonExtractor
import com.ibc.procrastinapp.data.service.quotes.QuoteAIService
import com.ibc.procrastinapp.ui.assistant.AssistantViewModel
import com.ibc.procrastinapp.ui.assistant.QuoteViewModel
import com.ibc.procrastinapp.ui.tasklist.TaskListViewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// Módulo para dependencias globales de la aplicación (disponibles en toda la app)
val appModule = module {

    // Proporciona la instancia de DataStore<Preferences> usando el contexto de la aplicación
    single<DataStore<Preferences>> {
        val application: Application = get<Application>()
        application.applicationContext.dataStore
    }

    // Proporciona un CoroutineScope con SupervisorJob para operaciones en segundo plano
    // Es compartido por servicios que lo necesiten (como IA o almacenamiento)
    single<CoroutineScope> {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}


/**
 * Módulo que define las dependencias de acceso a datos (persistencia local).
 * Incluye la base de datos Room, el DAO y el repositorio principal.
 */
val dataModule = module {

    // Base de datos Room (singleton para toda la aplicación)
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "tasks_database"
        )
            .fallbackToDestructiveMigration(false) // Solo durante desarrollo
            .build()
    }

    // Proporciona el DAO de tareas desde la base de datos
    single { get<AppDatabase>().taskDao() }
    single <AlarmScheduler> { AlarmSchedulerImpl(androidContext()) }

    // Repositorio principal de tareas, usando DAO + base de datos (para transacciones)
    single { TaskRepository(get(), get(), get()) }
}

/**
 * Módulo que configura los servicios relacionados con inteligencia artificial.
 * Incluye el servicio general AI, la extracción de tareas, el almacenamiento
 * de mensajes y la generación de frases motivadoras.
 */
val chatAIModule = module {

    // AIService único para toda la aplicación (inicialización lazy)
    single {

        // Inicializamos el servicio con la clave API desde BuildConfig
        AIService.initialize(BuildConfig.OPENAI_API_KEY)

        // Devolvemos la instancia singleton
        AIService.getInstance()
    }

    // El mecanismo para extraer las tareas desde el JSON
    single { TaskJsonExtractor() }

    // Implementación de almacenamiento de mensajes
    single<MessageStorage> { MessageStorageImpl(get()) }

    // El servicio de chat con IA
    single {
        ChatAIService(
            get<MessageStorage>(),  // Inyectamos la implementación de MessageStorage
            get<AIService>(),
            get<CoroutineScope>()
        )
    }

    // Servicio para la generación de frases inspiradoras usando IA
    single { 
        QuoteAIService(
            get<AIService>(), 
            get<CoroutineScope>(),
            androidContext().getString(R.string.quote_prompt)
        ) 
    }

}

/**
 * Módulo que expone los ViewModel principales de la aplicación.
 * Cada ViewModel se construye con las dependencias necesarias desde otros módulos.
 */
val viewModelModule = module {

    // ViewModel del asistente con IA (chats + tareas)
    viewModel { AssistantViewModel(get<ChatAIService>(), get<TaskRepository>()) }

    // ViewModel para la pantalla de lista de tareas
    viewModel { TaskListViewModel(get<TaskRepository>()) }

    // ViewModel para la visualización de frases motivadoras (en el asistente)
    viewModel { QuoteViewModel(get<QuoteAIService>()) } // 👉 nuevo

}

