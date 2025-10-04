/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.tasklist.elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
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
 * y un botón para acceder al asistente de IA.
 *
 * @param currentListType Tipo actual de consulta de tareas.
 * @param onCurrentListTypeChange Función que se invoca cuando se selecciona un nuevo tipo de consulta.
 * @param onGoToAssistant Acción a realizar al pulsar el botón que navega al asistente de IA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListTopBar(
    currentListType: TaskListQueryType,
    onCurrentListTypeChange: (TaskListQueryType) -> Unit,
    onGoToAssistant: () -> Unit,
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

            // 🎯 Ir al Assistant
            IconButton(
                onClick = onGoToAssistant
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = androidx.compose.ui.res.stringResource(id = R.string.tasklist_go_to_assistant_cd))
            }
        },

        /*
        SUPRIMO LAS FLECHAS DE NAVEGACIÓN ESTÁNDAR A LA IZQUIERDA DE LA TOOLBAR
        Solo hay dos pantallas, y el icono de la derecha ya se hace cargo de la navegación
//        navigationIcon = {
//            IconButton(onClick = onBackClick) {
//                Icon(
//                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                    contentDescription = "Volver atrás"
//                )
//            }
//        }
         */
    )
}