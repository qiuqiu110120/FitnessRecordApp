package com.example.fitnessrecord.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.AiTokenUsage

@Composable
fun AiProviderSettingsScreen(
    innerPadding: PaddingValues,
    config: AiProviderConfig,
    isTestingConnection: Boolean,
    testMessage: AiConnectionTestMessage?,
    tokenUsage: AiTokenUsage?,
    onProviderChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AiModelSettingsCard(
                config = config,
                onProviderChange = onProviderChange,
                onBaseUrlChange = onBaseUrlChange,
                onApiKeyChange = onApiKeyChange,
                onModelChange = onModelChange
            )
        }
        item { TokenUsageSettingsCard(tokenUsage) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onSave,
                    enabled = !isTestingConnection
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onClear,
                    enabled = !isTestingConnection
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("清除")
                }
            }
        }
        item {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onTestConnection,
                enabled = !isTestingConnection
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Outlined.WifiTethering, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isTestingConnection) "正在测试连接" else "测试连通性")
            }
        }
        val message = testMessage
        if (message != null) {
            item { ConnectionTestMessageCard(message) }
        }
    }
}

@Composable
fun AiModelSettingsCard(
    config: AiProviderConfig,
    onProviderChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("大模型厂商配置", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "填写 OpenAI 兼容接口后，AI 建议会使用真实大模型生成。留空或厂商填写 Mock 时使用本地模拟建议。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = config.provider,
                onValueChange = onProviderChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("厂商") },
                placeholder = { Text("OpenAI / DeepSeek / 通义千问 / Mock") },
                singleLine = true
            )
            OutlinedTextField(
                value = config.baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
                placeholder = { Text("https://api.example.com/v1") },
                singleLine = true
            )
            OutlinedTextField(
                value = config.model,
                onValueChange = onModelChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("模型名称") },
                placeholder = { Text("gpt-4.1-mini / deepseek-chat") },
                singleLine = true
            )
            OutlinedTextField(
                value = config.apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }
    }
}

@Composable
fun TokenUsageSettingsCard(tokenUsage: AiTokenUsage?) {
    val prompt = tokenUsage?.promptTokens?.toString() ?: "未返回"
    val completion = tokenUsage?.completionTokens?.toString() ?: "未返回"
    val total = tokenUsage?.totalTokens?.toString() ?: "未返回"
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("上次 AI 消耗", style = MaterialTheme.typography.titleMedium)
            Text("输入 tokens：$prompt", style = MaterialTheme.typography.bodyMedium)
            Text("输出 tokens：$completion", style = MaterialTheme.typography.bodyMedium)
            Text("总 tokens：$total", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ConnectionTestMessageCard(message: AiConnectionTestMessage) {
    val colors = if (message.isSuccess) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    }
    val textColor = if (message.isSuccess) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Card(colors = colors) {
        Text(
            text = message.text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}
