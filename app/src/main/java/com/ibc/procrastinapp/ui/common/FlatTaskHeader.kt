package com.ibc.procrastinapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.model.TaskPriority
import java.time.LocalDate

/** Cabecera de la fila plana; completar y desplegar son acciones independientes. */
@Composable
internal fun FlatTaskHeader(
    task: Task,
    referenceDate: LocalDate,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onComplete: (Task) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val deadline = flatTaskDateText(task.deadline, referenceDate, locale)
    val notify = flatTaskDateText(task.notify, referenceDate, locale)
    val priority = when (TaskPriority.fromLevel(task.priority)) {
        TaskPriority.High -> stringResource(R.string.task_priority_high_inline)
        TaskPriority.Urgent -> stringResource(R.string.task_priority_urgent_inline)
        else -> null
    }
    val metadata = listOfNotNull(deadline, priority).joinToString(" · ")
    val checkboxLabel = stringResource(
        if (task.completed) R.string.task_mark_pending_cd else R.string.task_mark_complete_cd,
        task.title
    )

    Row(verticalAlignment = Alignment.Top) {
        Checkbox(
            checked = task.completed,
            onCheckedChange = { onComplete(task) },
            colors = CheckboxDefaults.colors(uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.semantics { contentDescription = checkboxLabel }
        )
        Column(
            modifier = Modifier.weight(1f).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (metadata.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (deadline != null) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(metadata, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (notify != null) {
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.notification_cd),
                        modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(notify, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (task.subtasks.isNotEmpty()) {
            IconButton(onClick = onExpandClick) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.tasklist_collapse_subtasks_cd else R.string.tasklist_expand_subtasks_cd
                    )
                )
            }
        }
    }
}
