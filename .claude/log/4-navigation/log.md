# #4 [기능] 장소를 차량 내비 목적지로 설정(NaviHelper 길안내 연동)
- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/4
- 종류: 기능
- 담당: 최성호
- 브랜치: navigation/main · spec · impl

## 1 스펙 끝 — 2026-11-04 13:35 · 최성호
- 산출물: `docs/spec/navigation/artifacts/set-destination.md` · spec PR(→ navigation/main)
- 에이전트: spec-writer × 2(첫 작성 · 미확정 답 반영)
- 센서: 실행 안 함(스펙 단계) · 참고로 `check_manifest.py` 는 develop 기준 이미 빨강 1건(`NAVI_ROUTE` 허용 밖)
- 리뷰 지적: 없음(스펙 단계)
- 막힘: 없음. 노션 회의록은 spec-writer 가 읽지 못해 이슈 본문만 근거
- 사람 결정: 서버 없음 · 시연용 경량 앱 / 주행 중에도 한 번 터치 / 진입점은 홈 임시 목록 / 미확정 U2 ~ U12 추천안대로 확정 · U1(임시 장소 구체 값)만 남김
- 다음: 사람이 spec PR 확인 · 머지 → 2단계 구현. 매니페스트 센서 녹색은 하네스 이슈 #2(ALLOWED · DO) 머지가 전제
- 활동: 도구 호출 95 · 실패 0 · guard 차단 0

