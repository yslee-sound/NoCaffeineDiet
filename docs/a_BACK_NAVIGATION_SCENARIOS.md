# 뒤로가기 동작 시나리오 정의

## 현재 상태 분석 (2025-01-25 - 최종 적용 완료)

### 구현된 방식
- **구조**: Multi-Activity 구조 (각 화면이 독립된 Activity)
- **메인 홈 개념**: StartActivity(금주 설정) / RunActivity(금주 진행)
- **네비게이션**: 드로어 메뉴를 통한 화면 전환
- **Back Stack**: 메인 홈만 singleTask, 나머지는 standard
- **핵심 원칙**: 모든 뒤로가기는 메인 홈으로 복귀

---

## 메인 홈 화면 개념

### 🏠 메인 홈 (singleTask)
1. **StartActivity** - 금주 설정 화면 (금주 진행 전)
2. **RunActivity** - 금주 진행 화면 (금주 진행 중)

**메인 홈 결정 로직:**
```kotlin
금주 진행 여부 확인 (start_time > 0)
  → 진행 중: RunActivity가 메인 홈
  → 진행 전: StartActivity가 메인 홈
```

### 📱 일반 화면 (standard)
- **RecordsActivity** - 금주 기록
- **LevelActivity** - 레벨
- **SettingsActivity** - 설정
- **AboutActivity** - 앱 정보

### 📄 서브 화면 (2단계, standard)
- **AllRecordsActivity** ← RecordsActivity
- **DetailActivity** ← RecordsActivity
- **AboutLicensesActivity** ← AboutActivity
- **NicknameEditActivity** ← 드로어
- **AddRecordActivity** ← RecordsActivity
- **QuitActivity** ← RunActivity

---

## 적용된 뒤로가기 동작

### 시나리오 1: 메인 홈에서 뒤로가기
**StartActivity (금주 설정 화면)**
```
사용자: 뒤로가기 버튼 클릭
앱: 네이티브 광고 팝업 표시 → 확인 시 앱 종료
```
✅ **상태**: 정상 작동 (종료 의사 재확인)

**RunActivity (금주 진행 화면)**
```
사용자: 뒤로가기 버튼 클릭
앱: QuitActivity(종료 확인 화면)로 이동
```
✅ **상태**: 정상 작동 (실수로 종료 방지)

---

### 시나리오 2: 일반 화면에서 뒤로가기 → 메인 홈으로
**RecordsActivity / LevelActivity / SettingsActivity / AboutActivity**

**예시 1: 금주 진행 전**
```
Start(메인 홈) → 드로어 → Records → 드로어 → Level
사용자: 뒤로가기
앱: Level 종료 → Start(메인 홈)로 복귀
```

**예시 2: 금주 진행 중**
```
Run(메인 홈) → 드로어 → Records → 드로어 → Settings
사용자: 뒤로가기
앱: Settings 종료 → Run(메인 홈)로 복귀
```

✅ **상태**: 구현 완료
- BackHandler 추가로 뒤로가기 시 `navigateToMainHome()` 호출
- 금주 진행 여부에 따라 자동으로 Start 또는 Run으로 이동

---

### 시나리오 3: 2단계 서브 화면 → 부모 → 메인 홈

**AboutActivity → AboutLicensesActivity**
```
메인 홈 → 드로어 → About → "오픈 라이선스" 클릭 → Licenses

사용자: Licenses에서 뒤로가기
앱: Licenses 종료 → About으로 복귀

사용자: About에서 뒤로가기
앱: About 종료 → 메인 홈으로 복귀
```

**RecordsActivity → AllRecordsActivity**
```
메인 홈 → 드로어 → Records → "전체보기" 클릭 → AllRecords

사용자: AllRecords에서 뒤로가기
앱: AllRecords 종료 → Records로 복귀

사용자: Records에서 뒤로가기
앱: Records 종료 → 메인 홈으로 복귀
```

✅ **상태**: 정상 작동
- 서브 화면은 `showBackButton = true`로 설정되어 finish() 호출
- 부모 화면은 BackHandler로 메인 홈 복귀

---

### 시나리오 4: QuitActivity → RunActivity 복귀
```
RunActivity에서 종료 버튼 클릭 → QuitActivity

사용자: 뒤로가기 (또는 취소 버튼)
앱: QuitActivity 종료 → RunActivity로 복귀
```

✅ **상태**: 정상 작동 (기존 동작 유지)

---

## 구현 세부사항

