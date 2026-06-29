package com.example.fitnessrecord.ui.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.AiDashboardData
import com.example.fitnessrecord.model.AttendancePoint

@Composable
fun AiDashboardCard(data: AiDashboardData) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("本月训练概览", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("训练天数", "${data.totalTrainingDays}天", Modifier.weight(1f))
                MetricTile("总时长", "${data.totalMinutes}分", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("动作数", "${data.totalActions}", Modifier.weight(1f))
                MetricTile("总组数", "${data.totalSets}", Modifier.weight(1f))
            }
            AttendanceBarChart(data.attendanceTrend)
            TypeBreakdownChart(data.typeBreakdown)
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AttendanceBarChart(points: List<AttendancePoint>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("周出勤趋势", style = MaterialTheme.typography.titleSmall)
        if (points.isEmpty()) {
            Text(
                text = "本月还没有训练记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        val maxCount = points.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            points.forEachIndexed { index, point ->
                val heightFraction = point.count.toFloat() / maxCount.toFloat()
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(point.count.toString(), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((92 * heightFraction).coerceAtLeast(8f).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = cleanWeekLabel(point.label, index),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBreakdownChart(points: List<AttendancePoint>) {
    val chartColors = chartColors()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("训练类型分布", style = MaterialTheme.typography.titleSmall)
        if (points.isEmpty()) {
            Text(
                text = "记录训练类型后会显示分布。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DonutChart(points = points, modifier = Modifier.size(92.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val total = points.sumOf { it.count }.coerceAtLeast(1)
                points.take(4).forEachIndexed { index, point ->
                    TypeLegendRow(
                        label = point.label.ifBlank { "未分类" },
                        count = point.count,
                        percent = point.count * 100 / total,
                        color = chartColors[index % chartColors.size]
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    points: List<AttendancePoint>,
    modifier: Modifier = Modifier,
) {
    val chartColors = chartColors()
    val total = points.sumOf { it.count }.coerceAtLeast(1)
    Canvas(modifier = modifier) {
        val strokeWidth = 16.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var startAngle = -90f
        points.forEachIndexed { index, point ->
            val sweep = point.count.toFloat() / total.toFloat() * 360f
            drawArc(
                color = chartColors[index % chartColors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun TypeLegendRow(
    label: String,
    count: Int,
    percent: Int,
    color: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "$count 次 · $percent%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun cleanWeekLabel(label: String, index: Int): String {
    return if (label.any { it.code in 0xE000..0xF8FF }) {
        "第${index + 1}周"
    } else {
        label.replace(" ", "")
    }
}

@Composable
private fun chartColors(): List<Color> {
    val colorScheme = MaterialTheme.colorScheme
    return listOf(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary,
        colorScheme.primaryContainer,
        colorScheme.secondaryContainer,
    )
}
