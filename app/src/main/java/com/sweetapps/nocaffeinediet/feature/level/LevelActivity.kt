package com.sweetapps.nocaffeinediet.feature.level

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sweetapps.nocaffeinediet.core.ui.AppElevation
import com.sweetapps.nocaffeinediet.core.ui.AppBorder
import com.sweetapps.nocaffeinediet.core.ui.BaseActivity
import com.sweetapps.nocaffeinediet.core.util.Constants
import com.sweetapps.nocaffeinediet.core.data.RecordsDataLoader
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import com.sweetapps.nocaffeinediet.R
import java.util.Locale

class LevelActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BackHandler(enabled = true) { navigateToMainHome() }
            BaseScreen(applyBottomInsets = false) { LevelScreen() }
        }
    }

    override fun getScreenTitle(): String = "노카페인 레벨"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen() {
    val context = LocalContext.current

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    val startTime = sharedPref.getLong(Constants.PREF_START_TIME, 0L)

    val currentElapsedTime = if (startTime > 0) currentTime - startTime else 0L

    val pastRecords = RecordsDataLoader.loadSobrietyRecords(context)
    val totalPastDuration = pastRecords.sumOf { record -> (record.endTime - record.startTime) }

    val totalElapsedTime = totalPastDuration + currentElapsedTime
    // 추가: 총 경과 일수(소수점 포함) 계산
    val totalElapsedDaysFloat = totalElapsedTime / Constants.DAY_IN_MILLIS.toFloat()

    val levelDays = Constants.calculateLevelDays(totalElapsedTime)
    val currentLevel = LevelDefinitions.getLevelInfo(levelDays)

    // backgroundBrush 제거 (BaseScreen 배경 사용)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 변경: float 경과 일수 전달
        CurrentLevelCard(currentLevel = currentLevel, currentDays = levelDays, elapsedDaysFloat = totalElapsedDaysFloat, startTime = startTime)
        LevelListCard(currentLevel = currentLevel, currentDays = levelDays)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CurrentLevelCard(
    currentLevel: LevelDefinitions.LevelInfo,
    currentDays: Int,
    elapsedDaysFloat: Float,
    startTime: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LevelUiTokens.CardPaddingH, vertical = LevelUiTokens.CardPaddingV),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(colors = listOf(currentLevel.color.copy(alpha = 0.8f), currentLevel.color))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = currentLevel.name.take(2), style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentLevel.name,
                style = MaterialTheme.typography.headlineLarge.copy(color = currentLevel.color),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(text = "$currentDays", style = MaterialTheme.typography.headlineLarge.copy(color = Color(0xFF1976D2)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "일차", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, color = Color(0xFF666666)))
            }

            val nextLevel = getNextLevel(currentLevel)
            if (nextLevel != null) {
                Spacer(modifier = Modifier.height(24.dp))

                // 변경: 정수 일수 대신 실수 일수 기반 진행률 계산
                val progress = if (nextLevel.start > currentLevel.start) {
                    val progressInLevel = elapsedDaysFloat - currentLevel.start
                    val totalNeeded = (nextLevel.start - currentLevel.start).toFloat()
                    if (totalNeeded > 0f) (progressInLevel / totalNeeded).coerceIn(0f, 1f) else 0f
                } else 0f

                // 추가: 남은 시간(일+시간) 문자열 생성
                val remainingDaysFloat = (nextLevel.start - elapsedDaysFloat).coerceAtLeast(0f)
                val remainingDaysInt = kotlin.math.floor(remainingDaysFloat.toDouble()).toInt()
                val remainingHoursInt = kotlin.math.floor(((remainingDaysFloat - remainingDaysInt) * 24f).toDouble()).toInt()
                val remainingText = when {
                    remainingDaysInt > 0 && remainingHoursInt > 0 -> "${remainingDaysInt}일 ${remainingHoursInt}시간 남음"
                    remainingDaysInt > 0 -> "${remainingDaysInt}일 남음"
                    remainingHoursInt > 0 -> "${remainingHoursInt}시간 남음"
                    else -> "곧 레벨업"
                }

                ProgressToNextLevel(
                    currentLevel = currentLevel,
                    nextLevel = nextLevel,
                    progress = progress,
                    remainingDays = (nextLevel.start - currentDays).coerceAtLeast(0),
                    remainingText = remainingText,
                    isSobrietyActive = startTime > 0
                )
            }
        }
    }
}