### 1. BaseActivity.kt - 공통 함수 추가
```kotlin
protected fun navigateToMainHome() {
    val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val isRunning = startTime > 0
    
    val targetActivity = if (isRunning) RunActivity::class.java else StartActivity::class.java
    
    // 이미 메인 홈이면 아무것도 하지 않음
    if ((isRunning && this is RunActivity) || (!isRunning && this is StartActivity)) {
        return
    }
    
    val intent = Intent(this, targetActivity).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (targetActivity == StartActivity::class.java) {
            putExtra("skip_splash", true)
        }
    }
    startActivity(intent)
    overridePendingTransition(0, 0)
    finish()
}
```

### 2. 각 일반 화면 - BackHandler 추가
```kotlin
// RecordsActivity, LevelActivity, SettingsActivity, AboutActivity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
        BackHandler(enabled = true) {
            navigateToMainHome()
        }
        // ...
    }
}
```

### 3. RunActivity - QuitActivity로 이동
```kotlin
BackHandler(enabled = true) {
    context.startActivity(Intent(context, QuitActivity::class.java))
}
```

### 4. AndroidManifest.xml - singleTask 설정
```xml
<!-- 메인 홈만 singleTask -->
<activity android:name=".feature.start.StartActivity"
    android:launchMode="singleTask" />
    
<activity android:name=".feature.run.RunActivity"
    android:launchMode="singleTask" />

<!-- 나머지는 standard (기본값) -->
<activity android:name=".feature.records.RecordsActivity" />
<activity android:name=".feature.level.LevelActivity" />
<activity android:name=".feature.settings.SettingsActivity" />
<activity android:name=".feature.about.AboutActivity" />
```

---

## 테스트 시나리오

### ✅ 테스트 1: 금주 진행 전 - 일반 화면 뒤로가기
```
1. StartActivity 실행 (메인 홈)
2. 드로어 → "기록" 선택
3. 드로어 → "레벨" 선택
4. 뒤로가기 클릭
예상: StartActivity로 바로 복귀
```

### ✅ 테스트 2: 금주 진행 중 - 일반 화면 뒤로가기
```
1. 금주 시작 → RunActivity (메인 홈)
2. 드로어 → "기록" 선택
3. 드로어 → "설정" 선택
4. 뒤로가기 클릭
예상: RunActivity로 바로 복귀
```

### ✅ 테스트 3: 2단계 서브 화면
```
1. 메인 홈 → 드로어 → "앱 정보"
2. "오픈 라이선스" 클릭
3. 뒤로가기 클릭
예상: AboutActivity로 복귀
4. 뒤로가기 클릭
예상: 메인 홈으로 복귀
```

### ✅ 테스트 4: RunActivity 보호
```
1. 금주 시작 → RunActivity
2. 뒤로가기 클릭
예상: QuitActivity(종료 확인) 표시
```

### ✅ 테스트 5: 금주 진행 중 → 진행 완료 후
```
1. RunActivity에서 금주 완료
2. DetailActivity 표시 (완료 기록)
3. DetailActivity 종료 → StartActivity로 이동
4. 드로어 → "기록" 선택
5. 뒤로가기 클릭
예상: StartActivity로 복귀 (금주 완료되어 메인 홈이 Start로 변경)
```

---

## 장점

### 🎯 사용자 경험
- **일관된 동작**: 모든 화면에서 뒤로가기 = 메인 홈 복귀
- **빠른 네비게이션**: 불필요한 중간 화면 거치지 않음
- **실수 방지**: 금주 진행 중 실수로 종료 불가

### 💻 코드 품질
- **중앙화된 로직**: BaseActivity.navigateToMainHome() 공통 사용
- **유지보수 용이**: 메인 홈 변경 시 한 곳만 수정
- **명확한 구조**: 메인 홈 vs 일반 화면 vs 서브 화면

### 📊 Back Stack 관리
- **메모리 효율**: 메인 홈만 singleTask로 단일 인스턴스
- **깔끔한 스택**: 불필요한 Activity 누적 방지
- **예측 가능**: 항상 메인 홈으로 복귀하는 단순한 규칙

---

## 구현 완료 체크리스트

### ✅ P0 (즉시 수정 필요)
- [x] RunActivity BackHandler 추가 (QuitActivity로 이동)
- [x] BaseActivity.navigateToMainHome() 함수 구현

### ✅ P1 (핵심 기능)
- [x] RecordsActivity BackHandler 추가
- [x] LevelActivity BackHandler 추가
- [x] SettingsActivity BackHandler 추가
- [x] AboutActivity BackHandler 추가
- [x] AndroidManifest.xml singleTask 설정 (Start, Run만)

### ✅ P2 (검증)
- [x] 2단계 서브 화면 동작 확인 (Licenses, AllRecords)
- [x] 에러 없음 확인
- [x] 문서 업데이트

### 메인 화면들 (드로어 메뉴 접근)
1. **StartActivity** - 시작 화면 (금주 시작 전)
2. **RunActivity** - 금주 진행 화면
3. **RecordsActivity** - 금주 기록 화면
4. **LevelActivity** - 레벨 화면
5. **SettingsActivity** - 설정 화면
6. **AboutActivity** - 앱 정보 화면

