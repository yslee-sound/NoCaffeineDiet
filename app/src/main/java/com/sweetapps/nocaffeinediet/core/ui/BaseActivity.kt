package com.sweetapps.nocaffeinediet.core.ui

import android.content.Intent
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sweetapps.nocaffeinediet.core.ui.theme.AlcoholicTimerTheme
import com.sweetapps.nocaffeinediet.feature.level.LevelActivity
import com.sweetapps.nocaffeinediet.feature.profile.NicknameEditActivity
import com.sweetapps.nocaffeinediet.feature.run.RunActivity
import com.sweetapps.nocaffeinediet.feature.run.QuitActivity
import com.sweetapps.nocaffeinediet.feature.settings.SettingsActivity
import com.sweetapps.nocaffeinediet.feature.start.StartActivity
import com.sweetapps.nocaffeinediet.feature.records.RecordsActivity
import com.sweetapps.nocaffeinediet.feature.records.AllRecordsActivity
import com.sweetapps.nocaffeinediet.feature.detail.DetailActivity
import com.sweetapps.nocaffeinediet.feature.about.AboutActivity
import com.sweetapps.nocaffeinediet.feature.about.AboutLicensesActivity
import kotlinx.coroutines.launch
import com.sweetapps.nocaffeinediet.core.util.Constants.DEFAULT_NICKNAME
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.ui.viewinterop.AndroidView
import android.view.MotionEvent

abstract class BaseActivity : ComponentActivity() {
    private var nicknameState = mutableStateOf("")

    // Ensure declaration before first usage
    private fun getNickname(): String {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        return sharedPref.getString("nickname", DEFAULT_NICKNAME) ?: DEFAULT_NICKNAME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android SplashScreen은 런처(시작) 액티비티에서만 설치
        if (this is StartActivity) {
            val splashScreen: SplashScreen = installSplashScreen()
            splashScreen.setOnExitAnimationListener { provider -> provider.remove() }
        }
        super.onCreate(savedInstanceState)
        nicknameState.value = getNickname()
    }

    override fun onResume() {
        super.onResume()
        nicknameState.value = getNickname()
    }

