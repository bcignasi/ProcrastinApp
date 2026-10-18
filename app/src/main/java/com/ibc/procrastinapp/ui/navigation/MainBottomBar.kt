package com.ibc.procrastinapp.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ibc.procrastinapp.R

@Composable
fun MainBottomBar(selectedRoute: String, onSelect: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomBarItem(
                    label = stringResource(R.string.navigation_agenda),
                    icon = R.drawable.ic_agenda_outline,
                    selected = selectedRoute == AGENDA_ROUTE,
                    onClick = { onSelect(AGENDA_ROUTE) },
                    modifier = Modifier.weight(1f)
                )
                BottomBarItem(
                    label = stringResource(R.string.navigation_assistant),
                    icon = R.drawable.ic_assistant_outline,
                    selected = selectedRoute == ASSISTANT_ROUTE,
                    onClick = { onSelect(ASSISTANT_ROUTE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val contentColor = if (selected) colors.primary else colors.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.primaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
