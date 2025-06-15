/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.tasklist.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.common.TaskItemContent
import com.ibc.procrastinapp.ui.common.setContainerColor

@Composable
fun TaskItemSelectable(
    task: Task,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showInlineActions: Boolean = false,
    onComplete: (Task) -> Unit = {},
    onDelete: (Task) -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth()
            // Detectar clic largo para iniciar selección
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                setContainerColor(task),
                //TaskPriority.fromLevel(task.priority).containerColor(MaterialTheme.colorScheme)
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
//        TaskItemContent(task = task)
        TaskItemContent(
            task = task,
            actionsEnabled = showInlineActions,
            onComplete = onComplete,
            onDelete = onDelete
        )
    }
}

/////////////////////////////////////////////////////////////////

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

@Preview(
    name = "Preview3",
    group = "TaskItemSelectablePreviews",
)
@Composable
fun TaskPreview3() {
    val task = createPreviewTask(priority = 3)
    TaskItemSelectable(task = task, isSelected = false, onLongClick = {}, onClick = {})
}

@Preview(
    name = "Preview2",
    group = "TaskItemSelectablePreviews",
)
@Composable
fun TaskItemSelectablePreview2() {
    val task = createPreviewTask(priority = 2)
    TaskItemSelectable(task = task, isSelected = false, onLongClick = {}, onClick = {})
}

@Preview(
    name = "Preview1",
    group = "TaskItemSelectablePreviews",
)
@Composable
fun TaskItemSelectablePreview1() {
    val task = createPreviewTask(priority = 1)
    TaskItemSelectable(task = task, isSelected = false, onLongClick = {}, onClick = {})
}

@Preview(
    name = "Preview0",
    group = "TaskItemSelectablePreviews",
)
@Composable
fun TaskItemSelectablePreview0() {
    val task = createPreviewTask(priority = 0)
    TaskItemSelectable(task = task, isSelected = false, onLongClick = {}, onClick = {})
}

@Preview(
    name = "Select",
    group = "TaskItemPreviews",
)
@Composable
fun TaskItemSelectablePreviewSelected() {
    val task = createPreviewTask(priority = 3)
    TaskItemSelectable(task = task, isSelected = true, onLongClick = {}, onClick = {})
}
