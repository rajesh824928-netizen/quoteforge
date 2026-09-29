package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.QuoteForgeTheme
import com.example.ui.viewmodel.QuotationViewModel

import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.viewmodel.AppThemeMode

class MainActivity : ComponentActivity() {

    private val viewModel: QuotationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            QuoteForgeTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuoteForgeApp(viewModel = viewModel)
                }
            }
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home, "bottom_nav_home")
    object Quotations : BottomNavItem("quotations", "Quotations", Icons.Filled.Description, Icons.Outlined.Description, "bottom_nav_quotations")
    object Clients : BottomNavItem("clients", "Clients", Icons.Filled.People, Icons.Outlined.People, "bottom_nav_clients")
    object Projects : BottomNavItem("projects", "Projects", Icons.Filled.Business, Icons.Outlined.Business, "bottom_nav_projects")
    object More : BottomNavItem("more", "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "bottom_nav_more")
}

@Composable
fun QuoteForgeApp(viewModel: QuotationViewModel) {
    val navController = rememberNavController()
    var showAiAssistantDialog by remember { mutableStateOf(false) }

    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val workspaces by viewModel.allWorkspaces.collectAsState()

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Quotations,
        BottomNavItem.Clients,
        BottomNavItem.Projects,
        BottomNavItem.More
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (!isAuthenticated) "landing" else "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("landing") {
                LandingScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = {
                        navController.navigate("home") {
                            popUpTo("landing") { inclusive = true }
                        }
                    },
                    onNavigateToWorkspaceSetup = {
                        navController.navigate("workspace_setup") {
                            popUpTo("landing") { inclusive = true }
                        }
                    }
                )
            }

            composable("workspace_setup") {
                WorkspaceSetupScreen(
                    viewModel = viewModel,
                    onWorkspaceCreated = {
                        navController.navigate("home") {
                            popUpTo("workspace_setup") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToNewQuotation = { isEstimate ->
                        viewModel.initNewQuotation(isEstimate = isEstimate)
                        navController.navigate("builder")
                    },
                    onNavigateToEditQuotation = { quotationId ->
                        viewModel.loadQuotation(quotationId)
                        navController.navigate("edit_quotation/$quotationId")
                    },
                    onNavigateToPreviewQuotation = { quotationId ->
                        navController.navigate("preview/$quotationId")
                    },
                    onNavigateToQuotations = {
                        navController.navigate("quotations") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToClients = {
                        navController.navigate("clients") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProjects = {
                        navController.navigate("projects") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToMaterials = { navController.navigate("materials") },
                    onNavigateToCompanies = { navController.navigate("companies") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onOpenAiAssistant = { showAiAssistantDialog = true },
                    onNavigateToWorkspaces = { navController.navigate("workspaces") },
                    onNavigateToGoogleDrive = { navController.navigate("drive") },
                    onNavigateToChangelog = { navController.navigate("changelog") }
                )
            }

            composable("quotations") {
                QuotationsListScreen(
                    viewModel = viewModel,
                    onNavigateToNewQuotation = { isEstimate ->
                        viewModel.initNewQuotation(isEstimate = isEstimate)
                        navController.navigate("builder")
                    },
                    onNavigateToEditQuotation = { quotationId ->
                        viewModel.loadQuotation(quotationId)
                        navController.navigate("edit_quotation/$quotationId")
                    },
                    onNavigateToPreviewQuotation = { quotationId ->
                        navController.navigate("preview/$quotationId")
                    }
                )
            }

            composable("clients") {
                ClientListScreen(
                    viewModel = viewModel,
                    onNavigateBack = null,
                    onSelectClientForQuotation = { clientId ->
                        viewModel.initNewQuotation(clientId = clientId)
                        navController.navigate("builder")
                    }
                )
            }

            composable("projects") {
                ProjectListScreen(
                    viewModel = viewModel,
                    onNavigateBack = null
                )
            }

            composable("more") {
                MoreScreen(
                    viewModel = viewModel,
                    onNavigateToWorkspaces = { navController.navigate("workspaces") },
                    onNavigateToGoogleDrive = { navController.navigate("drive") },
                    onNavigateToPdfStudio = { navController.navigate("pdf_studio") },
                    onNavigateToActivityLogs = { navController.navigate("activity_logs") },
                    onNavigateToCompanies = { navController.navigate("companies") },
                    onNavigateToMaterials = { navController.navigate("materials") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onOpenAiAssistant = { showAiAssistantDialog = true },
                    onNavigateToLanding = { navController.navigate("landing") },
                    onNavigateToChangelog = { navController.navigate("changelog") }
                )
            }

            composable("workspaces") {
                WorkspaceManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("drive") {
                GoogleDriveSettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("pdf_studio") {
                PdfDesignStudioScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("activity_logs") {
                ActivityLogScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }


            composable("builder") {
                QuotationBuilderScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPreview = { quotationId ->
                        navController.navigate("preview/$quotationId") {
                            popUpTo("home")
                        }
                    },
                    onOpenAiAssistant = { showAiAssistantDialog = true }
                )
            }

            composable(
                route = "edit_quotation/{quotationId}",
                arguments = listOf(navArgument("quotationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val quotationId = backStackEntry.arguments?.getLong("quotationId") ?: 0L
                LaunchedEffect(quotationId) {
                    viewModel.loadQuotation(quotationId)
                }
                QuotationBuilderScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPreview = { qId ->
                        navController.navigate("preview/$qId") {
                            popUpTo("home")
                        }
                    },
                    onOpenAiAssistant = { showAiAssistantDialog = true }
                )
            }

            composable(
                route = "preview/{quotationId}",
                arguments = listOf(navArgument("quotationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val quotationId = backStackEntry.arguments?.getLong("quotationId") ?: 0L
                QuotationPreviewScreen(
                    viewModel = viewModel,
                    quotationId = quotationId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { qId ->
                        navController.navigate("edit_quotation/$qId")
                    }
                )
            }

            composable("materials") {
                MaterialLibraryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("companies") {
                CompanyProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("changelog") {
                ChangelogScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showAiAssistantDialog) {
        AiAssistantDialog(
            onDismiss = { showAiAssistantDialog = false },
            onAddItemsToQuotation = { items ->
                viewModel.addItems(items)
                if (navController.currentDestination?.route != "builder" &&
                    navController.currentDestination?.route?.startsWith("edit_quotation") != true) {
                    if (viewModel.currentQuotation.value == null) {
                        viewModel.initNewQuotation()
                    }
                    navController.navigate("builder")
                }
            }
        )
    }
}