## 1 스펙 수정(리뷰 반영) 끝 — 2026-11-05 · 최성호
- 산출물: `docs/spec/navigation/artifacts/set-destination.md` 고침 · spec PR #5 에 추가 커밋
- 계기: spec PR #5 리뷰(giboJK) 2건 — 임시 고정 장소의 뜻 · 빈 상태 / 추천용 서버 전송
- 에이전트: spec-writer × 2(리뷰 반영 · 미확정 답 반영)
- 센서: 실행 안 함(스펙 단계)
- 사람 결정: 「가 볼 장소」는 사용자가 핀 고정(정차 때만 · 최대 3곳) · 빈 상태는 안내 한 줄 · 서버 전송 없음 · 기기 안 안내 요청 기록(장소 id · 시각만 · 시작됨 때만 · 최근 100건) · U16 ~ U23 추천안대로 확정 · U26(#6 주행 전용 화면 충돌)은 #6 담당에게 주행 화면 안내 버튼을 제안하고 답 대기
- 남은 결정: U1(후보 장소 구체 값) · U24(새 문구) · U25(이슈 본문 고침) · U26(#6 답)
- 막힘: 없음
- 다음: 리뷰 답글 · #7 제안 댓글(사람 확인 뒤) → 사람이 #5 확인 · 머지 → 2단계

## 1 스펙 수정(서버 전송) 끝 — 2026-11-05 · 최성호
- 산출물: `docs/spec/navigation/artifacts/set-destination.md` 고침(영역 app + server · 5절 API 계약 새로) · spec PR #5 에 추가 커밋
- 계기: 기보님 리뷰 2(추천용 서버 전송)를 적극 수용하기로 사람 결정 — 앞 커밋의 「서버 전송 없음(U15)」을 뒤집음
- 에이전트: spec-writer × 2(서버 전송 반영 · 미확정 답 반영)
- 센서: `check_api_contract.py` 를 계약 초안에 돌림 — 계약 2 · 앱 호출 0 · 위반 0
- 사람 결정: app + server(server/ FastAPI 를 #4 가 처음 만듦 · 기록 수신 · 장소별 건수 조회) · 전송 칸 장소 id · 시각 · 계정(좌표 · 이름 · 주소 없음) · U27 ~ U34 추천안대로 확정(Room 전송 대기 큐 · 재시도 · 365일 보관 · Docker PostgreSQL 등)
- 남은 결정: U1 · U24 · U25(이슈 본문 고침) · U26(#6 답) · U28 · U31 은 #1 담당(기보)과 맞춤
- 막힘: `docs/api/openapi.json` 초안 복사 명령이 권한 거부 — 계약은 스펙 5절에만 둠
- 다음: #5 답글 · #4 · #2 본문 고침(사람 확인 뒤) → 사람이 #5 확인 · 머지 → 2단계(app-builder · server-builder)

## 1 스펙 수정(#6 답변 반영) 끝 — 2026-11-05 · 최성호
- 산출물: `docs/spec/navigation/artifacts/set-destination.md` 고침 · spec PR #5 에 추가 커밋
- 계기: 희주님(#6 담당)의 PR #7 답변 — 주행 전용 화면에 길안내 버튼을 넣지 않기로 함(#6 D16) · 정차 홈 순서(D17) · 진단 카드 debug 전용(D9)
- 에이전트: spec-writer × 1(재호출)
- 센서: 실행 안 함(스펙 단계)
- 사람 결정: 「여기로 안내」는 정차 홈에서만(2026-11-04 「주행 중에도 한 번 터치」를 뒤집음 · 결정 기록 U35) · 홈 순서는 #6 기준 · 진단 카드 debug 전용 · 주행 판정은 #6 것 사용 · #6 impl 먼저 머지
- 남은 결정: U1(후보 장소 값) · U24(새 문구) · U25(이슈 본문 — 이번에 고침) · U39(서버 유지 여부 — 11-05 14:00 회의에서 팀 확인)
- 막힘: 없음
- 다음: PR #7 답글 · 이슈 #4 본문 반영 → 사람이 #5 확인 · 머지 → 구현은 #6 impl 머지 뒤

## 1 스펙 수정(서버는 계약만 · Mock 시연) 끝 — 2026-11-05 · 최성호
- 산출물: `docs/spec/navigation/artifacts/set-destination.md` 고침(영역 app 하나 · 서버 구현은 후속 이슈 · 계약은 5절에 「후속 서버가 따를 계약」) · spec PR #5 에 추가 커밋
- 계기: 기보님 조언 — 서버 없이 Mock 데이터로 시연하며 기획을 다듬으면 수정 범위가 앱뿐이라 가벼움
- 에이전트: spec-writer × 1(재호출)
- 센서: 실행 안 함(스펙 단계)
- 사람 결정: U39 = (c) 계약만 두고 Mock 시연(선택지 (a) 서버 유지 (b) 접음 중) · 최성호 결정 · 팀 확인 필요 · 서버 관련 결정 U15 · U27 ~ U34 는 「서버 구현 시 적용」 표시로 남김
- 남은 결정: U1(후보 장소 값) · U24(새 문구) · U25(이슈 본문) · U40(Mock 집계 보관 — 추천: 장소별 「내 안내 수」만 DataStore) · U41(시연 확인 수단 — 추천: debug 진단 카드 한 줄 · 자리는 #6 몫)
- 막힘: 없음
- 다음: 이슈 #4 본문 반영 · 하네스 ADR 0014 · #2 · PR #9 문구 고침 → 사람이 #5 확인 · 머지 → 구현은 #6 impl 머지 뒤

## 1 스펙 수정(U40 · U41 확정 · #6 머지 반영) 끝 — 2026-11-05 · 최성호
- 산출물: `docs/spec/navigation/artifacts/set-destination.md` 고침 · spec PR #5 에 추가 커밋
- 계기: 최성호 — 새 미확정 U40 · U41 추천안대로 확정 · #6 스펙(PR #7)이 머지돼 참조 표기를 맞춤
- 에이전트: spec-writer × 1(재호출)
- 센서: 실행 안 함(스펙 단계)
- 사람 결정: U40 = 받으면 큐에서 지우고 장소별 「내 안내 수」만 DataStore · U41 = debug 진단 카드에 「안내 건수(Mock)」 한 줄(자리는 #6 몫 · 희주님과 맞춤 · 못 맞추면 단위 시험만)
- 남은 결정: U1(후보 장소 값) · U24(새 문구) · U25(이슈 본문 — 고침 반영 확인) · U39 팀 확인 · U41 #6 담당과 자리 맞춤
- 막힘: 없음
- 다음: 사람이 #5 확인 · 머지 → #6 impl 머지 뒤 구현(app 하나)

## 1 스펙 수정(여행 시나리오 확정 — 목적지 = 핀) 끝 — 2026-11-05 · 최성호
- 산출물: `docs/spec/navigation/artifacts/set-destination.md` 고침 · spec PR #5 에 추가 커밋
- 계기: 최성호가 제품 시나리오를 분명히 함 — 여행 중 목적지가 핀으로 박히고 · 끝나면 로드맵 · 공유 · 다른 사람 여행지 참고. 지금 스펙의 「핀」(미리 고정하는 즐겨찾기형)과 「안내 시작 기록」(장소별 건수)이 이 시나리오와 어긋났음
- 에이전트: spec-writer × 1(재호출)
- 센서: 실행 안 함(스펙 단계)
- 사람 결정: 용어 — 「가 볼 장소」(앞으로 갈 곳)와 「핀」(진행 중 여행의 목적지 기록: 장소 id · 시각 · 순서)을 나눔 · 안내 시작 = 핀 1건(기록 중일 때만) · Mock 시드를 시드 여행 2~3건으로 · 이에 따라 U20 · U22 · U23 · U29 · U40 의 기록 부분을 뒤집음(U46) · 로드맵 #3 · 공유 업무 4 · 여행 · 기록 상태 #6 몫
- 남은 결정: U1 · U24(문구 후보: 「담기」 · 「빼기」 · 「가 볼 곳 고르기」) · U42(일시정지 중 핀 — 추천 남기지 않음) · U43(핀 저장 — 추천 Room · 상한 없음 · 삭제는 #3) · U44(후속) · U39 · U41
- 막힘: 없음
- 다음: 이슈 #4 본문 반영 · 시나리오를 희주님과 공유(#6 「기록 중」 · 「이 장소 기록」이 같은 핀 흐름) → 사람이 #5 확인 · 머지

## 1 스펙 수정(서버 사용 팀 결정) 끝 — 2026-11-05 · 최성호
- 산출물: `docs/spec/navigation/artifacts/set-destination.md` 고침(영역 app + server 로 되돌림) · navigation/spec(origin/navigation/main 병합 뒤) 커밋 · 후속 spec PR 은 사람 지시로 미룸
- 계기: spec PR #5 는 17:35 에 giboJK 가 머지(시나리오 수정 2커밋은 그 뒤 푸시라 빠짐) · 11-05 회의에서 팀이 서버(FastAPI) 사용 · 첫 도입은 #4 로 정함(#6 스펙 D19 · 최성호 확인)
- 에이전트: spec-writer × 1(재호출)
- 센서: `check_api_contract.py` 를 계약 초안에 — 위반 0
- 사람 결정: U39 뒤집음 — (c) 계약만 + Mock → (a) 서버 유지(팀 결정) · U27 · U28 · U31 기보님 합의됨(PR #5 리뷰) · 핀 시나리오 유지(핀은 기기 원본 · 보냄 상태 대기 · 끝 · 거절) · #6 D18(창 2개 영역 전제) 반영
- 남은 결정: U1 · U24 · U42 · U43 · U44(추천: 순서 · 여행 id 안 보냄) · U47(추천: 시드 여행은 서버 개발용 시드 명령)
- 막힘: 없음
- 다음: 사람 지시 — 이슈 · PR 은 미루고 #6 구현(app-shell/impl) 위에서 #4 앱 구현 · 빌드부터

## 2 구현(app) · 6 PR 끝 — 2026-11-06 · 최성호
- 산출물: navigation/impl `8151f54`(58파일) · impl PR(→ navigation/main · 리뷰어 giboJK) · 보고서 `_workspace/4/02_builder_app_report.md`
- 브랜치 예외: navigation/impl = navigation/spec(미머지 스펙 포함) + origin/app-shell/impl(#6 · PR #13 머지 전) 병합 — 사람 지시로 #6 위에서 바로 구현
- 에이전트: builder(app-builder) × 1 · R1 수정으로 재호출 중 · 심사관 · 구조 리뷰는 세션 종료로 중단 → R1 수정 뒤 다시
- 센서: gradle lint · 단위 143(#4 70) · assembleRelease 녹색 · secrets · hygiene · frontmatter 녹색 · check_manifest 빨강 3(하네스 PR #9 미머지 · #9 판으로 녹색) · check_api_contract 는 openapi.json 없음(초안으로 녹색)
- 에뮬레이터: U1 5곳 모두 경로 탐색 · 4곳 onRouteStarted · POI 빈 채 시작 됨 · 고정 재실행 유지 · 주행 화면 전환 · 핀 순서 1 ~ 5 · 서버 없을 때 재시도 예약
- 사람 결정: 범위 = app(서버 칸 다음 단계) · Codex 검증 안 함 · R1 = 내비 상태 대조(2026-11-06)
- 막힘: R1 — 내비가 앞에 오면 약 11초 뒤 앱이 얼려져 늦은 onRouteStarted 유실 · S1 길안내 대기 한도 30초 임시
- 다음: R1 수정 커밋 · 리뷰 2관점 · 스펙 반영(U4 넓힘 · S1) → 같은 PR 에 덧붙임

## 4 수정 끝 — 2026-11-06 · 최성호
- 산출물: navigation/impl 8cb98b6(R1 내비 상태 대조) · 3d23b39(스펙 U4 넓힘 · U48 · JobScheduler) · 9f48c99(리뷰 1회차) · 678bde9(리뷰 2회차) · PR #14 본문 · 리뷰 결과 댓글
- 에이전트: builder × 3(R1 · 1회차 · 2회차) · pleos-reviewer × 2 · structure-reviewer × 2 · spec-writer × 1
- 센서: gradle lint · 단위 162 · assembleRelease 녹색 · secrets · hygiene 녹색 · check_manifest 는 하네스 #9 판으로 녹색
- 리뷰 지적: 1회차 심사 반려 1(TIMEOUT 뒤 늦은 시작) · 확인 6 · 구조 확인 2 → 2회차 심사 반려 0 · 새 확인 2 · 구조 위반 0 · 새 확인 2 · 센서 후보 1(구조 S1 ViewModel 의 android.util.Log — 두 번째 지적)
- 막힘: 앞 세션 종료로 리뷰 2건 중단 → 다시 돌림 · 앱 얼림(cached app freezer)으로 늦은 onRouteStarted 유실(R1) → 내비 상태 대조로 해결
- 사람 결정: R1 = 내비 상태 대조 · debug 카드 「안내 건수를 불러오지 못했어요」 그대로 둠 · Codex 검증 안 함
- 남은 결정: `_workspace/4/05_open_findings.md` — U48(30초) · U1 · 삭제 경로(#1 · #3) · 동의 게이트(#1) · 요청 상태 domain 분리 · S1 하네스
- 다음: 시연 리허설(U1 5곳 실기 · T96) · #13 · #9 머지 뒤 rebase · 서버 단계

## A-14 실패 폴백(범위 넓힘) · 1 스펙 · 2 구현 · 4 수정 끝 — 2026-11-07 · 최성호
- 계기: 2026-11-06 팀 미팅 데이터 표준 v0.3 · ERD v0.3 · 와이어프레임 v5(A-14 길안내 연결과 실패 폴백 · 담당 성호) — 사람 결정으로 #4 범위 넓힘(결정 기록 U50) · 이슈 본문 고침은 초안만(미게시)
- 산출물: navigation/impl a1ec9dd(스펙 · U50 ~ U62 · T97 ~ T114) · f8775b4(「다시 시도」 · 「더미 경로」(debug) · NAVI_REQ_STATUS 앱 상수) · bfe770e(리뷰 1회차) — 로컬 커밋 · 푸시 전
- 에이전트: spec-writer × 1 · builder(app) × 2(구현 · 1회차 수정) · pleos-reviewer × 2 · structure-reviewer × 2
- 센서: gradle lint · 단위 191 · assembleRelease 녹색 · hygiene · secrets · frontmatter 녹색 · check_api_contract 초안으로 녹색 · check_manifest 이 브랜치 판 빨강 3 / develop(#9) 판 녹색
- 에뮬레이터: T113(실패 → 더미 경로 → 닫기 → 다시 시도 → 안내 시작) · T103(주행 전환 뒤 패널 닫힘) 통과 · T109 일부 · T108 미실시
- 리뷰 지적: 1회차 심사 반려 0 · 확인 3 · 구조 위반 0 · 확인 4 → 2회차 심사 해소 1 · 남은 확인 2 · 구조 해소 2 · 새 확인 1 · 센서 후보 3(심사 A1 release 잔존 2회 · 구조 N2 ViewModel 비대 2회 · S1 ViewModel Log 3회)
- 사람 결정: U55 더미 경로 debug 빌드에만 · U57 와이어프레임 문구 그대로 — U51 ~ U54 · U56 은 스펙 작성자 추천을 기본값으로 쓴 것이고 사람이 확인한 것이 아님(2026-11-07 정정 · U52 패널 방식은 같은 날 사람이 「전용 화면까지」로 바꿈)
- 남은 결정: `_workspace/4/05_open_findings_a14.md` — A1 · A3 · R3-2 · N2 · R4-1 · S1 · U58 ~ U62(팀) · 이슈 본문
- 막힘: 없음(에뮬레이터에서 실패를 만들려고 요청 직후 기본 내비 앱 강제 종료 → TIMEOUT)
- 다음: 푸시(PR #14 에 반영) · 이슈 #4 본문 고침 · 팀 결정 U58 ~ U62

## 입구 분리(구조 정리) · 4 수정 끝 — 2026-11-07 · 최성호
- 계기: 와이어프레임 v5 가 같은 길안내 동작을 음성 · A-13 「다시 가기」 · B-09 「이 장소만 추가」 에서도 부르게 함 + 구조 리뷰가 두 바퀴 연속 지적한 N2(HomeViewModel 비대) · R4-1 — 사람 선택 「안내 입구 분리」 · 동작 변화 0 · 스펙 문서 변경 없음
- 산출물: navigation-guide/impl 5756e10(`domain/GuideRequestTracker` · `GuidePorts` · `di/GuideModule` · HomeViewModel 406 → 275줄) · 2f5cd83(리뷰 1회차) — 로컬 커밋 · 푸시 전 · navigation-a14/impl(PR #20) 위에 쌓음
- 에이전트: builder(app) × 2 · pleos-reviewer × 1 · structure-reviewer × 2
- 센서: gradle lint · 단위 198 · assembleRelease 녹색 · hygiene · secrets · frontmatter 녹색 · check_api_contract 초안으로 녹색 · check_manifest develop 판 녹색
- 에뮬레이터: T113 통과 · Activity 만 끝냈다 다시 열기(요청 중 · TIMEOUT 뒤 5분 안) 크래시 없음 · 핀 중복 없음
- 리뷰 지적: 1회차 심사 반려 0 · 확인 1(P1 Activity 수명 → 에뮬레이터로 닫음) · 구조 위반 0 · 확인 4(G1 ~ G4) → 2회차 구조 해소 G3 · G4 · 새 확인 0 · 이전 N2 · R4-1 · S1 해소 · 센서 후보 1(ViewModel Log import 금지 · 재발 방지)
- 사람 결정: 안내 입구 분리(2026-11-07)
- 남은 결정: `_workspace/4/05_open_findings_guide.md` — G1 · G2=R3-2(하네스 규칙 문장) · 스펙 3.1 · 3.2(요청 상태 출처 · Activity 재진입 때 결과 문구 유지) · 정차 거르기(음성 · 다시 가기 붙일 때)
- 막힘: 없음 · BACK 키가 이 플랫폼에서 MainActivity 를 끝내지 않아 `am start --activity-clear-task` 로 Activity 만 끝냄
- 다음: 푸시 · PR(PR #20 머지 순서 확인) · 음성 · 「다시 가기」 · 장소 여러 곳 안내가 이 입구를 쓰도록
