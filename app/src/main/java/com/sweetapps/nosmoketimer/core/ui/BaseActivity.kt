package com.sweetapps.nosmoketimer.core.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sweetapps.nosmoketimer.core.ui.theme.AlcoholicTimerTheme
import com.sweetapps.nosmoketimer.feature.level.LevelActivity
import com.sweetapps.nosmoketimer.feature.profile.NicknameEditActivity
import com.sweetapps.nosmoketimer.feature.run.RunActivity
import com.sweetapps.nosmoketimer.feature.settings.SettingsActivity
import com.sweetapps.nosmoketimer.feature.start.StartActivity
import kotlinx.coroutines.launch

abstract class BaseActivity : ComponentActivity() {
    private var nicknameState = mutableStateOf("")

    // Ensure declaration before first usage
    private fun getNickname(): String {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        return sharedPref.getString("nickname", "끽연이1") ?: "끽연이1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install SplashScreen for Android 12+
        val splashScreen: SplashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { provider -> provider.remove() }
        super.onCreate(savedInstanceState)
        nicknameState.value = getNickname()
    }

    override fun onResume() {
        super.onResume()
        nicknameState.value = getNickname()
    }

    // Returns the drawer menu title that matches current screen, or null if none
    private fun currentDrawerSelection(): String? = when (this) {
        is RunActivity, is StartActivity,
        is com.sweetapps.nosmoketimer.feature.run.QuitActivity -> "금연"
        is com.sweetapps.nosmoketimer.feature.records.RecordsActivity,
        is com.sweetapps.nosmoketimer.feature.records.AllRecordsActivity,
        is com.sweetapps.nosmoketimer.feature.detail.DetailActivity -> "기록"
        is LevelActivity -> "레벨"
        is SettingsActivity -> "설정"
        is com.sweetapps.nosmoketimer.feature.about.AboutActivity,
        is com.sweetapps.nosmoketimer.feature.about.AboutLicensesActivity -> "앱 정보"
        else -> null
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BaseScreen(
        applyBottomInsets: Boolean = true,
        applySystemBars: Boolean = true,
        showBackButton: Boolean = false,
        onBackClick: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        AlcoholicTimerTheme(darkTheme = false, applySystemBars = applySystemBars) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val currentNickname by nicknameState

            val blurRadius by animateFloatAsState(
                targetValue = if (drawerState.targetValue == DrawerValue.Open) 8f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "blur"
            )

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.fillMaxWidth(0.8f),
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
                    }
                }
            }
        }
    }

    private fun handleMenuSelection(menuItem: String) {
        when (menuItem) {
            "금연" -> {
                val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
                val startTime = sharedPref.getLong("start_time", 0L)
                if (startTime > 0) {
                    if (this !is RunActivity) navigateToActivity(RunActivity::class.java)
                } else {
                    if (this !is StartActivity) navigateToActivity(StartActivity::class.java)
                }
            }
            "기록" -> if (this !is com.sweetapps.nosmoketimer.feature.records.RecordsActivity) {
                navigateToActivity(com.sweetapps.nosmoketimer.feature.records.RecordsActivity::class.java)
            }
            "레벨" -> if (this !is LevelActivity) navigateToActivity(LevelActivity::class.java)
            "설정" -> if (this !is SettingsActivity) navigateToActivity(SettingsActivity::class.java)
            "앱 정보" -> if (this !is com.sweetapps.nosmoketimer.feature.about.AboutActivity) {
                navigateToActivity(com.sweetapps.nosmoketimer.feature.about.AboutActivity::class.java)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    @Suppress("DEPRECATION")
    private fun navigateToNicknameEdit() {
        val intent = Intent(this, NicknameEditActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
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
        "금연" to Icons.Filled.PlayArrow,
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
