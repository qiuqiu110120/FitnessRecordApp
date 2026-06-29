package com.example.fitnessrecord.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.AppVersion
import com.example.fitnessrecord.data.repository.AppRelease
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.AiTokenUsage
import com.example.fitnessrecord.ui.ai.AiConnectionTestMessage
import com.example.fitnessrecord.ui.ai.ConnectionTestMessageCard
import com.example.fitnessrecord.ui.ai.TokenUsageSettingsCard
import com.example.fitnessrecord.ui.theme.ThemeColorOptions
import com.example.fitnessrecord.ui.theme.isCustomThemeColor
import com.example.fitnessrecord.ui.theme.normalizeThemeColorInput
import com.example.fitnessrecord.ui.theme.themeSeedColor

private const val GITHUB_URL = "https://github.com/qiuqiu110120/FitnessRecordApp"
private const val RELEASES_URL = "https://github.com/qiuqiu110120/FitnessRecordApp/releases"

private enum class SettingsSection(val title: String) {
    Home("设置"),
    Theme("主题配色"),
    AiModel("大模型配置"),
    Export("数据导出"),
    Logs("运行日志"),
    Version("版本信息"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    innerPadding: PaddingValues,
    themeColorKey: String,
    updateCheckState: UpdateCheckState,
    availableUpdate: AppRelease?,
    config: AiProviderConfig,
    isTestingConnection: Boolean,
    testMessage: AiConnectionTestMessage?,
    tokenUsage: AiTokenUsage?,
    runtimeLogText: String,
    onThemeColorChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onExportData: () -> Unit,
    onRefreshLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
    onProviderChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    var section by rememberSaveable { mutableStateOf(SettingsSection.Home) }

    if (section != SettingsSection.Home) {
        BackHandler { section = SettingsSection.Home }
    }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                title = { Text(section.title) },
                navigationIcon = {
                    if (section != SettingsSection.Home) {
                        IconButton(onClick = { section = SettingsSection.Home }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { contentPadding ->
        when (section) {
            SettingsSection.Home -> SettingsHomeScreen(
                innerPadding = contentPadding,
                themeColorKey = themeColorKey,
                config = config,
                updateCheckState = updateCheckState,
                onOpenTheme = { section = SettingsSection.Theme },
                onOpenAiModel = { section = SettingsSection.AiModel },
                onOpenExport = { section = SettingsSection.Export },
                onOpenLogs = { section = SettingsSection.Logs },
                onOpenVersion = { section = SettingsSection.Version }
            )

            SettingsSection.Theme -> ThemeSettingsScreen(
                innerPadding = contentPadding,
                themeColorKey = themeColorKey,
                onThemeColorChange = onThemeColorChange
            )

            SettingsSection.AiModel -> AiModelSettingsScreen(
                innerPadding = contentPadding,
                config = config,
                isTestingConnection = isTestingConnection,
                testMessage = testMessage,
                tokenUsage = tokenUsage,
                onProviderChange = onProviderChange,
                onBaseUrlChange = onBaseUrlChange,
                onApiKeyChange = onApiKeyChange,
                onModelChange = onModelChange,
                onTestConnection = onTestConnection,
                onSave = onSave,
                onClear = onClear
            )

            SettingsSection.Export -> ExportDataScreen(
                innerPadding = contentPadding,
                onExportData = onExportData
            )

            SettingsSection.Logs -> RuntimeLogScreen(
                innerPadding = contentPadding,
                logText = runtimeLogText,
                onRefreshLogs = onRefreshLogs,
                onClearLogs = onClearLogs,
                onExportLogs = onExportLogs
            )

            SettingsSection.Version -> VersionInfoScreen(
                innerPadding = contentPadding,
                updateCheckState = updateCheckState,
                availableUpdate = availableUpdate,
                onCheckUpdates = onCheckUpdates
            )
        }
    }
}

@Composable
private fun SettingsHomeScreen(
    innerPadding: PaddingValues,
    themeColorKey: String,
    config: AiProviderConfig,
    updateCheckState: UpdateCheckState,
    onOpenTheme: () -> Unit,
    onOpenAiModel: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenVersion: () -> Unit,
) {
    val themeName = ThemeColorOptions.firstOrNull { it.key == themeColorKey }?.label ?: themeColorKey
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsEntryCard(
                title = "大模型配置",
                subtitle = "当前：${config.provider.ifBlank { "Mock" }}",
                icon = Icons.Outlined.WifiTethering,
                onClick = onOpenAiModel
            )
        }
        item {
            SettingsEntryCard(
                title = "主题配色",
                subtitle = "当前：$themeName",
                icon = Icons.Outlined.Settings,
                onClick = onOpenTheme
            )
        }
        item {
            SettingsEntryCard(
                title = "数据导出",
                subtitle = "导出训练记录 JSON 文件",
                icon = Icons.Outlined.FileDownload,
                onClick = onOpenExport
            )
        }
        item {
            SettingsEntryCard(
                title = "运行日志",
                subtitle = "查看、导出或清空错误信息",
                icon = Icons.Outlined.Article,
                onClick = onOpenLogs
            )
        }
        item {
            SettingsEntryCard(
                title = "版本信息",
                subtitle = versionEntrySubtitle(updateCheckState),
                icon = Icons.Outlined.Info,
                onClick = onOpenVersion
            )
        }
    }
}

@Composable
private fun SettingsEntryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            },
            leadingContent = {
                IconBadge(icon = icon)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp),
            colors = androidx.compose.material3.ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun ExportDataScreen(
    innerPadding: PaddingValues,
    onExportData: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("导出健身数据", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "导出为 JSON 文件，包含训练日期、类型、时长、备注、动作、组数、次数和重量。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onExportData
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("导出 JSON")
                    }
                }
            }
        }
    }
}

