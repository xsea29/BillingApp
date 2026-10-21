package com.example.billingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.billingapp.util.Page

/**
 * "11–20 of 47" with ← Page 2 of 5 → under it. Shows nothing when everything fits on one page.
 * [onPageChange] gets the new page number.
 */
@Composable
fun PaginationBar(
    page: Page<*>,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (page.pageCount <= 1) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallCaps(text = "${page.from}–${page.to} of ${page.total}")

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageButton(
                glyph = "←",
                description = "Previous page",
                enabled = page.hasPrevious,
                onClick = { onPageChange(page.page - 1) }
            )

            Text(
                text = "Page ${page.page} of ${page.pageCount}",
                style = MaterialTheme.typography.titleMedium
            )

            PageButton(
                glyph = "→",
                description = "Next page",
                enabled = page.hasNext,
                onClick = { onPageChange(page.page + 1) }
            )
        }
    }
}

/** Round arrow button; black when it can be used, pale grey when there is nowhere to go. */
@Composable
private fun PageButton(
    glyph: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.primary else colors.surfaceVariant)
            .clickable(
                enabled = enabled,
                onClickLabel = description,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) colors.onPrimary else colors.outline
        )
    }
}
