# #19 [하네스 피드백] 데이터 표준 정의서를 하네스 규칙에 넣음
- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/19
- 종류: 하네스 피드백
- 담당: giboJK
- 브랜치: harness/19-data-standard

## 반영 끝 — 2026-11-07 19:35 · giboJK
- 산출물: `docs/data/`(standard.md · erd.md · archive/standard-v0.2.md · README.md) · `CLAUDE.md` · `.claude/README.md` · 스킬 5(server-architecture · android-architecture · pleos-review-checklist · sensors · team-workflow) · 에이전트 3(spec-writer · builder · structure-reviewer) · 센서 2(`check_data_standard.py` 신설 · `check_api_contract.py` 좌표 규칙 제거) · `.github/workflows/ci.yml` · ADR 0015 · 0016 · ADR 0014 에 대체 표시
- 에이전트: 없음(하네스 흐름 — `harness:evolve` 를 메인이 직접 수행). spec-writer · builder · 리뷰어 단계 없음
- 센서: check_repo_hygiene 녹색 · check_secrets 녹색 · check_skill_frontmatter 녹색 · check_data_standard 녹색(server/ 없음 · 용어 75개 읽음) · test_guard 11 OK · test_trace 5 OK · test_check_manifest 7 OK · YAML 5개 파싱 OK · check_api_contract 연기 시험(좌표 통과 · userId 위반 유지)
- 리뷰 지적: 없음(리뷰어 단계 없는 흐름)
- 막힘: 없음. doctor 가 FAIL 5 로 0단계에서 멈췄고, git autocrlf · longpaths 둘을 고쳐 해소. 남은 FAIL 3(SDK 플랫폼 34 · AVD · Docker)은 앱 · 서버 빌드용이라 이 변경과 무관 — 사람 몫으로 남음
- 사람 결정: 저장 위치 = 서버 PostgreSQL(로컬) · 로그인 = Pleos 아이디 값을 자체 DB 에 저장 · 기본키 = 정의서대로 시퀀스 + 외부 `_uuid` · 삭제 = 논리 삭제와 하드 삭제를 나눔 · 판 관리 = `standard.md` 가 항상 최신판 · 지난 판은 `archive/`
- 다음: PR → develop · 상대 승인 · 머지는 사람
- 활동: 도구 호출 35 · 실패 0 · guard 차단 0
