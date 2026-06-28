package com.example.fitnessrecord.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.WorkoutAction
import com.example.fitnessrecord.model.WorkoutDay

@Composable
fun ActionVolumeChartCard(day: WorkoutDay) {
    val volumes = remember(day.actions) {
        day.actions
            .mapNotNull { action -> action.toVolumeItem() }
            .sortedByDescending { it.volume }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("动作容量", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "按选中日期统计每个动作的训练量",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val totalWeightedVolume = volumes
                    .filter { it.unit == ActionVolumeUnit.Kilogram }
                    .sumOf { it.volume }
                if (totalWeightedVolume > 0.0) {
                    Text(
                        text = totalWeightedVolume.formatVolume(ActionVolumeUnit.Kilogram),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (volumes.isEmpty()) {
                Text(
                    text = "为当天动作填写组数、次数和重量后，这里会显示容量对比。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                VolumeBars(volumes)
            }
        }
    }
}

@Composable
private fun VolumeBars(items: List<ActionVolumeItem>) {
    val maxVolume = items.maxOfOrNull { it.volume }?.coerceAtLeast(1.0) ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.volume.formatVolume(item.unit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((item.volume / maxVolume).toFloat().coerceIn(0.04f, 1f))
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${item.setCount} 组",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun WorkoutAction.toVolumeItem(): ActionVolumeItem? {
    val validSets = sets.filter { it.reps != null || it.weightKg != null }
    if (validSets.isEmpty()) return null

    val weightedVolume = validSets.sumOf { set ->
        val reps = set.reps ?: 0
        val weight = set.weightKg ?: 0.0
        reps * weight
    }
    val fallbackRepsVolume = validSets.sumOf { it.reps ?: 0 }.toDouble()
    val volume = if (weightedVolume > 0.0) weightedVolume else fallbackRepsVolume
    if (volume <= 0.0) return null

    return ActionVolumeItem(
        name = name.ifBlank { "未命名动作" },
        setCount = sets.size,
        volume = volume,
        unit = if (weightedVolume > 0.0) ActionVolumeUnit.Kilogram else ActionVolumeUnit.Repetition
    )
}

private fun Double.formatVolume(unit: ActionVolumeUnit): String {
    val value = if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        "%.1f".format(this)
    }
    return when (unit) {
        ActionVolumeUnit.Kilogram -> "$value kg"
        ActionVolumeUnit.Repetition -> "$value 次"
    }
}

private enum class ActionVolumeUnit {
    Kilogram,
    Repetition,
}

@Immutable
private data class ActionVolumeItem(
    val name: String,
    val setCount: Int,
    val volume: Double,
    val unit: ActionVolumeUnit,
)
