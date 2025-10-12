package com.sweetapps.nosmoketimer.feature.start

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.core.content.edit
import com.sweetapps.nosmoketimer.core.ui.AppElevation
import com.sweetapps.nosmoketimer.core.ui.BaseActivity
import com.sweetapps.nosmoketimer.core.ui.StandardScreenWithBottomButton
import com.sweetapps.nosmoketimer.core.ui.components.AppUpdateDialog
import com.sweetapps.nosmoketimer.core.util.AppUpdateManager
import com.sweetapps.nosmoketimer.core.util.Constants
import com.sweetapps.nosmoketimer.feature.run.RunActivity
import com.google.android.play.core.install.model.AppUpdateType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.material3.SnackbarResult
import com.sweetapps.nosmoketimer.R
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.max
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
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

        setContent {
            // 첫 실행 화면에서는 edge-to-edge 비활성화하여 상태바를 OS가 분리 렌더링
            BaseScreen(applyBottomInsets = false, applySystemBars = false) {
                StartScreenWithUpdate(appUpdateManager)
            }
        }
    }

    override fun getScreenTitle(): String = "금연 설정"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreenWithUpdate(appUpdateManager: AppUpdateManager) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 업데이트 다이얼로그 상태
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.google.android.play.core.appupdate.AppUpdateInfo?>(null) }
    var availableVersionName by remember { mutableStateOf("") }

    // 앱 시작 시 업데이트 확인
    LaunchedEffect(Unit) {
        scope.launch {
            appUpdateManager.checkForUpdate(
                forceCheck = true,
                onUpdateAvailable = { info ->
                    updateInfo = info
                    availableVersionName = info.availableVersionCode().toString()
                    showUpdateDialog = true
                },
                onNoUpdate = {
                    // 업데이트 없음
                }
            )
        }
    }

    // 업데이트 다운로드 완료 리스너: 사용자 액션(다시 시작) 시에만 설치 완료
    LaunchedEffect(Unit) {
        appUpdateManager.registerInstallStateListener {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "업데이트가 다운로드되었습니다. 다시 시작하여 설치하세요.",
                    actionLabel = "다시 시작",
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    appUpdateManager.completeFlexibleUpdate()
                }
            }
        }
    }
    // 화면 소멸 시 리스너 정리
    DisposableEffect(Unit) {
        onDispose {
            appUpdateManager.unregisterInstallStateListener()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StartScreen()

        // 업데이트 다이얼로그
        AppUpdateDialog(
            isVisible = showUpdateDialog,
            versionName = availableVersionName,
            updateMessage = "새로운 기능과 개선사항이 포함되어 있습니다.",
            onUpdateClick = {
                updateInfo?.let { info ->
                    val immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    if (appUpdateManager.isMaxPostponeReached() && immediateAllowed) {
                        appUpdateManager.startImmediateUpdate(info)
                    } else {
                        appUpdateManager.startFlexibleUpdate(info)
                    }
                }
                showUpdateDialog = false
            },
            onDismiss = {
                appUpdateManager.markUserPostpone()
                showUpdateDialog = false
            },
            canDismiss = !appUpdateManager.isMaxPostponeReached()
        )

        // 스낵바
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen() {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)

    if (startTime != 0L && !timerCompleted) {
        LaunchedEffect(Unit) {
            context.startActivity(Intent(context, RunActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
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
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD), // down from CARD_HIGH
                border = BorderStroke(1.dp, colorResource(id = R.color.color_border_light))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "목표 기간 설정",
                        style = MaterialTheme.typography.titleLarge,
                        color = colorResource(id = R.color.color_title_primary),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Card(
                            modifier = Modifier.width(100.dp).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_bg_card_light)),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
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
                                        val selection = if (isTextSelected) TextRange(finalText.length) else TextRange(finalText.length)
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
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
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
                        text = "금연할 목표 기간을 입력해주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(id = R.color.color_hint_gray),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // 카드 아래 중앙 여백을 파란 금연 아이콘으로 채움
            Spacer(modifier = Modifier.height(16.dp))
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // 기존 대비 2.0배 확대
                val iconSize = (maxWidth * 0.4f).coerceIn(120.dp, 320.dp) * 2.0f
                // 스플래시 인셋(1/6)을 픽셀 그리드에 반올림 정렬
                val density = LocalDensity.current
                val insetPadding = with(density) {
                    val iconPx = iconSize.toPx()
                    val padPxRounded = (iconPx / 8f).roundToInt()
                    padPxRounded.toDp()
                }
                Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                    // 벡터를 안전하게 비트맵으로 래스터라이즈 후 최근접 샘플링 적용
                    val contentSizeDp = iconSize - insetPadding * 2
                    val (contentW, contentH) = with(density) {
                        val w = max(1, contentSizeDp.toPx().roundToInt())
                        val h = w // 정사각 아이콘
                        w to h
                    }
                    val drawable = remember {
                        ResourcesCompat.getDrawable(context.resources, R.drawable.ic_launcher_foreground, context.theme)
                    }
                    val bitmap = remember(contentW, contentH, drawable) {
                        drawable?.toBitmap(contentW, contentH, Bitmap.Config.ARGB_8888)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "금연 아이콘",
                            modifier = Modifier.fillMaxSize().padding(insetPadding),
                            filterQuality = FilterQuality.None,
                            alpha = 0.3f // 더 흐리게
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
