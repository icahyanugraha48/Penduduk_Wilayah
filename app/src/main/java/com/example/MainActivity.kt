package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create MainViewModel using standard reliable Android ViewModelProvider
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            MyApplicationTheme {
                AppShell(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val notificationList by viewModel.notifications.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe permission states
    val menuMutasiEnabled by viewModel.menuMutasiEnabled.collectAsState()
    val menuPelaporanEnabled by viewModel.menuPelaporanEnabled.collectAsState()
    val menuPengumumanEnabled by viewModel.menuPengumumanEnabled.collectAsState()

    // State Router: Pure state-driven navigation for zero-crashing bulletproof previews
    var currentScreen by remember { mutableStateOf("dashboard") }
    // Parameterized storage for switching screen with defaults
    var initialWargaFilter by remember { mutableStateOf("all") }
    
    // Switch between public recap dashboard and operator login portal
    var preLoginState by remember { mutableStateOf("recap") }

    val isAdmin = currentUser?.role == "Kepala Wilayah"

    // Automatically redirect if a screen gets disabled
    LaunchedEffect(currentUser, menuMutasiEnabled, menuPelaporanEnabled, menuPengumumanEnabled) {
        if (currentUser != null && !isAdmin) {
            if (currentScreen == "mutasi" && !menuMutasiEnabled) currentScreen = "dashboard"
            if (currentScreen == "pelaporan" && !menuPelaporanEnabled) currentScreen = "dashboard"
            if (currentScreen == "pengumuman" && !menuPengumumanEnabled) currentScreen = "dashboard"
        }
    }

    // Listen for core ViewModel operations messages
    LaunchedEffect(key1 = true) {
        viewModel.operationStatus.collect { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    if (currentUser == null) {
        if (preLoginState == "recap") {
            PreLoginRecapScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    preLoginState = "login"
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Masuk Portal Operator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { preLoginState = "recap" }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali ke Rekap Data"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
                Box(modifier = Modifier.weight(1f)) {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            currentScreen = "dashboard"
                        }
                    )
                }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Sistem Kependudukan",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                modifier = Modifier.testTag("officer_scope"),
                                text = if (currentUser?.role == "Kepala Wilayah") "Kepala Wilayah" else "RT ${currentUser?.rt} / RW ${currentUser?.rw}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        // Quick Profile badge displaying first letters
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.displayName?.take(2)?.uppercase() ?: "RT",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    },
                    actions = {
                        // Notifications active indicator badge
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable {
                                    if (currentScreen != "mutasi") {
                                        currentScreen = "mutasi" // Jump to mutasi for notification details
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Melihat Pembaruan Data Realtime...")
                                        }
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Sistem Notifikasi"
                            )
                            if (notificationList.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            }
                        }

                        // Logout Icon
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Keluar Akun",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("app_navigation_bar"),
                    tonalElevation = 8.dp
                ) {
                    // Item 1: Beranda
                    NavigationBarItem(
                        selected = currentScreen == "dashboard",
                        onClick = { currentScreen = "dashboard" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                        label = { Text("Mulai", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )

                    // Item 2: Warga
                    NavigationBarItem(
                        selected = currentScreen == "warga",
                        onClick = {
                            initialWargaFilter = "all"
                            currentScreen = "warga"
                        },
                        icon = { Icon(Icons.Default.People, contentDescription = "Warga") },
                        label = { Text("Warga", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )

                    // Item 3: Mutasi
                    if (isAdmin || menuMutasiEnabled) {
                        NavigationBarItem(
                            selected = currentScreen == "mutasi",
                            onClick = { currentScreen = "mutasi" },
                            icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Mutasi") },
                            label = { Text("Mutasi", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Item 4: Pelaporan
                    if (isAdmin || menuPelaporanEnabled) {
                        NavigationBarItem(
                            selected = currentScreen == "pelaporan",
                            onClick = { currentScreen = "pelaporan" },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = "Laporan") },
                            label = { Text("Laporan", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Item 5: Pengumuman / Broadcast (Only shows fully or triggers WhatsApp)
                    if (isAdmin || menuPengumumanEnabled) {
                        NavigationBarItem(
                            selected = currentScreen == "pengumuman",
                            onClick = { currentScreen = "pengumuman" },
                            icon = { Icon(Icons.Default.Campaign, contentDescription = "Pengumuman") },
                            label = { Text("Undangan", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Item 6: Pengaturan (Settings)
                    NavigationBarItem(
                        selected = currentScreen == "pengaturan",
                        onClick = { currentScreen = "pengaturan" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                        label = { Text("Setelan", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { width -> width / 12 }
                        ) + fadeIn(animationSpec = tween(300)))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(250),
                                targetOffsetX = { width -> -width / 12 }
                            ) + fadeOut(animationSpec = tween(250))
                        )
                    },
                    label = "screen_transition",
                    modifier = Modifier.fillMaxSize()
                ) { targetScreen ->
                    when (targetScreen) {
                        "dashboard" -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToWarga = { filter ->
                                initialWargaFilter = filter
                                currentScreen = "warga"
                            },
                            onNavigateToMutasi = { currentScreen = "mutasi" },
                            onNavigateToPelaporan = { currentScreen = "pelaporan" },
                            onNavigateToPengumuman = { currentScreen = "pengumuman" }
                        )
                        "warga" -> WargaScreen(
                            viewModel = viewModel,
                            initialFilter = initialWargaFilter
                        )
                        "mutasi" -> MutasiScreen(
                            viewModel = viewModel
                        )
                        "pelaporan" -> PelaporanScreen(
                            viewModel = viewModel
                        )
                        "pengumuman" -> PengumumanScreen(
                            viewModel = viewModel
                        )
                        "pengaturan" -> PengaturanScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

// Extension to get color surface at elevation smoothly in Material 3
fun ColorScheme.surfaceColorAtElevation(elevation: androidx.compose.ui.unit.Dp): Color {
    return this.surfaceVariant.copy(alpha = 0.5f) // clean approximation
}
