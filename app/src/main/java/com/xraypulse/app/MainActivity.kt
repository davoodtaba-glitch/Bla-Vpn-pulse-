package com.xraypulse.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xraypulse.app.ui.AppViewModel
import com.xraypulse.app.ui.i18n.AppStrings
import com.xraypulse.app.ui.i18n.LocalStrings
import com.xraypulse.app.ui.i18n.t
import com.xraypulse.app.ui.navigation.Route
import com.xraypulse.app.ui.screens.HomeScreen
import com.xraypulse.app.ui.screens.ImportScreen
import com.xraypulse.app.ui.screens.ManualServerScreen
import com.xraypulse.app.ui.screens.PerAppProxyScreen
import com.xraypulse.app.ui.screens.QrScanActivity
import com.xraypulse.app.ui.screens.QuickSetupScreen
import com.xraypulse.app.ui.screens.ServersScreen
import com.xraypulse.app.ui.screens.SettingsScreen
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.PulseMuted
import com.xraypulse.app.ui.theme.XrayPulseTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Allow keyboard to resize content so lower fields stay visible
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleImportIntent(intent)
        requestNotificationPermission()

        setContent {
            val state by vm.uiState.collectAsStateWithLifecycle()
            val i18nTick by vm.customI18nTick.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val strings = remember(state.settings.language, i18nTick) {
                AppStrings.resolve(context, state.settings.language)
            }
            val isRtl = state.settings.language.lowercase() in listOf("fa", "fa-ir", "persian", "farsi")
            XrayPulseTheme(
                darkTheme = state.settings.darkTheme,
                themeStyle = com.xraypulse.app.ui.theme.toAppThemeStyle(state.settings.themeStyle),
                accentArgb = state.settings.accentColor,
                accentSecondaryArgb = state.settings.accentColorSecondary
            ) {
                CompositionLocalProvider(
                    LocalStrings provides strings,
                    LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                val accent = LocalAccent.current
                val palette = com.xraypulse.app.ui.theme.LocalPalette.current
                val nav = rememberNavController()
                val snackbar = remember { SnackbarHostState() }
                val backStack by nav.currentBackStackEntryAsState()
                val current = backStack?.destination?.route ?: Route.Home.path

                val vpnPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        vm.onVpnPermissionGranted()
                    } else {
                        vm.toast("VPN permission denied")
                    }
                }

                val qrLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val text = result.data?.getStringExtra(QrScanActivity.EXTRA_RESULT)
                    if (!text.isNullOrBlank()) vm.importLinks(text)
                }

                LaunchedEffect(state.message) {
                    state.message?.let {
                        snackbar.showSnackbar(it)
                        vm.clearMessage()
                    }
                }
                val tabs = listOf(
                    TabItem(Route.Home.path, t("home"), Icons.Rounded.Home),
                    TabItem(Route.Servers.path, t("subscriptions"), Icons.Rounded.Subscriptions),
                    TabItem(Route.Import.path, t("import"), Icons.Rounded.AddCircle),
                    TabItem(Route.Settings.path, t("settings"), Icons.Rounded.Settings)
                )
                val showBar = current in tabs.map { it.route }
                var settingsDirty by remember { mutableStateOf(false) }
                var settingsLeaveAttempt by remember { mutableIntStateOf(0) }
                var pendingTabRoute by remember { mutableStateOf<String?>(null) }

                fun navigateToTab(route: String) {
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                // Edge-to-edge is enabled; keep system bars visible and inset content.
                // Status bar icons: light on dark theme, dark on light theme.
                val view = LocalView.current
                SideEffect {
                    val window = window
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !state.settings.darkTheme
                    controller.isAppearanceLightNavigationBars = !state.settings.darkTheme
                }

                Scaffold(
                    containerColor = palette.bg,
                    // Top (+ horizontal cutouts): push content below status bar / display cutout.
                    // Bottom nav bar applies navigationBarsPadding itself when visible;
                    // non-tab screens get navigationBarsPadding on the NavHost below.
                    contentWindowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    ),
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        if (showBar) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .shadow(
                                            elevation = 12.dp,
                                            shape = RoundedCornerShape(24.dp),
                                            ambientColor = accent.copy(0.15f),
                                            spotColor = com.xraypulse.app.ui.theme.LocalAccentSecondary.current.copy(0.12f)
                                        )
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    palette.surface2.copy(alpha = 0.95f),
                                                    palette.surface.copy(alpha = 0.98f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    accent.copy(0.35f),
                                                    com.xraypulse.app.ui.theme.LocalAccentSecondary.current.copy(0.25f),
                                                    palette.border.copy(0.4f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                ) {
                                    val accent2 = com.xraypulse.app.ui.theme.LocalAccentSecondary.current
                                    tabs.forEach { tab ->
                                        val selected = current == tab.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                if (tab.route == current) return@NavigationBarItem
                                                if (current == Route.Settings.path && settingsDirty) {
                                                    pendingTabRoute = tab.route
                                                    settingsLeaveAttempt++
                                                } else {
                                                    navigateToTab(tab.route)
                                                }
                                            },
                                            icon = {
                                                com.xraypulse.app.ui.components.NeonIcon(
                                                    tab.icon,
                                                    tab.label,
                                                    size = if (selected) 26.dp else 22.dp,
                                                    tintOverride = if (selected) accent2 else PulseMuted
                                                )
                                            },
                                            label = { },
                                            alwaysShowLabel = false,
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = accent2,
                                                selectedTextColor = accent2,
                                                unselectedIconColor = PulseMuted,
                                                unselectedTextColor = PulseMuted,
                                                indicatorColor = accent2.copy(alpha = 0.16f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = Route.Home.path,
                        modifier = Modifier
                            .padding(padding)
                            // Full-screen routes (no bottom tabs) still clear the gesture/nav bar
                            .then(
                                if (!showBar) Modifier.navigationBarsPadding()
                                else Modifier
                            )
                            .imePadding()
                    ) {
                        composable(Route.Home.path) {
                            val activeSub = state.selected?.subscriptionId?.let { sid ->
                                state.subscriptions.find { it.id == sid }
                            }
                            HomeScreen(
                                connection = state.connection,
                                selected = state.selected,
                                activeSubscription = activeSub,
                                settings = state.settings,
                                isTesting = state.isTesting,
                                updateAvailableVersion = state.updateAvailableVersion,
                                onToggle = { vm.connectOrDisconnect(vpnPermission) },
                                onOpenServers = {
                                    nav.navigate(Route.Servers.path) {
                                        popUpTo(nav.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onQuickSetup = { nav.navigate(Route.QuickSetup.path) },
                                onQuickTest = {
                                    state.selected?.let { vm.testLatency(it) }
                                        ?: vm.toast("Select a server first")
                                },
                                onRefreshActiveSubscription = vm::refreshActiveSubscription,
                                onOpenUpdate = {
                                    nav.navigate(Route.Settings.path) {
                                        popUpTo(nav.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                isBusy = state.isBusy
                            )
                        }
                        composable(Route.Servers.path) {
                            ServersScreen(
                                servers = state.servers,
                                subscriptions = state.subscriptions,
                                selectedId = state.selected?.id,
                                search = state.searchQuery,
                                sortByDelay = state.settings.sortByDelay,
                                isTesting = state.isTesting,
                                testingServerId = state.testingServerId,
                                onSearch = vm::setSearch,
                                onSelect = vm::selectServer,
                                onDelete = vm::deleteServer,
                                onDeleteMany = vm::deleteServers,
                                onDeleteAll = vm::deleteAllServers,
                                onDeleteInvalid = vm::deleteInvalidServers,
                                onTestAll = vm::testAll,
                                onTestOne = vm::testLatency,
                                onEdit = { id -> nav.navigate(Route.ManualEdit.create(id)) },
                                onSortByDelay = vm::setSortByDelay,
                                onCancelTesting = vm::cancelTesting,
                                onRefreshSubscription = vm::refreshSubscription,
                                onDeleteSubscription = vm::deleteSubscription,
                                onRenameSubscription = vm::renameSubscription,
                                onUpdateSubscription = vm::updateSubscription
                            )
                        }
                        composable(Route.Import.path) {
                            ImportScreen(
                                busy = state.isBusy,
                                onImportMixed = { configs, subs, onDone ->
                                    vm.importMixed(configs, subs, onDone)
                                    if (subs.isNotEmpty()) {
                                        nav.navigate(Route.Servers.path)
                                    }
                                },
                                onScanQr = {
                                    qrLauncher.launch(
                                        Intent(this@MainActivity, QrScanActivity::class.java)
                                    )
                                },
                                onManual = { nav.navigate(Route.ManualEdit.create(0)) },
                                onOpenSubscriptions = { nav.navigate(Route.Servers.path) }
                            )
                        }
                        composable(Route.Settings.path) {
                            SettingsScreen(
                                settings = state.settings,
                                coreVersion = state.coreVersion,
                                onApply = vm::applySettings,
                                onApplyAppearance = vm::applyAppearance,
                                onApplyLanguage = vm::applyLanguage,
                                onOpenPerApp = { nav.navigate(Route.PerApp.path) },
                                onDirtyChange = { settingsDirty = it },
                                leaveAttempt = settingsLeaveAttempt,
                                onLeaveResolved = { leave ->
                                    if (leave) {
                                        val dest = pendingTabRoute
                                        pendingTabRoute = null
                                        settingsDirty = false
                                        if (dest != null) navigateToTab(dest)
                                    } else {
                                        pendingTabRoute = null
                                    }
                                }
                            )
                        }
                        composable(Route.QuickSetup.path) {
                            QuickSetupScreen(
                                busy = state.isBusy,
                                onBack = { nav.popBackStack() },
                                onOpenSettingsAppearance = {
                                    nav.popBackStack()
                                    nav.navigate(Route.Settings.path)
                                },
                                onOpenSettingsLanguage = {
                                    nav.popBackStack()
                                    nav.navigate(Route.Settings.path)
                                },
                                onOpenFullSettings = {
                                    nav.popBackStack()
                                    nav.navigate(Route.Settings.path)
                                },
                                onImportAndConnect = { text, onResult ->
                                    vm.quickSetupImportAndConnect(text, vpnPermission, onResult)
                                }
                            )
                        }
                        composable(Route.PerApp.path) {
                            PerAppProxyScreen(
                                settings = state.settings,
                                onUpdate = vm::updateSettings
                            )
                        }
                        composable(
                            route = "manual_edit?id={id}",
                            arguments = listOf(
                                navArgument("id") {
                                    type = NavType.LongType
                                    defaultValue = 0L
                                }
                            )
                        ) { entry ->
                            val id = entry.arguments?.getLong("id") ?: 0L
                            val initial = state.servers.find { it.id == id }
                            ManualServerScreen(
                                initial = initial,
                                onSave = {
                                    vm.saveManualServer(it)
                                    nav.popBackStack()
                                },
                                onBack = { nav.popBackStack() }
                            )
                        }
                    }
                }
                } // CompositionLocalProvider
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleImportIntent(intent)
    }

    private fun handleImportIntent(intent: Intent?) {
        val data = intent?.data?.toString() ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
        if (!data.isNullOrBlank()) {
            vm.importLinks(data)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private data class TabItem(val route: String, val label: String, val icon: ImageVector)
}
