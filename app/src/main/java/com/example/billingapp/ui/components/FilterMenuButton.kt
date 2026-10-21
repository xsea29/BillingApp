package com.example.billingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.billingapp.R

/**
 * A filter icon that opens a drop-down list of [choices]. Grey normally; black while a choice is
 * [selected] (null = no filter), so you can see at a glance that the list is filtered.
 * Same height and corners as [SegmentedToggle], to sit beside it.
 */
@Composable
fun <T> FilterMenuButton(
    choices: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }

    Box(modifier = modifier) {

        FilterIconButton(
            active = selected != null,
            contentDescription = contentDescription,
            onClick = { open = true }
        )

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(label(choice)) },
                    trailingIcon = { if (choice == selected) Text("✓") },
                    onClick = {
                        open = false
                        onSelect(choice)
                    }
                )
            }
        }
    }
}

/**
 * The square filter icon on its own: grey normally, black while a filter is [active]. Same
 * height and corners as [SegmentedToggle]. Use it directly when the filter isn't a drop-down
 * (a date picker, say).
 */
@Composable
fun FilterIconButton(
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) colors.primary else colors.surfaceVariant)
            .clickable(onClickLabel = contentDescription, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_filter),
            contentDescription = contentDescription,
            tint = if (active) colors.onPrimary else colors.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}
