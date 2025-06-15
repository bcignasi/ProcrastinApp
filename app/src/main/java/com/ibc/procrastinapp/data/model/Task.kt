/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.model

// data/model/Task.kt
data class Task(
    val id: Long = 0,
    val title: String,
    val deadline: String = "",
    val priority: Int = 0,
    val periodicity: String = "",
    val notes: String = "",
    val completed: Boolean = false,
    val notify: String = "",
    val subtasks: List<Task> = emptyList()
) {
    companion object {
        fun empty() = Task(title = "")
    }

    // Metodo para asegurarse de que no hay nulls
    // En el caso de title, no debería haber null, pero cuando se decodifica vía GSON
    // desde un message de ChatGPT, GSON sí devuelve el campo a null, provocando errores
    // al hacer las validaciones. Por eso esta programación defensiva para title.
    fun withNonNullStrings(): Task {
        return copy(
            id = 0,
            title = title ?: "",
            deadline = deadline ?: "",
            periodicity = periodicity ?: "",
            notes = notes ?: "",
            notify = notify ?: "",
            //subtasks = subtasks.map { it.withNonNullStrings() }
            subtasks = subtasks?.map { it.withNonNullStrings() } ?: emptyList()
        )
    }

    fun countCompletedSubtasks(): Int {
        return subtasks.count { it.completed }
    }

    fun countAllSubtasks(): Int {
        return subtasks.size
    }

}