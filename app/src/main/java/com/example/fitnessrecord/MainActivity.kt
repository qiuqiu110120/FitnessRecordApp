package com.example.fitnessrecord

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessrecord.ui.ai.AiAdviceRoute
import com.example.fitnessrecord.ui.ai.AiAdviceViewModel
import com.example.fitnessrecord.ui.ai.AiSettingsViewModel
import com.example.fitnessrecord.ui.home.HomeRoute
import com.example.fitnessrecord.ui.home.HomeViewModel
import com.example.fitnessrecord.ui.settings.AppSettingsViewModel
import com.example.fitnessrecord.ui.settings.SettingsRoute
import com.example.fitnessrecord.ui.settings.UpdateCheckState
import com.example.fitnessrecord.ui.theme.FitnessRecordTheme
import com.example.fitnessrecord.util.AppLogger
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as FitnessRecordApplication
            val settingsViewModel: AppSettingsViewModel = viewModel(
                factory = app.appContainer.appSettingsViewModelFactory
            )
            val settingsState by settingsViewModel.uiState.collectAsState()
            FitnessRecordTheme(themeColorKey = settingsState.resolvedThemeColorKey) {
                if (settingsState.isThemeLoaded) {
                    FitnessRecordApp(
                        appContainer = app.appContainer,
                        appSettingsViewModel = settingsViewModel
                    )
                } else {
                    Surface(modifier = Modifier) {}
                }
            }
        }
    }
}

private enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    Home("首页", Icons.Outlined.CalendarMonth),
    AiAdvice("AI建议", Icons.Outlined.AutoAwesome),
    Settings("设置", Icons.Outlined.Settings),
}

@Composable
private fun FitnessRecordApp(
    appContainer: AppContainer,
    appSettingsViewModel: AppSettingsViewModel,
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    val context = LocalContext.current
    val activity = context as? Activity
    var lastBackPressedAt by remember { mutableStateOf(0L) }
    val appSettingsState by appSettingsViewModel.uiState.collectAsState()

    val homeViewModel: HomeViewModel = viewModel(factory = appContainer.homeViewModelFactory)
    val aiAdviceViewModel: AiAdviceViewModel = viewModel(factory = appContainer.aiAdviceViewModelFactory)
    val aiSettingsViewModel: AiSettingsViewModel = viewModel(factory = appContainer.aiSettingsViewModelFactory)
    val scope = rememberCoroutineScope()
    var runtimeLogText by remember { mutableStateOf("正在读取日志...") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val json = homeViewModel.exportWorkoutDataJson()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(StandardCharsets.UTF_8))
                }
            }.onSuccess {
                AppLogger.i("Export", "Workout data exported")
                Toast.makeText(context, "健身数据已导出", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                AppLogger.e("Export", "Workout data export failed", error)
                Toast.makeText(context, error.message ?: "导出失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val logExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val logText = AppLogger.read()
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(logText.toByteArray(StandardCharsets.UTF_8))
                }
                AppLogger.i("Export", "Runtime log exported")
            }.onSuccess {
                Toast.makeText(context, "运行日志已导出", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                AppLogger.e("Export", "Runtime log export failed", error)
                Toast.makeText(context, error.message ?: "导出日志失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressedAt <= BACK_EXIT_INTERVAL_MS) {
            activity?.finish()
        } else {
            lastBackPressedAt = now
            Toast.makeText(context, "再按一次返回桌面", Toast.LENGTH_SHORT).show()
        }
    }

    val release = appSettingsState.availableUpdate
    if (appSettingsState.updateCheckState == UpdateCheckState.UpdateAvailable && release != null) {
        AlertDialog(
            onDismissRequest = appSettingsViewModel::dismissUpdate,
            title = { Text("发现新版本") },
            text = {
                Text(
                    text = "检测到 ${release.name}（${release.tagName}），建议更新后使用最新功能和修复。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appSettingsViewModel.dismissUpdate()
                        val updateUrl = release.apkUrl ?: release.pageUrl
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                    }
                ) {
                    Text("立即更新")
                }
            },
            dismissButton = {
                TextButton(onClick = appSettingsViewModel::dismissUpdate) {
                    Text("暂不更新")
                }
                TextButton(onClick = appSettingsViewModel::ignoreCurrentUpdate) {
                    Text("不再提示")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                        label = { Text(text = tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            AppTab.Home -> HomeRoute(
                innerPadding = innerPadding,
                viewModel = homeViewModel
            )

            AppTab.AiAdvice -> AiAdviceRoute(
                innerPadding = innerPadding,
                viewModel = aiAdviceViewModel
            )

            AppTab.Settings -> {
                val aiState by aiAdviceViewModel.uiState.collectAsState()
                val settingsState by aiSettingsViewModel.uiState.collectAsState()
                SettingsRoute(
                    innerPadding = innerPadding,
                    themeColorKey = settingsState.themeColorKey,
                    updateCheckState = appSettingsState.updateCheckState,
                    availableUpdate = appSettingsState.availableUpdate,
                    config = settingsState.draft,
                    isTestingConnection = settingsState.isTestingConnection,
                    testMessage = settingsState.testMessage,
                    tokenUsage = aiState.tokenUsage,
                    hasUnsavedAiConfig = settingsState.hasUnsavedChanges,
                    runtimeLogText = runtimeLogText,
                    onThemeColorChange = aiSettingsViewModel::saveThemeColor,
                    onCheckUpdates = { appSettingsViewModel.checkForUpdates(showUpToDateMessage = true) },
                    onExportData = { exportLauncher.launch("fra-workout-export.json") },
                    onRefreshLogs = {
                        scope.launch { runtimeLogText = AppLogger.read() }
                    },
                    onClearLogs = {
                        scope.launch {
                            AppLogger.clear()
                            runtimeLogText = AppLogger.read()
                            Toast.makeText(context, "运行日志已清空", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onExportLogs = { logExportLauncher.launch("fra-runtime-log.txt") },
                    onProviderChange = aiSettingsViewModel::updateProvider,
                    onBaseUrlChange = aiSettingsViewModel::updateBaseUrl,
                    onApiKeyChange = aiSettingsViewModel::updateApiKey,
                    onModelChange = aiSettingsViewModel::updateModel,
                    onTestConnection = aiSettingsViewModel::testConnection,
                    onSave = aiSettingsViewModel::save,
                    onClear = aiSettingsViewModel::clear
                )
            }
        }
    }
}

private const val BACK_EXIT_INTERVAL_MS = 2_000L


