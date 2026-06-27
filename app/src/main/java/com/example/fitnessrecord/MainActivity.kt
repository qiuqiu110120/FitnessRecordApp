package com.example.fitnessrecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessrecord.ui.ai.AiAdviceRoute
import com.example.fitnessrecord.ui.home.HomeRoute
import com.example.fitnessrecord.ui.theme.FitnessRecordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as FitnessRecordApplication
            val settingsViewModel: com.example.fitnessrecord.ui.settings.AppSettingsViewModel = viewModel(
                factory = app.appContainer.appSettingsViewModelFactory
            )
            val settingsState by settingsViewModel.uiState.collectAsState()
            FitnessRecordTheme(themeColorKey = settingsState.themeColorKey) {
                FitnessRecordApp(app.appContainer)
            }
        }
    }
}

private enum class AppTab(
    val label: String,
    val icon: ImageVector,
) {
    Home("首页", Icons.Outlined.CalendarMonth),
    AiAdvice("AI 建议", Icons.Outlined.AutoAwesome),
}

@Composable
private fun FitnessRecordApp(appContainer: AppContainer) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }

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
                viewModel = viewModel(factory = appContainer.homeViewModelFactory)
            )

            AppTab.AiAdvice -> AiAdviceRoute(
                innerPadding = innerPadding,
                viewModel = viewModel(factory = appContainer.aiAdviceViewModelFactory),
                settingsViewModel = viewModel(factory = appContainer.aiSettingsViewModelFactory)
            )
        }
    }
}
