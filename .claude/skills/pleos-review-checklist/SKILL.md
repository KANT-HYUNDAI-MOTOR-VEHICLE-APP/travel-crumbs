---
name: pleos-review-checklist
description: Pleos 앱 마켓 심사 가이드 5항목(콘텐츠 안전 · 기능/성능 안정성 · 운전자 주의 분산 최소화 · 등록 정보 · 개인정보/보안)을 <앱 이름> 코드에 대조하는 점검 질문 · 발견 표 형식 · 심각도 · 「린트로 만들 수 있나」 판단 기준. 서버 쪽은 개인정보 · 보안(좌표 · VIN · Pleos 토큰 · 로그 · LLM 프롬프트 · 동의 · 삭제)만 본다. pleos-reviewer 가 심사관 리뷰를 시작할 때 반드시 읽는다. 구조 규칙(레이어 · 파일 배치)은 android-architecture · server-architecture 가 맡는다.
---

# Pleos 심사 점검표

근거: 심사 가이드 https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/ (항목별 하위 문서) · 설계 가이드. 발견은 가이드 문장에 닿아야 함 — 닿지 않으면 발견이 아님.

## 절차

1. `git diff --name-only origin/<대상 브랜치>...HEAD` 로 바뀐 파일. 규칙이 걸치는 파일(매니페스트 · `network_security_config` · 로그 설정 · `prompts/` · 동의 화면 · 삭제 API)은 바뀌지 않았어도 열어 봄.
2. 아래 다섯 표를 순서대로. 질문마다 코드 근거(파일:줄)를 찾음. 못 찾으면 발견 없음.
3. 반복 여부: `grep -l "<규칙 이름>" _workspace/*/03_pleos-reviewer_findings.md` — 이전 바퀴 · 이전 이슈에 같은 규칙 지적이 있으면 「센서 후보」.
4. 표를 `_workspace/<이슈>/03_pleos-reviewer_findings.md` 에. 끝 줄 `발견 수: 반려 N · 확인 M`.

## 1. 콘텐츠 안전

| 질문 | 어디서 보나 |
|---|---|
| 앱이 보여 주거나 읽어 주는 텍스트(`assets/*.json` · LLM 출력)에 폭력 · 노출 · 공격적 표현이 없나 | assets · `prompts/` · LLM 출력 스키마의 금칙 처리 |
| 아동(Kids) 대상 콘텐츠에 보호자 동의 없이 부적절한 내용이 닿는 길이 없나 | 콘텐츠 출처가 assets(검수) 또는 사람 검수 뒤인가 |
| 제목 · 썸네일 · 미리보기(있다면)가 내용과 맞나 | 화면 · strings |

## 2. 기능 · 성능 안정성

| 질문 | 어디서 보나 |
|---|---|
| 기능이 스펙대로 동작하나 — 시험 케이스 표의 행마다 테스트가 있나 | `docs/spec/` 표 ↔ 테스트 이름 번호 |
| 임시 파일 · 디버그 화면 · 시험용 콘텐츠가 release 에 남아 있나 | `BuildConfig.DEBUG` 분기 · assets · 로그 레벨 |
| 백그라운드 전환 시 위치 수집 · 화면 갱신을 멈추나 · 복귀 시 상태 복원하나 | ViewModel 효과 · `SavedStateHandle` |
| 주행 중 CPU 5% 미만 · RAM 400 MB 이하를 깨는 구조(폴링 · 타이머 · 1초 미만 위치 갱신)가 없나 | `AppConfig` · 위치 요청 간격 · `while(true)` |
| 오디오 포커스 획득 · 상실 대응이 있나 | `TtsNarrator` |
| 메타데이터(앱 이름 · 설명)가 실제 서비스와 같나 | strings · 등록 콘솔(범위 밖이면 「확인」) |

## 3. 운전자 주의 분산 최소화

| 질문 | 어디서 보나 |
|---|---|
| 주행(DRIVING · UNKNOWN) 중 영상 · 애니메이션 · 자동 화면 전환 · 긴 글이 보이나 | `*Screen.kt` 의 상태 분기 · `AnimatedVisibility` · 스크롤 글 |
| 주행 중 정차 전용 화면 · 로그인 · 기록 화면 · 키보드 입력이 열리나 | `navigation/` 주행 중 허용 목록 · 각 화면의 가드 |
| UNKNOWN 을 PARKED 처럼 다루는 곳이 있나(위반) | `ObserveDrivingStateUseCase` · 화면 분기 |
| CarUxRestrictions 를 읽나 · 기어와 둘 다 보나 | `data/vehicle/` |
| 정차 중 버튼이 크고 고대비 · 한 번 터치로 끝나나 | 정차 전용 화면 · 로그인 화면 |
| 헤드업 알림 · 주행과 무관한 알림을 보내나(위반) | `NotificationManager` 사용 |
| 주행 중 미디어(영상 · 게임 · 브라우저) 기능이 있나 — WebView 로그인은 정차 중에만인가 | `PleosLoginWebScreen` 가드 |

