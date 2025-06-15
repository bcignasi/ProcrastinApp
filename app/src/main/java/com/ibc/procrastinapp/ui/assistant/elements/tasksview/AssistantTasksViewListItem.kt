/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.common.TaskItemContent
import com.ibc.procrastinapp.ui.common.setContainerColor

/**
 * Componente que muestra una tarea individual
 */
@Composable
fun AssistantTasksViewListItem(
    task: Task,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(setContainerColor(task)),
//            containerColor = when (task.priority) {
//                3 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) // Urgente
//                2 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) // Alta
//                1 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) // Media
//                else -> MaterialTheme.colorScheme.surfaceVariant // Normal
//            }
//        )
    ) {
        // TaskItemContent es solo para visualización aquí
        TaskItemContent(
            task = task,
            modifier = modifier,
            depth = 0,
            actionsEnabled = false,
            onComplete = {  },
            onDelete = {  }
        )
    }
}

///////////////////////////////////////////////////////////////

private fun createPreviewTask(priority: Int): Task {
    return Task(
        id = 0,
        title = "Task 1",
        deadline = "2025-12-31 12:00",
        priority = priority,
        notify = "2025-12-31 10:00",
        notes = "Notes for task 1",
    )
}

@Preview(name = "Priority3")
@Composable
fun TaskPreview3() {
    val task = createPreviewTask(priority = 3)
    AssistantTasksViewListItem(task = task)
}

@Preview(name = "Priority2")
@Composable
fun TaskItemPreview2() {
    val task = createPreviewTask(priority = 2)
    AssistantTasksViewListItem(task = task)
}

@Preview(name = "Priority1")
@Composable
fun TaskItemPreview1() {
    val task = createPreviewTask(priority = 1)
    AssistantTasksViewListItem(task = task)
}

@Preview(name = "Priority0")
@Composable
fun TaskItemPreview0() {
    val task = createPreviewTask(priority = 0)
    AssistantTasksViewListItem(task = task)
}