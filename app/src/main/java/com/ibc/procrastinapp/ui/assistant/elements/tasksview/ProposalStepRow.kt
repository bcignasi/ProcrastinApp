package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.common.flatTaskDateText
import java.time.LocalDate

/** Fila de lectura; los descendientes conservan su jerarquía y su numeración. */
@Composable
internal fun ProposalStepRow(
    task: Task,
    number: String?,
    referenceDate: LocalDate,
    modifier: Modifier = Modifier,
    connectBefore: Boolean = false,
    connectAfter: Boolean = false
) {
    val locale = LocalConfiguration.current.locales[0]
    val reminder = flatTaskDateText(task.notify, referenceDate, locale)
    val state = stringResource(
        if (task.completed) R.string.proposal_completed_read_only else R.string.proposal_pending_read_only
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            if (number != null) ProposalStepMarker(number, task.completed, connectBefore, connectAfter)
            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                color = if (task.completed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (task.completed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { stateDescription = state },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (reminder != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Notifications, stringResource(R.string.notification_cd),
                                        modifier = Modifier.size(16.dp), tint = LocalContentColor.current.copy(alpha = 0.8f))
                                    Text(reminder, style = MaterialTheme.typography.bodySmall,
                                        color = LocalContentColor.current.copy(alpha = 0.8f))
                                }
                            }
                        }
                        // Indicador decorativo: el estado accesible pertenece a la fila, sin acción de completar.
                        Checkbox(
                            checked = task.completed,
                            onCheckedChange = null,
                            modifier = Modifier.clearAndSetSemantics {},
                            colors = CheckboxDefaults.colors(uncheckedColor = LocalContentColor.current)
                        )
                    }
                    ProposalTaskDetails(task, referenceDate)
                }
            }
        }
        if (task.subtasks.isNotEmpty()) {
            Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                task.subtasks.forEachIndexed { index, child ->
                    ProposalStepRow(
                        task = child, number = "${number?.let { "$it." }.orEmpty()}${index + 1}",
                        referenceDate = referenceDate,
                        connectBefore = index > 0, connectAfter = index < task.subtasks.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun ProposalStepMarker(number: String, completed: Boolean, connectBefore: Boolean, connectAfter: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.width(IntrinsicSize.Min).fillMaxHeight().padding(end = 8.dp).drawBehind {
            val center = 24.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
            if (connectBefore) drawLine(colors.outlineVariant, Offset(size.width / 2, -8.dp.toPx()),
                Offset(size.width / 2, center), strokeWidth = 1.dp.toPx(), pathEffect = dash)
            if (connectAfter) drawLine(colors.outlineVariant, Offset(size.width / 2, center),
                Offset(size.width / 2, size.height + 8.dp.toPx()), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        },
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier.padding(top = 12.dp), shape = CircleShape,
            color = if (completed) colors.primary else colors.background,
            contentColor = if (completed) colors.onPrimary else colors.onSurfaceVariant,
            border = if (completed) null else BorderStroke(1.dp, colors.onSurfaceVariant)
        ) {
            Box(Modifier.defaultMinSize(minWidth = 24.dp, minHeight = 24.dp).padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center) {
                Text(number, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
