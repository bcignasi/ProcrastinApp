/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.ibc.procrastinapp.R

/**
 * Componente de título con menú desplegable para TopAppBar.
 *
 * @param currentType Tipo de lista actualmente seleccionado
 * @param allTypes Lista de todos los tipos disponibles
 * @param onTypeSelected Callback invocado cuando se selecciona un nuevo tipo
 * @param modifier Modifier opcional para personalización
 */
@Composable
fun <T: DropdownMenuOption> TitleDropdownMenu(
    currentType: T,
    allTypes: List<T>,
    onTypeSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var clickCount by remember { mutableStateOf(0) }

    // Función para seleccionar el siguiente elemento
    fun selectNext() {
        val currentIndex = allTypes.indexOf(currentType)
        val nextIndex = (currentIndex + 1) % allTypes.size
        onTypeSelected(allTypes[nextIndex])
    }

    // Manejo del timeout para detectar doble click
    LaunchedEffect(clickCount) {
        if (clickCount == 1) {
            delay(300)
            if (clickCount == 1) {
                // Solo un click - abrir menú
                isMenuExpanded = true
            }
            clickCount = 0
        } else if (clickCount == 2) {
            // Doble click - ejecutar next
            selectNext()
            clickCount = 0
        }
    }
    Box(modifier = modifier) {
        // Título interactivo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { clickCount++ }
                    )
                }
                //.clickable { isMenuExpanded = true }
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = stringResource(currentType.titleResId),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.title_dropdown_change_type_cd),
                modifier = Modifier.size(24.dp)
            )
        }

        // Menú desplegable
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        ) {
            allTypes.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(type.titleResId),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onTypeSelected(type)
                        isMenuExpanded = false
                    },
                    leadingIcon = {
                        if (currentType == type) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
}