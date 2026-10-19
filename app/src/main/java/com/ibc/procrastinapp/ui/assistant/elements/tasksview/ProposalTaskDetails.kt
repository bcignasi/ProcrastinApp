package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.model.TaskPriority
import com.ibc.procrastinapp.ui.common.flatTaskDateText
import java.time.LocalDate

/** Conserva los metadatos sin repetirlos en la fila principal. */
@Composable
internal fun ProposalTaskDetails(task: Task, referenceDate: LocalDate, includeReminder: Boolean = false) {
    val hasDetails = task.notes.isNotBlank() || task.periodicity.isNotBlank() || task.deadline.isNotBlank() ||
        task.priority > 0 || (includeReminder && task.notify.isNotBlank())
    if (!hasDetails) return
    var expanded by rememberSaveable(task.id, task.title) { mutableStateOf(false) }
    val description = stringResource(
        if (expanded) R.string.proposal_hide_details_cd else R.string.proposal_show_details_cd, task.title
    )
    val locale = LocalConfiguration.current.locales[0]
    Column {
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.semantics { contentDescription = description }
        ) { Text(stringResource(R.string.proposal_details)) }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val lines = listOfNotNull(
                    task.notes.takeIf { it.isNotBlank() },
                    task.periodicity.takeIf { it.isNotBlank() }?.let { stringResource(R.string.tasklist_periodicity_prefix, it) },
                    flatTaskDateText(task.deadline, referenceDate, locale)?.let { stringResource(R.string.proposal_deadline, it) },
                    if (includeReminder) flatTaskDateText(task.notify, referenceDate, locale)?.let {
                        stringResource(R.string.proposal_reminder, it)
                    } else null,
                    if (task.priority > 0) stringResource(R.string.proposal_priority,
                        stringResource(TaskPriority.fromLevel(task.priority).labelResId)) else null
                )
                lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
