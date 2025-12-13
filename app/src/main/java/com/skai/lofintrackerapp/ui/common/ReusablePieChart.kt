// In ...ui.common/ReusablePieChart.kt
package com.skai.lofintrackerapp.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background // <-- ADDED THIS
import androidx.compose.foundation.layout.* // <-- CHANGED TO WILDCARD to include all layout modifiers
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random

@Composable
fun ReusablePieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val total = remember(data) { data.values.sum() }

    // Generate stable random colors
    val colors = remember(data.keys) {
        data.keys.map {
            Color(
                Random.nextInt(256),
                Random.nextInt(256),
                Random.nextInt(256)
            )
        }
    }

    val strokeWidth = with(LocalDensity.current) { 60.dp.toPx() }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.height(250.dp), // <-- This will now work
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                var startAngle = -90f

                data.values.forEachIndexed { index, value ->
                    if (value > 0) {
                        val sweepAngle = (value / total).toFloat() * 360f
                        drawArc(
                            color = colors[index],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = center - Offset(radius, radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweepAngle
                    }
                }
            }
            Text(
                text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // --- Legend ---
        Column(
            modifier = Modifier
                .fillMaxWidth() // <-- This will now work
                .padding(top = 16.dp)
        ) {
            data.keys.forEachIndexed { index, category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .padding(4.dp)
                            .then(
                                Modifier.background( // <-- This will now work
                                    color = colors[index],
                                    shape = MaterialTheme.shapes.small
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}