## 4. 앱 마켓 등록 정보

| 질문 | 어디서 보나 |
|---|---|
| 위치 · 개인 데이터를 쓰는 앱 — 약관 · 개인정보 처리방침 링크 · 문구가 앱에 있나(제출 대상) | 동의 화면 · strings |
| 앱 설명 · 카테고리(Music/Audio)와 실제 기능이 맞나 | 「확인」(등록 콘솔은 범위 밖) |

## 5. 개인정보 · 보안(앱 + 서버)

| 질문 | 어디서 보나 |
|---|---|
| 권한이 기능과 1:1 인가 — 허용 목록 밖 권한(위반) · 허용된 권한도 근거가 가리키는 기능이 이 변경에 실제로 있나 | 매니페스트(센서 `check_manifest.py` 결과). 목록 원본은 그 스크립트의 `ALLOWED`(권한마다 근거) · 늘리는 기준은 `sensors` 「권한 허용 목록을 늘리는 길」 |
| 좌표 · 기어 · 속도 · VIN 이 서버로 가나(위반) | 앱 DTO · 서버 schemas · models |
| Pleos 토큰 · clientSecret 이 앱에 있나 · 서버가 Pleos 토큰을 저장하나(위반) | 앱 전체 grep · 서버 `auth/pleos/` · models |
| 로그에 pleos_user_id · 토큰 · 이름 · 좌표 · LLM 원문이 원문으로 남나 | 서버 logging · 앱 `Log.` |
| LLM 프롬프트에 개인 데이터가 들어가나 — 외부 전송 = 제3자 제공 | `prompts/` · `llm/service` 입력 조립 |
| 동의 화면 — 동의 버튼만 동의로 인정하나(화면 닫기 · 뒤로가기가 동의로 처리되면 위반) · 수집 항목 · 목적 · 보유 기간 · 제3자 제공 여부가 적혀 있나 | `ConsentScreen` · `consents` API |
| 철회 · 탈퇴 시 즉시 삭제되나(앱 로컬 + 서버 CASCADE + Pleos 웹훅 처리) | `DELETE /me` · 웹훅 · Room · DataStore 삭제 |
| 전송이 HTTPS 만인가(release) · debug cleartext 가 `10.0.2.2` 로 한정인가 | `network_security_config` |
| 토큰 저장이 Keystore 로 감싼 암호화인가 · 비밀번호 저장이 없나 | `SessionStore` |
| 운전자 교체 시 로그아웃 · 계정 전환이 한 번에 되나 | 기록 화면 |
| 분석 · 광고 SDK · 차량 ID 수집이 있나(있으면 가명화 정책 필요 → 「확인」) | 의존성 · 매니페스트 |

## 표 형식

| 파일:줄 | 어긋난 규칙(항목 · 문장) | 심각도 | 고칠 방향 | 린트로 만들 수 있나 | 반복 |
|---|---|---|---|---|---|
| `app/.../HomeScreen.kt:88` | 3 주행 중 UI — DRIVING 분기에서 정차 전용 버튼 표시 | 반려 | `drivingState == PARKED` 일 때만 렌더 | 예 — Compose UI 테스트(주행 상태 → 버튼 없음) | 2회 → 센서 후보 |

심각도: **반려**(가이드 문장 위반 확실) · **확인**(문서가 애매 · 사람 판단 · 범위 밖).

## 「린트로 만들 수 있나」 기준

- 예: 파일 · 문자열 · 선언의 유무(권한 · import · 칸 이름 · 애너테이션) → 스크립트 · Konsist · import-linter. 상태 분기의 결과 → UI 테스트.
- 아니오: 문장 · 내용의 뜻(콘텐츠 안전 · 문구 적절성 · 메타데이터 정확성) → 사람.
- 같은 규칙 지적 2회 → 「센서 후보」 → `harness:evolve` 에서 `sensors` 스킬로 옮김.

## 참고(항목별 문서)
콘텐츠 안전 …/review-content-safety · 기능 · 성능 …/review-functionality-performance-stability · 주의 분산 …/review-driver-distraction-elements · 등록 정보 …/review-app-registration-info · 개인정보 · 보안 …/review-user-privacy-security (앞은 모두 https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/) · 설계 가이드 보안 https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy · 주행 안전 https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline
