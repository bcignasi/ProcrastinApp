package com.ibc.procrastinapp.ui.assistant.elements.tasksview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ibc.procrastinapp.R
import com.ibc.procrastinapp.data.local.ProjectEntity
import com.ibc.procrastinapp.data.repository.ProjectDestination
import kotlinx.coroutines.flow.Flow

@Composable
internal fun ProjectSelector(
    destination: ProjectDestination, editing: Boolean, editingUnassignedGoals: Boolean, enabled: Boolean,
    projects: () -> Flow<List<ProjectEntity>>, onChoose: (ProjectDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val label = when (destination) {
        ProjectDestination.Keep -> stringResource(when {
            editingUnassignedGoals -> R.string.project_add
            editing -> R.string.project_change
            else -> R.string.project_choose
        })
        ProjectDestination.Unassigned -> stringResource(R.string.project_add)
        is ProjectDestination.Existing, is ProjectDestination.Create -> stringResource(R.string.project_change)
    }
    ProposalActionButton(label, Icons.Outlined.Folder, onClick = { open = true }, modifier = modifier, enabled = enabled)
    if (!open) return

    val options by remember(projects) { projects() }.collectAsStateWithLifecycle(emptyList())
    var name by rememberSaveable { mutableStateOf("") }
    fun select(value: ProjectDestination) {
        onChoose(value)
        open = false
    }

    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.project_choose)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, singleLine = true,
                    label = { Text(stringResource(R.string.project_new_name)) },
                    modifier = Modifier.fillMaxWidth())
                options.forEach { project ->
                    TextButton(onClick = { select(ProjectDestination.Existing(project.id, project.name)) }) {
                        Text(project.name)
                    }
                }
                TextButton(onClick = { select(ProjectDestination.Unassigned) }) {
                    Text(stringResource(R.string.project_unassigned))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { select(ProjectDestination.Create(name.trim())) }) {
                Text(stringResource(R.string.project_use_name))
            }
        },
        dismissButton = {
            TextButton(onClick = { open = false }) { Text(stringResource(R.string.assistant_cancel)) }
        }
    )
}
