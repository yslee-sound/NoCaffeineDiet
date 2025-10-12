# Changelog

모든 눈에 띄는 변경 사항은 이 파일에 기록됩니다.
형식: [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/) & Semantic Versioning.

## [Unreleased]
### Added
- (예정) Crashlytics / Analytics 도입
- (예정) 날짜/통계 계산 단위 테스트 확대

### Changed
- (예정) 접근성 개선 (터치 타겟/콘트라스트)

### Fixed
- 상세 화면: 기록 삭제가 되지 않던 버그(JSON 키 불일치 start_time/end_time vs startTime/endTime) 수정
- (예정) 통계 경계 케이스 보정

## [1.0.0] - 2025-10-05
### Added
- 금연(금연 시작~현재) 기록 생성/조회 기본 플로우
- 주/월/년/전체 통계 화면(성공률, 목표 진행률 등)
- 목표 진행률/성공률 계산 로직(기초)
- 기본 SplashScreen 적용
- Jetpack Compose UI 구조(Material3)

### Changed
- Release 빌드: R8 minify / resource shrink 활성화
- Gradle: configuration cache, build cache, parallel build 활성화

### Security
- 서명정보 환경변수 기반 로딩(로컬 미설정 시 unsigned 빌드 허용)

## 형식 가이드
버전 태그 예: v1.0.0
- Added: 새 기능
- Changed: 기존 기능 변경(비 호환 여부 PR에서 명시)
- Deprecated: 곧 제거될 항목
- Removed: 제거된 항목
- Fixed: 버그 수정
- Security: 취약점 관련 또는 보안 영향 변경

[Unreleased]: https://example.com/compare/v1.0.0...HEAD
[1.0.0]: https://example.com/releases/v1.0.0
