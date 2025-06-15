/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.tasklist.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.data.model.Task

/**
 * Lista de tareas con LazyColumn
 */
@Composable
fun TaskList(
    tasks: List<Task>,
    selectedTaskIds: Set<Long>,
    onTaskClick: (Long) -> Unit,
    onTaskLongClick: (Long) -> Unit,
    showInlineActionsForId: Long?,
    onInlineComplete: (Task) -> Unit,
    onInlineDelete: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { it.id }) { task ->

            // Determina si mostrar acciones en línea para esta tarea
            val showActions = showInlineActionsForId != null && showInlineActionsForId == task.id

            TaskItemSelectable(
                task = task,
                isSelected = task.id in selectedTaskIds,
                onLongClick = { onTaskLongClick(task.id) },
                onClick = { onTaskClick(task.id) },
                showInlineActions = showActions, // ✅
                onComplete = { t -> onInlineComplete(t) },  // Captura el t: Task en cada scope
                onDelete = { t -> onInlineDelete(t) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
