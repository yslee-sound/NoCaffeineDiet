package com.sweetapps.nosmoketimer.feature.records

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sweetapps.nosmoketimer.core.ui.theme.AlcoholicTimerTheme
import com.sweetapps.nosmoketimer.feature.detail.DetailActivity
import com.sweetapps.nosmoketimer.core.model.SobrietyRecord

class AllRecordsActivity : ComponentActivity() {

    companion object { private const val TAG = "AllRecordsActivity" }

    private var externalRefreshTriggerState by mutableIntStateOf(0)

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "DetailActivity RESULT_OK 수신 → 리스트 새로고침 트리거 증가")
            externalRefreshTriggerState++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlcoholicTimerTheme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    com.sweetapps.nosmoketimer.feature.records.components.AllRecordsScreen(
                        externalRefreshTrigger = externalRefreshTriggerState,
                        onNavigateBack = { finish() },
                        onNavigateToDetail = { record -> handleRecordClick(record) }
                    )
                }
            }
        }
    }

    private fun handleRecordClick(record: SobrietyRecord) {
        Log.d(TAG, "===== 기록 클릭 시작 =====")
        Log.d(TAG, "기록 클릭: ${record.id}")
        Log.d(TAG, "actualDays=${record.actualDays}, targetDays=${record.targetDays}")

        try {
            if (record.actualDays < 0) {
                Log.e(TAG, "잘못된 기록 데이터: actualDays=${record.actualDays}")
                return
            }
            val safeTargetDays = if (record.targetDays <= 0) 30 else record.targetDays
            val intent = Intent(this@AllRecordsActivity, DetailActivity::class.java).apply {
                putExtra("start_time", record.startTime)
                putExtra("end_time", record.endTime)
                putExtra("target_days", safeTargetDays.toFloat())
                putExtra("actual_days", record.actualDays)
                putExtra("is_completed", record.isCompleted)
            }
            Log.d(TAG, "DetailActivity 호출(결과 대기)...")
            detailLauncher.launch(intent)
            Log.d(TAG, "===== 기록 클릭 종료 =====")
        } catch (e: Exception) {
            Log.e(TAG, "DetailActivity 화면 이동 중 오류", e)
        }
    }
}