@Composable
private fun ProgressToNextLevel(
    currentLevel: LevelDefinitions.LevelInfo,
    nextLevel: LevelDefinitions.LevelInfo,
    progress: Float,
    remainingDays: Int,
    remainingText: String,
    isSobrietyActive: Boolean
) {
    var blink by remember { mutableStateOf(true) }

    LaunchedEffect(remainingDays, isSobrietyActive) {
        if (remainingDays > 0 && isSobrietyActive) {
            while (true) {
                delay(1000)
                blink = !blink
            }
        } else {
            blink = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (blink) 1f else 0.3f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "level_blink"
    )

    val percentText = String.format(Locale.getDefault(), "%.1f%%", (progress * 100f).coerceIn(0f, 100f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // .graphicsLayer(alpha = alpha) // 전체 깜빡임 제거
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "다음 레벨까지",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(LevelUiTokens.IndicatorDot)
                    .clip(CircleShape)
                    .background(currentLevel.color.copy(alpha = alpha)) // 러닝 화면과 동일한 방식으로 알파 적용
            )
        }
        Spacer(Modifier.height(8.dp))

        // 진행 바(트랙+채움) — 바 유효 폭을 줄이기 위해 추가 가로 패딩 적용
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = LevelUiTokens.ProgressInnerHPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LevelUiTokens.ProgressHeight)
                    .clip(RoundedCornerShape(LevelUiTokens.ProgressCorner))
                    .background(color = Color(0xFFE0E0E0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(currentLevel.color)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 하단 텍스트도 바 폭과 정렬되도록 동일 패딩 적용
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = LevelUiTokens.ProgressInnerHPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = percentText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = remainingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LevelListCard(currentLevel: LevelDefinitions.LevelInfo, currentDays: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.ZERO),
        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "전체 레벨",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF333333)),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LevelDefinitions.levels.forEach { level ->
                LevelItem(
                    level = level,
                    isCurrent = level == currentLevel,
                    isAchieved = currentDays >= level.start,
                    isNext = level == getNextLevel(currentLevel)
                )

                if (level != LevelDefinitions.levels.last()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun LevelItem(
    level: LevelDefinitions.LevelInfo,
    isCurrent: Boolean,
    isAchieved: Boolean,
    isNext: Boolean
) {
    // 모든 상태에서 그림자 제거
    val itemElevation = AppElevation.ZERO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrent -> level.color.copy(alpha = 0.1f)
                isAchieved -> level.color.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            isCurrent -> BorderStroke(1.5.dp, level.color)
            isAchieved -> BorderStroke(1.dp, level.color.copy(alpha = 0.6f))
            else -> BorderStroke(0.75.dp, colorResource(id = R.color.color_border_light))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = itemElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isAchieved) level.color else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.name.take(1),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = if (isAchieved) Color.White else Color(0xFF757575))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.name,
                    style = (if (isCurrent) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium)
                        .copy(color = if (isAchieved) level.color else Color(0xFF757575))
                )

                val rangeText = if (level.end == Int.MAX_VALUE) "${level.start}일 이상" else "${level.start}~${level.end}일"
                Text(text = rangeText, style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF666666)))
            }

            if (isCurrent) {
                Icon(imageVector = Icons.Filled.Star, contentDescription = "현재 레벨", tint = level.color, modifier = Modifier.size(20.dp))
            } else if (isAchieved) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "달성 완료", tint = level.color, modifier = Modifier.size(20.dp))
            } else {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = "미달성", tint = Color(0xFFBDBDBD), modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun getNextLevel(currentLevel: LevelDefinitions.LevelInfo): LevelDefinitions.LevelInfo? {
    val currentIndex = LevelDefinitions.levels.indexOf(currentLevel)
    return if (currentIndex < LevelDefinitions.levels.size - 1) LevelDefinitions.levels[currentIndex + 1] else null
}

@Preview(showBackground = true, name = "LevelScreen - 기본", widthDp = 360, heightDp = 800)
@Composable
fun LevelScreenPreview() {
    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) { LevelScreen() }
    }
}

private object LevelUiTokens {
    val CardPaddingH = 16.dp
    val CardPaddingV = 32.dp
    val ProgressInnerHPadding = 16.dp
    val ProgressHeight = 8.dp
    val ProgressCorner = 4.dp
    val IndicatorDot = 6.dp // 러닝 화면과 동일하게 6dp
}
