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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.fitnessrecord.AppVersion
import com.example.fitnessrecord.data.importer.quickImportAiPrompt
import com.example.fitnessrecord.data.importer.quickImportExampleJson
import com.example.fitnessrecord.data.repository.AppRelease
import com.example.fitnessrecord.model.AiAdvicePromptConfig
import com.example.fitnessrecord.model.AiAdvicePromptPreset
import com.example.fitnessrecord.model.AiProviderConfig
import com.example.fitnessrecord.model.AiTokenUsage
import com.example.fitnessrecord.model.MAX_CUSTOM_AI_ADVICE_PROMPT_CHARS
import com.example.fitnessrecord.model.QuickImportPreview
import com.example.fitnessrecord.model.QuickImportResult
import com.example.fitnessrecord.model.QuickImportActionMatch
import com.example.fitnessrecord.ui.ai.AiConnectionTestMessage
import com.example.fitnessrecord.ui.ai.ConnectionTestMessageCard
import com.example.fitnessrecord.ui.ai.TokenUsageSettingsCard
import com.example.fitnessrecord.ui.home.QuickImportUiState
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
    Export("数据管理"),
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
    promptConfig: AiAdvicePromptConfig,
    promptMessage: AiConnectionTestMessage?,
    tokenUsage: AiTokenUsage?,
    runtimeLogText: String,
    hasPreviousCrash: Boolean,
    hasUnsavedAiConfig: Boolean,
    quickImportState: QuickImportUiState,
    onDismissPreviousCrash: () -> Unit,
    onThemeColorChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onExportData: () -> Unit,
    onPickImportFile: () -> Unit,
    onConfirmQuickImport: () -> Unit,
    onOrganizeImportedActions: () -> Unit,
    onClearQuickImportState: () -> Unit,
    onRefreshLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
    onProviderChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onPromptPresetChange: (AiAdvicePromptPreset) -> Unit,
    onUseCustomPromptChange: (Boolean) -> Unit,
    onCustomPromptChange: (String) -> Unit,
    onRestoreDefaultPrompt: () -> Unit,
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
                promptConfig = promptConfig,
                promptMessage = promptMessage,
                tokenUsage = tokenUsage,
                onProviderChange = onProviderChange,
                hasUnsavedAiConfig = hasUnsavedAiConfig,
                onBaseUrlChange = onBaseUrlChange,
                onApiKeyChange = onApiKeyChange,
                onModelChange = onModelChange,
                onPromptPresetChange = onPromptPresetChange,
                onUseCustomPromptChange = onUseCustomPromptChange,
                onCustomPromptChange = onCustomPromptChange,
                onRestoreDefaultPrompt = onRestoreDefaultPrompt,
                onTestConnection = onTestConnection,
                onSave = onSave,
                onClear = onClear
            )

            SettingsSection.Export -> ExportDataScreen(
                innerPadding = contentPadding,
                quickImportState = quickImportState,
                onExportData = onExportData,
                onPickImportFile = onPickImportFile,
                onConfirmQuickImport = onConfirmQuickImport,
                onOrganizeImportedActions = onOrganizeImportedActions,
                onClearQuickImportState = onClearQuickImportState
            )
            SettingsSection.Version -> VersionInfoScreen(
                innerPadding = contentPadding,
                updateCheckState = updateCheckState,
                availableUpdate = availableUpdate,
                logText = runtimeLogText,
                hasPreviousCrash = hasPreviousCrash,
                onDismissPreviousCrash = onDismissPreviousCrash,
                onCheckUpdates = onCheckUpdates,
                onRefreshLogs = onRefreshLogs,
                onClearLogs = onClearLogs,
                onExportLogs = onExportLogs
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
                title = "数据管理",
                subtitle = "导出备份、导入 JSON、查看示例",
                icon = Icons.Outlined.FileDownload,
                onClick = onOpenExport
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
    quickImportState: QuickImportUiState,
    onExportData: () -> Unit,
    onPickImportFile: () -> Unit,
    onConfirmQuickImport: () -> Unit,
    onOrganizeImportedActions: () -> Unit,
    onClearQuickImportState: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val exampleJson = quickImportExampleJson()
    val aiPrompt = quickImportAiPrompt()
    var showExample by rememberSaveable { mutableStateOf(false) }
    var showPrompt by rememberSaveable { mutableStateOf(false) }

    QuickImportStateDialogs(
        state = quickImportState,
        onConfirmQuickImport = onConfirmQuickImport,
        onOrganizeImportedActions = onOrganizeImportedActions,
        onClearQuickImportState = onClearQuickImportState
    )

    if (showExample) {
        TextContentDialog(
            title = "导入示例 JSON",
            text = exampleJson,
            onCopy = { clipboard.setText(AnnotatedString(exampleJson)) },
            onDismiss = { showExample = false }
        )
    }
    if (showPrompt) {
        TextContentDialog(
            title = "AI 整理提示词",
            text = aiPrompt,
            onCopy = { clipboard.setText(AnnotatedString(aiPrompt)) },
            onDismiss = { showPrompt = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DataManagementCard(
                title = "导出数据",
                description = "导出为完整 JSON 文件，适合备份当前训练记录。导入前建议先导出一份备份。",
                primaryButtonText = "导出 JSON",
                primaryIcon = Icons.Outlined.FileDownload,
                onPrimaryClick = onExportData
            )
        }
        item {
            DataManagementCard(
                title = "导入 JSON",
                description = "仅支持快速导入格式。导入会追加训练记录，不覆盖、不删除、不自动合并。文件超过 5MB 时请拆分后导入。",
                primaryButtonText = "选择 JSON 文件",
                primaryIcon = Icons.Outlined.FileUpload,
                onPrimaryClick = onPickImportFile,
                isLoading = quickImportState.isLoading
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("导入示例", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "示例严格符合第一版校验规则。sets 中每一组至少需要 reps、durationSeconds、distanceKm 之一，weightKg 不能单独作为有效训练数据。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FieldDescriptionText()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { showExample = true }
                        ) {
                            Icon(Icons.Outlined.Article, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("查看示例")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { clipboard.setText(AnnotatedString(exampleJson)) }
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("复制示例")
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("AI 整理提示词", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "复制提示词给 AI，再把你的杂乱训练记录粘贴到最后。AI 输出的 JSON 仍会经过校验、预览和确认。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { showPrompt = true }
                        ) {
                            Icon(Icons.Outlined.Article, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("查看提示词")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { clipboard.setText(AnnotatedString(aiPrompt)) }
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("复制提示词")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataManagementCard(
    title: String,
    description: String,
    primaryButtonText: String,
    primaryIcon: ImageVector,
    onPrimaryClick: () -> Unit,
    isLoading: Boolean = false,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onPrimaryClick,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(primaryIcon, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isLoading) "正在处理" else primaryButtonText)
            }
        }
    }
}

@Composable
private fun FieldDescriptionText() {
    Text(
        text = """
date：训练日期，格式 YYYY-MM-DD，必须是真实日期，必填
title：训练标题，可选
notes：备注，可选
name：动作名称，必填
sets：训练组，必填且非空
weightKg：重量，单位 kg，可选，允许 0 或正数
reps：次数，可选，必须是正整数
durationSeconds：时长，单位秒，可选，必须是正数
distanceKm：距离，单位公里，可选，必须是正数
        """.trimIndent(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun QuickImportStateDialogs(
    state: QuickImportUiState,
    onConfirmQuickImport: () -> Unit,
    onOrganizeImportedActions: () -> Unit,
    onClearQuickImportState: () -> Unit,
) {
    val errors = state.errors
    if (errors.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onClearQuickImportState,
            title = { Text("导入文件有错误") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    errors.take(8).forEach { Text(it) }
                    if (errors.size > 8) {
                        Text("还有 ${errors.size - 8} 条错误，请修正后再导入。")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onClearQuickImportState) {
                    Text("知道了")
                }
            }
        )
    }

    val preview = state.preview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = onClearQuickImportState,
            title = { Text("确认导入") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(preview.summaryText())
                    ImportActionMatchSection(
                        title = "将新增到动作库",
                        actions = preview.newActions,
                        emptyText = "没有需要新增的动作"
                    )
                    ImportActionMatchSection(
                        title = "已匹配已有动作",
                        actions = preview.matchedActions,
                        emptyText = "没有匹配到已有动作"
                    )
                    ImportActionMatchSection(
                        title = "需要手动整理",
                        actions = preview.ambiguousActions,
                        emptyText = "没有需要手动整理的动作"
                    )
                    Text("导入会追加训练记录，建议导入前先导出当前数据备份。")
                    preview.warnings.forEach { warning ->
                        Text("提示：$warning", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmQuickImport,
                    enabled = !state.isLoading
                ) {
                    Text(if (state.isLoading) "导入中" else "确认导入")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onClearQuickImportState,
                    enabled = !state.isLoading
                ) {
                    Text("取消")
                }
            }
        )
    }

    val result = state.result
    if (result != null) {
        AlertDialog(
            onDismissRequest = onClearQuickImportState,
            title = { Text("导入成功") },
            text = { Text(result.summaryText()) },
            confirmButton = {
                TextButton(onClick = onClearQuickImportState) {
                    Text("完成")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onClearQuickImportState()
                        onOrganizeImportedActions()
                    }
                ) {
                    Text("去整理新增动作")
                }
            }
        )
    }
}

@Composable
private fun ImportActionMatchSection(
    title: String,
    actions: List<QuickImportActionMatch>,
    emptyText: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        if (actions.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        actions.take(10).forEach { action ->
            val target = action.folderName?.let { " -> $it / ${action.name}" }.orEmpty()
            Text(
                text = "- ${action.name}$target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (actions.size > 10) {
            Text("还有 ${actions.size - 10} 个", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TextContentDialog(
    title: String,
    text: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text("复制")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun QuickImportPreview.summaryText(): String = buildString {
    appendLine("将新增训练 $workoutCount 次")
    appendLine("将新建动作 $newActionCount 个，保存到：未分类")
    appendLine("已匹配已有动作 $matchedActionCount 个")
    appendLine("需要手动整理 $ambiguousActionCount 个")
    appendLine("将新增训练组 $setCount 组")
    append("发现同日期训练记录 $existingDateCount 条")
}

private fun QuickImportResult.summaryText(): String = buildString {
    appendLine("新增训练 $workoutCount 次")
    appendLine("已新建动作 $newActionCount 个，保存到：未分类")
    appendLine("已匹配已有动作 $matchedActionCount 个")
    appendLine("需要手动整理 $ambiguousActionCount 个")
    appendLine("新增训练组 $setCount 组")
    append("其中包含同日期训练记录 $existingDateCount 条")
}

@Composable
private fun RuntimeLogCard(
    logText: String,
    hasPreviousCrash: Boolean,
    onDismissPreviousCrash: () -> Unit,
    onRefreshLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onRefreshLogs()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("运行日志", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "这里保存 App 启动、AI 请求、导出等关键运行信息。日志可能包含设备信息、错误信息和部分接口响应片段，请确认后再分享。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasPreviousCrash) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "检测到上次异常退出。建议先导出日志，再清空或继续排查。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = onDismissPreviousCrash) {
                                Text("已知晓")
                            }
                        }
                    }
                }
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
    promptConfig: AiAdvicePromptConfig,
    promptMessage: AiConnectionTestMessage?,
    tokenUsage: AiTokenUsage?,
    onProviderChange: (String) -> Unit,
    hasUnsavedAiConfig: Boolean,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onPromptPresetChange: (AiAdvicePromptPreset) -> Unit,
    onUseCustomPromptChange: (Boolean) -> Unit,
    onCustomPromptChange: (String) -> Unit,
    onRestoreDefaultPrompt: () -> Unit,
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
        item {
            AiAdvicePromptSettingsCard(
                config = promptConfig,
                message = promptMessage,
                onPresetChange = onPromptPresetChange,
                onUseCustomPromptChange = onUseCustomPromptChange,
                onCustomPromptChange = onCustomPromptChange,
                onRestoreDefaultPrompt = onRestoreDefaultPrompt
            )
        }
        if (hasUnsavedAiConfig) {
            item { UnsavedAiConfigCard() }
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
private fun AiAdvicePromptSettingsCard(
    config: AiAdvicePromptConfig,
    message: AiConnectionTestMessage?,
    onPresetChange: (AiAdvicePromptPreset) -> Unit,
    onUseCustomPromptChange: (Boolean) -> Unit,
    onCustomPromptChange: (String) -> Unit,
    onRestoreDefaultPrompt: () -> Unit,
) {
    var showRestoreConfirm by rememberSaveable { mutableStateOf(false) }
    val selectedPreset = config.selectedPreset
    val trimmedCustomLength = config.customPrompt.trim().length
    val customPromptTooLong = trimmedCustomLength > MAX_CUSTOM_AI_ADVICE_PROMPT_CHARS

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("恢复默认提示词") },
            text = { Text("将清空自定义提示词、关闭自定义开关，并切回通用训练建议。是否继续？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        onRestoreDefaultPrompt()
                    }
                ) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AI 建议提示词", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "第一版作为全局配置，所有 AI 训练建议都会使用这里的建议偏好。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "系统会自动加入基础安全约束，确保建议基于训练记录生成，不做医疗诊断。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "预设提示词：${selectedPreset.label}",
                style = MaterialTheme.typography.bodyMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AiAdvicePromptPreset.entries, key = { it.key }) { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { onPresetChange(preset) },
                        enabled = !config.useCustomPrompt,
                        label = { Text(preset.label) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("使用自定义提示词", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "自定义只影响建议偏好，不会覆盖系统安全约束。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.useCustomPrompt,
                    onCheckedChange = onUseCustomPromptChange
                )
            }
            if (config.useCustomPrompt) {
                Text(
                    text = "自定义为空时将使用：${selectedPreset.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = config.customPrompt,
                    onValueChange = onCustomPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("建议偏好提示词") },
                    placeholder = { Text(selectedPreset.prompt) },
                    minLines = 5,
                    maxLines = 8,
                    isError = customPromptTooLong,
                    supportingText = {
                        Text(
                            if (customPromptTooLong) {
                                "保存前去除前后空格后为 $trimmedCustomLength/$MAX_CUSTOM_AI_ADVICE_PROMPT_CHARS 字，请缩短内容。"
                            } else {
                                "$trimmedCustomLength/$MAX_CUSTOM_AI_ADVICE_PROMPT_CHARS 字，保存时会自动去除前后空格。"
                            }
                        )
                    }
                )
            }
            Text(
                text = "生成 AI 建议时，系统会将必要的训练记录发送给你配置的大模型服务，请确认你信任该服务及其隐私政策。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showRestoreConfirm = true }
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("恢复默认提示词")
            }
            if (message != null) {
                ConnectionTestMessageCard(message)
            }
        }
    }
}

@Composable
private fun UnsavedAiConfigCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AI 配置尚未保存",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "你修改了大模型或提示词配置，但 AI 建议仍会使用上一次保存的配置。请点击保存；测试连通性通过后只会自动保存大模型连接配置。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
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
    logText: String,
    hasPreviousCrash: Boolean,
    onDismissPreviousCrash: () -> Unit,
    onCheckUpdates: () -> Unit,
    onRefreshLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
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
        item {
            RuntimeLogCard(
                logText = logText,
                hasPreviousCrash = hasPreviousCrash,
                onDismissPreviousCrash = onDismissPreviousCrash,
                onRefreshLogs = onRefreshLogs,
                onClearLogs = onClearLogs,
                onExportLogs = onExportLogs
            )
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








