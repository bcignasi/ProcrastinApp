package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.common.flatTaskDateText
import java.time.LocalDate

@Composable
internal fun ProposalStepRow(
    task: Task,
    number: String?,
    referenceDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val scheduledDay = flatTaskDateText(task.scheduledDate, referenceDate, locale)
    val reminder = flatTaskDateText(task.notify, referenceDate, locale)
    val hasDetails = task.notes.isNotBlank() || task.periodicity.isNotBlank() ||
        task.deadline.isNotBlank() || task.priority > 0
    var expanded by rememberSaveable(task) { mutableStateOf(false) }
    val status = stringResource(if (task.completed) R.string.proposal_completed_read_only else R.string.proposal_pending_read_only)
    val actionLabel = stringResource(if (expanded) R.string.proposal_hide_details_cd else R.string.proposal_show_details_cd, task.title)
    val disclosure = if (hasDetails) Modifier.clickable(role = Role.Button, onClickLabel = actionLabel) {
        expanded = !expanded
    } else Modifier

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (task.completed) colors.primaryContainer else colors.surface,
            contentColor = if (task.completed) colors.onPrimaryContainer else colors.onSurface,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column {
                Row(Modifier.fillMaxWidth().then(disclosure)
                    .semantics(mergeDescendants = true) { stateDescription = status }
                    .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (number != null) {
                        Surface(shape = CircleShape, color = colors.secondaryContainer, contentColor = colors.onSecondaryContainer) {
                            Box(Modifier.defaultMinSize(minWidth = 26.dp, minHeight = 26.dp).padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center) {
                                Text(number, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (scheduledDay != null) {
                            Text(stringResource(R.string.proposal_scheduled_date, scheduledDay),
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        if (reminder != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Notifications, stringResource(R.string.notification_cd), Modifier.size(16.dp))
                                Text(reminder, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (task.completed) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                    if (hasDetails) Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                }
                if (expanded && hasDetails) {
                    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider(color = colors.outlineVariant)
                        ProposalTaskDetails(task, referenceDate, showInFull = true)
                    }
                }
            }
        }
        if (task.subtasks.isNotEmpty()) {
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                task.subtasks.forEachIndexed { index, child ->
                    ProposalStepRow(child, "${number?.let { "$it." }.orEmpty()}${index + 1}", referenceDate)
                }
            }
        }
    }
}
