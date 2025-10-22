package com.sweetapps.nocaffeinediet.feature.run

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.edit
import java.util.Locale
import com.sweetapps.nocaffeinediet.core.ui.BaseActivity
import com.sweetapps.nocaffeinediet.core.ui.StandardScreenWithBottomButton
import com.sweetapps.nocaffeinediet.core.util.Constants
import com.sweetapps.nocaffeinediet.feature.level.LevelDefinitions
import com.sweetapps.nocaffeinediet.core.util.FormatUtils
import kotlinx.coroutines.delay
import com.sweetapps.nocaffeinediet.feature.start.StartActivity
import com.sweetapps.nocaffeinediet.feature.detail.DetailActivity
import com.sweetapps.nocaffeinediet.core.ui.AppElevation
import com.sweetapps.nocaffeinediet.R
import com.sweetapps.nocaffeinediet.core.ui.AppBorder
import com.sweetapps.nocaffeinediet.core.util.LifePlusUtils

class RunActivity : BaseActivity() {

    override fun getScreenTitle(): String = getString(R.string.run_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseScreen(applyBottomInsets = false) {
                RunScreen()
            }
        }
    }
}

@Composable
private fun RunScreen() {
    val context = LocalContext.current

    val sp = remember { context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE) }
    val startTime = remember { sp.getLong(Constants.PREF_START_TIME, 0L) }
    val targetDays = remember { sp.getFloat(Constants.PREF_TARGET_DAYS, 30f) }
    val timerCompleted = remember { sp.getBoolean(Constants.PREF_TIMER_COMPLETED, false) }

    LaunchedEffect(startTime, timerCompleted) {
        if (timerCompleted || startTime == 0L) {
            context.startActivity(Intent(context, StartActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            (context as? RunActivity)?.finish()
        }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val elapsedMillis by remember(now, startTime) {
        derivedStateOf { if (startTime > 0) now - startTime else 0L }
    }
    val elapsedDaysFloat = remember(elapsedMillis) { elapsedMillis / Constants.DAY_IN_MILLIS.toFloat() }
    val elapsedDays = remember(elapsedDaysFloat) { elapsedDaysFloat.toInt() }

    val levelDays = remember(elapsedMillis) { Constants.calculateLevelDays(elapsedMillis) }
    val levelInfo = remember(levelDays) { LevelDefinitions.getLevelInfo(levelDays) }
    val levelName = levelInfo.name

    val elapsedHours = ((elapsedMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)).toInt()
    val elapsedMinutes = ((elapsedMillis % (60 * 60 * 1000)) / (60 * 1000)).toInt()
    val elapsedSeconds = ((elapsedMillis % (60 * 1000)) / 1000).toInt()
    val progressTimeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds)
    val progressTimeTextHM = String.format(Locale.getDefault(), "%02d:%02d", elapsedHours, elapsedMinutes)

    val (selectedCost, selectedFrequency) = Constants.getUserSettings(context)
    val costVal = when (selectedCost) {
        "가성비" -> 1500
        "프랜차이즈" -> 4500
        "프리미엄" -> 7000
        else -> 4500
    }
    // 일일 주입량(잔/일)
    val cupsPerDay = when (selectedFrequency) {
        "1잔" -> 1.0
        "2잔" -> 2.0
        "3잔 이상" -> 3.0
        else -> 1.0
    }
    val weeks = elapsedDaysFloat / 7.0
    val savedMoney = remember(elapsedDaysFloat, cupsPerDay, costVal) { elapsedDaysFloat * cupsPerDay * costVal }
    val lifeGainDays = remember(elapsedDaysFloat) { elapsedDaysFloat / 30.0 }
    // 노카페인 달성량(mg): 경과일 × 잔/일 × 150mg
    val savedCaffeineMg = remember(elapsedDaysFloat, cupsPerDay) { elapsedDaysFloat * cupsPerDay * Constants.CAFFEINE_MG_PER_CUP }

    // LifePlus(%) 계산: 현재 진행 중 세션의 경과일만 사용 (최근 30일 누적 제외)
    val currentSessionDays = remember(elapsedMillis) { (elapsedMillis.toDouble() / Constants.DAY_IN_MILLIS.toDouble()).coerceAtLeast(0.0) }
    val lifePlusPercent1 = remember(currentSessionDays, selectedCost, selectedFrequency) {
        LifePlusUtils.computeLifePlusPercentOneDecimal(currentSessionDays, selectedCost, selectedFrequency)
    }

    val totalTargetMillis = (targetDays * Constants.DAY_IN_MILLIS).toLong()
    val progress = remember(elapsedMillis, totalTargetMillis) {
        if (totalTargetMillis > 0) (elapsedMillis.toFloat() / totalTargetMillis).coerceIn(0f, 1f) else 0f
    }

    val indicatorKey = remember(startTime) { Constants.keyCurrentIndicator(startTime) }
    var currentIndicator by remember { mutableIntStateOf(sp.getInt(indicatorKey, 0).coerceIn(0, 4)) }

    fun toggleIndicator() { val next = (currentIndicator + 1) % 5; currentIndicator = next; sp.edit { putInt(indicatorKey, next) } }

    var hasCompleted by remember { mutableStateOf(false) }
    LaunchedEffect(progress) {
        if (!hasCompleted && progress >= 1f && startTime > 0) {
            try {
                saveCompletedRecord(
                    context = context,
                    startTime = startTime,
                    endTime = System.currentTimeMillis(),
                    targetDays = targetDays,
                    actualDays = (elapsedMillis / Constants.DAY_IN_MILLIS).toInt()
                )
                sp.edit { remove(Constants.PREF_START_TIME); putBoolean(Constants.PREF_TIMER_COMPLETED, true) }
                hasCompleted = true
                Toast.makeText(context, context.getString(R.string.toast_goal_completed), Toast.LENGTH_SHORT).show()
                DetailActivity.start(
                    context = context,
                    startTime = startTime,
                    endTime = System.currentTimeMillis(),
                    targetDays = targetDays,
                    actualDays = (elapsedMillis / Constants.DAY_IN_MILLIS).toInt(),
                    isCompleted = true
                )
                (context as? RunActivity)?.finish()
            } catch (_: Exception) { }
        }
    }

    StandardScreenWithBottomButton(
        topContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RunStatChip(title = stringResource(id = R.string.stat_goal_days), value = "${targetDays.toInt()}일", color = colorResource(id = R.color.color_stat_goal), modifier = Modifier.weight(1f))
                        RunStatChip(title = stringResource(id = R.string.stat_level), value = levelName.take(6), color = levelInfo.color, modifier = Modifier.weight(1f))
                        RunStatChip(title = stringResource(id = R.string.stat_time), value = progressTimeTextHM, color = colorResource(id = R.color.color_stat_time), modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth().height(168.dp).clickable { toggleIndicator() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    val labelBoxH = 36.dp; val valueBoxH = 66.dp; val hintBoxH = 20.dp; val gapSmall = 6.dp; val gapMedium = 8.dp
                    val (label, valueText, valueColor) = when (currentIndicator) {
                        0 -> Triple(stringResource(id = R.string.indicator_title_days), String.format(Locale.getDefault(), "%.1f", elapsedDaysFloat), colorResource(id = R.color.color_indicator_days))
                        1 -> Triple(stringResource(id = R.string.indicator_title_time), progressTimeText, colorResource(id = R.color.color_indicator_time))
                        2 -> Triple(stringResource(id = R.string.indicator_title_saved_money), String.format(Locale.getDefault(), "%,.0f원", savedMoney).replace(" ", ""), colorResource(id = R.color.color_indicator_money))
                        3 -> Triple(stringResource(id = R.string.indicator_title_life_gain), String.format(Locale.getDefault(), "%.1f%%", lifePlusPercent1), colorResource(id = R.color.color_indicator_life))
                        4 -> Triple(stringResource(id = R.string.indicator_title_saved_caffeine), String.format(Locale.getDefault(), "%,.0f mg", savedCaffeineMg).replace(" ", ""), colorResource(id = R.color.color_indicator_days))
                        else -> Triple(stringResource(id = R.string.indicator_title_life_gain), String.format(Locale.getDefault(), "%.1f%%", lifePlusPercent1), colorResource(id = R.color.color_indicator_life))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.fillMaxWidth().height(labelBoxH), contentAlignment = Alignment.Center) {
                            val base = MaterialTheme.typography.titleMedium
                            Text(
                                text = label,
                                style = base.copy(
                                    color = colorResource(id = R.color.color_indicator_label_gray),
                                    lineHeight = base.fontSize * 1.2f,
                                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(gapSmall))
                        Box(modifier = Modifier.fillMaxWidth().height(valueBoxH), contentAlignment = Alignment.Center) {
                            val baseStyle = MaterialTheme.typography.headlineMedium
                            val bigSize = (baseStyle.fontSize.value * 1.5f).sp
                            val bigStyle = baseStyle.copy(
                                fontWeight = FontWeight.Bold,
                                color = valueColor,
                                fontSize = bigSize,
                                lineHeight = bigSize * 1.1f,
                                platformStyle = PlatformTextStyle(includeFontPadding = true),
                                fontFeatureSettings = "tnum"
                            )
                            val unitStyle = baseStyle.copy(
                                color = valueColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = baseStyle.fontSize,
                                lineHeight = baseStyle.fontSize * 1.1f,
                                platformStyle = PlatformTextStyle(includeFontPadding = true)
                            )
                            val isMoney = currentIndicator == 2
                            val isLifeGain = false // '%': 시간 포맷 분기 비활성화
                            if (isMoney) {
                                val numeric = valueText.replace("원", "")
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Text(text = numeric, style = bigStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.alignByBaseline())
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(text = "원", style = unitStyle, modifier = Modifier.alignByBaseline())
                                }
                            } else if (isLifeGain) {
                                // ...existing time-splitting code (dead path)
                                Text(text = valueText, style = bigStyle, textAlign = TextAlign.Center, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                            } else {
                                Text(
                                    text = valueText,
                                    style = bigStyle,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(gapMedium))
                        Box(modifier = Modifier.fillMaxWidth().height(hintBoxH), contentAlignment = Alignment.Center) {
                            val base = MaterialTheme.typography.labelMedium
                            Text(
                                text = stringResource(id = R.string.tap_to_switch_indicator),
                                style = base.copy(
                                    color = colorResource(id = R.color.color_hint_gray),
                                    lineHeight = base.fontSize * 1.2f,
                                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { ModernProgressIndicatorSimple(progress = progress) }
            }
        },
        bottomButton = {
            ModernStopButtonSimple(onStop = {
                val intent = Intent(context, QuitActivity::class.java).apply {
                    putExtra("elapsed_days", elapsedDays)
                    putExtra("elapsed_hours", elapsedHours)
                    putExtra("elapsed_minutes", elapsedMinutes)
                    putExtra("saved_money", savedMoney)
                    putExtra("life_gain_days", lifeGainDays)
                    putExtra("level_name", levelName)
                    putExtra("level_color", levelInfo.color.value.toLong())
                    putExtra("quit_timestamp", System.currentTimeMillis())
                    putExtra("saved_caffeine_mg", savedCaffeineMg)
                }
                context.startActivity(intent)
            })
        }
    )
}

@Composable
private fun ModernProgressIndicatorSimple(progress: Float) {
    var blink by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { delay(1000); blink = !blink } }
    val alpha by animateFloatAsState(targetValue = if (blink) 1f else 0.3f, animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "blink")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = (progress * 100).toInt().coerceIn(0, 100).toString() + "%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = colorResource(id = R.color.color_progress_primary)))
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colorResource(id = R.color.color_progress_primary).copy(alpha = alpha)))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { progress }, color = colorResource(id = R.color.color_progress_primary), trackColor = colorResource(id = R.color.color_progress_track), modifier = Modifier.fillMaxWidth().height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernStopButtonSimple(onStop: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onStop,
        modifier = modifier.size(96.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_stop_button)),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD_HIGH)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(id = R.string.cd_stop),
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun AutoResizeSingleLineText(
    text: String,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier,
    minFontSizeSp: Float = 10f,
    step: Float = 0.95f,
    color: Color? = null,
    textAlign: TextAlign? = null,
) {
    var style by remember(text) { mutableStateOf(baseStyle) }
    var tried by remember(text) { mutableStateOf(0) }
    Text(
        text = text,
        style = style,
        color = color ?: style.color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && tried < 20) {
                val current = style.fontSize.value
                val next = (current * step).coerceAtLeast(minFontSizeSp)
                if (next < current - 0.1f) {
                    style = style.copy(fontSize = next.sp, lineHeight = (next.sp * 1.1f))
                    tried++
                }
            }
        }
    )
}

@Composable
private fun RunStatChip(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(84.dp), shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val baseValue = MaterialTheme.typography.titleMedium
            val isTime = value.contains(":")
            val baseFactor = if (isTime) 0.92f else 0.98f
            val valueSize = (baseValue.fontSize.value * baseFactor).sp
            val valueStyle = baseValue.copy(
                fontWeight = FontWeight.Bold,
                fontSize = valueSize,
                lineHeight = valueSize * 1.1f,
                platformStyle = PlatformTextStyle(includeFontPadding = true),
                fontFeatureSettings = "tnum"
            )
            Box(modifier = Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) {
                AutoResizeSingleLineText(
                    text = value,
                    baseStyle = valueStyle,
                    minFontSizeSp = (baseValue.fontSize.value * 0.75f),
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val baseLabel = MaterialTheme.typography.labelMedium
            val labelStyle = baseLabel.copy(
                lineHeight = baseLabel.fontSize * 1.2f,
                platformStyle = PlatformTextStyle(includeFontPadding = true)
            )
            Box(modifier = Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
                AutoResizeSingleLineText(
                    text = title,
                    baseStyle = labelStyle,
                    minFontSizeSp = (baseLabel.fontSize.value * 0.85f),
                    color = colorResource(id = R.color.color_stat_title_gray),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun saveCompletedRecord(context: Context, startTime: Long, endTime: Long, targetDays: Float, actualDays: Int) {
    try {
        val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val recordId = System.currentTimeMillis().toString()
        val isCompleted = actualDays >= targetDays
        val status = if (isCompleted) "완료" else "중지"
        val record = org.json.JSONObject().apply {
            put("id", recordId); put("startTime", startTime); put("endTime", endTime); put("targetDays", targetDays.toInt()); put("actualDays", actualDays); put("isCompleted", isCompleted); put("status", status); put("createdAt", System.currentTimeMillis())
        }
        val recordsJson = sharedPref.getString(Constants.PREF_SOBRIETY_RECORDS, "[]") ?: "[]"
        val list = try { org.json.JSONArray(recordsJson) } catch (_: Exception) { org.json.JSONArray() }
        list.put(record)
        sharedPref.edit { putString(Constants.PREF_SOBRIETY_RECORDS, list.toString()) }
    } catch (_: Exception) { }
}
