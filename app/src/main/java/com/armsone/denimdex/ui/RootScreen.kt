package com.armsone.denimdex.ui

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armsone.denimdex.core.catalog.DeterministicCatalog
import com.armsone.denimdex.core.design.DenimColors
import com.armsone.denimdex.core.design.DenimShapes
import com.armsone.denimdex.core.design.DenimTestTags
import com.armsone.denimdex.core.design.DenimTypography
import com.armsone.denimdex.feature.archive.ArchiveScreen
import com.armsone.denimdex.feature.archive.ArchiveViewModel
import com.armsone.denimdex.feature.guide.LearnScreen
import com.armsone.denimdex.feature.scan.ScanScreen
import com.armsone.denimdex.feature.scan.ScanViewModel
import com.armsone.denimdex.feature.settings.SettingsScreen
import com.armsone.denimdex.feature.settings.SettingsViewModel

enum class DenimDexTab(val label: String, val icon: ImageVector) {
    SCAN("감정", Icons.Default.CameraAlt),
    ARCHIVE("아카이브", Icons.Default.Layers),
    GUIDE("가이드", Icons.Default.MenuBook),
    SETTINGS("설정", Icons.Default.Settings)
}

/** Width breakpoints per handoff 8.2: Expanded >= 840dp two-pane; Medium >= 600dp uses a nav rail. */
private const val EXPANDED_WIDTH_DP = 840
private const val MEDIUM_WIDTH_DP = 600

fun isAndroidTv(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

fun hasCameraFeature(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
}

@Composable
fun RootScreen(catalogFixture: String? = null) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTv = remember { isAndroidTv(context) }
    val hasCamera = remember { hasCameraFeature(context) }

    val widthDp = configuration.screenWidthDp
    val isExpandedWidth = widthDp >= EXPANDED_WIDTH_DP
    val isMediumOrWider = widthDp >= MEDIUM_WIDTH_DP || isTv

    var selectedTab by rememberSaveable { mutableStateOf(DenimDexTab.SCAN) }
    var showExitConfirm by remember { mutableStateOf(false) }

    val scanViewModel: ScanViewModel = viewModel()
    val archiveViewModel: ArchiveViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    LaunchedEffect(catalogFixture) {
        if (catalogFixture != null && catalogFixture in DeterministicCatalog.ALL_FIXTURES) {
            DeterministicCatalog.applyFixture(
                fixtureId = catalogFixture,
                scanViewModel = scanViewModel,
                archiveViewModel = archiveViewModel,
                settingsViewModel = settingsViewModel,
                onTabSelected = { selectedTab = it },
                onShowExitConfirm = { showExitConfirm = it }
            )
        }
    }

    // Google TV: Back key at the root/home tab prompts an exit confirmation (8.2.3)
    BackHandler(enabled = isTv && selectedTab == DenimDexTab.SCAN) {
        showExitConfirm = true
    }

    Surface(color = DenimColors.canvas) {
        if (isMediumOrWider) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(containerColor = DenimColors.cardSurface) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DenimDexTab.entries.forEach { tab ->
                        NavigationRailItem(
                            modifier = Modifier.testTag(tab.testTag),
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = DenimColors.indigo,
                                selectedTextColor = DenimColors.indigo,
                                indicatorColor = DenimColors.fadedDenim
                            )
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    TabContent(
                        tab = selectedTab,
                        scanViewModel = scanViewModel,
                        archiveViewModel = archiveViewModel,
                        settingsViewModel = settingsViewModel,
                        isExpandedWidth = isExpandedWidth,
                        hasCamera = hasCamera
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = DenimColors.canvas,
                bottomBar = {
                    DenimFloatingBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TabContent(
                        tab = selectedTab,
                        scanViewModel = scanViewModel,
                        archiveViewModel = archiveViewModel,
                        settingsViewModel = settingsViewModel,
                        isExpandedWidth = isExpandedWidth,
                        hasCamera = hasCamera
                    )
                }
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("DenimDex를 종료할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    (context as? android.app.Activity)?.finish()
                }) { Text("종료") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("취소") }
            }
        )
    }
}

/**
 * Floating rounded-pill bottom tab bar matching the iOS default Scan chrome:
 * a white capsule container inset from the screen edges, with a soft rounded
 * selection capsule behind the active tab's icon/label.
 */
@Composable
private fun DenimFloatingBottomBar(
    selectedTab: DenimDexTab,
    onTabSelected: (DenimDexTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = DenimShapes.pill,
        color = DenimColors.cardSurface,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DenimDexTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .testTag(tab.testTag)
                        .clip(DenimShapes.pill)
                        .then(
                            if (selected) Modifier.background(DenimColors.fadedDenim) else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) DenimColors.indigo else DenimColors.inkSoft,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        style = DenimTypography.caption.copy(
                            fontSize = 11.sp,
                            color = if (selected) DenimColors.indigo else DenimColors.inkSoft,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

private val DenimDexTab.testTag: String
    get() = when (this) {
        DenimDexTab.SCAN -> DenimTestTags.TAB_SCAN
        DenimDexTab.ARCHIVE -> DenimTestTags.TAB_ARCHIVE
        DenimDexTab.GUIDE -> DenimTestTags.TAB_GUIDE
        DenimDexTab.SETTINGS -> DenimTestTags.TAB_SETTINGS
    }

@Composable
private fun TabContent(
    tab: DenimDexTab,
    scanViewModel: ScanViewModel,
    archiveViewModel: ArchiveViewModel,
    settingsViewModel: SettingsViewModel,
    isExpandedWidth: Boolean,
    hasCamera: Boolean
) {
    when (tab) {
        DenimDexTab.SCAN -> ScanScreen(viewModel = scanViewModel, hasCamera = hasCamera)
        DenimDexTab.ARCHIVE -> ArchiveScreen(viewModel = archiveViewModel, isExpandedWidth = isExpandedWidth)
        DenimDexTab.GUIDE -> LearnScreen()
        DenimDexTab.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
    }
}
