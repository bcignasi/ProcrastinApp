package com.ibc.procrastinapp.ui.tasklist.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.common.ShowDatesRow
import com.ibc.procrastinapp.ui.common.TaskItemContent
import com.ibc.procrastinapp.ui.common.TaskPresentation
import com.ibc.procrastinapp.ui.common.flatTaskDateText
import java.time.LocalDate

/** Las raíces son metas; abrirlas consulta el plan sin entrar en el asistente. */
@Composable
fun GoalList(
    tasks: List<Task>,
    selectedTaskIds: Set<Long>,
    onTaskClick: (Long) -> Unit,
    onTaskLongClick: (Long) -> Unit,
    onCompleteTask: (Task) -> Unit,
    referenceDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val grouped = tasks.sortedWith(compareBy({ it.projectId == null }, { it.projectName }, { it.projectId }))
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
    ) {
        itemsIndexed(grouped, key = { _, goal -> goal.id }) { index, goal ->
            if ((index == 0 || grouped[index - 1].projectId != goal.projectId) && grouped.first().projectId != null) {
                Text(goal.projectName ?: stringResource(R.string.project_unassigned),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
            var expanded by rememberSaveable(goal.id) { mutableStateOf(false) }
            val expansionState = stringResource(if (expanded) R.string.goal_expanded else R.string.goal_collapsed)
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (goal.id in selectedTaskIds) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .combinedClickable(
                            role = Role.Button,
                            onClick = {
                                if (selectedTaskIds.isEmpty()) expanded = !expanded else onTaskClick(goal.id)
                            },
                            onLongClick = { onTaskLongClick(goal.id) }
                        )
                        .semantics { stateDescription = expansionState }
                        .heightIn(min = 56.dp)
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (goal.completed) {
                            Text(stringResource(R.string.agenda_filter_completed),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
                if (expanded) {
                    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (goal.notes.isNotBlank()) {
                            Text(goal.notes, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (goal.periodicity.isNotBlank()) {
                            Text(stringResource(R.string.tasklist_periodicity_prefix, goal.periodicity),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        val scheduledDay = flatTaskDateText(goal.scheduledDate, referenceDate, LocalConfiguration.current.locales[0])
                        if (scheduledDay != null) {
                            Text(stringResource(R.string.task_scheduled_date, scheduledDay), style = MaterialTheme.typography.bodyMedium)
                        }
                        ShowDatesRow(goal)
                        val editDescription = stringResource(R.string.goal_edit_cd, goal.title)
                        TextButton(
                            onClick = { onTaskClick(goal.id) },
                            enabled = selectedTaskIds.isEmpty(),
                            modifier = Modifier.semantics { contentDescription = editDescription }
                        ) { Text(stringResource(R.string.goal_edit)) }
                        goal.subtasks.forEach { subtask ->
                            key(subtask.id) {
                                TaskItemContent(
                                    task = subtask,
                                    presentation = TaskPresentation.FLAT,
                                    referenceDate = referenceDate,
                                    actionsEnabled = false,
                                    onComplete = onCompleteTask
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
