# 업데이트 UI 통합 가이드(데모 모드, 금주 설정 화면 롱프레스 트리거)

> 문서 버전
> - 버전: v1.0.0
> - 최근 업데이트: 2025-10-22
> - 변경 요약: 3개 문서 통합 — 금주 설정 화면의 ‘목표 기간 설정’ 제목 롱프레스 데모 트리거, In‑App Update 게이트/오버레이/다이얼로그/스낵바 흐름, SDK30 호환 패치와 버전 표기 전략을 한 문서로 정리
>
> 변경 이력(Changelog)
> - v1.0.0 (2025-10-22)
>   - 합본: `docs/update-ui-integration.md` + `docs/update-ui-prompt.md` + `docs/update-ui-prompt.txt`
>   - 게이트/오버레이/다이얼로그/스낵바/데모 모드/버전 매핑/SDK30 패치/체크리스트를 단일 규격으로 통일

---

## 0) 전제/대상
- 기술 스택: Kotlin, Jetpack Compose
- 사용 컴포넌트: AppUpdateManager(Play In‑App Update 래퍼), AppUpdateDialog(커스텀)
- 화면: 금주 설정 화면. 상단 제목 “목표 기간 설정” Text를 길게 눌러(롱프레스) 디버그 데모 트리거
- 문자열 리소스: `checking_update` = "업데이트 확인 중..."

---

## 1) 목표(요약)
- 앱 첫 진입에서 업데이트 확인을 gate로 수행(자동 네비게이션 잠시 보류)
- 확인이 300ms 이상 지속될 때만 반투명 오버레이(+Progress, “업데이트 확인 중…”) 표시
- 업데이트 가능 시 커스텀 다이얼로그 표시 → 사용자 선택에 따라 Flexible/Immediate 시작
- Flexible 다운로드 완료 시 스낵바 Indefinite로 ‘다시 시작’ 액션 제공 → `completeFlexibleUpdate()`
- 데모 모드: DEBUG 빌드에서만 롱프레스/인텐트로 시연, Release에서는 완전 비활성화
- 정책: 모든 체크는 `forceCheck=false`(24시간 정책 준수)

---

## 2) 상태와 네비게이션 게이트
- 상태: `isCheckingUpdate`, `showUpdateDialog`, `updateInfo`, `availableVersionName`, `snackbarHostState`, `showOverlay`
- 게이트: `gateNavigation = isCheckingUpdate || showUpdateDialog`
  - true 동안 다음 화면으로의 자동 이동을 보류

---

## 3) 업데이트 체크 플로우
- 진입 시 `checkForUpdate(forceCheck=false)` 호출
- 콜백 처리
  - `onUpdateAvailable(info)`: `updateInfo` 저장 → 코드→버전명 매핑(`availableVersionName`) → 다이얼로그 표시
  - `onNoUpdate()`: 오버레이/게이트 해제
- 설치 상태 리스너
  - `registerInstallStateListener { ... }` 등록
  - Flexible 다운로드 완료 → 스낵바(Indefinite)로 ‘다시 시작’ 액션 → `completeFlexibleUpdate()`
  - 화면 수명 종료 시 `unregisterInstallStateListener()`
- 300ms 지연 오버레이
  - 확인 시작 즉시가 아니라 300ms 후에도 진행 중이면 `showOverlay=true`
  - 다이얼로그가 열리면 오버레이 숨김
  - 터치 차단: `Modifier.clickable(indication = null, interactionSource = MutableInteractionSource())`

---

## 4) 데모 모드(디버그 전용)
- 활성화 조건: `BuildConfig.DEBUG`
- 트리거
  - 롱프레스: 금주 설정 화면의 제목 Text("목표 기간 설정")에 `Modifier.combinedClickable(...)` 부착
    - 필수 파라미터: `interactionSource`, `indication`, `onClick`, `onLongClick`
    - `onLongClick`에서 데모 활성 플래그를 `true`로 설정
  - 인텐트: `demo_update_ui=true`로 앱을 시작하면 자동 시연
- 동작
  - 데모 활성 시 실제 `checkForUpdate`를 건너뜀
  - 약 600ms 지연 후 가짜 버전 코드(예: `2025101001`)로 다이얼로그 표시
  - 사용자 표시 버전명은 매핑 유틸로 변환(예: `1.0.7`) — 미매핑 시 코드값 문자열을 폴백
  - 다이얼로그 ‘업데이트’ 클릭은 실제 플로우 대신 스낵바/토스트 안내만