### 서브 화면들 (특정 화면에서만 접근)
7. **AllRecordsActivity** - 전체 기록 화면 (RecordsActivity → 전체보기)
8. **DetailActivity** - 상세 기록 화면 (RecordsActivity → 카드 클릭)
9. **QuitActivity** - 금주 종료 확인 화면 (RunActivity → 종료)
10. **NicknameEditActivity** - 닉네임 편집 (드로어 → 닉네임 클릭)
11. **AddRecordActivity** - 수동 기록 추가 (RecordsActivity → 추가)
12. **AboutLicensesActivity** - 라이선스 화면 (AboutActivity → 라이선스)

---

## 시나리오별 뒤로가기 동작

### 시나리오 1: 런처 → StartActivity
**현재 동작:**
```
사용자: 뒤로가기 버튼 클릭
앱: 네이티브 광고 팝업 표시 → 확인 시 앱 종료
```

**의도:**
- ✅ 올바름: 메인 화면에서는 즉시 종료하지 않고 종료 의사 재확인

**코드 위치:**
- `StartActivity.kt` 라인 413: `BackHandler(enabled = true) { showExitPopup = true }`

---

### 시나리오 2: 드로어 메뉴로 화면 이동 (예: Start → Records → Level)
**현재 동작:**
```
사용자: StartActivity에서 드로어 → "기록" 선택
앱: RecordsActivity 시작 (StartActivity는 스택에 유지)

사용자: RecordsActivity에서 드로어 → "레벨" 선택  
앱: LevelActivity 시작 (Start, Records 모두 스택에 유지)

사용자: 뒤로가기
앱: LevelActivity 종료 → RecordsActivity 표시

사용자: 뒤로가기
앱: RecordsActivity 종료 → StartActivity 표시

사용자: 뒤로가기
앱: 네이티브 광고 팝업 → 앱 종료
```

**문제점:**
- ❌ 모든 화면이 스택에 누적되어 불필요한 뒤로가기 단계가 많음
- ❌ 사용자는 "기록" → "레벨"로 이동했는데, 뒤로가기 시 "기록"으로 돌아가는 것이 직관적이지 않을 수 있음

**코드 위치:**
- `BaseActivity.kt` 라인 395-411: `handleMenuSelection()` - Intent 플래그 없이 `startActivity()` 호출

---

### 시나리오 3: RunActivity에서 뒤로가기
**현재 동작:**
```
사용자: RunActivity에서 뒤로가기
앱: RunActivity 종료 → 이전 화면으로 이동 (스택에 있는 경우)
```

**문제점:**
- ❌ 금주 진행 중에는 실수로 뒤로가기를 눌러 화면이 닫히면 안됨
- ❌ BackHandler가 구현되어 있지 않아 실수로 종료 가능

**예상 시나리오:**
- 사용자가 StartActivity에서 금주 시작 → RunActivity
- 실수로 뒤로가기 → RunActivity 종료 → StartActivity로 돌아감
- 하지만 금주는 계속 진행 중 (타이머는 백그라운드에서 작동)

---

### 시나리오 4: RecordsActivity → DetailActivity
**현재 동작:**
```
사용자: RecordsActivity에서 기록 카드 클릭
앱: DetailActivity 시작

사용자: 뒤로가기
앱: DetailActivity 종료 → RecordsActivity로 복귀
```

**의도:**
- ✅ 올바름: 상세 화면에서 뒤로가기 시 목록으로 복귀

**코드 위치:**
- `DetailActivity.kt`: `showBackButton = true`, `onBackClick = { finish() }`

---

### 시나리오 5: RecordsActivity → AllRecordsActivity
**현재 동작:**
```
사용자: RecordsActivity에서 "전체보기" 클릭
앱: AllRecordsActivity 시작

사용자: 뒤로가기
앱: AllRecordsActivity 종료 → RecordsActivity로 복귀
```

**의도:**
- ✅ 올바름: 전체 보기에서 뒤로가기 시 이전 화면으로

---

### 시나리오 6: NicknameEditActivity
**현재 동작:**
```
사용자: 드로어에서 닉네임 클릭
앱: NicknameEditActivity 시작

사용자: 뒤로가기 (또는 취소 버튼)
앱: finish() → 이전 화면으로 복귀
```

**의도:**
- ✅ 올바름: 편집 화면에서는 뒤로가기로 취소

---

### 시나리오 7: RunActivity → QuitActivity
**현재 동작:**
```
사용자: RunActivity에서 종료 버튼 클릭
앱: QuitActivity 시작 (종료 확인 화면)

사용자: 뒤로가기
앱: QuitActivity 종료 → RunActivity로 복귀
```

