package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.MemoryType
import com.example.data.model.SourceType
import com.example.data.model.UserSession
import com.example.ui.components.GeminiAssistantOverlay
import com.example.ui.screens.AskMementoScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryHubScreen
import com.example.ui.screens.PeopleScreen
import com.example.ui.screens.PrivacyCenterScreen
import com.example.ui.screens.PromisesScreen
import com.example.ui.screens.RememberScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TermsAndPrivacyDialog
import com.example.ui.screens.TimelineScreen
import com.example.ui.screens.WaitingScreen
import com.example.ui.theme.MementoCyan
import com.example.ui.theme.MementoTheme

object NavRoutes {
    const val HOME = "home"
    const val REMEMBER = "remember"
    const val ASK = "ask"
    const val VAULT = "vault"
    const val SETTINGS = "settings"
    const val TIMELINE = "timeline/{entityTitle}/{memoryId}"
    const val PEOPLE = "people"
    const val PROMISES = "promises"
    const val WAITING = "waiting"
    const val PRIVACY = "privacy"

    fun timeline(entityTitle: String, memoryId: String = ""): String {
        val safeTitle = if (entityTitle.isBlank()) "Object" else Uri.encode(entityTitle)
        val safeId = if (memoryId.isBlank()) "all" else Uri.encode(memoryId)
        return "timeline/$safeTitle/$safeId"
    }
}

