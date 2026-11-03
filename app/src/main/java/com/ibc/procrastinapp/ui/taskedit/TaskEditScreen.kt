/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.taskedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ibc.procrastinapp.R
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla de edición clásica (formulario) de una tarea existente.
 *
 * Primera iteración: solo cablea la navegación y los campos básicos (título, notas,
 * completada). El resto de campos del modelo (fecha límite, prioridad, periodicidad,
 * aviso, día planificado) se incorporarán cuando se diseñe el formulario completo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: Long,
    onBack: () -> Unit,
    viewModel: TaskEditViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.task_edit_back_cd)
                        )
                    }
                }
            )
        }
    ) { padding ->
        val task = uiState.task
        when {
            uiState.notFound -> Text(
                stringResource(R.string.task_edit_not_found),
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )

            task != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = task.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text(stringResource(R.string.task_edit_field_title)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = task.notes,
                    onValueChange = viewModel::updateNotes,
                    label = { Text(stringResource(R.string.task_edit_field_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.task_edit_field_completed))
                    Switch(
                        checked = task.completed,
                        onCheckedChange = { viewModel.toggleCompleted() }
                    )
                }
                Button(
                    onClick = { viewModel.save(onSaved = onBack) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.task_edit_save))
                }
            }

            else -> Unit // Cargando: nada que mostrar todavía.
        }
    }
}
