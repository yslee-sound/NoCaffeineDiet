package com.sweetapps.nocaffeinediet.feature.detail.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweetapps.nocaffeinediet.core.ui.AppCard
import com.sweetapps.nocaffeinediet.core.ui.AppElevation

/**
 * 공통 값/단위 포맷팅 카드
 * - 기존 DetailStatCard와 동일한 외곽/레이아웃 스타일
 * - 값과 단위를 베이스라인 정렬로 분리 표기하여 재사용성 향상
 */
@Composable
fun DetailStatCardValueUnit(
    valueNumeric: String,
    unit: String?,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    AppCard(
        modifier = modifier,
        elevation = AppElevation.CARD,
        contentPadding = PaddingValues(16.dp)
    ) {
        val resolvedValueColor = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        val valueStyle = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = resolvedValueColor,
            textAlign = TextAlign.Center
        )
        val unitStyle = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = resolvedValueColor,
            textAlign = TextAlign.Center
        )
        Row { // 값 + 단위
            Text(text = valueNumeric, style = valueStyle)
            if (!unit.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = unit, style = unitStyle)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF718096),
            textAlign = TextAlign.Center
        )
    }
}

