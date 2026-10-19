/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.data.model.Task
import androidx.compose.ui.res.stringResource
import com.ibc.procrastinapp.R
import java.time.LocalDate

@Composable
fun TaskItemContent(
    task: Task,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    actionsEnabled: Boolean,
    onComplete: (Task) -> Unit = {},
    onDelete: (Task) -> Unit = {},
    presentation: TaskPresentation = TaskPresentation.CARDS,
    referenceDate: LocalDate = LocalDate.now()
) {
    val isFlat = presentation == TaskPresentation.FLAT
    val paddingStart = (if (isFlat) 0.dp else 8.dp) + (depth * 16).dp

    // 👇 rememberSaveable para mantener expand/collapse incluso tras recomposición
    var isExpanded by rememberSaveable(task.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = paddingStart,
                top = 8.dp,
                bottom = if (isFlat) 8.dp else 4.dp,
                end = if (isFlat) 0.dp else 8.dp
            ),
        shape = if (isFlat) RectangleShape else MaterialTheme.shapes.medium,
        elevation = if (isFlat) CardDefaults.cardElevation(defaultElevation = 0.dp)
                    else CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
//        colors = CardDefaults.cardColors(
//            containerColor = TaskPriority.fromLevel(task.priority).color(MaterialTheme.colorScheme)
//        )
    ) {
        Column(modifier = Modifier.padding(if (isFlat) 0.dp else 4.dp)) {

            if (isFlat) {
                FlatTaskHeader(
                    task = task,
                    referenceDate = referenceDate,
                    isExpanded = isExpanded,
                    onExpandClick = { isExpanded = !isExpanded },
                    onComplete = onComplete
                )
            } else {
                ShowUrgentRow(
                    task = task,
                    isExpanded = isExpanded,
                    onExpandClick = { isExpanded = !isExpanded }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ShowTitleRow(task = task)
                Spacer(modifier = Modifier.height(8.dp))
                ShowDatesRow(task)
            }


            // Notes
            task.notes.takeIf { it.isNotEmpty() }?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Periodicity
            task.periodicity.takeIf { it.isNotEmpty() }?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.tasklist_periodicity_prefix, it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (actionsEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End // Alinea los botones a la derecha
                ) {
                    Button(
                        onClick = { onComplete(task) },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(end = 8.dp), // Espacio entre botones
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(id = R.string.tasklist_complete))
                    }
                    Button(
                        onClick = { onDelete(task) },
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Color distintivo para eliminar
                    ) {
                        Text(stringResource(id = R.string.tasklist_delete))
                    }
                }
            }


            // Subtasks (only if expanded)
            if (isExpanded && task.subtasks.isNotEmpty()) {

                Spacer(modifier = Modifier.height(2.dp))

                if (!isFlat) Row(
                    modifier = Modifier.fillMaxWidth(), // Asegúrate de que la Row padre ocupe el ancho
                    verticalAlignment = Alignment.CenterVertically // Para alinear el icono y el divisor verticalmente
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .weight(1f) // El divisor tomará el espacio restante
                            .padding(end = 8.dp), // Opcional: añade un poco de espacio antes del icono
                        thickness = 1.dp, // Puedes ajustar el grosor
                        color = if (isExpanded) MaterialTheme.colorScheme.outline // Opcional: ajusta el color
                        else Color.Transparent // Opcional: ajusta el color
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Icono para expandir/contraer subtareas
                    if (task.subtasks.isNotEmpty()) {
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) 
                                    stringResource(id = R.string.tasklist_collapse_subtasks_cd) 
                                else 
                                    stringResource(id = R.string.tasklist_expand_subtasks_cd)
                            )
                        }
                    }

                }

                task.subtasks.forEach { subtask ->
                    TaskItemContent(
                        task = subtask,
                        presentation = presentation,
                        referenceDate = referenceDate,
                        depth = depth + 1,
                        actionsEnabled = actionsEnabled,
                        onComplete = onComplete,
                        onDelete = onDelete

                    )
                }
            }
        }
    }
}

