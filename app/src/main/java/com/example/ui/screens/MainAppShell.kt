package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.FarmerAccountEntity
import com.example.ui.components.AgriAppBackground
import com.example.ui.components.AgriSignalLogoMark
import com.example.ui.viewmodel.EnvironmentUiState
import com.example.ui.viewmodel.EnvironmentViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.ScanViewModel

const val ROUTE_BERANDA = "beranda"
const val ROUTE_SCAN = "scan_daun"
const val ROUTE_RIWAYAT = "riwayat"
const val ROUTE_PROFIL = "profil"
const val ROUTE_WEATHER = "fitur_cuaca"

private data class BottomNavTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_TABS = listOf(
    BottomNavTab(
        route = ROUTE_BERANDA,
        labelResId = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_beranda"
    ),
    BottomNavTab(
        route = ROUTE_SCAN,
        labelResId = R.string.nav_scan,
        selectedIcon = Icons.Rounded.DocumentScanner,
        unselectedIcon = Icons.Outlined.DocumentScanner,
        testTag = "nav_tab_scan"
    ),
    BottomNavTab(
        route = ROUTE_RIWAYAT,
        labelResId = R.string.nav_history,
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_riwayat"
    ),
    BottomNavTab(
        route = ROUTE_PROFIL,
        labelResId = R.string.nav_profile,
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_tab_profil"
    )
)

@Composable
fun MainAppShell(
    farmer: FarmerAccountEntity,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_BERANDA

    val environmentViewModel: EnvironmentViewModel = viewModel(key = "env_${farmer.phoneNumber}")
    val scanViewModel: ScanViewModel = viewModel(key = "scan_${farmer.phoneNumber}")
    val historyViewModel: HistoryViewModel = viewModel(key = "history_${farmer.phoneNumber}")

    LaunchedEffect(farmer.phoneNumber) {
        historyViewModel.setActiveUserPhone(farmer.phoneNumber)
    }

    val envState by environmentViewModel.uiState.collectAsStateWithLifecycle()
    val selectedImageUri by scanViewModel.selectedImageUri.collectAsStateWithLifecycle()
    val scanState by scanViewModel.scanState.collectAsStateWithLifecycle()
    val filteredRecords by historyViewModel.filteredRecords.collectAsStateWithLifecycle()
    val allRecords by historyViewModel.allRecords.collectAsStateWithLifecycle()
    val activeFilter by historyViewModel.filter.collectAsStateWithLifecycle()
    val selectedHistoryRecord by historyViewModel.selectedRecord.collectAsStateWithLifecycle()

    fun navigateToTab(route: String) {
        if (route == ROUTE_RIWAYAT) {
            historyViewModel.closeDetail()
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val effectiveTabRoute = if (currentRoute == ROUTE_WEATHER) ROUTE_BERANDA else currentRoute

    AgriAppBackground(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            windowInsets = WindowInsets.navigationBars,
                            tonalElevation = 3.dp
                        ) {
                            NAV_TABS.forEach { tab ->
                                val selected = effectiveTabRoute == tab.route
                                val label = stringResource(id = tab.labelResId)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            header = {
                                AgriSignalLogoMark(
                                    size = 44.dp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        ) {
                            NAV_TABS.forEach { tab ->
                                val selected = effectiveTabRoute == tab.route
                                val label = stringResource(id = tab.labelResId)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_BERANDA
                        ) {
                            composable(ROUTE_BERANDA) {
                                HomeScreen(
                                    farmer = farmer,
                                    environmentState = envState,
                                    recentScans = allRecords,
                                    onPermissionResult = environmentViewModel::onPermissionResult,
                                    onRefreshWeather = environmentViewModel::refreshEnvironment,
                                    onOpenWeatherDetail = { navController.navigate(ROUTE_WEATHER) },
                                    onNavigateToScan = { navigateToTab(ROUTE_SCAN) },
                                    onNavigateToHistory = { navigateToTab(ROUTE_RIWAYAT) },
                                    onOpenScanDetail = { record ->
                                        historyViewModel.openDetail(record)
                                        navigateToTab(ROUTE_RIWAYAT)
                                    }
                                )
                            }
                            composable(ROUTE_WEATHER) {
                                WeatherDetailScreen(
                                    environmentState = envState,
                                    onRefresh = environmentViewModel::refreshEnvironment,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable(ROUTE_SCAN) {
                                val envSnapshot = (envState as? EnvironmentUiState.Success)?.snapshot
                                ScanLeafScreen(
                                    selectedImageUri = selectedImageUri,
                                    scanState = scanState,
                                    onImagesSelected = { uris ->
                                        scanViewModel.setSelectedImagesAndAnalyze(
                                            uris = uris,
                                            userPhone = farmer.phoneNumber,
                                            locationSummary = envSnapshot?.place?.shortLabel().orEmpty(),
                                            temperatureCelsius = envSnapshot?.temperatureCelsius
                                        )
                                    },
                                    onSelectBatchRecord = scanViewModel::selectBatchRecord,
                                    onAnalyzeClick = {
                                        scanViewModel.analyzeSelectedLeaf(
                                            userPhone = farmer.phoneNumber,
                                            locationSummary = envSnapshot?.place?.shortLabel().orEmpty(),
                                            temperatureCelsius = envSnapshot?.temperatureCelsius
                                        )
                                    },
                                    onResetClick = scanViewModel::reset
                                )
                            }
                            composable(ROUTE_RIWAYAT) {
                                HistoryScreen(
                                    records = filteredRecords,
                                    activeFilter = activeFilter,
                                    selectedRecord = selectedHistoryRecord,
                                    onFilterChange = historyViewModel::setFilter,
                                    onSelectRecord = historyViewModel::openDetail,
                                    onCloseDetail = historyViewModel::closeDetail,
                                    onDeleteRecord = historyViewModel::deleteRecord,
                                    onNavigateToScan = { navigateToTab(ROUTE_SCAN) }
                                )
                            }
                            composable(ROUTE_PROFIL) {
                                ProfileScreen(
                                    farmer = farmer,
                                    allRecords = allRecords,
                                    onNavigateToHistory = { navigateToTab(ROUTE_RIWAYAT) },
                                    onLogout = onLogout
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
