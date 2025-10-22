package com.sweetapps.nocaffeinediet.core.util

/**
 * In-App Update에서 제공하는 availableVersionCode(Int)를 사용자에게 노출할 버전명(String)으로 매핑합니다.
 * 최신 릴리스부터 순차적으로 추가하세요. 미매핑 시 null을 반환합니다.
 */
object UpdateVersionMapper {
    // 최신 → 과거 순으로 관리 권장
    private val codeToName: Map<Int, String> = mapOf(
        // 예시 매핑 — 실제 배포 시 최신 코드/이름을 여기에 추가하세요.
        // 2025101001 to "1.0.7",
        // 2025092301 to "1.0.6",
    )

    fun toVersionName(code: Int): String? = codeToName[code]

    // 필요 시 Long 형태를 지원
    fun toVersionName(code: Long): String? = if (code in Int.MIN_VALUE..Int.MAX_VALUE) codeToName[code.toInt()] else null
}