    // Returns the drawer menu title that matches current screen, or null if none
    private fun currentDrawerSelection(): String? = when (javaClass) {
        RunActivity::class.java, StartActivity::class.java, QuitActivity::class.java -> "노카페인"
        RecordsActivity::class.java, AllRecordsActivity::class.java, DetailActivity::class.java -> "기록"
        LevelActivity::class.java -> "레벨"
        SettingsActivity::class.java -> "설정"
        AboutActivity::class.java, AboutLicensesActivity::class.java -> "앱 정보"
        else -> null
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BaseScreen(
        applyBottomInsets: Boolean = false,
        applySystemBars: Boolean = true,
        showBackButton: Boolean = false,
        onBackClick: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        AlcoholicTimerTheme(darkTheme = false, applySystemBars = applySystemBars) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val currentNickname by nicknameState
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            val blurRadius by animateFloatAsState(
                targetValue = if (drawerState.targetValue == DrawerValue.Open) 8f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "blur"
            )

            // 드로어 입력 가드(애니메이션 중/닫힘 직후 그레이스 타임)
            var drawerInputGuardActive by remember { mutableStateOf(false) }
            val drawerGuardGraceMs = 200L
            LaunchedEffect(drawerState) {
                snapshotFlow { Triple(drawerState.isAnimationRunning, drawerState.currentValue, drawerState.targetValue) }
                    .collect { (isAnimating, current, target) ->
                        if (isAnimating || target != DrawerValue.Closed || current != DrawerValue.Closed) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            drawerInputGuardActive = true
                        } else {
                            drawerInputGuardActive = true
                            delay(drawerGuardGraceMs)
                            drawerInputGuardActive = false
                        }
                    }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    ) {
                        DrawerMenu(
                            nickname = currentNickname,
                            selectedItem = currentDrawerSelection(),
                            onNicknameClick = {
                                scope.launch {
                                    drawerState.close()
                                    var navigated = false
                                    snapshotFlow { drawerState.isAnimationRunning }
                                        .collect { isAnimating ->
                                            if (!isAnimating && drawerState.currentValue == DrawerValue.Closed && !navigated) {
                                                navigated = true
                                                navigateToNicknameEdit()
                                                return@collect
                                            }
                                        }
                                }
                            },
                            onItemSelected = { menuItem ->
                                scope.launch {
                                    drawerState.close()
                                    snapshotFlow { drawerState.isAnimationRunning }
                                        .collect { isAnimating ->
                                            if (!isAnimating && drawerState.currentValue == DrawerValue.Closed) {
                                                handleMenuSelection(menuItem)
                                                return@collect
                                            }
                                        }
                                }
                            }
                        )
                    }
                }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    topBar = {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (applySystemBars) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column {
                                TopAppBar(
                                    title = {
                                        CompositionLocalProvider(
                                            LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.2f)
                                        ) {
                                            Text(
                                                text = getScreenTitle(),
                                                color = Color(0xFF2C3E50),
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        titleContentColor = Color(0xFF2C3E50),
                                        navigationIconContentColor = Color(0xFF2C3E50),
                                        actionIconContentColor = Color(0xFF2C3E50)
                                    ),
                                    navigationIcon = {
                                        Surface(
                                            modifier = Modifier.padding(8.dp).size(48.dp),
                                            shape = CircleShape,
                                            color = Color(0xFFF8F9FA),
                                            shadowElevation = 2.dp
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (showBackButton) {
                                                        onBackClick?.invoke() ?: run { this@BaseActivity.onBackPressedDispatcher.onBackPressed() }
                                                    } else {
                                                        // 드로어 열기 전에 포커스/키보드 정리
                                                        focusManager.clearFocus(force = true)
                                                        keyboardController?.hide()
                                                        scope.launch { drawerState.open() }
                                                    }
                                                }
                                            ) {
                                                if (showBackButton) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "뒤로가기",
                                                        tint = Color(0xFF2C3E50),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Filled.Menu,
                                                        contentDescription = "메뉴",
                                                        tint = Color(0xFF2C3E50),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                                // Global subtle divider under app bar
                                HorizontalDivider(
                                    thickness = 1.5.dp,
                                    color = Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                        val insetModifier = if (applyBottomInsets) {
                            Modifier.windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                            )
                        } else {
                            Modifier.windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .then(insetModifier)
                                .blur(radius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blurRadius.dp else 0.dp)
                        ) { content() }

                        // 드로어 입력 가드: 애니메이션 중 및 닫힘 직후 포인터 이벤트 전체 소비 + 접근성 포커스 차단
                        if (drawerInputGuardActive) {
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clearAndSetSemantics { },
                                factory = { context ->
                                    android.view.View(context).apply {
                                        isClickable = true
                                        isFocusable = true
                                        setOnTouchListener { v, event ->
                                            if (event.action == MotionEvent.ACTION_UP) {
                                                v.performClick()
                                            }
                                            true
                                        }
                                        setOnClickListener { /* consume */ }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleMenuSelection(menuItem: String) {
        when (menuItem) {
            "노카페인" -> {
                val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
                val startTime = sharedPref.getLong("start_time", 0L)
                if (startTime > 0) {
                    if (this !is RunActivity) navigateToActivity(RunActivity::class.java)
                } else {
                    if (this !is StartActivity) navigateToActivity(StartActivity::class.java)
                }
            }
            "기록" -> if (this !is RecordsActivity) {
                navigateToActivity(RecordsActivity::class.java)
            }
            "레벨" -> if (this !is LevelActivity) navigateToActivity(LevelActivity::class.java)
            "설정" -> if (this !is SettingsActivity) navigateToActivity(SettingsActivity::class.java)
            "앱 정보" -> if (this !is AboutActivity) {
                navigateToActivity(AboutActivity::class.java)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        // 문서 방안 2 적용: 이미 스택에 있는 대상이면 위의 액티비티 제거 및 재사용
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    @Suppress("DEPRECATION")
    private fun navigateToNicknameEdit() {
        val intent = Intent(this, NicknameEditActivity::class.java)
        startActivity(intent)
    }

    // 문서 스펙: 공통 메인 홈 복귀 로직
    @Suppress("unused", "DEPRECATION")
    protected fun navigateToMainHome() {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val isRunning = startTime > 0
        val targetActivity = if (isRunning) RunActivity::class.java else StartActivity::class.java

        // 이미 메인 홈이면 아무것도 하지 않음
        if ((isRunning && this is RunActivity) || (!isRunning && this is StartActivity)) {
            return
        }

        val intent = Intent(this, targetActivity).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (targetActivity == StartActivity::class.java) {
                putExtra("skip_splash", true)
            }
        }
        startActivity(intent)
        finish()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        when (this) {
            is RecordsActivity,
            is LevelActivity,
            is SettingsActivity,
            is AboutActivity -> {
                navigateToMainHome()
            }
            is RunActivity -> {
                // 러닝 화면: 뒤로가기 시 종료 확인 화면으로 이동
                val intent = Intent(this, QuitActivity::class.java)
                startActivity(intent)
            }
            else -> super.onBackPressed()
        }
    }

    protected abstract fun getScreenTitle(): String
}

@Composable
fun DrawerMenu(
    nickname: String,
    selectedItem: String?,
    onNicknameClick: () -> Unit,
    onItemSelected: (String) -> Unit
) {
    val menuItems = listOf(
        "노카페인" to Icons.Filled.PlayArrow,
        "기록" to Icons.AutoMirrored.Filled.List,
        "레벨" to Icons.Filled.Star
    )
    val settingsItems = listOf(
        "설정" to Icons.Filled.Settings,
        "앱 정보" to Icons.Filled.Info
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onNicknameClick() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "아바타",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = nickname,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "프로필 편집",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp
        )
        Text(
            text = "메뉴",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        menuItems.forEach { (title, icon) ->
            val isSelected = title == selectedItem
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) AppColors.SurfaceOverlaySoft else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp
        )
        Text(
            text = "설정",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        settingsItems.forEach { (title, icon) ->
            val isSelected = title == selectedItem
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onItemSelected(title) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) AppColors.SurfaceOverlaySoft else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
