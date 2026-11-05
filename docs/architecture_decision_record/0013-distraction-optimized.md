# ADR 0013 — 주행 중에도 쓰는 앱: DO 선언 + 코드로 제한(2026-11-04 제안 · 승인 대기)

> 원본 규칙: `pleos-vehicle-sdk` 5절 · `pleos-review-checklist` 3절 · `sensors/scripts/check_manifest.py`. 이슈 #2.

## 무엇이 있었나

- `check_manifest.py` 가 이름에 `distraction` 이 들어간 meta-data 를 위반으로 잡았음(「주행 중 사용 선언 meta-data 는 두지 않음 — 샘플 앱 기준」).
- Pleos 「지역별 주행 규제 정책」은 DO(distraction optimization)를 선언하지 않은 앱을 주행(Drive) 중 사용 불가로 봄.
- 회의(2026-11-03) P0 2-3(주행 중 / 정차 중 UI 분기) · #4 「주행 중에도 「여기로 안내」 한 번 터치」 · #6 주행 전용 화면이 모두 주행 중 화면을 전제로 함 → 지금 센서대로면 만들 수 없음.

## 결정

- 주행 중에 쓰는 화면이 있는 액티비티는 `<meta-data android:name="distractionOptimized" android:value="true"/>` 를 선언함(2026-11-04 · 최성호).
- 선언은 「주행 중에도 열림」일 뿐 — 무엇을 숨길지는 코드가 주행 판정(기어 + `CarUxRestrictions` · 모름은 주행처럼)으로 정함.
- `check_manifest.py` 는 DO meta-data 를 더 이상 잡지 않음. 「선언했는데 제한 코드가 없음」은 심사관 리뷰(`pleos-review-checklist` 3절 첫 줄)가 봄.

## 이유

- Pleos 주행 규제 정책: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy — DO 미선언 앱은 주행 중 사용 불가.
- 주행 규제 개발 가이드: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide — `CarUxRestrictions` 의 `ActiveRestrictions` 로 제한.
- 「샘플 앱 기준」은 이 앱(주행 중 기록 · 길안내)의 근거가 아님(CLAUDE.md 「다른 프로젝트의 규칙 · 경험을 근거로 끌어오지 않음」).

## 버린 안

| 안 | 버린 이유 |
|---|---|
| DO 를 선언하지 않는 정차 전용 앱 | 주행 중에 앱이 가려짐 — 회의 P0 2-3 · #4 · #6 이 성립하지 않음 |
| 센서가 「DO 선언 + 제한 코드 없음」을 기계로 잡음 | 매니페스트만 보는 센서로는 화면별 제한 코드를 판정할 수 없음 · 반복 지적이 쌓이면 Compose UI 테스트 · Konsist 로 센서화 검토 |

## 바뀌는 파일

| 파일 | 바뀌는 것 |
|---|---|
| `.claude/skills/sensors/scripts/check_manifest.py` | DO meta-data 위반 판정 삭제 |
| `.claude/guardrails/tests/test_check_manifest.py` | 액티비티의 DO meta-data 통과 사례 추가 |
| `.claude/skills/pleos-vehicle-sdk/SKILL.md` | 5절 DO 선언 줄 · 7절 Google Fused 와 Pleos FusedLocation 구분 |
| `.claude/skills/pleos-review-checklist/SKILL.md` | 3절 첫 질문(DO 선언 ↔ 제한 코드) |
| `CLAUDE.md` | 머리말에 한 줄 |
