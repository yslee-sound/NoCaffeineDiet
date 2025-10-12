package com.sweetapps.nosmoketimer.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material3.MaterialTheme

private val MaxContentWidth: Dp = 600.dp

@Composable
fun StandardScreen(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(LayoutConstants.SCREEN_HORIZONTAL_PADDING)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
        content = content
    )
}

@Composable
fun StandardScreenWithBottomButton(
    topContent: @Composable ColumnScope.() -> Unit,
    bottomButton: @Composable () -> Unit,
    imePaddingEnabled: Boolean = true
) {
    val rootModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .then(if (imePaddingEnabled) Modifier.imePadding() else Modifier)

    val navBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()
    val navBarBottom = navBarPaddingValues.calculateBottomPadding()
    val buttonSize = 96.dp
    val extraGap = 32.dp
    // 콘텐츠 영역이 버튼과 겹치지 않도록 버튼 반지름 + 추가 여백 + 시스템 바 높이만큼만 예약
    val reservedBottom = (buttonSize / 2) + extraGap + navBarBottom

    Box(
        modifier = rootModifier
    ) {
        // Centered column with max width constraint
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    end = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    top = LayoutConstants.SCREEN_HORIZONTAL_PADDING,
                    bottom = reservedBottom
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = MaxContentWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LayoutConstants.CARD_SPACING),
                content = topContent
            )
        }

        // 버튼: 시스템 바 높이 + 적당한 여백(24dp) 적용
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING,
                    end = LayoutConstants.BOTTOM_BUTTON_HORIZONTAL_PADDING,
                    bottom = navBarBottom + 24.dp
                )
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = MaxContentWidth),
            contentAlignment = Alignment.Center
        ) {
            bottomButton()
        }
    }
}
