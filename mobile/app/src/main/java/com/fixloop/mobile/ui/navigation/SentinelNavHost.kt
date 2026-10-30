package com.fixloop.mobile.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.fixloop.mobile.model.CategoryId
import com.fixloop.mobile.state.SentinelAppState
import com.fixloop.mobile.ui.screens.*
import com.fixloop.mobile.ui.theme.*
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")
    object Complaints : Screen("complaints")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    object ComplaintForm : Screen("complaint_form")
    object ReviewSubmit : Screen("review_submit")
    object SubmissionSuccess : Screen("submission_success")
    object ComplaintDetail : Screen("complaint_detail/{complaintId}") {
        fun createRoute(complaintId: String) = "complaint_detail/$complaintId"
    }
}

@Composable
fun SentinelNavHost(
    navController: NavHostController,
    appState: SentinelAppState,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Modals and Bottom Sheets active state
    var activeModalComplaintId by remember { mutableStateOf<String?>(null) }
    var showReplySheet by remember { mutableStateOf(false) }
    var showRateSheet by remember { mutableStateOf(false) }
    var showReopenDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }
    var showDraftsSheet by remember { mutableStateOf(false) }
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showNetworkConfigSheet by remember { mutableStateOf(false) }

    // Auto-dismiss transient toast
    LaunchedEffect(appState.showDraftToast) {
        if (appState.showDraftToast) {
            delay(2000)
            appState.showDraftToast = false
        }
    }

    val isMainTab = currentRoute in listOf(
        Screen.Home.route,
        Screen.Complaints.route,
        Screen.Notifications.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgPaper,
        bottomBar = {
            if (isMainTab) {
                SentinelBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    needsAttentionCount = appState.needsAttentionCount,
                    unreadNotifsCount = appState.unreadNotificationsCount
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onFinished = {
                            if (appState.user.isLoggedIn) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Login.route) {
                    LoginScreen(
                        appState = appState,
                        onLoginSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToSignup = {
                            navController.navigate(Screen.Signup.route)
                        }
                    )
                }

                composable(Screen.Signup.route) {
                    SignupScreen(
                        appState = appState,
                        onSignupSuccess = {
                            navController.navigate(Screen.ProfileSetup.route) {
                                popUpTo(Screen.Signup.route) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Signup.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.ProfileSetup.route) {
                    ProfileSetupScreen(
                        appState = appState,
                        onFinished = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        appState = appState,
                        onCategoryClick = { category ->
                            appState.startNewComplaint(category)
                            navController.navigate(Screen.ComplaintForm.route)
                        },
                        onOpenNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        },
                        onOpenDrafts = {
                            showDraftsSheet = true
                        },
                        onComplaintClick = { complaintId ->
                            navController.navigate(Screen.ComplaintDetail.createRoute(complaintId))
                        }
                    )
                }

                composable(Screen.Complaints.route) {
                    MyComplaintsScreen(
                        appState = appState,
                        onComplaintClick = { complaintId ->
                            navController.navigate(Screen.ComplaintDetail.createRoute(complaintId))
                        },
                        onQuickReplyClick = { complaintId ->
                            activeModalComplaintId = complaintId
                            showReplySheet = true
                        },
                        onQuickRateClick = { complaintId ->
                            activeModalComplaintId = complaintId
                            showRateSheet = true
                        }
                    )
                }

                composable(Screen.Notifications.route) {
                    NotificationsScreen(
                        appState = appState,
                        onNotificationClick = { complaintId ->
                            navController.navigate(Screen.ComplaintDetail.createRoute(complaintId))
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        appState = appState,
                        onOpenEditProfile = { showEditProfileSheet = true },
                        onOpenDrafts = { showDraftsSheet = true },
                        onOpenNetworkConfig = { showNetworkConfigSheet = true },
                        onOpenLogoutDialog = { showLogoutDialog = true }
                    )
                }

                composable(Screen.ComplaintForm.route) {
                    ComplaintFormScreen(
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToReview = { navController.navigate(Screen.ReviewSubmit.route) },
                        onOpenLocationPicker = { showLocationSheet = true }
                    )
                }

                composable(Screen.ReviewSubmit.route) {
                    ReviewSubmitScreen(
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() },
                        onSubmitSuccess = {
                            navController.navigate(Screen.SubmissionSuccess.route) {
                                popUpTo(Screen.ComplaintForm.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.SubmissionSuccess.route) {
                    SubmissionSuccessScreen(
                        appState = appState,
                        onTrackComplaint = { complaintId ->
                            navController.navigate(Screen.ComplaintDetail.createRoute(complaintId)) {
                                popUpTo(Screen.SubmissionSuccess.route) { inclusive = true }
                            }
                        },
                        onNavigateHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.ComplaintDetail.route) { backStackEntry ->
                    val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                    ComplaintDetailScreen(
                        complaintId = complaintId,
                        appState = appState,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenReplySheet = {
                            activeModalComplaintId = complaintId
                            showReplySheet = true
                        },
                        onOpenRateSheet = {
                            activeModalComplaintId = complaintId
                            showRateSheet = true
                        },
                        onOpenReopenDialog = {
                            activeModalComplaintId = complaintId
                            showReopenDialog = true
                        }
                    )
                }
            }

            // Transient Toast Overlay
            AnimatedVisibility(
                visible = appState.showDraftToast,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Ink)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(text = appState.toastMessage, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal Bottom Sheets & Dialogs
    if (showReplySheet && activeModalComplaintId != null) {
        ReplyBottomSheet(
            complaintId = activeModalComplaintId!!,
            appState = appState,
            onDismiss = { showReplySheet = false },
            onSubmit = { showReplySheet = false }
        )
    }

    if (showRateSheet && activeModalComplaintId != null) {
        RateBottomSheet(
            complaintId = activeModalComplaintId!!,
            appState = appState,
            onDismiss = { showRateSheet = false },
            onSubmit = { showRateSheet = false }
        )
    }

    if (showReopenDialog && activeModalComplaintId != null) {
        ReopenDialog(
            complaintId = activeModalComplaintId!!,
            appState = appState,
            onDismiss = { showReopenDialog = false },
            onConfirm = { showReopenDialog = false }
        )
    }

    if (showLogoutDialog) {
        LogoutDialog(
            appState = appState,
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    if (showLocationSheet) {
        LocationPickerBottomSheet(
            appState = appState,
            onDismiss = { showLocationSheet = false }
        )
    }

    if (showDraftsSheet) {
        DraftsBottomSheet(
            appState = appState,
            onResumeDraft = { draft ->
                appState.resumeDraft(draft)
                navController.navigate(Screen.ComplaintForm.route)
            },
            onDismiss = { showDraftsSheet = false }
        )
    }

    if (showEditProfileSheet) {
        EditProfileBottomSheet(
            appState = appState,
            onDismiss = { showEditProfileSheet = false }
        )
    }

    if (showNetworkConfigSheet) {
        NetworkConfigBottomSheet(
            onDismiss = { showNetworkConfigSheet = false }
        )
    }
}

@Composable
fun SentinelBottomNavBar(
    currentRoute: String?,
    onNavigateTo: (String) -> Unit,
    needsAttentionCount: Int,
    unreadNotifsCount: Int
) {
    // Frosted Frozen Glass Background Container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF5FFFFFF),
                        Color(0xE6F8F5EE)
                    )
                )
            )
            .drawBehind {
                // 1. Solid Ink top border (2.5dp)
                drawLine(
                    color       = Color(0xFF1B1B17),
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 2.5.dp.toPx()
                )
                // 2. Frozen glass specular highlight line (1.5dp)
                drawLine(
                    color       = Color.White.copy(alpha = 0.95f),
                    start       = Offset(0f, 2.5.dp.toPx()),
                    end         = Offset(size.width, 2.5.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "Home",
                icon = Icons.Default.Home,
                isActive = currentRoute == Screen.Home.route,
                onClick = { onNavigateTo(Screen.Home.route) }
            )

            BottomNavItem(
                label = "Complaints",
                icon = Icons.Default.ConfirmationNumber,
                isActive = currentRoute == Screen.Complaints.route,
                badgeCount = needsAttentionCount.takeIf { it > 0 },
                onClick = { onNavigateTo(Screen.Complaints.route) }
            )

            BottomNavItem(
                label = "Alerts",
                icon = Icons.Default.Notifications,
                isActive = currentRoute == Screen.Notifications.route,
                badgeCount = unreadNotifsCount.takeIf { it > 0 },
                onClick = { onNavigateTo(Screen.Notifications.route) }
            )

            BottomNavItem(
                label = "Profile",
                icon = Icons.Default.Person,
                isActive = currentRoute == Screen.Profile.route,
                onClick = { onNavigateTo(Screen.Profile.route) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    badgeCount: Int? = null,
    onClick: () -> Unit
) {
    val tabFg = if (isActive) Color.White else InkMuted

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    isActive -> Ink
                    isPressed -> Ink.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tabFg,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = tabFg,
                letterSpacing = 0.1.sp
            )

            // Active Radiant Pip Dot
            if (isActive) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(HazardAmber)
                )
            }
        }

        // Red Notification Badge at Top Right of Tab (Unclipped & Crisp)
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 3.dp)
                    .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                    .clip(CircleShape)
                    .background(AlertRed)
                    .border(1.5.dp, Color.White, CircleShape)
                    .padding(horizontal = 3.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeCount.toString(),
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
