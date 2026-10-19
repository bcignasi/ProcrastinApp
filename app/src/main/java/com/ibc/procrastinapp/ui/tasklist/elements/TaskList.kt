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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.common.TaskPresentation
import com.ibc.procrastinapp.R
import java.time.LocalDate

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
    modifier: Modifier = Modifier,
    presentation: TaskPresentation = TaskPresentation.CARDS,
    overdueTasks: List<Task> = emptyList(),
    showTodaySections: Boolean = false,
    referenceDate: LocalDate = LocalDate.now()
) {
    val sections: List<Pair<Int?, List<Task>>> = if (showTodaySections) {
        listOf(R.string.agenda_overdue to overdueTasks, R.string.task_type_Today to tasks)
    } else {
        listOf(null to tasks)
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (presentation == TaskPresentation.FLAT) 0.dp else 8.dp)
    ) {
        sections.forEach { (title, sectionTasks) ->
            if (title != null && (sectionTasks.isNotEmpty() || title == R.string.task_type_Today)) {
                item(key = "section_$title") {
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp).semantics { heading() }
                    )
                    if (sectionTasks.isEmpty()) {
                        Text(
                            stringResource(R.string.agenda_empty_today),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            itemsIndexed(sectionTasks, key = { _, task -> "${title}_${task.id}" }) { index, task ->

                // Determina si mostrar acciones en línea para esta tarea
                val showActions = showInlineActionsForId != null && showInlineActionsForId == task.id

                TaskItemSelectable(
                    task = task,
                    presentation = presentation,
                    referenceDate = referenceDate,
                    isSelected = task.id in selectedTaskIds,
                    onLongClick = { onTaskLongClick(task.id) },
                    onClick = { onTaskClick(task.id) },
                    showInlineActions = showActions,
                    onComplete = { t -> onInlineComplete(t) },
                    onDelete = { t -> onInlineDelete(t) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (presentation == TaskPresentation.FLAT && index < sectionTasks.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
