package com.example.indianbloom.ui.main

import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.indianbloom.BloomApplication
import com.example.indianbloom.data.database.BlockedAppEntity
import com.example.indianbloom.data.database.FocusState
import com.example.indianbloom.ui.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isWaitingToPairCard: Boolean,
    onSetWaitingToPair: (Boolean) -> Unit,
    isWaitingToDisableStrictMode: Boolean,
    onSetWaitingToDisableStrictMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as BloomApplication
    val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(app.repository) }
    val scope = rememberCoroutineScope()

    val blockedApps by viewModel.blockedApps.collectAsStateWithLifecycle()
    val pairedCards by viewModel.pairedCards.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    var overlayGranted by remember { mutableStateOf(PermissionUtils.hasOverlayPermission(context)) }
    var usageGranted by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    var accessGranted by remember { mutableStateOf(PermissionUtils.hasAccessibilityPermission(context)) }

    // Onboarding flow steps: 0 = Choose profile, 1 = Select apps (handled by app selector), 2 = Normal state
    var onboardingStep by remember { mutableStateOf(0) }

    // Active bottom navigation tab: 0 = Auren (Home), 1 = Activity, 2 = Settings
    var selectedTab by remember { mutableStateOf(0) }

    // App Selector visibility state
    var showAppSelector by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Full device apps listing (loaded in background)
    var rawInstalledAppsList by remember { mutableStateOf<List<RawAppInfo>>(emptyList()) }
    var isAppsLoading by remember { mutableStateOf(false) }
    var showSplashScreen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplashScreen = false
    }

    // Periodically poll permissions
    LaunchedEffect(Unit) {
        while (true) {
            overlayGranted = PermissionUtils.hasOverlayPermission(context)
            usageGranted = PermissionUtils.hasUsageStatsPermission(context)
            accessGranted = PermissionUtils.hasAccessibilityPermission(context)
            delay(1500)
        }
    }

    // Query installed launcher apps on the device ONCE in background
    LaunchedEffect(Unit) {
        isAppsLoading = true
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val apps = resolveInfos.map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val name = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                RawAppInfo(packageName = pkg, appName = name, icon = icon)
            }.distinctBy { it.packageName }
             .sortedBy { it.appName.lowercase() }
            rawInstalledAppsList = apps
            isAppsLoading = false
        }
    }

    // Map RawAppInfo to InstalledAppInfo dynamically with in-memory check
    val installedAppsList by remember(rawInstalledAppsList, blockedApps) {
        derivedStateOf {
            rawInstalledAppsList.map { rawApp ->
                val isBlocked = blockedApps.any { it.packageName == rawApp.packageName }
                InstalledAppInfo(
                    packageName = rawApp.packageName,
                    appName = rawApp.appName,
                    icon = rawApp.icon,
                    isBlocked = isBlocked
                )
            }
        }
    }

    val allPermissionsGranted = overlayGranted && usageGranted
    val hasPairedCard = pairedCards.isNotEmpty()

    // Base background color
    val backgroundColor = Color(0xFF09090A)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        if (showSplashScreen) {
            AurenSplashScreen()
        } else {
            when {
            // Step A: Force permission setup
            !allPermissionsGranted -> {
                AccessibilityOnboarding(
                    overlayGranted = overlayGranted,
                    usageGranted = usageGranted,
                    accessGranted = accessGranted,
                    onGrantOverlay = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    onGrantUsage = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    onGrantAccess = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }

            // Step B: Setup first focus profile (Choose mode)
            blockedApps.isEmpty() && onboardingStep == 0 -> {
                ChooseFirstModeScreen(
                    onContinue = {
                        onboardingStep = 1
                        showAppSelector = true
                    }
                )
            }

            // Step C: Render main screen structure or App Selector overlay
            showAppSelector -> {
                AppSelectorScreen(
                    installedApps = installedAppsList,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    isAppsLoading = isAppsLoading,
                    onToggleApp = { appInfo ->
                        if (appInfo.isBlocked) {
                            viewModel.removeAppFromBlocklist(appInfo.packageName)
                        } else {
                            viewModel.addAppToBlocklist(appInfo.packageName, appInfo.appName)
                        }
                    },
                    onClose = {
                        showAppSelector = false
                        searchQuery = ""
                        if (blockedApps.isNotEmpty()) {
                            onboardingStep = 2 // Transition to dashboard
                        } else {
                            onboardingStep = 0 // Go back to choice if they cleared all
                        }
                    }
                )
            }

            // Main Scaffold with tabs
            else -> {
                Scaffold(
                    bottomBar = {
                        AurenBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    },
                    containerColor = backgroundColor
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> AurenHomeTab(
                                blockedAppsCount = blockedApps.size,
                                activeSession = activeSession,
                                hasPairedCard = hasPairedCard,
                                isWaitingToPair = isWaitingToPairCard,
                                onStartPairing = { onSetWaitingToPair(true) },
                                onCancelPairing = { onSetWaitingToPair(false) },
                                onConfigureApps = { showAppSelector = true }
                            )
                            1 -> ActivityTab()
                            2 -> SettingsTab(
                                pairedCards = pairedCards,
                                onConfigureApps = { showAppSelector = true },
                                onUnregisterCard = { viewModel.unregisterCard(pairedCards.first().uid) },
                                isWaitingToDisableStrictMode = isWaitingToDisableStrictMode,
                                onSetWaitingToDisableStrictMode = onSetWaitingToDisableStrictMode
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

// ==========================================
// 1. ONBOARDING & SETUP SCREENS
// ==========================================

@Composable
fun AccessibilityOnboarding(
    overlayGranted: Boolean,
    usageGranted: Boolean,
    accessGranted: Boolean,
    onGrantOverlay: () -> Unit,
    onGrantUsage: () -> Unit,
    onGrantAccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Auren Star Logo Display
        Image(
            painter = painterResource(id = com.example.indianbloom.R.drawable.auren_logo),
            contentDescription = "Auren Logo",
            modifier = Modifier
                .size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Set Up Auren Focus",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enable two standard permissions to start locking distracting apps.",
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Onboarding checklist card container
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131316)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF26262D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Overlay Permission Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!overlayGranted) onGrantOverlay() }
                        .padding(vertical = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (overlayGranted) Color(0xFFD4AF37) else Color(0xFF26262D))
                    ) {
                        Text(if (overlayGranted) "✓" else "1", color = if (overlayGranted) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Display Over Apps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Lets Auren show the focus blocker screen on top of distracting apps.", color = Color(0xFF8E8E93), fontSize = 11.sp)
                    }
                }

                Divider(color = Color(0xFF26262D), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Usage Access Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (!usageGranted) onGrantUsage() }
                        .padding(vertical = 12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (usageGranted) Color(0xFFD4AF37) else Color(0xFF26262D))
                    ) {
                        Text(if (usageGranted) "✓" else "2", color = if (usageGranted) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Usage Tracking Access", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Allows background detection of when you open blacklisted apps.", color = Color(0xFF8E8E93), fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Dynamic Primary Onboarding Button
        val buttonText = when {
            !overlayGranted -> "Allow Display Over Apps"
            !usageGranted -> "Allow Usage Tracking"
            else -> "Get Started"
        }
        val buttonAction = when {
            !overlayGranted -> onGrantOverlay
            !usageGranted -> onGrantUsage
            else -> { {} }
        }

        Button(
            onClick = buttonAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4AF37),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(56.dp)
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        val hintText = when {
            !overlayGranted -> "Tap to enable 'Draw over other apps' permissions."
            !usageGranted -> "Tap to toggle on 'Usage access' for Auren."
            else -> "All core configuration complete."
        }
        Text(
            text = hintText,
            color = Color(0xFF5E5E66),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}


@Composable
fun ChooseFirstModeScreen(onContinue: () -> Unit) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    val options = listOf(
        "Focus at work",
        "Be present",
        "Spend time with family",
        "Get better sleep",
        "Cut back on social media"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "How should your first\nmode help you?",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        Text(
            text = "Each mode blocks the apps you choose. You can add more anytime.",
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Render option buttons
        options.forEachIndexed { index, text ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedOption == index) Color(0xFF26262D) else Color(0xFF131316)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF26262D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { selectedOption = index }
            ) {
                Row(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    RadioButton(
                        selected = (selectedOption == index),
                        onClick = { selectedOption = index },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFD4AF37),
                            unselectedColor = Color(0xFF5E5E66)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            enabled = selectedOption != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4AF37),
                disabledContainerColor = Color(0xFF26262D)
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(56.dp)
        ) {
            Text(
                text = "Continue",
                color = if (selectedOption != null) Color.Black else Color(0xFF5E5E66),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

// ==========================================
// 2. FULLSCREEN APP SELECTOR SCREEN (BRICK STYLE)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorScreen(
    installedApps: List<InstalledAppInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isAppsLoading: Boolean,
    onToggleApp: (InstalledAppInfo) -> Unit,
    onClose: () -> Unit
) {
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val selectedCount = remember(installedApps) {
        installedApps.count { it.isBlocked }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090A))
            .safeDrawingPadding()
    ) {
        // App Selector Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Select apps",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Search Bar Pill (Spotify style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF131316))
                .border(BorderStroke(1.dp, Color(0xFF26262D)), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search apps", color = Color(0xFF5E5E66), fontSize = 14.sp)
                    }
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF8E8E93))
                    }
                }
            }
        }

        // Subtitle counters
        Text(
            text = if (isAppsLoading) "Loading applications..." else "Found ${filteredApps.size} apps",
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        // Loading indicator
        if (isAppsLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD4AF37))
            }
        } else {
            // Launcher Apps Checklist
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredApps) { app ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131316)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF26262D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleApp(app) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                             ) {
                                // Dynamic App Icon renderer
                                if (app.icon != null) {
                                    AndroidView(
                                        factory = { context ->
                                            ImageView(context).apply {
                                                setImageDrawable(app.icon)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF26262D))
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(app.appName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(app.packageName, color = Color(0xFF8E8E93), fontSize = 10.sp)
                                }
                            }

                            // Custom Round Checkbox (Gold Metal Theme)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (app.isBlocked) Color(0xFFD4AF37) else Color.Transparent)
                                    .border(
                                        BorderStroke(2.dp, if (app.isBlocked) Color(0xFFD4AF37) else Color(0xFF26262D)),
                                        CircleShape
                                    )
                            ) {
                                if (app.isBlocked) {
                                    Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            // Floating Bottom CTA Button (Brushed Gold design)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onClose,
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        disabledContainerColor = Color(0xFF26262D)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(54.dp)
                ) {
                    Text(
                        text = if (selectedCount > 0) "Continue with $selectedCount apps" else "Select apps to continue",
                        color = if (selectedCount > 0) Color.Black else Color(0xFF5E5E66),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. TAB NAVIGATION UI
// ==========================================

@Composable
fun AurenBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf("Auren", "Activity", "Settings")

    NavigationBar(
        containerColor = Color(0xFF131316),
        tonalElevation = 8.dp,
        modifier = Modifier.height(72.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                val active = selectedTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(index) }
                        )
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = label,
                        color = if (active) Color(0xFFD4AF37) else Color(0xFF8E8E93),
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Black else FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Brick-style active tab gold dot indicator
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (active) Color(0xFFD4AF37) else Color.Transparent)
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. TAB PANELS
// ==========================================

@Composable
fun AurenHomeTab(
    blockedAppsCount: Int,
    activeSession: com.example.indianbloom.data.database.FocusSessionEntity?,
    hasPairedCard: Boolean,
    isWaitingToPair: Boolean,
    onStartPairing: () -> Unit,
    onCancelPairing: () -> Unit,
    onConfigureApps: () -> Unit
) {
    val context = LocalContext.current
    val isActive = activeSession != null && activeSession.state == FocusState.ACTIVE

    // Pulse animations for wait pairing / focus active glow
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Tab Header Branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "AUREN",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 6.sp
            )
            Text(
                text = "NFC FOCUS SYSTEM",
                color = Color(0xFF8A8A93),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Focus State Pill Card
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isActive) Color(0xFFD4AF37).copy(alpha = 0.15f) else Color(0xFF131316))
                .border(BorderStroke(1.dp, if (isActive) Color(0xFFD4AF37) else Color(0xFF26262D)), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isActive) "AUREN ACTIVE" else "AUREN DEACTIVATED",
                color = if (isActive) Color(0xFFD4AF37) else Color(0xFF8E8E93),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // --- Sleek 3D Brushed Black Metal Card (Auren Metal Edition) ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
        ) {
            // Pulse backing glow if in focus mode or waiting tap
            if (isActive || isWaitingToPair) {
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            (if (isActive) Color(0xFFD4AF37) else Color(0xFF131316))
                                .copy(alpha = 0.08f * pulseScale)
                        )
                )
            }

            // Metal Card Body
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .size(width = 210.dp, height = 210.dp)
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFD4AF37), // Gold shine top
                                    Color(0xFF8E8E93),
                                    Color(0xFF1E1E22)  // Dark steel bottom
                                )
                            )
                        ),
                        RoundedCornerShape(30.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E1E22), // Dark graphite
                                    Color(0xFF0F0F10)  // Deep obsidian black
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    // Brushed metal background micro details (silver fleck simulation)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.01f))
                    )

                    // LED focus active breathing light in top right
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color(0xFFD4AF37).copy(alpha = glowAlpha)
                                else if (isWaitingToPair) Color(0xFFFFCC00).copy(alpha = glowAlpha)
                                else Color(0xFF26262D)
                            )
                            .align(Alignment.TopEnd)
                    )

                    // Card engravings in center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = if (isWaitingToPair) "⚡" else "AUREN",
                            color = Color(0xFFE2E2E6),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 6.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isWaitingToPair) "WAITING FOR TAP" else "METAL EDITION",
                            color = Color(0xFF8E8E93),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // NFC symbol in bottom left
                    Text(
                        text = "📡",
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Focus Mode dropdown layout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    Toast
                        .makeText(context, "No Socials profile active", Toast.LENGTH_SHORT)
                        .show()
                }
                .padding(8.dp)
        ) {
            Text(
                text = "Mode : No Socials",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("▼", color = Color(0xFF8E8E93), fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Dynamic blocked app counters
        Text(
            text = if (blockedAppsCount > 0) "Blocking $blockedAppsCount apps" else "No apps selected",
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
            modifier = Modifier.clickable { onConfigureApps() }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Large Bottom Capsule pairing trigger button
        if (!hasPairedCard) {
            if (!isWaitingToPair) {
                Button(
                    onClick = onStartPairing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(56.dp)
                ) {
                    Text("Pair Auren Card", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onCancelPairing,
                    border = BorderStroke(1.dp, Color(0xFFFF3B30)),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3B30)),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(56.dp)
                ) {
                    Text("Cancel Waiting", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Card is paired! Instructions button showing click instructions
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF131316))
                    .border(BorderStroke(1.dp, Color(0xFF26262D)), RoundedCornerShape(28.dp))
                    .fillMaxWidth(0.9f)
                    .height(56.dp)
                    .clickable {
                        Toast
                            .makeText(
                                context,
                                "Tap your physical Auren card on the back of your phone to activate focus.",
                                Toast.LENGTH_LONG
                            )
                            .show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap Auren Card to start",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ActivityTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Focused time stats layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Today", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("0h 0m", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            }
            // Vertical Divider line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(Color(0xFF26262D))
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Average", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("0h 0m", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            }
        }

        Spacer(modifier = Modifier.weight(0.8f))

        Text(
            text = "Activities will appear after your\nfirst day using Auren",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.weight(1.2f))
    }
}

@Composable
fun SettingsTab(
    pairedCards: List<com.example.indianbloom.data.database.NfcCardEntity>,
    onConfigureApps: () -> Unit,
    onUnregisterCard: () -> Unit,
    isWaitingToDisableStrictMode: Boolean,
    onSetWaitingToDisableStrictMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var showAccessibilityGuide by remember { mutableStateOf(false) }

    val sharedPrefs = remember { context.getSharedPreferences("auren_prefs", Context.MODE_PRIVATE) }
    var strictModeEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("strict_mode", false))
    }

    DisposableEffect(sharedPrefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "strict_mode") {
                strictModeEnabled = sharedPrefs.getBoolean("strict_mode", false)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Read active accessibility service registration
    LaunchedEffect(Unit) {
        while (true) {
            val expectedComponentName = "${context.packageName}/com.example.indianbloom.service.BloomAccessibilityService"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            isAccessibilityEnabled = enabledServicesSetting.split(':').any {
                it.equals(expectedComponentName, ignoreCase = true)
            }
            delay(1500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .safeDrawingPadding()
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Rules & App Picker Config
        SettingsCard {
            SettingsItem(
                title = "My Auren Rules",
                onClick = onConfigureApps,
                trailingText = "Configure Apps"
            )
            Divider(color = Color(0xFF26262D), thickness = 1.dp)
            SettingsItem(
                title = "Emergency Bypass Rules",
                onClick = {
                    Toast.makeText(context, "Bypass is active (24H limit)", Toast.LENGTH_SHORT).show()
                },
                trailingText = "5 left"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Strict Mode (Anti-Uninstall)
        SettingsCard {
            SettingsSwitchItem(
                title = "Strict Mode (Anti-Uninstall)",
                subtitle = "Require NFC card to disable or uninstall Auren",
                checked = strictModeEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (isAccessibilityEnabled) {
                            sharedPrefs.edit().putBoolean("strict_mode", true).apply()
                            Toast.makeText(context, "Strict Mode Enabled!", Toast.LENGTH_SHORT).show()
                        } else {
                            showAccessibilityGuide = true
                        }
                    } else {
                        onSetWaitingToDisableStrictMode(true)
                    }
                }
            )
        }

        if (isWaitingToDisableStrictMode) {
            AlertDialog(
                onDismissRequest = { onSetWaitingToDisableStrictMode(false) },
                title = {
                    Text(
                        text = "Scan Auren Card",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Please tap your physical Auren card against the back of your phone to turn off Strict Mode.",
                            color = Color(0xFF8E8E93),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF26262D))
                        ) {
                            Text("📡", fontSize = 28.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { onSetWaitingToDisableStrictMode(false) }
                    ) {
                        Text("Cancel", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF131316),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(BorderStroke(1.dp, Color(0xFF26262D)), RoundedCornerShape(20.dp))
            )
        }

        if (showAccessibilityGuide) {
            AlertDialog(
                onDismissRequest = { showAccessibilityGuide = false },
                title = {
                    Text(
                        text = "Enable Accessibility",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "To enable Strict Mode (Anti-Uninstall), Auren needs Accessibility permission.",
                            color = Color(0xFFE2E2E6),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "⚠️ Restricted Setting Info (Android 13+):",
                            color = Color(0xFFD4AF37),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Android blocks accessibility for sideloaded apps. To allow it safely:",
                            color = Color(0xFF8E8E93),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        AccessibilityStepItem(
                            stepNumber = "1",
                            description = "Click below to open App Info. At top-right, tap the 3 dots (⋮) and select 'Allow restricted settings'. Confirm with PIN/fingerprint.",
                            buttonText = "Open App Settings",
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        AccessibilityStepItem(
                            stepNumber = "2",
                            description = "Click below to open Accessibility. Under 'Installed apps', find 'Auren' and switch it ON.",
                            buttonText = "Open Accessibility",
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showAccessibilityGuide = false }
                    ) {
                        Text("Done", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF131316),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(BorderStroke(1.dp, Color(0xFF26262D)), RoundedCornerShape(24.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device system connection configurations
        SettingsCard {
            SettingsItem(
                title = "Notifications",
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )
            Divider(color = Color(0xFF26262D), thickness = 1.dp)
            SettingsItem(
                title = "Accessibility Engine",
                onClick = {
                    showAccessibilityGuide = true
                },
                trailingText = if (isAccessibilityEnabled) "Active" else "Setup required"
            )
            Divider(color = Color(0xFF26262D), thickness = 1.dp)
            SettingsItem(
                title = "Background Protection",
                onClick = {
                    Toast.makeText(context, "Foreground Monitor running safely", Toast.LENGTH_SHORT).show()
                },
                trailingText = "Running"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Paired hardware unregistration configurations
        if (pairedCards.isNotEmpty()) {
            SettingsCard {
                SettingsItem(
                    title = "Unregister Auren Card",
                    onClick = onUnregisterCard,
                    textColor = Color(0xFFFF3B30)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom version text metadata
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AUREN",
                color = Color(0xFFD4AF37),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "Version 1.1.5 (Pre-Release)",
                color = Color(0xFF8E8E93),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun AccessibilityStepItem(
    stepNumber: String,
    description: String,
    buttonText: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFD4AF37))
        ) {
            Text(
                text = stepNumber,
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = description,
                color = Color(0xFFE2E2E6),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            if (buttonText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF26262D),
                        contentColor = Color(0xFFD4AF37)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131316)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF26262D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    onClick: () -> Unit,
    trailingText: String? = null,
    textColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(trailingText, color = Color(0xFF8E8E93), fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp))
            }
            Text("›", color = Color(0xFF5E5E66), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color(0xFF8E8E93), fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFD4AF37),
                uncheckedThumbColor = Color(0xFF8E8E93),
                uncheckedTrackColor = Color(0xFF26262D),
                checkedBorderColor = Color(0xFFD4AF37),
                uncheckedBorderColor = Color(0xFF26262D)
            )
        )
    }
}

@Composable
fun AurenSplashScreen() {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "SplashLogoAlpha"
    )

    // Breathing glow backing animation
    val infiniteTransition = rememberInfiniteTransition(label = "SplashGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SplashGlowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SplashGlowAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090A)),
        contentAlignment = Alignment.Center
    ) {
        // Soft golden glow circle behind logo
        Box(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer(
                    scaleX = glowScale,
                    scaleY = glowScale,
                    alpha = glowAlpha * alphaAnim
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD4AF37).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Luxury Auren Icon
        Image(
            painter = painterResource(id = com.example.indianbloom.R.drawable.ic_launcher_auren),
            contentDescription = "Auren Logo",
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.2f * alphaAnim)), RoundedCornerShape(24.dp))
                .alpha(alphaAnim)
        )
    }
}