**의도:**
- ✅ 올바름: 종료 확인 화면에서 뒤로가기 시 취소 의미

---

## 개선 방안

### 방안 1: SingleTask LaunchMode (권장)
**적용 대상:** 메인 화면들 (Start, Run, Records, Level, Settings, About)

**효과:**
- 드로어 메뉴로 이동 시 기존 인스턴스 재사용
- Back Stack에 하나의 인스턴스만 유지
- 예: Start → Records → Level 이동 시, 뒤로가기 하면 Start로 바로 이동

**구현:**
```xml
<!-- AndroidManifest.xml -->
<activity
    android:name=".feature.start.StartActivity"
    android:launchMode="singleTask"
    ... />
```

---

### 방안 2: FLAG_ACTIVITY_CLEAR_TOP + FLAG_ACTIVITY_SINGLE_TOP
**적용 대상:** 드로어 메뉴 네비게이션

**효과:**
- 이미 스택에 있는 Activity로 이동 시 위의 모든 Activity 제거
- 새로운 인스턴스를 생성하지 않음

**구현:**
```kotlin
// BaseActivity.kt - navigateToActivity()
intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
```

---

### 방안 3: RunActivity BackHandler 추가
**적용 대상:** RunActivity

**효과:**
- 금주 진행 중 실수로 뒤로가기 눌러도 종료되지 않음
- 다이얼로그로 "정말 종료하시겠습니까?" 확인

**구현:**
```kotlin
// RunActivity.kt - RunScreen()
BackHandler(enabled = true) {
    // QuitActivity 호출 또는 확인 다이얼로그
}
```

---

### 방안 4: 메인 화면들 사이의 Back Stack 비우기
**적용 대상:** 드로어 메뉴로 메인 화면 이동 시

**효과:**
- Start → Records 이동 시 Start를 스택에서 제거
- Records에서 뒤로가기 하면 앱 종료 (또는 Start로 이동)

**구현:**
```kotlin
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
```

---

## 권장 구현 방식

### 화면 유형별 분류

#### A. 루트 화면 (Back Stack 최하단)
- **StartActivity**: 앱 진입점, 뒤로가기 시 종료 확인
- **RunActivity**: 금주 진행 화면, 뒤로가기 시 종료 확인

#### B. 메인 화면 (드로어 접근, singleTask)
- **RecordsActivity**: 독립 화면
- **LevelActivity**: 독립 화면  
- **SettingsActivity**: 독립 화면
- **AboutActivity**: 독립 화면

#### C. 서브 화면 (부모 화면이 명확)
- **DetailActivity** ← RecordsActivity
- **AllRecordsActivity** ← RecordsActivity
- **QuitActivity** ← RunActivity
- **NicknameEditActivity** ← 모든 화면
- **AddRecordActivity** ← RecordsActivity
- **AboutLicensesActivity** ← AboutActivity

---

## 구현 우선순위

### P0 (즉시 수정 필요)
1. ✅ **RunActivity BackHandler 추가** - 금주 진행 중 실수로 종료 방지

### P1 (사용자 경험 개선)
2. ✅ **메인 화면 singleTask 모드** - 불필요한 Back Stack 누적 방지
3. ✅ **드로어 네비게이션 플래그** - 화면 전환 시 중복 제거

### P2 (선택사항)
4. ⚠️ **Navigation Component 전환** - 장기적으로 권장하지만 대규모 리팩토링 필요

---

## 테스트 시나리오

### 테스트 1: 메인 화면 간 이동
```
1. StartActivity 실행
2. 드로어 → "기록" 선택
3. 드로어 → "레벨" 선택
4. 드로어 → "설정" 선택
5. 뒤로가기 3번 클릭
예상: StartActivity로 바로 이동 (모든 화면 건너뜀)
```

### 테스트 2: 서브 화면 네비게이션
```
1. RecordsActivity 실행
2. 기록 카드 클릭 → DetailActivity
3. 뒤로가기
예상: RecordsActivity로 복귀
```

### 테스트 3: 금주 진행 중 뒤로가기
```
1. StartActivity에서 금주 시작
2. RunActivity로 이동
3. 뒤로가기 클릭
예상: 종료 확인 다이얼로그 표시 (즉시 종료 안됨)
```

### 테스트 4: StartActivity 종료
```
1. StartActivity에서 뒤로가기
예상: 네이티브 광고 팝업 → 확인 시 앱 종료
```

---

## 다음 단계

1. 이 문서를 검토하고 각 시나리오의 **예상 동작** 확정
2. 우선순위에 따라 개선 사항 구현
3. 테스트 시나리오 실행 및 검증
4. 사용자 피드백 수집 및 조정

