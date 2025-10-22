package com.sweetapps.nocaffeinediet.feature.start

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sweetapps.nocaffeinediet.BuildConfig
import com.sweetapps.nocaffeinediet.R
import com.sweetapps.nocaffeinediet.core.ui.AppBorder
import com.sweetapps.nocaffeinediet.core.ui.AppElevation
import com.sweetapps.nocaffeinediet.core.ui.BaseActivity
import com.sweetapps.nocaffeinediet.core.ui.StandardScreenWithBottomButton
import com.sweetapps.nocaffeinediet.core.ui.components.AppUpdateDialog
import com.sweetapps.nocaffeinediet.core.util.AppUpdateManager
import com.sweetapps.nocaffeinediet.core.util.Constants
import com.sweetapps.nocaffeinediet.core.util.UpdateVersionMapper
import com.sweetapps.nocaffeinediet.feature.run.RunActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import android.graphics.Bitmap

class StartActivity : BaseActivity() {
    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Constants.initializeUserSettings(this)
        Constants.ensureInstallMarkerAndResetIfReinstalled(this)

        // 첫 프레임부터 상태바 표시 및 어두운 아이콘 적용 (Splash -> 첫 화면 전환 시 깜빡임 방지)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        controller.show(WindowInsetsCompat.Type.statusBars())
        controller.show(WindowInsetsCompat.Type.navigationBars())

        // In-App Update 초기화
        appUpdateManager = AppUpdateManager(this)

        val demoFromIntent = intent?.getBooleanExtra("demo_update_ui", false) == true

        setContent {
            // 첫 실행 화면에서는 edge-to-edge 비활성화하여 상태바를 OS가 분리 렌더링
            BaseScreen(applyBottomInsets = false, applySystemBars = false) {
                StartScreenWithUpdate(appUpdateManager, demoFromIntent)
            }
        }
    }

    override fun getScreenTitle(): String = "노카페인 설정"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreenWithUpdate(appUpdateManager: AppUpdateManager, demoFromIntent: Boolean = false) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 업데이트 UI 상태
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.google.android.play.core.appupdate.AppUpdateInfo?>(null) }
    var availableVersionName by remember { mutableStateOf("") }
    var showOverlay by remember { mutableStateOf(false) }

    // 데모 상태: 여러 번 실행 가능하도록 트리거 카운터 사용
    var demoTrigger by remember { mutableStateOf(0) }
    var isDemoDialog by remember { mutableStateOf(false) }

    // 인텐트 데모: 진입 직후 1회 트리거
    LaunchedEffect(Unit) {
        if (BuildConfig.DEBUG && demoFromIntent) demoTrigger++
    }

    // 데모 트리거마다 시퀀스 실행
    LaunchedEffect(demoTrigger) {
        if (!BuildConfig.DEBUG) return@LaunchedEffect
        if (demoTrigger == 0) return@LaunchedEffect
        // 기존 다이얼로그 닫고 데모 시작
        showUpdateDialog = false
        isCheckingUpdate = true
        showOverlay = true
        delay(600)
        val fake = 2025101001
        availableVersionName = UpdateVersionMapper.toVersionName(fake) ?: fake.toString()
        isDemoDialog = true
        showOverlay = false
        showUpdateDialog = true
    }

    // 실제 업데이트 확인 (인텐트 데모 진입 시에는 생략)
    LaunchedEffect(Unit) {
        if (!(BuildConfig.DEBUG && demoFromIntent)) {
            isCheckingUpdate = true
            // 300ms 지연 후에도 진행 중이면 오버레이 표시
            scope.launch {
                delay(300)
                if (isCheckingUpdate) showOverlay = true
            }
            appUpdateManager.checkForUpdate(
                forceCheck = false,
                onUpdateAvailable = { info ->
                    updateInfo = info
                    val code = info.availableVersionCode()
                    availableVersionName = UpdateVersionMapper.toVersionName(code) ?: code.toString()
                    isDemoDialog = false
                    showOverlay = false
                    showUpdateDialog = true
                },
                onNoUpdate = {
                    showOverlay = false
                    isCheckingUpdate = false
                }
            )
        }
    }

    // Flexible 다운로드 완료 → 스낵바 ‘다시 시작’ 제공
    DisposableEffect(Unit) {
        appUpdateManager.registerInstallStateListener {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "업데이트가 준비되었습니다",
                    actionLabel = "다시 시작",
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    appUpdateManager.completeFlexibleUpdate()
                }
            }
        }
        onDispose { appUpdateManager.unregisterInstallStateListener() }
    }

    val gateNavigation = isCheckingUpdate || showUpdateDialog

    Box(modifier = Modifier.fillMaxSize()) {
        StartScreen(
            gateNavigation = gateNavigation,
            onTitleLongPress = { if (BuildConfig.DEBUG) demoTrigger++ }
        )

        // 업데이트 다이얼로그
        AppUpdateDialog(
            isVisible = showUpdateDialog,
            versionName = availableVersionName,
            updateMessage = "새로운 기능과 개선사항이 포함되어 있습니다.",
            onUpdateClick = {
                showUpdateDialog = false
                if (isDemoDialog) {
                    scope.launch { snackbarHostState.showSnackbar("데모: 업데이트를 시작하지 않습니다") }
                    isDemoDialog = false
                    isCheckingUpdate = false
                    return@AppUpdateDialog
                }
                updateInfo?.let { info ->
                    val allowImmediate = appUpdateManager.isMaxPostponeReached()
                    if (allowImmediate && appUpdateManager.isImmediateAllowed(info)) {
                        appUpdateManager.startImmediateUpdate(info)
                    } else {
                        appUpdateManager.startFlexibleUpdate(info)
                    }
                }
                isCheckingUpdate = false
            },
            onDismiss = {
                if (!isDemoDialog) {
                    appUpdateManager.markUserPostpone()
                }
                showUpdateDialog = false
                isDemoDialog = false
                isCheckingUpdate = false
            },
            canDismiss = !appUpdateManager.isMaxPostponeReached() || isDemoDialog
        )

        // 스낵바
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 300ms 지연 후에만 보이는 반투명 오버레이 (터치 차단)
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = stringResource(R.string.checking_update))
                }
            }
        }
    }
}

