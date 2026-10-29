package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.data.model.TaskPriority
import com.ibc.procrastinapp.ui.common.flatTaskDateText
import java.time.LocalDate

/** Instrucción breve a primera vista, con acceso al contenido completo sin modificar los datos. */
@Composable
internal fun ProposalTaskDetails(task: Task, referenceDate: LocalDate, includeReminder: Boolean = false) {
    val hasDetails = task.notes.isNotBlank() || task.periodicity.isNotBlank() || task.deadline.isNotBlank() ||
        task.priority > 0 || (includeReminder && task.notify.isNotBlank())
    if (!hasDetails) return
    var expanded by rememberSaveable(task) { mutableStateOf(false) }
    var notesOverflow by remember(task.notes) { mutableStateOf(false) }
    val description = stringResource(
        if (expanded) R.string.proposal_hide_details_cd else R.string.proposal_show_details_cd, task.title
    )
    val locale = LocalConfiguration.current.locales[0]
    val lines = listOfNotNull(
        task.periodicity.takeIf { it.isNotBlank() }?.let { stringResource(R.string.tasklist_periodicity_prefix, it) },
        flatTaskDateText(task.deadline, referenceDate, locale)?.let { stringResource(R.string.proposal_deadline, it) },
        if (includeReminder) flatTaskDateText(task.notify, referenceDate, locale)?.let {
            stringResource(R.string.proposal_reminder, it)
        } else null,
        if (task.priority > 0) stringResource(R.string.proposal_priority,
            stringResource(TaskPriority.fromLevel(task.priority).labelResId)) else null
    )
    Column {
        if (task.notes.isNotBlank()) {
            Text(
                text = task.notes,
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.8f),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) notesOverflow = it.hasVisualOverflow }
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (expanded || notesOverflow || lines.isNotEmpty()) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier.semantics { contentDescription = description }
            ) { Text(stringResource(if (expanded) R.string.proposal_less else R.string.proposal_more)) }
        }
    }
}
