package com.kelasin.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kelasin.app.data.repository.*
import com.kelasin.app.ui.auth.LoginActivity
import com.kelasin.app.ui.theme.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Screen("home",     "Home",     Icons.Filled.Home)
    object Matkul   : Screen("matkul",   "Matkul",   Icons.AutoMirrored.Filled.MenuBook)
    object Tugas    : Screen("tugas",    "Tugas",    Icons.AutoMirrored.Filled.Assignment)
    object Kalender : Screen("kalender", "Kalender", Icons.Filled.CalendarMonth)
    object Materi   : Screen("materi",   "Materi",   Icons.Filled.Folder)
    object Catatan  : Screen("catatan",  "Catatan",  Icons.AutoMirrored.Filled.Notes)
    object Absensi  : Screen("absensi",  "Absensi",  Icons.Filled.CheckCircle)
}

val bottomNavItems = listOf(
    Screen.Matkul, Screen.Materi, Screen.Tugas, Screen.Home, Screen.Kalender, Screen.Catatan, Screen.Absensi
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authRepo = AuthRepository(this)
        val mkRepo   = MataKuliahRepository(this)
        val tugasRepo = TugasRepository(this)
        val materiRepo = MateriRepository(this)
        val catatanRepo = CatatanRepository(this)
        val chatRepo = ChatRepository(this)
        val absensiRepo = AbsensiRepository(this)
        val mhsRepo     = MahasiswaRepository(this)
        val prefsRepo   = UserPreferencesRepository(this)

        setContent {
            val themeMode by prefsRepo.themeMode.collectAsStateWithLifecycle(initialValue = 0)
            val isDark = when(themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            KelasinTheme(darkTheme = isDark) {
                val userId by authRepo.currentUserId.collectAsStateWithLifecycle(initialValue = null)
                val userName by authRepo.currentUserName.collectAsStateWithLifecycle(initialValue = "")
                val userRole by authRepo.currentUserRole.collectAsStateWithLifecycle(initialValue = "MAHASIGMA")

                if (userId == null) return@KelasinTheme

                if (userId.isNullOrBlank()) {
                    LaunchedEffect(Unit) {
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                    return@KelasinTheme
                }

                LaunchedEffect(userId) {
                    if (!userId.isNullOrBlank()) {
                        try {
                            mhsRepo.setCurrentOwner(SharedCloudScope.USER_ID)
                            mkRepo.seedMataKuliah(SharedCloudScope.USER_ID)
                            mhsRepo.seedMahasiswa()
                            catatanRepo.ensureDefaultRooms(SharedCloudScope.USER_ID)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }

                MainScaffold(
                    userId = userId!!,
                    userName = userName,
                    userRole = userRole,
                    authRepo = authRepo,
                    mkRepo = mkRepo,
                    tugasRepo = tugasRepo,
                    materiRepo = materiRepo,
                    catatanRepo = catatanRepo,
                    chatRepo = chatRepo,
                    absensiRepo = absensiRepo,
                    mhsRepo = mhsRepo,
                    prefsRepo = prefsRepo,
                    themeMode = themeMode,
                    onLogout = {
                        lifecycleScope.launch { authRepo.logout() }
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScaffold(
    userId: String,
    userName: String,
    userRole: String,
    authRepo: AuthRepository,
    mkRepo: MataKuliahRepository,
    tugasRepo: TugasRepository,
    materiRepo: MateriRepository,
    catatanRepo: CatatanRepository,
    chatRepo: ChatRepository,
    absensiRepo: AbsensiRepository,
    mhsRepo: MahasiswaRepository,
    prefsRepo: UserPreferencesRepository,
    themeMode: Int,
    onLogout: () -> Unit
) {
    val nonHomeSwipeItems = remember {
        listOf(
            Screen.Matkul,
            Screen.Materi,
            Screen.Tugas,
            Screen.Kalender,
            Screen.Catatan,
            Screen.Absensi
        )
    }
    val pagerState = rememberPagerState(
        initialPage = nonHomeSwipeItems.indexOf(Screen.Tugas),
        pageCount = { nonHomeSwipeItems.size }
    )
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showBottomBar by remember { mutableStateOf(true) }
    val darkTheme = LocalThemeMode.current

    LaunchedEffect(currentScreen) {
        if (currentScreen != Screen.Home) {
            val targetIndex = nonHomeSwipeItems.indexOf(currentScreen)
            if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, currentScreen) {
        if (currentScreen != Screen.Home) {
            val pageScreen = nonHomeSwipeItems[pagerState.currentPage]
            if (currentScreen != pageScreen) currentScreen = pageScreen
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                KelasinBottomBar(
                    currentScreen = currentScreen,
                    darkTheme = darkTheme,
                    onScreenClick = { screen ->
                        if (screen == Screen.Home) {
                            currentScreen = Screen.Home
                        } else {
                            currentScreen = screen
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (currentScreen == Screen.Home) {
            val homeSwipeThreshold = 90f
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var totalDragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDragX += dragAmount
                            },
                            onDragEnd = {
                                val targetScreen = when {
                                    totalDragX >= homeSwipeThreshold -> Screen.Tugas
                                    totalDragX <= -homeSwipeThreshold -> Screen.Kalender
                                    else -> null
                                } ?: return@detectHorizontalDragGestures

                                currentScreen = targetScreen
                            }
                        )
                    }
            ) {
                HomeScreen(
                    userId = userId, userName = userName, userRole = userRole,
                    authRepo = authRepo,
                    tugasRepo = tugasRepo, mkRepo = mkRepo, prefsRepo = prefsRepo, themeMode = themeMode, onLogout = onLogout
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(if (showBottomBar) innerPadding else PaddingValues(0.dp))
                    .fillMaxSize(),
                userScrollEnabled = showBottomBar,
                beyondViewportPageCount = 1
            ) { page ->
                when (nonHomeSwipeItems[page]) {
                    Screen.Matkul -> MataKuliahScreen(
                        userId = userId, userRole = userRole, repo = mkRepo, navController = null
                    )
                    Screen.Tugas -> TugasScreen(
                        userId = userId, userRole = userRole, tugasRepo = tugasRepo,
                        mkRepo = mkRepo, navController = null
                    )
                    Screen.Kalender -> KalenderScreen(
                        userId = userId, tugasRepo = tugasRepo, mkRepo = mkRepo
                    )
                    Screen.Materi -> MateriScreen(
                        userId = userId, userRole = userRole, repo = materiRepo,
                        mkRepo = mkRepo, navController = null
                    )
                    Screen.Catatan -> CatatanScreen(
                        userId = userId, userName = userName, userRole = userRole, repo = catatanRepo,
                        chatRepo = chatRepo,
                        mkRepo = mkRepo,
                        authRepo = authRepo,
                        navController = null,
                        onToggleBottomBar = { showBottomBar = it }
                    )
                    Screen.Absensi -> AbsensiScreen(
                        userId = userId, userRole = userRole, absensiRepo = absensiRepo,
                        mkRepo = mkRepo, mhsRepo = mhsRepo
                    )
                    Screen.Home -> Unit
                }
            }
        }
    }
}

@Composable
private fun KelasinBottomBar(
    currentScreen: Screen,
    darkTheme: Boolean,
    onScreenClick: (Screen) -> Unit
) {
    val view = LocalView.current
    val leftItems = listOf(Screen.Matkul, Screen.Materi, Screen.Tugas)
    val rightItems = listOf(Screen.Kalender, Screen.Catatan, Screen.Absensi)
    val pageBgColor = MaterialTheme.colorScheme.background
    val panelColor = if (darkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.96f) else Color.White
    val unselectedTint = if (darkTheme) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f) else KelasinSubtext
    val homeCircleColor by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF334155) else Color(0xFFE1EAFF),
        animationSpec = tween(durationMillis = 220),
        label = "home-circle-color"
    )
    val homeRingColor by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF4C5A72).copy(alpha = 0.55f) else Color.White,
        animationSpec = tween(durationMillis = 220),
        label = "home-ring-color"
    )
    val homeCutoutSize = 96.dp
    val homeRingSize = 75.dp
    val homeFabSize = 62.dp
    val homeYOffset = -1.dp
    val homeRingYOffset = -7.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 3.dp, start = 10.dp, end = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(top = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                color = panelColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leftItems.forEach { screen ->
                        BottomIconItem(
                            screen = screen,
                            selected = currentScreen == screen,
                            selectedTint = KelasinPrimary,
                            unselectedTint = unselectedTint,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onScreenClick(screen)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(86.dp))

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                color = panelColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rightItems.forEach { screen ->
                        BottomIconItem(
                            screen = screen,
                            selected = currentScreen == screen,
                            selectedTint = KelasinPrimary,
                            unselectedTint = unselectedTint,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onScreenClick(screen)
                            }
                        )
                    }
                }
            }
        }

        // Make the Home area visibly "bolong" like the reference design.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = homeYOffset)
                .size(homeCutoutSize)
                .clip(CircleShape)
                .background(pageBgColor)
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = homeRingYOffset)
                .size(homeRingSize),
            shape = CircleShape,
            color = homeRingColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {}

        val isHomeSelected = currentScreen == Screen.Home
        val homeScale by animateFloatAsState(
            targetValue = if (isHomeSelected) 1.08f else 1f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "home-fab-scale"
        )

        FloatingActionButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onScreenClick(Screen.Home)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = homeYOffset)
                .size(homeFabSize),
            shape = CircleShape,
            containerColor = homeCircleColor,
            contentColor = KelasinPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 1.dp
            )
        ) {
            Icon(
                imageVector = Screen.Home.icon,
                contentDescription = Screen.Home.label,
                modifier = Modifier.size(29.dp).scale(homeScale)
            )
        }
    }
}

@Composable
private fun BottomIconItem(
    screen: Screen,
    selected: Boolean,
    selectedTint: Color,
    unselectedTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val iconTint by animateColorAsState(
        targetValue = if (selected) selectedTint else unselectedTint,
        animationSpec = tween(durationMillis = 180),
        label = "bottom-icon-tint-${screen.route}"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val iconScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.90f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "bottom-icon-scale-${screen.route}"
    )
    val pressCircle by animateColorAsState(
        targetValue = when {
            pressed -> selectedTint.copy(alpha = 0.20f)
            selected -> selectedTint.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 160),
        label = "bottom-icon-press-bg-${screen.route}"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(pressCircle)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.label,
                tint = iconTint,
                modifier = Modifier.size(22.dp).scale(iconScale)
            )
        }
    }
}