@Composable
fun StartScreen(gateNavigation: Boolean = false, onTitleLongPress: () -> Unit = {}) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)

    if (!gateNavigation && startTime != 0L && !timerCompleted) {
        LaunchedEffect(Unit) {
            context.startActivity(
                Intent(context, RunActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
        return
    }

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = "30", selection = TextRange(0, 2)))
    }
    val isValid by remember { derivedStateOf { textFieldValue.text.toFloatOrNull()?.let { it > 0 } ?: false } }
    var isTextSelected by remember { mutableStateOf(true) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(50)
            val len = textFieldValue.text.length
            textFieldValue = textFieldValue.copy(selection = TextRange(0, len))
            isTextSelected = true
        }
    }

    StandardScreenWithBottomButton(
        topContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "목표 기간 설정",
                        style = MaterialTheme.typography.titleLarge,
                        color = colorResource(id = R.color.color_title_primary),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 24.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = onTitleLongPress
                            )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .width(100.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_bg_card_light)),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = textFieldValue,
                                    onValueChange = { newValue ->
                                        val filtered = newValue.text.filter { it.isDigit() || it == '.' }
                                        val dots = filtered.count { it == '.' }
                                        val finalFiltered = if (dots <= 1) filtered else textFieldValue.text
                                        val finalText = when {
                                            finalFiltered.isEmpty() -> "0"
                                            finalFiltered.length > 1 && finalFiltered.startsWith("0") && !finalFiltered.startsWith("0.") -> finalFiltered.substring(1)
                                            else -> finalFiltered
                                        }
                                        val selection = TextRange(finalText.length)
                                        textFieldValue = TextFieldValue(text = finalText, selection = selection)
                                        isTextSelected = false
                                    },
                                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                                        color = colorResource(id = R.color.color_indicator_days),
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    cursorBrush = SolidColor(colorResource(id = R.color.color_indicator_days)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { isFocused = it.isFocused }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "일",
                            style = MaterialTheme.typography.titleLarge,
                            color = colorResource(id = R.color.color_indicator_label_gray)
                        )
                    }
                    Text(
                        text = "노카페인 목표 기간을 입력해주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(id = R.color.color_hint_gray),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val iconSize = (maxWidth * 0.4f).coerceIn(120.dp, 320.dp) * 2.0f
                val density = LocalDensity.current
                val insetPadding = with(density) {
                    val iconPx = iconSize.toPx()
                    val padPxRounded = (iconPx / 8f).roundToInt()
                    padPxRounded.toDp()
                }
                Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                    val contentSizeDp = iconSize - insetPadding * 2
                    val (contentW, contentH) = with(density) {
                        val w = max(1, contentSizeDp.toPx().roundToInt())
                        val h = w
                        w to h
                    }
                    val drawable = remember {
                        ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.ic_launcher_foreground,
                            context.theme
                        )
                    }
                    val bitmap = remember(contentW, contentH, drawable) {
                        drawable?.toBitmap(contentW, contentH, Bitmap.Config.ARGB_8888)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "노카페인 아이콘",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(insetPadding),
                            filterQuality = FilterQuality.None,
                            alpha = 0.3f
                        )
                    }
                }
            }
        },
        bottomButton = {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                ModernStartButton(
                    isEnabled = isValid,
                    onStart = {
                        val targetTime = textFieldValue.text.toFloatOrNull() ?: 0f
                        if (targetTime > 0f) {
                            val formatted = String.format(Locale.US, "%.6f", targetTime).toFloat()
                            sharedPref.edit {
                                putFloat("target_days", formatted)
                                putLong("start_time", System.currentTimeMillis())
                                putBoolean("timer_completed", false)
                            }
                            context.startActivity(Intent(context, RunActivity::class.java))
                        }
                    }
                )
            }
        },
        imePaddingEnabled = false
    )
}

@Composable
fun ModernStartButton(isEnabled: Boolean, onStart: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = { if (isEnabled) onStart() },
        modifier = modifier.size(96.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) colorResource(id = R.color.color_progress_primary) else colorResource(id = R.color.color_button_disabled)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) AppElevation.CARD_HIGH else AppElevation.CARD)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, contentDescription = "시작", tint = Color.White, modifier = Modifier.size(48.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() { StartScreen() }
