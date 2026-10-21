package com.example.billingapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Small-caps heading that starts a block of a screen, with an optional note on the right. */
@Composable
fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SmallCaps(text = text, color = MaterialTheme.colorScheme.onSurface)
        if (trailing != null) {
            SmallCaps(text = trailing)
        }
    }
}

/** A small label over a value ("BILLS / 14"); put several in a Row with weights. */
@Composable
fun MiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SmallCaps(text = label)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
    }
}
