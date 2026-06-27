package com.example.fitnessrecord.ui.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiDashboardData
import com.example.fitnessrecord.model.AiTokenUsage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdviceRoute(
    innerPadding: PaddingValues,
    viewModel: AiAdviceViewModel,
    settingsViewModel: AiSettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        Scaffold(
            modifier = Modifier.padding(innerPadding),
            topBar = {
                TopAppBar(
                    title = { Text("AI 设置") },
                    navigationIcon = {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { settingsPadding ->
            AiProviderSettingsScreen(
                innerPadding = settingsPadding,
                config = settingsState.draft,
                themeColorKey = settingsState.themeColorKey,
                isTestingConnection = settingsState.isTestingConnection,
                testMessage = settingsState.testMessage,
                onProviderChange = settingsViewModel::updateProvider,
                onBaseUrlChange = settingsViewModel::updateBaseUrl,
                onApiKeyChange = settingsViewModel::updateApiKey,
                onModelChange = settingsViewModel::updateModel,
                onThemeColorChange = settingsViewModel::saveThemeColor,
                onTestConnection = settingsViewModel::testConnection,
                onSave = settingsViewModel::save,
                onClear = settingsViewModel::clear
            )
        }
    } else {
        Scaffold(
            modifier = Modifier.padding(innerPadding),
            topBar = {
                TopAppBar(
                    title = { Text("AI 健身建议") },
                    actions = {
                        IconButton(
                            onClick = { showSettings = true },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = "AI 厂商设置")
                        }
                        IconButton(
                            onClick = viewModel::refresh,
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Outlined.Refresh, contentDescription = "重新生成")
                            }
                        }
                    }
                )
            }
        ) { contentPadding ->
            val errorMessage = uiState.errorMessage
            val advice = uiState.advice
            when {
                uiState.isLoading -> LoadingAdvice(
                    innerPadding = contentPadding,
                    message = uiState.loadingMessage,
                    progress = uiState.progress,
                    remainingSeconds = uiState.remainingSeconds
                )
                errorMessage != null -> ErrorAdvice(contentPadding, errorMessage, uiState.dashboardData, viewModel::refresh)
                advice != null -> AiAdviceContent(contentPadding, advice, uiState.dashboardData, uiState.tokenUsage)
            }
        }
    }
}

@Composable
private fun LoadingAdvice(
    innerPadding: PaddingValues,
    message: String,
    progress: Float,
    remainingSeconds: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "预计还需要 ${remainingSeconds.coerceAtLeast(0)} 秒",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
        )
    }
}

@Composable
private fun ErrorAdvice(
    innerPadding: PaddingValues,
    message: String,
    dashboardData: AiDashboardData?,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        dashboardData?.let { item { AiDashboardCard(it) } }
        item {
            Text("生成失败", style = MaterialTheme.typography.titleLarge)
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun AiAdviceContent(
    innerPadding: PaddingValues,
    advice: AiAdvice,
    dashboardData: AiDashboardData?,
    tokenUsage: AiTokenUsage?,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        dashboardData?.let { item { AiDashboardCard(it) } }
        item { TokenUsageCard(tokenUsage) }
        item { AdviceCard(Icons.Outlined.Summarize, "本月总结", advice.summary) }
        item { AdviceCard(Icons.AutoMirrored.Outlined.TrendingUp, "训练频率分析", advice.frequencyAnalysis) }
        item {
            ListAdviceCard(
                icon = Icons.Outlined.FavoriteBorder,
                title = "恢复建议",
                items = advice.recoveryAdvice
            )
        }
        item {
            ListAdviceCard(
                icon = Icons.Outlined.AutoAwesome,
                title = "下周计划",
                items = advice.nextWeekPlan.map { "${it.day}: ${it.suggestion}" }
            )
        }
        item {
            ListAdviceCard(
                icon = Icons.Outlined.ReportProblem,
                title = "风险提醒",
                items = advice.riskWarnings,
                alert = true
            )
        }
        item { AdviceCard(Icons.Outlined.AutoAwesome, "鼓励", advice.motivation) }
    }
}

@Composable
private fun TokenUsageCard(tokenUsage: AiTokenUsage?) {
    val prompt = tokenUsage?.promptTokens?.toString() ?: "未返回"
    val completion = tokenUsage?.completionTokens?.toString() ?: "未返回"
    val total = tokenUsage?.totalTokens?.toString() ?: "未返回"
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AI 消耗", style = MaterialTheme.typography.titleMedium)
            Text("输入 tokens：$prompt", style = MaterialTheme.typography.bodyMedium)
            Text("输出 tokens：$completion", style = MaterialTheme.typography.bodyMedium)
            Text("总 tokens：$total", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AdviceCard(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ListAdviceCard(
    icon: ImageVector,
    title: String,
    items: List<String>,
    alert: Boolean = false,
) {
    val containerColor = if (alert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    val iconColor = if (alert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = iconColor)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            items.forEachIndexed { index, item ->
                Text(
                    text = "${index + 1}. $item",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
