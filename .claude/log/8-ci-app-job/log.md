# #8 [하네스 피드백] CI 에 app 잡이 없어 앱 PR 이 빌드 · 린트 · 테스트 없이 들어감
- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/8
- 종류: 하네스 피드백
- 담당: 노희주
- 브랜치: harness/8-ci-app-job

## 반영 끝 — 2026-11-05 · 노희주
- 산출물: `.github/workflows/ci.yml`(app 잡) · `team-workflow` 9절 · ADR 0012
- 에이전트: 없음(메인 직접 · harness:evolve 스킬이 이 환경에 없어 메인이 이슈의 바라는 동작대로 반영)
- 센서: YAML 파싱 녹색(jobs changes · app · server · common) · guardrails 시험 22 통과 · check_repo_hygiene 녹색 · check_secrets 녹색 · check_skill_frontmatter 녹색 · 로컬 `./gradlew lint testDebugUnitTest assembleRelease` 녹색 · check_manifest 빨강 1(NAVI_ROUTE — 하네스 #2 몫 · 기존 상태)
- 리뷰 지적: 없음
- 막힘: 처음 범위(권한 누락)는 develop 에 이미 있었음(6fb9607) → 범위를 app 잡 추가로 바꿈 · doctor 는 PYTHONUTF8=1 필요(하네스 #3)
- 사람 결정: 이슈 #8 범위를 「app 잡 추가」로 바꿔 진행(2026-11-05)
- 다음: 상대(성호) 승인 · 머지 → #6 · #4 구현 PR 에서 app 잡 실행 확인
- 활동: PR 때 trace.md