- 릴리스 빌드: 롱프레스/인텐트 모두 무시

간단 코드 포인트
- 제목 Text 롱프레스
  - `Modifier.combinedClickable(interactionSource=remember { MutableInteractionSource() }, indication=null, onClick={}, onLongClick={ if (BuildConfig.DEBUG) demoActive = true })`
- 데모 시퀀스
  - `delay(600); val fake=2025101001; availableVersionName = UpdateVersionMapper.toVersionName(fake) ?: fake.toString(); showUpdateDialog = true`

---

## 5) Flexible/Immediate 선택 규칙
- “최대 미루기 초과” && Immediate 허용 시 → Immediate
- 그 외 → Flexible(우선)

---

## 6) 버전 표기 전략(중요)
- In‑App Update API는 대상의 `versionName`이 아닌 `availableVersionCode()`만 제공
- 사용자에게는 앱 정보와 동일한 버전명(예: 1.0.7)을 보여주기 위해 코드→이름 매핑 유틸을 사용

운영 규칙
- `core/util/UpdateVersionMapper.kt`에 릴리스별 `versionCode -> versionName` 쌍을 최신부터 추가
- 미매핑 시 폴백으로 코드 문자열 값 노출

적용 예
- 실제 체크: `val code = info.availableVersionCode(); availableVersionName = UpdateVersionMapper.toVersionName(code) ?: code.toString()`
- 데모 모드: `val fake = 2025101001; availableVersionName = UpdateVersionMapper.toVersionName(fake) ?: fake.toString()`

---

## 7) AppUpdateDialog 수정(중요)
- ‘나중에’ 버튼을 항상 표시
- 매개변수 `canDismiss: Boolean` 추가 후 `enabled = canDismiss`로 연결
- `onDismissRequest`는 `canDismiss == true`일 때만 닫히도록 유지
- 호출부 예: `canDismiss = !appUpdateManager.isMaxPostponeReached() || demoActive`

---

## 8) SDK 30 호환성 패치
1) AndroidManifest 패키지 가시성
- `<queries><package android:name="com.android.vending"/></queries>` 추가

1‑b) Pre‑Android 12 스플래시 테마 보강
- `Theme.AlcoholicTimer.Splash`에 `<item name="splashScreenIconSize">240dp</item>` 추가
- 대상: `values/themes.xml`, `values-v23/themes.xml`, `values-v29/themes.xml`

1‑c) 스플래시 설치는 런처 액티비티에서만
- 공통 베이스에서 `installSplashScreen()` 호출 금지
- 런처 액티비티 `onCreate()` 초반에서만 호출
- 매니페스트에서 런처 액티비티에만 스플래시 테마 적용

2) Play Store 존재 가드(AppUpdateManager)
- 체크 전 `hasPlayStore(context)`로 `com.android.vending` 존재 확인
- 없으면 `onNoUpdate()`로 스킵
- 모든 예외는 크래시 대신 `onNoUpdate()`로 다운그레이드

3) 정책
- Immediate는 “최대 미루기 초과 && Immediate 허용”일 때만 사용
- 그 외에는 Flexible 우선

---

## 9) 문자열 리소스
- `res/values/strings.xml`
  - `checking_update` = "업데이트 확인 중..."

---

## 10) 테스트 시나리오
- 디버그 데모(ADB)
  - `adb shell am start -n <패키지>/<메인액티비티> -e demo_update_ui true`
- UI 확인
  - 금주 설정 화면의 “목표 기간 설정” 제목 롱프레스 → 데모 다이얼로그 표시
  - 다이얼로그에 `버전 1.0.x` 형태 표기(코드값 노출 시 매핑 누락)
- Flexible 완료 → 스낵바 ‘다시 시작’ 동작으로 설치 완료
- Release 빌드 → 데모 트리거/인텐트 무시 확인
- 300ms 이내로 확인 완료 시 오버레이 미표시 확인
- SDK 30 실기/에뮬레이터: 런처 및 내부 화면 전환 시 크래시 없음

---

## 11) 변경 산출물 체크리스트
- 금주 설정 화면(시작 화면) 로직 수정 및 관련 import
- AppUpdateDialog: `canDismiss` 매개변수, ‘나중에’ 버튼 항상 노출(+enabled 바인딩)
- `core/util/UpdateVersionMapper.kt` 생성/갱신
- `AndroidManifest.xml` `<queries>`에 `com.android.vending` 추가
- 스플래시 테마 보강 항목 반영
- AppUpdateManager: Play Store 가드 및 예외 다운그레이드 처리
- 문자열 `checking_update` 추가
- QA 스모크: 빌드/실행/데모/실 플로우/릴리스 무시/버전명 표기/SDK30 안정성

---

## 12) 참고 스니펫(요약 발췌)

```kotlin
@Composable
fun SobrietySetupScreen(appUpdateManager: AppUpdateManager) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var isCheckingUpdate by remember { mutableStateOf(false) }
  var showUpdateDialog by remember { mutableStateOf(false) }
  var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
  var availableVersionName by remember { mutableStateOf<String?>(null) }
  var showOverlay by remember { mutableStateOf(false) }
  var demoActive by remember { mutableStateOf(false) }

  // 제목 Text — “목표 기간 설정”
  Text(
    text = "목표 기간 설정",
    modifier = Modifier.combinedClickable(
      interactionSource = remember { MutableInteractionSource() },
      indication = null,
      onClick = {},
      onLongClick = { if (BuildConfig.DEBUG) demoActive = true }
    )
  )

  LaunchedEffect(demoActive) {
    if (demoActive && BuildConfig.DEBUG) {
      showOverlay = true
      delay(600)
      val fake = 2025101001
      availableVersionName = UpdateVersionMapper.toVersionName(fake) ?: fake.toString()
      showOverlay = false
      showUpdateDialog = true
      return@LaunchedEffect
    }
  }

  LaunchedEffect(Unit) {
    if (!BuildConfig.DEBUG || !demoActive) {
      isCheckingUpdate = true
      // 300ms 지연 오버레이
      launch {
        delay(300)
        if (isCheckingUpdate) showOverlay = true
      }
      appUpdateManager.checkForUpdate(
        forceCheck = false,
        onUpdateAvailable = { info ->
          updateInfo = info
          val code = info.availableVersionCode()
          availableVersionName = UpdateVersionMapper.toVersionName(code) ?: code.toString()
          showOverlay = false
          showUpdateDialog = true
        },
        onNoUpdate = {
          showOverlay = false
          isCheckingUpdate = false
        }
      )
    }
  }

  DisposableEffect(Unit) {
    val listener = appUpdateManager.registerInstallStateListener { state ->
      if (state.isFlexibleDownloadCompleted) {
        scope.launch {
          val result = snackbarHostState.showSnackbar(
            message = "업데이트가 준비되었습니다",
            actionLabel = "다시 시작",
            duration = SnackbarDuration.Indefinite
          )
          if (result == SnackbarResult.ActionPerformed) {
            appUpdateManager.completeFlexibleUpdate()
          }
        }
      }
    }
    onDispose { appUpdateManager.unregisterInstallStateListener(listener) }
  }

  if (showOverlay) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.3f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) {}
    ) {
      CircularProgressIndicator()
      Text(stringResource(R.string.checking_update))
    }
  }

  if (showUpdateDialog) {
    val canDismiss = (!appUpdateManager.isMaxPostponeReached()) || demoActive
    AppUpdateDialog(
      versionNameToShow = availableVersionName ?: "",
      canDismiss = canDismiss,
      onUpdate = {
        showUpdateDialog = false
        if (demoActive) {
          scope.launch { snackbarHostState.showSnackbar("데모: 업데이트를 시작하지 않습니다") }
          isCheckingUpdate = false
          return@AppUpdateDialog
        }
        val allowImmediate = appUpdateManager.isMaxPostponeReached()
        if (allowImmediate && appUpdateManager.isImmediateAllowed()) {
          appUpdateManager.startImmediateUpdate(updateInfo!!)
        } else {
          appUpdateManager.startFlexibleUpdate(updateInfo!!)
        }
      },
      onLater = {
        showUpdateDialog = false
        isCheckingUpdate = false
      },
      onDismissRequest = {
        if (canDismiss) {
          showUpdateDialog = false
          isCheckingUpdate = false
        }
      }
    )
  }

  val gateNavigation = isCheckingUpdate || showUpdateDialog
  // gateNavigation이 false가 될 때까지 자동 네비게이션 보류
}
```

---

## 13) 파일/경로 요약
- 문서: `docs/a_UPDATE_UI_GUIDE.md`(본 문서)
- 유틸: `core/util/UpdateVersionMapper.kt`
- 매니페스트: `app/src/main/AndroidManifest.xml`(`<queries>` 패키지 가시성 추가)
- 테마: `res/values*/themes.xml` 내 `Theme.AlcoholicTimer.Splash`
- 문자열: `res/values/strings.xml`

