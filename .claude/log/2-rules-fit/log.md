# #2 [하네스 피드백] 규칙 스킬 · 센서가 travel-crumbs 실제 구조와 어긋남
- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/2
- 종류: 하네스 피드백
- 담당: 최성호
- 브랜치: harness/2-rules-fit

## 반영 끝 — 2026-11-05 · 최성호
- 산출물: 하네스 16파일 · ADR 0013(DO 선언) · ADR 0014(기기 안 원본 · 서버는 로그인 · 추천 집계만)
- 에이전트: 없음 — `harness:evolve` 스킬이 이 세션에 설치돼 있지 않아 메인이 이슈 완료 조건대로 직접 고침
- 센서: guardrails 시험(pytest) 23 통과 · `check_repo_hygiene` 위반 0 · `check_secrets` 위반 0 · `check_skill_frontmatter` 위반 0 · `check_manifest`(앱 매니페스트) 위반 0(전에는 `NAVI_ROUTE` 1건)
- 리뷰 지적: 없음(하네스 흐름)
- 막힘: 이슈 #2 본문을 2026-11-05 에 고침(서버 제거 → 서버 유지 · 추천 집계만, #4 리뷰 반영 · 팀이 11-05 회의에서 서버 사용을 정함 D19 · 한때 「계약 + Mock」을 검토했다가 되돌림). guard 가 `_workspace/current` 를 바꾸는 명령과 브랜치 생성을 한 명령에 묶은 것을 막음 → 나눠서 다시
- 사람 결정: DO 선언(2026-11-04) · 서버 범위(2026-11-05) — 둘 다 최성호
- 남은 결정: `NAVI_ROUTE` · `NAVI_ROUTE_SEARCH` 의 근거인 #4 스펙(spec PR #5)이 아직 머지 전 — 이 PR 은 #5 뒤에 머지. `pleos-vehicle-sdk` 5절 「`screenOrientation` 미지정(portrait)」은 #6 의 landscape 결정과 어긋남 — 이 이슈 범위 밖 · #6 쪽에서 하네스 피드백으로
- 활동: 도구 호출 34 · 실패 0 · guard 차단 0(브랜치 생성 차단 1건은 바꾸기 전 폴더 `_workspace/4` 기록에 남음)
- 다음: PR(→ develop) · 상대 승인 · 머지는 사람
