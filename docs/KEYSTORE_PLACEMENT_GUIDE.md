# Keystore 배치/설정 가이드 (NoSmokeTimer)

이 문서는 업로드 키스토어를 안전한 위치(다른 드라이브 포함)에 보관하고, 빌드 시 환경변수로 서명 설정을 주입하는 본 레포의 표준 방식을 정리합니다.

현재 앱 정보
- applicationId: `com.sweetapps.nosmoketimer`
- 권장 키 파일명: `release-upload.jks`
- 권장 별칭(alias): `nosmoketimer_upload`

---
## TL;DR
- 레포(워크스페이스) 밖, 보호된 폴더(예: BitLocker/VeraCrypt)로 보관: `G:\secure\NoSmokeTimer\release-upload.jks`
- 빌드할 때 환경변수 4개 설정: `KEYSTORE_PATH / KEYSTORE_STORE_PW / KEY_ALIAS / KEY_PASSWORD`
- 확인: `:app:printReleaseSigningEnv` → 빌드: `:app:bundleRelease`
- 백업은 3-2-1 원칙(로컬·클라우드·오프사이트)을 따르고, 레포의 PowerShell 스크립트로 자동화 가능

---
## 1) 배치(위치) 모범 사례
- 레포 외부 고정 디스크(예: `G:`)의 전용 폴더에 보관: `G:\secure\NoSmokeTimer\release-upload.jks`
- 폴더 보안: OS 계정 권한 제한 + 드라이브 암호화(BitLocker 권장)
- 레포 안(.git)에는 절대 보관 금지(오누락 커밋 위험)
- 이동식 드라이브(USB)는 원본 저장소로 쓰지 말고, 백업 용도로만 활용
- 경로에 공백이 있어도 문제없음: 환경변수 설정 시 따옴표로 감싸기

예시 폴더 만들기
```bat
mkdir G:\secure\NoSmokeTimer
```

---
## 2) 환경변수로 서명 설정 (cmd.exe)
레포의 Gradle 설정은 릴리스 관련 태스크 실행 시 아래 환경변수가 없으면 빌드를 차단합니다.

```bat
set "KEYSTORE_PATH=G:\secure\NoSmokeTimer\release-upload.jks"
set "KEYSTORE_STORE_PW=키스토어_비밀번호"
set "KEY_ALIAS=nosmoketimer_upload"
set "KEY_PASSWORD=키_비밀번호"

rem 서명 환경 인식 확인
gradlew.bat :app:printReleaseSigningEnv --no-daemon --console=plain
```

- `printReleaseSigningEnv` 출력에서 KEYSTORE_PATH가 올바른 절대경로이고, 비밀번호 변수들이 set=true로 보이면 준비 완료입니다.
- 경로가 not set 또는 exists=false면 경로/권한을 점검하세요.

---
## 2.5) PowerShell에서 keytool 올바르게 실행하기
PowerShell은 cmd의 캐럿(^)을 줄바꿈 문자로 쓰지 않습니다. 다음 방법 중 하나를 사용하세요.

