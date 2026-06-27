package com.example.fitnessrecord.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.CustomAction
import com.example.fitnessrecord.model.WorkoutAction
import com.example.fitnessrecord.model.WorkoutDay
import com.example.fitnessrecord.model.WorkoutSet

@Composable
fun WorkoutEditorScreen(
    innerPadding: PaddingValues,
    day: WorkoutDay,
    customActions: List<CustomAction>,
    onTrainingTypeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddAction: () -> Unit,
    onAddCustomAction: (String) -> Unit,
    onActionNameChange: (Long, String) -> Unit,
    onDeleteAction: (Long) -> Unit,
    onAddSet: (Long) -> Unit,
    onSetChange: (Long, Long, String, String) -> Unit,
    onDeleteSet: (Long, Long) -> Unit,
    onSave: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WorkoutMetaCard(
                day = day,
                onTrainingTypeChange = onTrainingTypeChange,
                onDurationChange = onDurationChange,
                onNotesChange = onNotesChange
            )
        }

        item {
            WorkoutActionHeader(onAddAction = onAddAction)
        }

        if (customActions.isNotEmpty()) {
            item {
                CustomActionShortcuts(
                    actions = customActions,
                    onAddCustomAction = onAddCustomAction
                )
            }
        }

        if (day.actions.isEmpty()) {
            item { EmptyWorkoutCard(onAddAction) }
        }

        items(day.actions, key = { it.id }) { action ->
            WorkoutActionCard(
                action = action,
                onNameChange = { onActionNameChange(action.id, it) },
                onAddSet = { onAddSet(action.id) },
                onSetChange = { set, reps, weight -> onSetChange(action.id, set.id, reps, weight) },
                onDeleteSet = { set -> onDeleteSet(action.id, set.id) },
                onDeleteAction = { onDeleteAction(action.id) }
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave
            ) {
                Text("保存训练记录")
            }
        }
    }
}

@Composable
private fun WorkoutMetaCard(
    day: WorkoutDay,
    onTrainingTypeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("训练概况", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = day.trainingType,
                onValueChange = onTrainingTypeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("训练类型") },
                singleLine = true
            )
            OutlinedTextField(
                value = day.durationMinutes?.toString().orEmpty(),
                onValueChange = onDurationChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("训练时长（分钟）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = day.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                minLines = 2
            )
        }
    }
}

@Composable
private fun WorkoutActionHeader(onAddAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "训练动作", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "动作、组数、次数和重量会优先保存到本地 Room。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onAddAction) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("动作")
        }
    }
}

@Composable
private fun CustomActionShortcuts(
    actions: List<CustomAction>,
    onAddCustomAction: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("常用动作", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(actions, key = { it.id }) { action ->
                OutlinedButton(onClick = { onAddCustomAction(action.name) }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(action.name)
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkoutCard(onAddAction: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "今天还没有记录", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "添加动作后，可以继续添加组数、次数和重量。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onAddAction) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加动作")
            }
        }
    }
}

@Composable
private fun WorkoutActionCard(
    action: WorkoutAction,
    onNameChange: (String) -> Unit,
    onAddSet: () -> Unit,
    onSetChange: (WorkoutSet, String, String) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    onDeleteAction: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = action.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("动作名称") },
                    singleLine = true
                )
                IconButton(onClick = onDeleteAction) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除动作")
                }
            }

            if (action.sets.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("组", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium)
                    Text("次数", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Text("重量 kg", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            action.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (index + 1).toString(),
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = set.reps?.toString().orEmpty(),
                        onValueChange = { onSetChange(set, it, set.weightKg?.toString().orEmpty()) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = set.weightKg?.toString().orEmpty(),
                        onValueChange = { onSetChange(set, set.reps?.toString().orEmpty(), it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    IconButton(onClick = { onDeleteSet(set) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "删除组")
                    }
                }
            }

            TextButton(onClick = onAddSet) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("添加一组")
            }
        }
    }
}
