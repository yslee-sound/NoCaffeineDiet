import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.security.KeyStore

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// release 관련 태스크 실행 여부 (configuration 시점에 1회 계산)
// bundleRelease / assembleRelease / publishRelease / 끝이 Release 인 태스크 포함
val isReleaseTaskRequested: Boolean = gradle.startParameter.taskNames.any { name ->
    val lower = name.lowercase()
    ("release" in lower && ("assemble" in lower || "bundle" in lower || "publish" in lower)) || lower.endsWith("release")
}

android {
    namespace = "com.sweetapps.nosmoketimer"
    compileSdk = 36

    val releaseVersionCode = 2025101300
    val releaseVersionName = "1.0.2"

    defaultConfig {
        applicationId = "com.sweetapps.nosmoketimer"
        minSdk = 21
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Play Console 경고 대응: 네이티브 심볼 업로드용 심볼 테이블 생성 (FULL 은 용량↑)
            debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    signingConfigs {
        // 환경변수 기반 release 서명 (Provider API 사용, 구성 캐시 친화적)
        create("release") {
            val ksPath = providers.environmentVariable("KEYSTORE_PATH").orNull
            if (!ksPath.isNullOrBlank()) {
                storeFile = file(ksPath)
            } else {
                println("[WARN] Release keystore not configured - will build unsigned bundle. Set KEYSTORE_PATH before production release.")
            }
            storePassword = providers.environmentVariable("KEYSTORE_STORE_PW").orNull ?: ""
            keyAlias = providers.environmentVariable("KEY_ALIAS").orNull ?: ""
            keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull ?: ""
        }
    }

    buildTypes {
        release {
            // 릴리스 번들 최적화: 코드/리소스 축소 (ProGuard/R8)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 서명 강제: 실제 release 관련 태스크(assembleRelease/bundleRelease 등) 요청 시에만 검사
            val hasKeystoreProvider = providers.environmentVariable("KEYSTORE_PATH")
                .map { it.isNotBlank() }
                .orElse(false)
            val hasKeystore = hasKeystoreProvider.get()
            if (isReleaseTaskRequested && !hasKeystore) {
                throw GradleException("Unsigned release build blocked. Set KEYSTORE_PATH, KEYSTORE_STORE_PW, KEY_ALIAS, KEY_PASSWORD env vars before running a release build.")
            }
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // debug 설정 변경 없음
    }

    // Java/Kotlin 타깃 17로 상향
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // 필요시 buildConfig true (기본 true) / viewBinding 등 미사용
    }

    lint {
        // 릴리스 치명적 이슈 CI fail fast
        abortOnError = true
        warningsAsErrors = false // 초기 온보딩: 경고는 유지, 필요시 true
    }
}

kotlin {
    // Kotlin JVM 타깃 + JDK 툴체인을 17로 고정
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.app.update.ktx)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    // org.json (Android 내장) 를 JVM 유닛 테스트 환경에서 사용하기 위한 의존성
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// signingReport 대안: 서명 환경변수 및 키스토어 존재 여부를 출력하는 헬퍼 태스크
// 구성 캐시 문제로 signingReport 가 실패할 때 빠르게 상태를 확인하는 용도
tasks.register("printReleaseSigningEnv") {
    group = "help"
    description = "Prints release signing env vars and keystore file existence"
    // 구성 캐시 비호환(진단용 태스크): Project API 직접 호출을 피하고도 안전장치 추가
    notCompatibleWithConfigurationCache("diagnostic helper task")
    doLast {
        val ksPath = providers.environmentVariable("KEYSTORE_PATH").orNull
        val alias = providers.environmentVariable("KEY_ALIAS").orNull
        val hasStorePw = !providers.environmentVariable("KEYSTORE_STORE_PW").orNull.isNullOrEmpty()
        val hasKeyPw = !providers.environmentVariable("KEY_PASSWORD").orNull.isNullOrEmpty()
        println("KEYSTORE_PATH=${ksPath ?: "<not set>"}")
        if (!ksPath.isNullOrBlank()) {
            val f = File(ksPath)
            println(" - exists=${f.exists()} size=${if (f.exists()) f.length() else 0}")
        }
        println("KEY_ALIAS=${alias ?: "<not set>"}")
        println("KEYSTORE_STORE_PW set=${hasStorePw}")
        println("KEY_PASSWORD set=${hasKeyPw}")
    }
}

// keystore 내용을 직접 로드해서 alias 목록과 타깃 별칭 존재 여부를 출력하는 헬퍼 태스크
// PKCS12/JKS 모두 시도하여 여는 로직
fun loadKeystoreSmart(file: File, password: String): KeyStore {
    val candidates = listOf(KeyStore.getDefaultType(), "JKS", "PKCS12").distinct()
    val pw = password.toCharArray()
    for (type in candidates) {
        try {
            val ks = KeyStore.getInstance(type)
            file.inputStream().use { ks.load(it, pw) }
            println("[checkReleaseKeystore] Opened as type=$type")
            return ks
        } catch (_: Exception) {
            // try next
        }
    }
    throw GradleException("Unable to open keystore with provided password. Check password or store type (JKS/PKCS12).")
}

tasks.register("checkReleaseKeystore") {
    group = "help"
    description = "Loads release keystore and prints alias list; verifies KEY_ALIAS existence"
    // 구성 캐시 비호환(진단용 태스크)
    notCompatibleWithConfigurationCache("diagnostic helper task")
    doLast {
        val ksPath = providers.environmentVariable("KEYSTORE_PATH").orNull
        val storePw = providers.environmentVariable("KEYSTORE_STORE_PW").orNull
        val targetAlias = providers.environmentVariable("KEY_ALIAS").orNull
        if (ksPath.isNullOrBlank()) {
            println("[checkReleaseKeystore] KEYSTORE_PATH not set")
            return@doLast
        }
        val f = File(ksPath)
        if (!f.exists()) {
            println("[checkReleaseKeystore] Keystore not found at: ${f.absolutePath}")
            return@doLast
        }
        if (storePw.isNullOrEmpty()) {
            println("[checkReleaseKeystore] KEYSTORE_STORE_PW not set — cannot open keystore")
            return@doLast
        }
        val ks = loadKeystoreSmart(f, storePw)
        val list = mutableListOf<String>()
        val e = ks.aliases()
        while (e.hasMoreElements()) list.add(e.nextElement())
        println("[checkReleaseKeystore] aliases=${list}")
        if (!targetAlias.isNullOrBlank()) {
            println("[checkReleaseKeystore] KEY_ALIAS='${targetAlias}', exists=${ks.containsAlias(targetAlias)}")
        } else {
            println("[checkReleaseKeystore] KEY_ALIAS not set")
        }
    }
}

// (단순화) designTokenCheck 커스텀 태스크 제거.
// 필요 시 별도 스크립트나 독립 Gradle 플러그인/CI 스텝으로 수행 권장
