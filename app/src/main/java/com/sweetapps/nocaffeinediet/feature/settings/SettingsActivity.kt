package com.sweetapps.nocaffeinediet.feature.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.sweetapps.nocaffeinediet.core.ui.AppElevation
import com.sweetapps.nocaffeinediet.core.ui.BaseActivity
import com.sweetapps.nocaffeinediet.core.util.Constants
import com.sweetapps.nocaffeinediet.R
import com.sweetapps.nocaffeinediet.core.ui.AppBorder
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory

class SettingsActivity : BaseActivity() {
    override fun getScreenTitle(): String = "설정"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BackHandler(enabled = true) { navigateToMainHome() }
            BaseScreen(applyBottomInsets = false) { SettingsScreen() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val (initialCost, initialFrequency, _) = Constants.getUserSettings(context)
    val sharedPref = context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)

    var selectedCost by remember { mutableStateOf(initialCost) }
    var selectedFrequency by remember { mutableStateOf(initialFrequency) }

    // 문서 스펙: 실측 기반 스크롤 판정 및 외부 패딩 적용
    val density = LocalDensity.current
    val gap12Px = with(density) { 12.dp.roundToPx() }

    var viewportH by remember { mutableStateOf(0) }
    var costH by remember { mutableStateOf(0) }
    var freqH by remember { mutableStateOf(0) }

    val allowScroll by remember { derivedStateOf { (costH + freqH + gap12Px) > viewportH } }
    val listState = rememberLazyListState()

    val hPadding = 16.dp // H_PADDING 토큰이 없으므로 16.dp로 대체

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = hPadding, end = hPadding, top = 8.dp, bottom = 8.dp)
            .onSizeChanged { viewportH = it.height }
    ) {
        val listContent: @Composable () -> Unit = {
            LazyColumn(
                state = listState,
                userScrollEnabled = allowScroll,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(Modifier.onSizeChanged { costH = it.height }) {
                        SettingsCard(
                            title = "나의 카페인 스타일",
                            titleColor = colorResource(id = R.color.color_indicator_money)
                        ) {
                            SettingsOptionGroup(
                                selectedOption = selectedCost,
                                options = listOf("가성비", "프랜차이즈", "프리미엄"),
                                labels = listOf(
                                    "가성비 (2,000원 이하)",
                                    "프랜차이즈 (2,000원 ~ 5,000원)",
                                    "프리미엄 (5,000원 이상)"
                                ),
                                onOptionSelected = { newValue ->
                                    selectedCost = newValue
                                    sharedPref.edit { putString(Constants.PREF_SELECTED_COST, newValue) }
                                }
                            )
                        }
                    }
                }
                item {
                    Box(Modifier.onSizeChanged { freqH = it.height }) {
                        SettingsCard(
                            title = "일일 주입량",
                            titleColor = colorResource(id = R.color.color_progress_primary)
                        ) {
                            SettingsOptionGroup(
                                selectedOption = selectedFrequency,
                                options = listOf("1잔", "2잔", "3잔 이상"),
                                labels = listOf("1잔", "2잔", "3잔 이상"),
                                onOptionSelected = { newValue ->
                                    selectedFrequency = newValue
                                    sharedPref.edit { putString(Constants.PREF_SELECTED_FREQUENCY, newValue) }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (allowScroll) {
            listContent()
        } else {
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                listContent()
            }
        }
    }
}

@Composable
fun SettingsCard(title: String, titleColor: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD),
        border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = titleColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsOptionItem(isSelected: Boolean, label: String, onSelected: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.RadioButton, onClick = onSelected)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = colorResource(id = R.color.color_accent_blue),
                unselectedColor = colorResource(id = R.color.color_radio_unselected)
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = if (isSelected) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyLarge,
            color = if (isSelected) colorResource(id = R.color.color_indicator_days) else colorResource(id = R.color.color_text_primary_dark)
        )
    }
}

@Composable
fun SettingsOptionGroup(selectedOption: String, options: List<String>, labels: List<String>, onOptionSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { index, option ->
            SettingsOptionItem(
                isSelected = selectedOption == option,
                label = labels[index],
                onSelected = { onOptionSelected(option) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() { SettingsScreen() }
