/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.ui.assistant.AssistantScreenType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Cabecera Serena y selector de las dos vistas existentes del asistente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantTopBar(
    isSaving: Boolean,
    onDeleteAll: () -> Unit,
    currentScreenMode: AssistantScreenType,
    onCurrentScreenModeChange: (AssistantScreenType) -> Unit,
    onOpenSettings: () -> Unit,
    date: LocalDate
) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val formattedDate = date.format(
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale)
    )
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(color = colors.background) {
        Column(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .padding(horizontal = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle, contentDescription = null,
                    tint = colors.primary, modifier = Modifier.size(20.dp)
                )
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge, color = colors.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_open))
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.assistant_more_options))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.assistant_restart_conversation)) },
                            enabled = !isSaving,
                            onClick = {
                                menuExpanded = false
                                onDeleteAll()
                            }
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.assistant_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.agenda_date_today, formattedDate),
                style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )
            AssistantModeTabs(currentScreenMode, onCurrentScreenModeChange)
        }
    }
}

@Composable
private fun AssistantModeTabs(
    currentMode: AssistantScreenType,
    onModeChange: (AssistantScreenType) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = MaterialTheme.shapes.medium, color = colors.surfaceVariant
    ) {
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            AssistantScreenType.entries.forEach { mode ->
                val selected = currentMode == mode
                Row(
                    modifier = Modifier.weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (selected) colors.primaryContainer else Color.Transparent)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onModeChange(mode) })
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(mode.titleResId), style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
