package com.example.billingapp.ui.sales

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.theme.BarMuted
import com.example.billingapp.util.formatRupees
import com.example.billingapp.util.formatRupeesCompact

/**
 * Columns for sales over time. The selected bar is dark, the rest soft grey; tap or
 * drag across the chart to move the selection (the caller shows the selected value).
 * Bars are thin with rounded tops, one hairline marks the top of the scale.
 */
@Composable
fun SalesBarChart(
    buckets: List<Bucket>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (buckets.isEmpty()) return

    val colors = MaterialTheme.colorScheme
    val count = buckets.size
    val top = niceCeiling(buckets.maxOf { it.amount })
    val currentOnSelect by rememberUpdatedState(onSelect)

    fun indexAt(x: Float, width: Int): Int =
        ((x / width) * count).toInt().coerceIn(0, count - 1)

    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            text = formatRupeesCompact(top),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )

        HorizontalDivider()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .pointerInput(count) {
                    detectTapGestures { offset ->
                        currentOnSelect(indexAt(offset.x, size.width))
                    }
                }
                .pointerInput(count) {
                    detectHorizontalDragGestures { change, _ ->
                        currentOnSelect(indexAt(change.position.x, size.width))
                    }
                }
        ) {
            // Never wider than 24dp: with few bars the leftover becomes air, not fat blocks.
            val barWidth = minOf(maxWidth / count * 0.62f, 24.dp)

            Row(modifier = Modifier.fillMaxSize()) {
                buckets.forEachIndexed { index, bucket ->
                    val fraction = (bucket.amount / top).toFloat().coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics {
                                contentDescription = "${bucket.caption}, ${formatRupees(bucket.amount)}"
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (index == selectedIndex) colors.primary else BarMuted)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            buckets.forEach { bucket ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (bucket.showLabel) {
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            // A label can be wider than its thin column; let it spill evenly.
                            modifier = Modifier.wrapContentWidth(unbounded = true)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ring made of one arc per slice (the caller picks the colours), with a small gap between
 * arcs and a value in the middle. Pair it with a legend: colour alone never names a slice.
 */
@Composable
fun CategoryDonut(
    slices: List<CategorySales>,
    sliceColors: List<Color>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(176.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total = slices.sumOf { it.amount }
            if (total <= 0.0) return@Canvas

            val strokeWidth = 24.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val gap = if (slices.size > 1) 2f else 0f

            var start = -90f

            slices.forEachIndexed { index, slice ->
                val sweep = (slice.amount / total * 360.0).toFloat()

                drawArc(
                    color = sliceColors[index],
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth)
                )

                start += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.displaySmall
            )
            SmallCaps(text = centerLabel)
        }
    }
}
