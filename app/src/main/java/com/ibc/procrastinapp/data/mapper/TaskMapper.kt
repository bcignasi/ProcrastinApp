/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.mapper

import com.ibc.procrastinapp.data.local.TaskEntity
import com.ibc.procrastinapp.data.model.Task

// data/mapper/TaskMapper.kt
object TaskMapper {
    fun TaskEntity.toTask(): Task =
        Task(
            id = id,
            title = title,
            deadline = deadline,
            priority = priority,
            periodicity = periodicity,
            notes = notes,
            completed = completed,
            notify = notify,
            subtasks = emptyList()
        )

    fun Task.toEntity(): TaskEntity =
        TaskEntity(
            id = id,
            title = title,
            deadline = deadline?:  "",
            priority = priority,
            periodicity = periodicity?:  "",
            notes = notes?:  "",
            completed = completed,
            notify = notify?: "",
            parentTaskId = null // Si no es una subtarea
        )
}