@Composable
private fun RuntimeLogScreen(
    innerPadding: PaddingValues,
    logText: String,
    onRefreshLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onRefreshLogs()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("运行日志", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "这里保存 App 启动、AI 请求、导出等关键运行信息。导出后可以把 txt 文件发给我定位问题。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onRefreshLogs
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("刷新")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onClearLogs
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("清空")
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onExportLogs
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("导出日志 TXT")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(
                    text = logText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ThemeSettingsScreen(
    innerPadding: PaddingValues,
    themeColorKey: String,
    onThemeColorChange: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ThemeSettingsCard(themeColorKey, onThemeColorChange) }
    }
}

@Composable
private fun AiModelSettingsScreen(
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
private fun ThemeSettingsCard(
    themeColorKey: String,
    onThemeColorChange: (String) -> Unit,
) {
    var customColorInput by rememberSaveable(themeColorKey) {
        mutableStateOf(if (isCustomThemeColor(themeColorKey)) themeColorKey else "")
    }
    var showCustomColorError by rememberSaveable { mutableStateOf(false) }
    val normalizedCustomColor = normalizeThemeColorInput(customColorInput)
    val activeSeedColor = themeSeedColor(themeColorKey)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("主题配色", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "选择后会立即应用到全局 Material 3 主题色。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(activeSeedColor)
                )
                Text(
                    text = "当前主题色：${
                        ThemeColorOptions.firstOrNull { it.key == themeColorKey }?.label ?: themeColorKey
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ThemeColorOptions, key = { it.key }) { option ->
                    FilterChip(
                        selected = themeColorKey == option.key,
                        onClick = {
                            showCustomColorError = false
                            onThemeColorChange(option.key)
                        },
                        leadingIcon = {
                            Spacer(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(option.color)
                            )
                        },
                        label = { Text(option.label) }
                    )
                }
            }
            OutlinedTextField(
                value = customColorInput,
                onValueChange = {
                    customColorInput = it
                    showCustomColorError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("自定义颜色") },
                placeholder = { Text("#2E7D32 / rgb(46,125,50)") },
                supportingText = {
                    Text(
                        if (showCustomColorError) {
                            "请输入有效颜色，例如 #1565C0、1565C0 或 rgb(21,101,192)"
                        } else {
                            "支持 HEX、ARGB HEX 和 RGB 数值。应用后全局主题会同步更新。"
                        }
                    )
                },
                isError = showCustomColorError,
                singleLine = true
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val normalized = normalizedCustomColor
                    if (normalized == null) {
                        showCustomColorError = true
                    } else {
                        showCustomColorError = false
                        onThemeColorChange(normalized)
                    }
                }
            ) {
                Text("应用自定义颜色")
            }
        }
    }
}

@Composable
private fun AiModelSettingsCard(
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
                placeholder = { Text("https://api.example.com/v1 或 http://127.0.0.1:8000/v1") },
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
private fun VersionInfoScreen(
    innerPadding: PaddingValues,
    updateCheckState: UpdateCheckState,
    availableUpdate: AppRelease?,
    onCheckUpdates: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("FRA", style = MaterialTheme.typography.titleLarge)
                    Text("当前版本：${AppVersion.NAME} (${AppVersion.CODE})")
                    Text("这是一个本地优先的健身记录 App，代码托管在 GitHub。")
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                        Text("检查更新", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(updateStatusText(updateCheckState, availableUpdate))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCheckUpdates,
                        enabled = updateCheckState != UpdateCheckState.Checking
                    ) {
                        if (updateCheckState == UpdateCheckState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (updateCheckState == UpdateCheckState.Checking) "正在检查" else "检查更新")
                    }
                    Text(RELEASES_URL, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Code, contentDescription = null)
                        Text("GitHub 开源信息", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("仓库地址：")
                    Text(GITHUB_URL, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun updateStatusText(
    state: UpdateCheckState,
    release: AppRelease?,
): String = when (state) {
    UpdateCheckState.Idle -> "点击按钮检查 GitHub Releases 是否有新版本。"
    UpdateCheckState.Checking -> "正在连接 GitHub 检查最新版本。"
    UpdateCheckState.UpToDate -> "当前已经是最新版本。"
    UpdateCheckState.UpdateAvailable -> {
        val version = release?.tagName ?: "新版本"
        "发现 $version，可在弹窗中选择立即更新或暂不更新。"
    }
    is UpdateCheckState.Failed -> state.message
}

private fun versionEntrySubtitle(state: UpdateCheckState): String = when (state) {
    UpdateCheckState.Checking -> "正在检查更新"
    UpdateCheckState.UpdateAvailable -> "发现新版本"
    UpdateCheckState.UpToDate -> "当前版本 ${AppVersion.NAME}"
    is UpdateCheckState.Failed -> "检查更新失败"
    UpdateCheckState.Idle -> "当前版本 ${AppVersion.NAME}"
}
