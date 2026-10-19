/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ibc.procrastinapp.data.model.Task
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * Componente que muestra una tarea individual
 */
@Composable
fun AssistantTasksViewListItem(
    task: Task,
    modifier: Modifier = Modifier,
    referenceDate: LocalDate = LocalDate.now()
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (task.subtasks.isEmpty()) {
            ProposalStepRow(task = task, number = null, referenceDate = referenceDate)
        } else {
            ProposalGoalHeader(task = task, referenceDate = referenceDate)
            task.subtasks.forEachIndexed { index, subtask ->
                ProposalStepRow(
                    task = subtask, number = "${index + 1}", referenceDate = referenceDate,
                    connectBefore = index > 0, connectAfter = index < task.subtasks.lastIndex
                )
            }
        }
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
