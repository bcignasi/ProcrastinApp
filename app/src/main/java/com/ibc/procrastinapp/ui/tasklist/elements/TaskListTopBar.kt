/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.tasklist.elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import com.ibc.procrastinapp.ui.common.TitleDropdownMenu
import com.ibc.procrastinapp.ui.tasklist.TaskListQueryType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.ibc.procrastinapp.R

/**
 * Barra superior de la pantalla de lista de tareas.
 *
 * Muestra el tipo de vista seleccionada mediante un menú desplegable
 * y un botón para acceder a Ajustes.
 *
 * @param currentListType Tipo actual de consulta de tareas.
 * @param onCurrentListTypeChange Función que se invoca cuando se selecciona un nuevo tipo de consulta.
 * @param onOpenSettings Acción para abrir Ajustes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopBar(
    currentListType: TaskListQueryType,
    onCurrentListTypeChange: (TaskListQueryType) -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            // Usar nuestro componente modularizado
            TitleDropdownMenu(
                currentType = currentListType,
                allTypes = TaskListQueryType.entries,
                onTypeSelected = onCurrentListTypeChange
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        actions = {
            // 📅 Fecha actual (día mes)
            Text(
                text = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("d MMM", Locale("es", "ES"))
                ),
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.settings_open)
                )
            }
        },

    )
}