@Composable
fun MementoApp(
    viewModel: MementoViewModel = viewModel(),
    externalVoiceTrigger: Boolean = false,
    onResetVoiceTrigger: () -> Unit = {}
) {
    val navController = rememberNavController()
    val userSession by viewModel.activeUserSession.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val timelineEvents by viewModel.timelineEvents.collectAsState()
    val selectedTimelineEntity by viewModel.selectedTimelineEntity.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val isMissionControl by viewModel.isMissionControl.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showLegalDialog by remember { mutableStateOf<Boolean?>(null) } // null, false=Terms, true=Privacy
    var showGeminiAssistant by remember { mutableStateOf(false) }

    LaunchedEffect(externalVoiceTrigger) {
        if (externalVoiceTrigger) {
            showGeminiAssistant = true
            onResetVoiceTrigger()
        }
    }

    MementoTheme(themeMode = themeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth > 600.dp
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val isBottomNavVisible = currentRoute in listOf(
                NavRoutes.HOME,
                NavRoutes.REMEMBER,
                NavRoutes.ASK,
                NavRoutes.VAULT,
                NavRoutes.SETTINGS
            )

            if (isWideScreen) {
                // Tablet / Desktop Layout with Navigation Rail
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .width(90.dp)
                            .fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NavigationRailItem(
                            selected = currentRoute == NavRoutes.HOME,
                            onClick = { navigateBottom(navController, NavRoutes.HOME) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = MementoCyan)
                        )
                        NavigationRailItem(
                            selected = currentRoute == NavRoutes.REMEMBER,
                            onClick = { navigateBottom(navController, NavRoutes.REMEMBER) },
                            icon = { Icon(Icons.Default.Add, contentDescription = "Remember") },
                            label = { Text("Remember", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = MementoCyan)
                        )
                        NavigationRailItem(
                            selected = currentRoute == NavRoutes.ASK,
                            onClick = { navigateBottom(navController, NavRoutes.ASK) },
                            icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Ask") },
                            label = { Text("Ask", fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = MementoCyan)
                        )
                        NavigationRailItem(
                            selected = currentRoute == NavRoutes.VAULT,
                            onClick = { navigateBottom(navController, NavRoutes.VAULT) },
                            icon = { Icon(Icons.Default.Inventory2, contentDescription = "Vault") },
                            label = { Text("Vault", fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = MementoCyan)
                        )
                        NavigationRailItem(
                            selected = currentRoute == NavRoutes.SETTINGS,
                            onClick = { navigateBottom(navController, NavRoutes.SETTINGS) },
                            icon = { Icon(Icons.Default.Tune, contentDescription = "Me") },
                            label = { Text("Me", fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = MementoCyan)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AppNavHost(
                            navController = navController,
                            viewModel = viewModel,
                            memories = memories,
                            timelineEvents = timelineEvents,
                            selectedTimelineEntity = selectedTimelineEntity,
                            auditLogs = auditLogs,
                            isMissionControl = isMissionControl,
                            themeMode = themeMode,
                            userSession = userSession,
                            onOpenTerms = { showLegalDialog = false },
                            onOpenPrivacyPolicy = { showLegalDialog = true }
                        )
                    }
                }
            } else {
                // Mobile Handheld Layout with Navigation Compose Bottom Bar
                Scaffold(
                    bottomBar = {
                        if (isBottomNavVisible) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == NavRoutes.HOME,
                                    onClick = { navigateBottom(navController, NavRoutes.HOME) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MementoCyan,
                                        indicatorColor = MementoCyan.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_home_btn")
                                )
                                NavigationBarItem(
                                    selected = currentRoute == NavRoutes.REMEMBER,
                                    onClick = { navigateBottom(navController, NavRoutes.REMEMBER) },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(MementoCyan),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Remember",
                                                tint = Color.Black,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    label = { Text("Remember", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MementoCyan,
                                        indicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.testTag("nav_remember_btn")
                                )
                                NavigationBarItem(
                                    selected = currentRoute == NavRoutes.ASK,
                                    onClick = { navigateBottom(navController, NavRoutes.ASK) },
                                    icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Ask") },
                                    label = { Text("Ask", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MementoCyan,
                                        indicatorColor = MementoCyan.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_ask_btn")
                                )
                                NavigationBarItem(
                                    selected = currentRoute == NavRoutes.VAULT,
                                    onClick = { navigateBottom(navController, NavRoutes.VAULT) },
                                    icon = { Icon(Icons.Default.Inventory2, contentDescription = "Vault") },
                                    label = { Text("Vault", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MementoCyan,
                                        indicatorColor = MementoCyan.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_vault_btn")
                                )
                                NavigationBarItem(
                                    selected = currentRoute == NavRoutes.SETTINGS,
                                    onClick = { navigateBottom(navController, NavRoutes.SETTINGS) },
                                    icon = { Icon(Icons.Default.Tune, contentDescription = "Me") },
                                    label = { Text("Me", fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MementoCyan,
                                        indicatorColor = MementoCyan.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_me_btn")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AppNavHost(
                            navController = navController,
                            viewModel = viewModel,
                            memories = memories,
                            timelineEvents = timelineEvents,
                            selectedTimelineEntity = selectedTimelineEntity,
                            auditLogs = auditLogs,
                            isMissionControl = isMissionControl,
                            themeMode = themeMode,
                            userSession = userSession,
                            onOpenTerms = { showLegalDialog = false },
                            onOpenPrivacyPolicy = { showLegalDialog = true },
                            onOpenGeminiAssistant = { showGeminiAssistant = true }
                        )
                    }
                }
            }

            // Legal Terms & Privacy Policy Dialog
            showLegalDialog?.let { isPrivacy ->
                TermsAndPrivacyDialog(
                    isPrivacyPolicy = isPrivacy,
                    onDismiss = { showLegalDialog = null }
                )
            }

            // Gemini AI Assistant Floating Overlay
            GeminiAssistantOverlay(
                isOpen = showGeminiAssistant,
                onDismiss = { showGeminiAssistant = false },
                onSaveMemory = { input, source ->
                    viewModel.saveMemory(input, source, null)
                },
                onAskQuestion = { question ->
                    viewModel.askMemento(question)
                }
            )
        }
    }
}

private fun navigateBottom(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: MementoViewModel,
    memories: List<com.example.data.model.MemoryEntity>,
    timelineEvents: List<com.example.data.model.MemoryTimelineEvent>,
    selectedTimelineEntity: String,
    auditLogs: List<com.example.data.model.SecurityAuditLog>,
    isMissionControl: Boolean,
    themeMode: String,
    userSession: UserSession?,
    onOpenTerms: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenGeminiAssistant: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        // 1. HOME SCREEN
        composable(NavRoutes.HOME) {
            HomeScreen(
                userName = userSession?.displayName ?: "Alex",
                memories = memories,
                onRememberClick = { navController.navigate(NavRoutes.REMEMBER) },
                onFindClick = { navController.navigate(NavRoutes.ASK) },
                onPeopleClick = { navController.navigate(NavRoutes.PEOPLE) },
                onWaitingClick = { navController.navigate(NavRoutes.WAITING) },
                onQuickSave = { text ->
                    viewModel.saveMemory(text, SourceType.TEXT, null)
                },
                onViewTimeline = { mem ->
                    viewModel.loadTimelineForEntity(mem.title, mem.id)
                    navController.navigate(NavRoutes.timeline(mem.title, mem.id))
                },
                onCorrectLocation = { mem ->
                    viewModel.loadTimelineForEntity(mem.title, mem.id)
                    navController.navigate(NavRoutes.timeline(mem.title, mem.id))
                },
                onDeleteMemory = { mem -> viewModel.deleteMemory(mem) },
                onCameraClick = { navController.navigate(NavRoutes.REMEMBER) },
                isSimpleMode = !isMissionControl,
                onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onOpenGeminiAssistant = onOpenGeminiAssistant
            )
        }

        // 2. REMEMBER SCREEN
        composable(NavRoutes.REMEMBER) {
            RememberScreen(
                onSaveMemory = { raw, source, uri ->
                    viewModel.saveMemory(raw, source, uri)
                },
                onNavigateToAsk = {
                    navController.navigate(NavRoutes.ASK) {
                        popUpTo(navController.graph.findStartDestination().id)
                    }
                },
                onNavigateToVault = {
                    navController.navigate(NavRoutes.VAULT) {
                        popUpTo(navController.graph.findStartDestination().id)
                    }
                }
            )
        }

        // 3. ASK MEMENTO SCREEN
        composable(NavRoutes.ASK) {
            AskMementoScreen(
                onAsk = { q -> viewModel.askMemento(q) },
                onCorrectLocation = { id, loc -> viewModel.correctLocation(id, loc) },
                onDeleteMemory = { id ->
                    val mem = memories.find { it.id == id }
                    if (mem != null) viewModel.deleteMemory(mem)
                },
                onNavigateToTimeline = { title, id ->
                    viewModel.loadTimelineForEntity(title, id)
                    navController.navigate(NavRoutes.timeline(title, id))
                }
            )
        }

        // 4. MEMORY HUB / VAULT
        composable(NavRoutes.VAULT) {
            MemoryHubScreen(
                memories = memories,
                onViewTimeline = { mem ->
                    viewModel.loadTimelineForEntity(mem.title, mem.id)
                    navController.navigate(NavRoutes.timeline(mem.title, mem.id))
                },
                onCorrectLocation = { mem ->
                    viewModel.loadTimelineForEntity(mem.title, mem.id)
                    navController.navigate(NavRoutes.timeline(mem.title, mem.id))
                },
                onDeleteMemory = { mem -> viewModel.deleteMemory(mem) },
                onRememberClick = { navController.navigate(NavRoutes.REMEMBER) }
            )
        }

        // 5. TIMELINE SCREEN
        composable(
            route = NavRoutes.TIMELINE,
            arguments = listOf(
                navArgument("entityTitle") { type = NavType.StringType },
                navArgument("memoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val entityTitle = backStackEntry.arguments?.getString("entityTitle") ?: selectedTimelineEntity
            TimelineScreen(
                entityTitle = entityTitle,
                timelineEvents = timelineEvents,
                onAddCorrection = { newLoc ->
                    val matchingMem = memories.find { it.title.equals(entityTitle, ignoreCase = true) }
                    if (matchingMem != null) {
                        viewModel.correctLocation(matchingMem.id, newLoc)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 6. PEOPLE SCREEN
        composable(NavRoutes.PEOPLE) {
            PeopleScreen(
                memories = memories,
                onBack = { navController.popBackStack() }
            )
        }

        // 7. PROMISES SCREEN
        composable(NavRoutes.PROMISES) {
            PromisesScreen(
                promises = memories.filter { it.type == MemoryType.PROMISE },
                onMarkDone = { mem -> viewModel.deleteMemory(mem) },
                onDeletePromise = { mem -> viewModel.deleteMemory(mem) },
                onBack = { navController.popBackStack() }
            )
        }

        // 8. WAITING SCREEN
        composable(NavRoutes.WAITING) {
            WaitingScreen(
                waitingList = memories.filter { it.type == MemoryType.WAITING },
                onMarkReceived = { mem -> viewModel.deleteMemory(mem) },
                onDeleteWaiting = { mem -> viewModel.deleteMemory(mem) },
                onBack = { navController.popBackStack() }
            )
        }

        // 9. PRIVACY CENTER
        composable(NavRoutes.PRIVACY) {
            PrivacyCenterScreen(
                memories = memories,
                onExportData = { viewModel.exportData() },
                onDeleteAllMemories = { viewModel.deleteAllMemories() },
                onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
                onDeleteAccount = {
                    viewModel.deleteAllMemories()
                    navController.navigate(NavRoutes.HOME)
                },
                onOpenTerms = onOpenTerms,
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                onBack = { navController.popBackStack() }
            )
        }

        // 10. SETTINGS / ME
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                userSession = userSession ?: UserSession(
                    userId = "usr_local_vault",
                    displayName = "Alex",
                    emailOrPhone = "alex@memento.local",
                    authProvider = "LOCAL_VAULT"
                ),
                totalMemoriesCount = memories.size,
                activeCount = memories.count { it.status == com.example.data.model.MemoryStatus.ACTIVE },
                waitingCount = memories.count { it.type == MemoryType.WAITING },
                auditLogs = auditLogs,
                isMissionControl = isMissionControl,
                onToggleMissionControl = { viewModel.toggleMissionControl(it) },
                currentThemeMode = themeMode,
                onThemeChange = { viewModel.setThemeMode(it) },
                onOpenPrivacyCenter = { navController.navigate(NavRoutes.PRIVACY) },
                onOpenTerms = onOpenTerms,
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                onLogout = {
                    viewModel.deleteAllMemories()
                    navController.navigate(NavRoutes.HOME)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