- PowerShell 멀티라인(백틱 ` 사용) 예시
```powershell
& "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\keytool.exe" `
  -genkeypair `
  -v `
  -keystore "G:\secure\NoSmokeTimer\release-upload.jks" `
  -alias nosmoketimer_upload `
  -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 36500
```

- PowerShell 원라이너 예시
```powershell
& "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore "G:\secure\NoSmokeTimer\release-upload.jks" -alias nosmoketimer_upload -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 36500
```

- JAVA_HOME이 설정된 경우
```powershell
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore "G:\secure\NoSmokeTimer\release-upload.jks" -alias nosmoketimer_upload -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 36500
```

> 참고: 경로에 공백이 있으면 & "경로" 형태로 호출해야 합니다.

- 인증서 내보내기(PEM)
```powershell
& "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\keytool.exe" -exportcert -rfc -alias nosmoketimer_upload -keystore "G:\secure\NoSmokeTimer\release-upload.jks" -file "G:\secure\NoSmokeTimer\upload_cert.pem"
```

---
## 2.6) cmd.exe에서 keytool 실행 예시(참고)
```bat
"%USERPROFILE%\AppData\Local\Programs\Android Studio\jbr\bin\keytool.exe" -genkeypair ^
 -v ^
 -keystore G:\secure\NoSmokeTimer\release-upload.jks ^
 -alias nosmoketimer_upload ^
 -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 36500
```

---
## 2.7) 프롬프트가 반복될 때(로케일/입력 이슈)
- 증상: "Enter the distinguished name…" → 값 입력 → "…맞습니까? [아니오]:" 에서 한국어로 `예` 입력 후, 동일 프롬프트가 다시 시작함.
- 원인: keytool 확인 질문은 영문 `yes`/`y` 또는 `no`/`n`만 인식하는 경우가 많습니다. `예`가 인식되지 않아 기본값([아니오])로 처리되어 다시 질문이 반복됩니다.
- 해결 1: 확인 질문에 `y` 또는 `yes`(영문)로 응답하세요.
- 해결 2(권장): 대화형을 피하고 비대화식으로 생성합니다. `-dname`과 비밀번호 옵션을 함께 쓰면 확인 질문 없이 한 번에 생성됩니다.

비대화식 생성 예시
- PowerShell
```powershell
& "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\keytool.exe" `
  -genkeypair -v `
  -keystore "G:\secure\NoSmokeTimer\release-upload.jks" `
  -alias nosmoketimer_upload `
  -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 36500 `
  -storepass "<키스토어_비밀번호>" `
  -keypass   "<키_비밀번호>" `
  -dname "CN=youngseok lee, OU=sweetapps, O=none, L=seoul, ST=seoul, C=KR"
```

- cmd.exe 원라이너
```bat
"%USERPROFILE%\AppData\Local\Programs\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore "G:\secure\NoSmokeTimer\release-upload.jks" -alias nosmoketimer_upload -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 36500 -storepass "<키스토어_비밀번호>" -keypass "<키_비밀번호>" -dname "CN=youngseok lee, OU=sweetapps, O=none, L=seoul, ST=seoul, C=KR"
```

추가 팁
- 경로의 폴더가 없으면 먼저 생성하세요: `mkdir G:\secure\NoSmokeTimer`
- 동일 alias가 이미 존재하면 실패하거나 덮어쓰기를 물을 수 있습니다. 새로운 파일로 생성하거나 alias를 바꾸세요.
- 생성 후 파일 확인: `dir G:\secure\NoSmokeTimer\release-upload.jks`

---
## 3) 서명된 AAB 빌드
```bat
gradlew.bat clean :app:bundleRelease --no-daemon --console=plain
```
산출물 경로
- AAB: `app\build\outputs\bundle\release\app-release.aab`
- 매핑: `app\build\outputs\mapping\release\mapping.txt`
- 해시(검증):
```bat
certutil -hashfile app\build\outputs\bundle\release\app-release.aab SHA256
```

---
## 4) 업로드 키와 앱 서명 키
- Play App Signing 사용 시 Google이 **앱 서명 키**를 보관하고, 여러분은 **업로드 키**(이 문서의 jks)로만 서명해 업로드합니다.
- 최초 등록 시 업로드 인증서(PEM 또는 SHA-1/SHA-256 지문)를 콘솔에 제출해야 합니다.

인증서 내보내기(예)
```bat
"%USERPROFILE%\AppData\Local\Programs\Android Studio\jbr\bin\keytool.exe" -exportcert -rfc ^
 -alias nosmoketimer_upload ^
 -keystore G:\secure\NoSmokeTimer\release-upload.jks ^
 -file G:\secure\NoSmokeTimer\upload_cert.pem
```

---
## 5) 백업(3-2-1) 요약
- 3개 사본: 원본 + 백업 2개
- 2종 매체: 로컬 디스크 + 클라우드/USB
- 1개 오프사이트: 물리적으로 다른 장소에 보관

레포 제공 스크립트로 자동 백업(해시/ZIP/USB)
```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\backup_keystore.ps1 `
  -KeystorePath "G:\secure\NoSmokeTimer\release-upload.jks" `
  -BackupBaseDir "G:\Workspace\NoSmokeTimer\keystore-backups" `
  -UsbDrive "E:" `
  -CreateEncryptedZip
```

> 클라우드는 반드시 암호화(7-Zip AES‑256, Cryptomator, Personal Vault 등)를 사용하세요.

---
## 6) 자주 묻는 질문(FAQ)
- Q. 다른 드라이브(`D:`, `E:`)여도 괜찮나요?
  - A. 예. 절대경로만 정확히 `KEYSTORE_PATH`에 넣으면 됩니다. 고정 디스크 권장.
- Q. 경로에 공백이 있어도 되나요?
  - A. 예. 위와 같이 `set "VAR=값"` 형태로 따옴표로 감싸면 안전합니다.
- Q. 이동식 드라이브에 두면?
  - A. 원본 보관은 비추천(문자 변경/분실 위험). 백업 전용으로 사용하세요.
- Q. 빌드가 "Unsigned release build blocked"로 실패합니다.
  - A. 2) 환경변수를 재설정하고, `:app:printReleaseSigningEnv`로 인식 여부를 먼저 확인하세요.

---
## 7) 체크리스트
- [ ] 키스토어 원본을 레포 외부 보호 폴더에 저장
- [ ] 환경변수 4개 설정 및 `:app:printReleaseSigningEnv`로 확인
- [ ] `:app:bundleRelease` 빌드 성공
- [ ] 업로드 인증서 제출(최초 1회)
- [ ] 3-2-1 백업 완료(클라우드 암호화 + USB + 비밀번호 관리자 메모)

---
## 8) 참고 문서
- `docs/PLAY_CONSOLE_UPLOAD_GUIDE.md` — Play 업로드 전 과정을 단계별로 안내
- `docs/KEYSTORE_SECURITY_GUIDE.md` — 보안/백업 모범 사례(상세)
