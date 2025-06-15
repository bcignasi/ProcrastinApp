/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["title", "deadline", "notify"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val deadline: String = "",  // Ahora es un String en formato "yyyy-MM-dd (hh:mm opcional)"
    val priority: Int = 0,
    val periodicity: String = "",
    val notes: String = "",
    val completed: Boolean = false,
    // Para pedir una notificación en una fecha dada
    val notify: String = "",  // Ahora es un String en formato "yyyy-MM-dd (hh:mm opcional)"

    @ColumnInfo(index = true)
    val parentTaskId: Long? = null
)