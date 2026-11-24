package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LocalActiveSheetCount
import com.example.ui.components.LocalPocketlyFeedback
import com.example.ui.components.rememberPocketlyFeedbackController
import com.example.ui.screens.AccountDetailScreen
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AddInvestmentSheet
import com.example.ui.screens.AddTransactionBottomSheet
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.CategoriesManagerScreen
import com.example.ui.screens.GrowthCalculatorScreen
import com.example.ui.screens.HoldingDetailScreen
import com.example.ui.screens.HomeOverviewScreen
import com.example.ui.screens.InvestScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScheduledScreen
import com.example.ui.screens.TransactionSearchScreen
import com.example.ui.theme.PocketlyTextStyles
import com.example.ui.theme.PocketlyTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.PocketlyViewModel
import com.example.ui.viewmodel.SubScreen
import kotlinx.coroutines.launch

@Composable
fun PocketlyApp(
    viewModel: PocketlyViewModel,
    biometricAvailable: Boolean,
    onRequestBiometricUnlock: (onSuccess: () -> Unit) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddInvestmentSheet by remember { mutableStateOf(false) }
    val activeSheetCount = remember { mutableIntStateOf(0) }

    // Zero-permission SAF launchers for CSV export, JSON backup export, and JSON backup import
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = viewModel.buildCsvExport()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    viewModel.emitNotice("Transactions exported to CSV")
                }.onFailure {
                    viewModel.emitNotice("Could not write CSV file.")
                }
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.buildJsonBackup()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess {
                    viewModel.emitNotice("Full backup saved")
                }.onFailure {
                    viewModel.emitNotice("Could not write backup file.")
                }
            }
        }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("Empty file")
                }.onSuccess { raw ->
                    viewModel.importJsonBackup(raw)
                }.onFailure {
                    viewModel.emitNotice("Could not read that backup file.")
                }
            }
        }
    }

    // Collect notices for snackbar with optional 5s Undo action
    LaunchedEffect(Unit) {
        viewModel.notices.collect { notice ->
            val result = snackbarHostState.showSnackbar(
                message = notice.message,
                actionLabel = if (notice.undoTransaction != null) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoNoticeAction(notice)
            }
        }
    }

    val feedback = rememberPocketlyFeedbackController(soundEnabled = state.settings.soundEffects)

    PocketlyTheme(
        themeMode = state.settings.theme,
        reduceMotion = state.settings.reduceMotion
    ) {
        CompositionLocalProvider(LocalPocketlyFeedback provides feedback) {
        val colors = PocketlyTheme.colors

        if (!state.isInitialized) {
            Box(modifier = Modifier.fillMaxSize().background(colors.bg))
            return@CompositionLocalProvider
        }

        // First-launch 3-step onboarding
        if (!state.settings.onboarded) {
            OnboardingScreen(
                biometricAvailable = biometricAvailable,
                onComplete = { currency, pin, bio ->
                    viewModel.completeOnboarding(currency, pin, bio)
                }
            )
            return@CompositionLocalProvider
        }

        // Back handling for sub-screens and non-Home tabs
        BackHandler(enabled = state.subScreen != SubScreen.None) {
            viewModel.closeSubScreen()
        }
        BackHandler(enabled = state.subScreen == SubScreen.None && state.activeTab != MainTab.HOME) {
            viewModel.selectTab(MainTab.HOME)
        }

        CompositionLocalProvider(LocalActiveSheetCount provides activeSheetCount) {
        val chromeHidden = activeSheetCount.intValue > 0 || state.addTxSheet.isOpen || showAddInvestmentSheet
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg),
            contentAlignment = Alignment.TopCenter
        ) {
            // Main App Content (fluid mobile-first, clamped cleanly on wide screens)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .statusBarsPadding()
            ) {
                if (state.subScreen != SubScreen.None) {
                    when (val sub = state.subScreen) {
                        SubScreen.Search -> TransactionSearchScreen(
                            state = state,
                            onBack = viewModel::closeSubScreen,
                            onEditTransaction = viewModel::openEditTransaction,
                            onDeleteTransaction = viewModel::deleteTransactionWithUndo
                        )
                        SubScreen.Scheduled -> ScheduledScreen(
                            state = state,
                            onBack = viewModel::closeSubScreen,
                            onSaveItem = viewModel::saveScheduledItem,
                            onTogglePaused = viewModel::toggleScheduledPaused,
                            onDeleteItem = viewModel::deleteScheduledItem
                        )
                        SubScreen.Activity -> ActivityScreen(
                            state = state,
                            onBack = viewModel::closeSubScreen,
                            onTapItem = viewModel::markActivityReadAndNavigate,
                            onMarkAllRead = viewModel::markAllActivityRead
                        )
                        is SubScreen.AccountDetail -> AccountDetailScreen(
                            accountId = sub.accountId,
                            state = state,
                            onBack = viewModel::closeSubScreen,
                            onEditTransaction = viewModel::openEditTransaction,
                            onDeleteTransaction = viewModel::deleteTransactionWithUndo,
                            onAddTransactionForAccount = { accId -> viewModel.openAddTransaction(accountId = accId) },
                            onArchiveAccount = viewModel::archiveAccount
                        )
                        is SubScreen.HoldingDetail -> HoldingDetailScreen(
                            holdingId = sub.holdingId,
                            state = state,
                            onBack = viewModel::closeSubScreen,
                            onDepositOrWithdraw = viewModel::addHoldingDepositOrWithdraw,
                            onEditRate = viewModel::updateHoldingRate,
                            onUpdateManualValue = viewModel::updateManualHoldingValue,
                            onArchive = viewModel::archiveHolding
                        )
                        SubScreen.GrowthCalculator -> GrowthCalculatorScreen(
                            calc = state.calculator,
                            currencyCode = state.settings.currency,
                            onBack = viewModel::closeSubScreen,
                            onUpdateCalc = viewModel::updateCalculator,
                            onSaveAsInvestment = viewModel::saveCalculatorAsInvestment
                        )
                        SubScreen.CategoriesManager -> CategoriesManagerScreen(
                            state = state,
                            onBack = viewModel::closeSubScreen,
                            onSaveCategory = viewModel::saveCategory,
                            onArchiveCategory = viewModel::archiveCategory
                        )
                        SubScreen.None -> Unit
                    }
                } else {
                    // 5-Tab Main Destinations with calm 220ms crossfade + subtle vertical offset
                    AnimatedContent(
                        targetState = state.activeTab,
                        transitionSpec = {
                            if (state.settings.reduceMotion) {
                                fadeIn(tween(120)) togetherWith fadeOut(tween(100))
                            } else {
                                (fadeIn(tween(220)) + slideInVertically(tween(220)) { 18 }) togetherWith fadeOut(tween(140))
                            }
                        },
                        label = "main_tab_transition"
                    ) { tab ->
                        when (tab) {
                            MainTab.HOME -> HomeOverviewScreen(
                                state = state,
                                onOpenActivity = { viewModel.openSubScreen(SubScreen.Activity) },
                                onOpenSearch = { viewModel.openSubScreen(SubScreen.Search) },
                                onOpenScheduled = { viewModel.openSubScreen(SubScreen.Scheduled) },
                                onSelectHomeMode = viewModel::setHomeMode,
                                onSelectMonth = viewModel::selectMonth,
                                onToggleHomeCategory = viewModel::toggleHomeCategoryFilter,
                                onSelectOverviewCategory = viewModel::selectOverviewCategory,
                                onOpenAddTransaction = { catId -> viewModel.openAddTransaction(categoryId = catId) },
                                onEditTransaction = viewModel::openEditTransaction,
                                onDeleteTransaction = viewModel::deleteTransactionWithUndo,
                                onLoadDemoData = viewModel::loadDemoData
                            )
                            MainTab.ACCOUNTS -> AccountsScreen(
                                state = state,
                                onOpenAccountDetail = { id -> viewModel.openSubScreen(SubScreen.AccountDetail(id)) },
                                onOpenInvestTab = { viewModel.selectTab(MainTab.INVEST) },
                                onSaveAccount = viewModel::saveAccount,
                                onMoveAccount = viewModel::moveAccountOrder
                            )
                            MainTab.BUDGETS -> BudgetsScreen(
                                state = state,
                                onSelectMonth = viewModel::selectMonth,
                                onSaveBudget = viewModel::saveBudget
                            )
                            MainTab.INVEST -> InvestScreen(
                                state = state,
                                onSelectRange = viewModel::selectInvestRange,
                                onOpenHoldingDetail = { id -> viewModel.openSubScreen(SubScreen.HoldingDetail(id)) },
                                onOpenGrowthCalculator = { viewModel.openSubScreen(SubScreen.GrowthCalculator) },
                                onOpenAddHolding = { showAddInvestmentSheet = true }
                            )
                            MainTab.PROFILE -> ProfileScreen(
                                state = state,
                                biometricAvailable = biometricAvailable,
                                onUpdateProfile = viewModel::updateProfile,
                                onChangePin = viewModel::changePin,
                                onRemovePin = viewModel::removePin,
                                onToggleBiometric = viewModel::updateBiometric,
                                onUpdateLockAfter = viewModel::updateLockAfter,
                                onUpdateCurrency = viewModel::updateCurrency,
                                onOpenCategories = { viewModel.openSubScreen(SubScreen.CategoriesManager) },
                                onOpenAccountsTab = { viewModel.selectTab(MainTab.ACCOUNTS) },
                                onOpenBudgetsTab = { viewModel.selectTab(MainTab.BUDGETS) },
                                onExportCsv = { csvExportLauncher.launch("pocketly-transactions.csv") },
                                onExportJson = { jsonExportLauncher.launch("pocketly-backup.json") },
                                onImportJson = { jsonImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                onLoadDemoData = viewModel::loadDemoData,
                                onClearAllData = viewModel::clearAllData,
                                onUpdateTheme = viewModel::updateTheme,
                                onUpdateReduceMotion = viewModel::updateReduceMotion,
                                onUpdateSoundEffects = viewModel::updateSoundEffects
                            )
                        }
                    }

                    // Contextual floating mint "+" action — hidden behind any open bottom sheet scrim
                    AnimatedVisibility(
                        visible = !chromeHidden && state.activeTab != MainTab.PROFILE,
                        enter = fadeIn(tween(if (state.settings.reduceMotion) 0 else 140)),
                        exit = fadeOut(tween(if (state.settings.reduceMotion) 0 else 100)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 18.dp, bottom = 92.dp)
                    ) {
                        if (state.activeTab == MainTab.INVEST) {
                            Surface(
                                onClick = {
                                    feedback.tap()
                                    showAddInvestmentSheet = true
                                },
                                shape = CircleShape,
                                color = colors.mint,
                                shadowElevation = if (colors.isDark) 0.dp else 6.dp,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 52.dp)
                                    .testTag("fab_add_investment")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Text("Add investment", style = PocketlyTextStyles.button, color = Color.White)
                                }
                            }
                        } else {
                            FloatingActionButton(
                                onClick = {
                                    feedback.tap()
                                    viewModel.openAddTransaction()
                                },
                                shape = CircleShape,
                                containerColor = colors.mint,
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = if (colors.isDark) 0.dp else 6.dp
                                ),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("fab_add_transaction")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add transaction", modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                    // Floating 5-tab Bottom Navigation Bar — hidden cleanly whenever any bottom sheet is open
                    AnimatedVisibility(
                        visible = !chromeHidden,
                        enter = fadeIn(tween(if (state.settings.reduceMotion) 0 else 140)),
                        exit = fadeOut(tween(if (state.settings.reduceMotion) 0 else 100)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        PocketlyFloatingBottomBar(
                            activeTab = state.activeTab,
                            onSelectTab = viewModel::selectTab
                        )
                    }
                }

                // Undo / Feedback Snackbar Host positioned cleanly above bottom nav
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = if (state.subScreen == SubScreen.None) 88.dp else 16.dp
                        )
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = if (colors.isDark) colors.surface2 else colors.ink,
                        contentColor = if (colors.isDark) colors.ink else Color.White,
                        actionColor = colors.mint,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Add / Edit Transaction ModalBottomSheet
            if (state.addTxSheet.isOpen) {
                AddTransactionBottomSheet(
                    state = state,
                    onDismiss = viewModel::closeTransactionSheet,
                    onSave = viewModel::saveTransaction,
                    onDelete = viewModel::deleteTransactionWithUndo
                )
            }

            // Add Investment Bottom Sheet
            if (showAddInvestmentSheet) {
                AddInvestmentSheet(
                    currencyCode = state.settings.currency,
                    onDismiss = { showAddInvestmentSheet = false },
                    onAdd = { name, type, mode, principal, apy, start, maturity, icon, color ->
                        viewModel.addHolding(name, type, mode, principal, apy, start, maturity, icon, color)
                        showAddInvestmentSheet = false
                    }
                )
            }

            // Lock Screen Overlay (lifts off smoothly upward on unlock)
            AnimatedVisibility(
                visible = state.isLocked,
                enter = fadeIn(tween(180)),
                exit = slideOutVertically(tween(if (state.settings.reduceMotion) 0 else 420)) { -it } + fadeOut(tween(260))
            ) {
                LockScreen(
                    biometricEnabled = state.settings.biometricEnabled && biometricAvailable,
                    lockoutUntilEpochMs = state.settings.lockoutUntilEpochMs,
                    onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                    onTriggerBiometric = {
                        onRequestBiometricUnlock { viewModel.unlockApp() }
                    },
                    onUnlocked = { viewModel.unlockApp() }
                )
            }
        }
        }
        }
    }
}

@Composable
private fun PocketlyFloatingBottomBar(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val feedback = LocalPocketlyFeedback.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = colors.surface,
        border = if (colors.isDark) BorderStroke(1.dp, colors.line) else null,
        shadowElevation = if (colors.isDark) 0.dp else 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == activeTab
                val icon = when (tab) {
                    MainTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
                    MainTab.ACCOUNTS -> if (selected) Icons.Rounded.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
                    MainTab.BUDGETS -> if (selected) Icons.Rounded.PieChart else Icons.Outlined.PieChart
                    MainTab.INVEST -> if (selected) Icons.Rounded.TrendingUp else Icons.Outlined.TrendingUp
                    MainTab.PROFILE -> if (selected) Icons.Rounded.Person else Icons.Outlined.Person
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) colors.mintSoft else Color.Transparent)
                        .clickable {
                            if (!selected) {
                                feedback.tap()
                                onSelectTab(tab)
                            }
                        }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                        .testTag("tab_${tab.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        tint = if (selected) colors.mint else colors.inkMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = if (selected) PocketlyTextStyles.navLabelSelected else PocketlyTextStyles.navLabel,
                        color = if (selected) colors.mint else colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
