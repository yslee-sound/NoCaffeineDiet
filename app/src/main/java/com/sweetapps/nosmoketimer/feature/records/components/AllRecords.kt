package com.sweetapps.nosmoketimer.feature.records.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweetapps.nosmoketimer.R
import com.sweetapps.nosmoketimer.core.data.RecordsDataLoader
import com.sweetapps.nosmoketimer.core.model.SobrietyRecord
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRecordsScreen(
    externalRefreshTrigger: Int = 0,
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (SobrietyRecord) -> Unit = {},
    fontScale: Float = 1.06f
) {
    val context = LocalContext.current
    var records by remember { mutableStateOf<List<SobrietyRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val loadRecords: () -> Unit = remember {
        {
            isLoading = true
            loadError = null
            try {
                val loadedRecords = RecordsDataLoader.loadSobrietyRecords(context)
                records = loadedRecords.sortedByDescending { it.startTime }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                loadError = e.message ?: "unknown"
            }
        }
    }

    LaunchedEffect(retryTrigger) { loadRecords() }
    LaunchedEffect(externalRefreshTrigger) { loadRecords() }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = LocalDensity.current.fontScale * fontScale
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    color = Color.White
                ) {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(id = R.string.all_records_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(id = R.string.cd_navigate_back),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showDeleteAllDialog = true },
                                    enabled = !isLoading && records.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(id = R.string.cd_delete_all_records),
                                        tint = if (!isLoading && records.isNotEmpty()) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        HorizontalDivider(
                            thickness = 1.5.dp,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    loadError != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.error_loading_records),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { retryTrigger++ }) {
                                Text(text = stringResource(id = R.string.retry))
                            }
                        }
                    }

                    records.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { EmptyRecordsState() }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = navBarBottom + 8.dp)
                        ) {
                            items(
                                items = records,
                                key = { it.id }
                            ) { record ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    RecordSummaryCard(
                                        record = record,
                                        onClick = { onNavigateToDetail(record) },
                                        compact = false,
                                        headerIconSizeDp = 56.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showDeleteAllDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteAllDialog = false },
                    title = {
                        Text(
                            text = stringResource(id = R.string.all_records_delete_title),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748)
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(id = R.string.all_records_delete_message),
                            color = Color(0xFF4A5568)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteAllDialog = false
                                val success = RecordsDataLoader.clearAllRecords(context)
                                if (success) {
                                    onNavigateBack()
                                } else {
                                    // 실패 시 머무름
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(id = R.string.all_records_delete_confirm),
                                color = Color(0xFFE53E3E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteAllDialog = false }) {
                            Text(text = stringResource(id = R.string.dialog_cancel), color = Color(0xFF718096))
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyRecordsState() {
    val emptyCd = stringResource(id = R.string.empty_records_cd)

    // 카드 제거: 단순 중앙 정렬 안내만 표시
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .semantics { contentDescription = emptyCd },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📝",
            fontSize = 48.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.empty_records_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.empty_records_subtitle),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AllRecordsScreenPreview() {
    AllRecordsScreen()
}
