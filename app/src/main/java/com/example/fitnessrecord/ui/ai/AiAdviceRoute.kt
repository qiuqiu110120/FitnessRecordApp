package com.example.fitnessrecord.ui.ai

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
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ReportProblem
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.AiAdvice
import com.example.fitnessrecord.model.AiDashboardData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdviceRoute(
    innerPadding: PaddingValues,
    viewModel: AiAdviceViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.eventMessage) {
        val message = uiState.eventMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeEventMessage()
        }
    }

    Scaffold(
            modifier = Modifier.padding(innerPadding),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("AI 健身建议") },
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
                advice != null -> AiAdviceContent(contentPadding, advice, uiState.dashboardData, uiState.isLoading, viewModel::refresh)
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
        dashboardData?.let {
            item { AiDashboardCard(it) }
            item { GetAiAdviceButton(isLoading = false, onClick = onRetry) }
        }
        item {
            Text("生成失败", style = MaterialTheme.typography.titleLarge)
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AiAdviceContent(
    innerPadding: PaddingValues,
    advice: AiAdvice,
    dashboardData: AiDashboardData?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        dashboardData?.let {
            item { AiDashboardCard(it) }
            item { GetAiAdviceButton(isLoading = isLoading, onClick = onRefresh) }
        }
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
private fun GetAiAdviceButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Text("  ")
        }
        Text("获取AI建议")
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

