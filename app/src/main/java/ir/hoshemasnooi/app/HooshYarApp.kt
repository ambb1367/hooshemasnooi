package ir.hoshemasnooi.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hoshemasnooi.app.ui.HomeScreen
import ir.hoshemasnooi.app.ui.HooshYarTheme
import ir.hoshemasnooi.app.ui.SettingsScreen
import ir.hoshemasnooi.app.viewmodel.FeedViewModel

private enum class Screen { HOME, SETTINGS }

@Composable
fun HooshYarApp() {
    HooshYarTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.fillMaxSize()) {
                    val vm: FeedViewModel = viewModel()
                    val state by vm.state.collectAsState()
                    var screen by remember { mutableStateOf(Screen.HOME) }

                    when (screen) {
                        Screen.HOME -> HomeScreen(
                            state = state,
                            onQueryChange = vm::onQueryChange,
                            onCategorySelected = vm::onCategorySelected,
                            onRefresh = { vm.refresh() },
                            onOpenSettings = { screen = Screen.SETTINGS },
                            onMessageShown = vm::clearMessage
                        )

                        Screen.SETTINGS -> SettingsScreen(
                            initialTranslation = vm.isTranslationEnabled(),
                            initialNitter = vm.nitterInstance(),
                            initialRemoteUrl = vm.remoteUrl(),
                            onTranslationChange = vm::toggleTranslation,
                            onNitterChange = vm::setNitterInstance,
                            onRemoteUrlChange = vm::setRemoteUrl,
                            onBack = {
                                screen = Screen.HOME
                                vm.refresh()
                            }
                        )
                    }
                }
            }
        }
    }
}
