# Google Play 업로드 가이드 (NoSmokeTimer)

현재 기준 정보
- 앱 이름: NoSmokeTimer
- applicationId: `com.sweetapps.nosmoketimer`
- 현재 버전: `1.0.0` (versionCode: `2025101100`)
- 기본 트랙 권장: 내부 테스트(Internal Testing) → 프로덕션 단계적 출시

---
## 1) 업로드 키 생성 및 백업 (최초 1회)

Windows에서 cmd.exe 기준으로 안내합니다. keytool 경로는 Android Studio의 jbr/bin 예시를 사용했습니다.

### 1-1. 업로드 키스토어 생성
```bat
"%USERPROFILE%\AppData\Local\Programs\Android Studio\jbr\bin\keytool.exe" -genkeypair ^
 -v -keystore G:\secure\NoSmokeTimer\release-upload.jks ^
 -alias nosmoketimer_upload ^
 -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 36500
```
프롬프트에 따라 비밀번호/조직 정보를 입력하세요.

### 1-2. 업로드 인증서(Public) 내보내기(Play 최초 등록에 필요)
```bat
"%USERPROFILE%\AppData\Local\Programs\Android Studio\jbr\bin\keytool.exe" -exportcert -rfc ^
 -alias nosmoketimer_upload ^
 -keystore G:\secure\NoSmokeTimer\release-upload.jks ^
 -file G:\secure\NoSmokeTimer\upload_cert.pem
```

### 1-3. 키스토어 무결성 확인(지문)
```bat
"%USERPROFILE%\AppData\Local\Programs\Android Studio\jbr\bin\keytool.exe" -list -v ^
 -keystore G:\secure\NoSmokeTimer\release-upload.jks
```

### 1-4. 안전 백업(권장)
PowerShell 스크립트로 날짜 폴더/해시/ZIP까지 백업합니다.
```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\backup_keystore.ps1 `
  -KeystorePath "G:\secure\NoSmokeTimer\release-upload.jks" `
  -BackupBaseDir "G:\Workspace\NoSmokeTimer\keystore-backups" `
  -UsbDrive "E:" `
  -CreateEncryptedZip
```

> 중요: 업로드 키 분실 시 기존 앱 업데이트가 불가능합니다. 최소 2~3중 백업(로컬/클라우드/오프라인)을 권장합니다.

---
## 2) 서명 환경변수 설정 (이 레포 빌드 규칙)
Gradle 릴리스 태스크는 아래 환경변수가 없으면 차단되도록 설정돼 있습니다. cmd.exe 세션에서 설정하세요.

```bat
set "KEYSTORE_PATH=G:\secure\NoSmokeTimer\release-upload.jks"
set "KEYSTORE_STORE_PW=키스토어_비밀번호"
set "KEY_ALIAS=nosmoketimer_upload"
set "KEY_PASSWORD=키_비밀번호"

REM 인식 상태 확인
gradlew.bat :app:printReleaseSigningEnv --no-daemon --console=plain
```

---
## 3) 버전 업데이트(필요 시)
현재 설정: versionName=`1.0.0`, versionCode=`2025101100` (오늘 날짜 기반 반영됨)

필요 시 PowerShell 스크립트로 갱신할 수 있습니다.
```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\update_version.ps1 -VersionName 1.0.1 -VersionCode 2025101101
```

규칙 권장: `yyyyMMddNN` (날짜 + 2자리 시퀀스) → 예: 2025101100, 2025101101 …

---
## 4) 서명된 AAB 빌드
```bat
gradlew.bat clean :app:bundleRelease --no-daemon --console=plain
```
산출물
- AAB: `app\build\outputs\bundle\release\app-release.aab`
- 매핑: `app\build\outputs\mapping\release\mapping.txt`
- SHA256(검증):
```bat
certutil -hashfile app\build\outputs\bundle\release\app-release.aab SHA256
```

선택) 파이프라인으로 아카이브까지 한 번에
```bat
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\release_pipeline.ps1 -SkipVersionBump
```

---
## 5) Play Console 설정 및 업로드

### 5-1. 최초 1회: 업로드 인증서 제출
- https://play.google.com/console 접속 → 앱 생성/선택
- App signing by Google Play(권장) 참여
- `upload_cert.pem` 또는 지문(SHA‑1/256) 제출

### 5-2. 내부 테스트(권장) 업로드
1) Play Console → 테스트 → 내부 테스트 → 새 릴리스 만들기
2) `app-release.aab` 업로드 (버전 코드 증가 필수)
3) 릴리스 노트(ko 필수, en 선택) 작성
4) 검토 → 게시
5) 생성된 테스터 옵트인 URL을 테스터에게 전달 → 참여 후 설치/업데이트

### 5-3. 프로덕션 단계적 출시(권장)
- 내부 테스트 안정 확인 후 프로덕션 새 릴리스 생성
- 동일 AAB 선택 가능(해시 동일 OK)
- 10% → 24~48시간 모니터 → 100% 확대

릴리스 노트 예시(ko)
```
1.0.0 첫 공개 릴리스
- 금연 목표 설정, 진행률/레벨 확인
- 버그 수정 및 성능 개선
```

---
## 6) 빠른 QA 체크(실기기 10분)
- 첫 실행 값/네비게이션 정상
- 재실행 시 상태 보존
- 1~2분 진행률 갱신 정상, 크래시 없음
- 다크모드/영문 로케일 전환 이상 없음
- Logcat Error 크래시 스택 없음

---
## 7) 문제 해결 (Troubleshooting)
- 버전 코드 중복: `app/build.gradle.kts`의 `releaseVersionCode`를 +1(같은 날짜면 시퀀스만 증가) → 재빌드
- 서명 키 불일치: 최초 등록에 사용한 동일 키스토어/별칭/비번 사용
- 서명 미설정으로 빌드 차단: 2) 환경변수부터 재점검 후 `:app:printReleaseSigningEnv`
- 테스터가 업데이트 못 봄: “검토/출시” 버튼 누락 여부, 옵트인 링크 확인, 잠시 대기(캐시)

---
## 8) 스토어 메타데이터 체크리스트
- 아이콘 512×512 PNG, 스크린샷 1080×1920 5~8장
- 짧은 설명(ko) 80자 이내, 전체 설명 정책 위반 표현 없음
- 개인정보 처리방침 URL(레포 `docs/PRIVACY_POLICY.md` 웹 호스팅)
- 국가/가격(무료) 설정, 콘텐츠 등급 설문 완료

---
## 9) 부록 (명령 모음)
```bat
REM 디버그 빌드
gradlew.bat :app:assembleDebug

REM 릴리스 AAB 빌드
gradlew.bat clean :app:bundleRelease

REM 캐시 클리어
gradlew.bat clean
```

---
본 문서는 NoSmokeTimer 레포의 빌드/서명 규칙에 맞춰 작성되었습니다. 운영 중 발견한 개선점은 소규모 PR로 수시 반영하세요.

