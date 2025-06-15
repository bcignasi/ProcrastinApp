/*
 * Copyright (c) 2025 Ignasi Buendia Corruchaga
 * Licensed under MIT License
 * See LICENSE file in project root for full license text
 */
package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.data.model.Task
import com.ibc.procrastinapp.ui.assistant.QuoteViewModel
import com.ibc.procrastinapp.ui.common.AquiNoHayNadaBox

/**
 * Lista de tareas extraídas
 */
@Composable
fun AssistantTasksViewList(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    quoteViewModel: QuoteViewModel
) {
    if (tasks.isEmpty()) {
        AquiNoHayNadaBox(modifier, quoteViewModel)

    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                AssistantTasksViewListItem(task)
            }
        }
    }
}