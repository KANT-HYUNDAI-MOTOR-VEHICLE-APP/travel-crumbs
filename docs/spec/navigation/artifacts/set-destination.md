- 이슈: #4
- 기획: 없음(이슈 「참고」의 회의록 2-7 · 2-8 은 노션 · 저장소 밖이라 읽지 않음 · 이슈 본문만 근거)

# 장소를 차량 내비 목적지로 설정(NaviHelper 길안내 연동)

## 1. 요약 (수정: 2026-11-05 서버 전송 결정 반영)

- 홈 맨 위 「가 볼 장소」 구역 = 사용자가 핀 고정한 장소(최대 3곳 · 결정 기록 U16 · 장소 이름 · 주소)와 장소마다 「여기로 안내」 버튼 · 고정한 장소가 없으면 구역 제목 + 안내 한 줄. 고정 · 해제는 정차 중 홈의 「장소 고르기」 구역에서 앱 안 후보 장소(assets JSON 고정 값) 중 고름 · 고정 목록은 기기 안에 저장되어 앱을 다시 켜도 남음.
- 버튼 한 번 터치 → NaviHelper 로 그 장소의 좌표 · 이름 · 주소를 기본 내비 앱에 넘겨 경로 안내 시작 · 시작 · 실패(짧은 이유)를 홈 상태에 남김 · 내비 서비스 미연결이면 버튼 비활성 + 연결 안 됨 문구.
- 「여기로 안내」는 정차 · 주행 · 모름 모두 같은 한 번 터치(의존: #2) · 고정 · 해제는 정차 때만 · 안내가 시작되면 「안내 시작 기록」(장소 id · 시각 · 계정)을 서버로 보냄 — 이 이슈가 `server/`(FastAPI)를 처음 만들고 기록 수신 API · 장소별 안내 건수 조회 API(추천 근거)까지 · 추천 화면은 범위 밖(결정 기록 U15). 좌표 · 장소 이름 · 주소는 서버로 가지 않음 · 좌표는 기기 안 NaviHelper 로만 · 위치 수집 없음.

## 2. 화면

### 2.1 홈(`ui/home`) — 「가 볼 장소」 구역 추가 (수정: 2026-11-05 리뷰 · 미확정 답 반영)

기존 홈(SDK 상태 진단 카드 3개)은 그대로 두고, **맨 위**(제목 바로 아래 · 진단 카드보다 위)에 「가 볼 장소」 구역, 그 아래에 「장소 고르기」 구역(2.2)을 추가. 장소 상세 화면 · 화면 구조(Navigation Graph · NavigationRail)는 만들지 않음(2-1 담당).

#### 요소

| 번호 | 요소 | 내용 |
|---|---|---|
| E1 | 구역 제목 | 「가 볼 장소」 — 고정한 장소가 없어도 늘 보임(결정 기록 U14) |
| E2 | 내비 연결 안 됨 문구 | 내비 서비스 미연결일 때만 · 구역 제목 아래 한 줄 · 「내비 서비스에 연결되지 않았어요」 |
| E3 | 결과 문구 | 마지막 길안내 요청의 결과 한 줄 · 구역 제목 아래(E2 가 있으면 E2 아래) · 대기 상태면 없음 |
| E4 | 고정 장소 항목(고정 수만큼 · 최대 3곳 · 결정 기록 U16) | 장소 이름(한 줄) · 주소(한 줄 · 넘치면 말줄임) · 오른쪽에 「여기로 안내」 버튼 · 고정 해제 버튼은 두지 않음(해제는 2.2) |
| E5 | 「여기로 안내」 버튼 | 항목마다 하나 · 큰 터치 영역 · 고대비(Pleos UI 디자인 가이드의 버튼 크기 · 간격) · 한 번 터치로 끝(확인 대화상자 없음) |
| E6 | 빈 안내 | 고정한 장소가 0곳일 때만 · E4 자리에 한 줄 · 정차 · 주행에 따라 문구 다름(문구 표) |

- 항목 순서 = 고정 순서(결정 기록 U19 — 먼저 고정한 장소가 위 · 새로 고정한 장소는 맨 아래). 정렬 · 검색 · 필터 · 순서 바꾸기 없음.
- 기기 저장에서 고정 목록을 읽기 전에는 E4 · E6 둘 다 그리지 않음 — 빈 안내가 잠깐 보였다 사라지는 깜빡임 막음.
- 저장된 고정 장소 id 가 후보 목록에 없으면(후보 JSON 이 바뀐 경우) 그 항목은 보이지 않고 고정 수에도 세지 않음.
- 글 입력 칸 · 키보드 없음(모든 상태).
- 움직이는 표시(회전 표시 · 애니메이션) 없음 — 요청 중도 글자로만 알림.

#### 문구

아래 문구 표로 확정(결정 기록 U9). 2026-11-05 새로 생긴 문구(E6 · 2.2)는 초안 — U24.

| 자리 | 문구 |
|---|---|
| E1 구역 제목 | 「가 볼 장소」 |
| E5 버튼(평소) | 「여기로 안내」 |
| E5 버튼(그 장소 요청 중) | 「안내 요청 중…」 |
| E2 미연결 | 「내비 서비스에 연결되지 않았어요」 |
| E3 시작됨 | 「<장소 이름>까지 안내를 시작했어요」 |
| E3 실패 — 미연결 | 「안내를 시작하지 못했어요 · 내비 서비스에 연결되지 않았어요」 |
| E3 실패 — 너무 가까움 | 「안내를 시작하지 못했어요 · 너무 가까운 곳이에요」 |
| E3 실패 — 너무 멂 | 「안내를 시작하지 못했어요 · 너무 먼 곳이에요」 |
| E3 실패 — 배터리 부족 | 「안내를 시작하지 못했어요 · 배터리가 부족해요」 |
| E3 실패 — 경로 못 찾음(그 밖의 내비 오류) | 「안내를 시작하지 못했어요 · 경로를 찾지 못했어요」 |
| E3 실패 — 응답 없음 | 「안내를 시작하지 못했어요 · 내비가 응답하지 않아요」 |
| E3 실패 — 요청 못 보냄 | 「안내를 시작하지 못했어요 · 요청을 보내지 못했어요」 |
| E6 빈 안내(정차) | 「아직 가 볼 장소가 없어요 · 아래에서 장소를 고정해 보세요」(초안 · U24) |
| E6 빈 안내(주행 · 모름) | 「아직 가 볼 장소가 없어요 · 정차 중에 고정할 수 있어요」(초안 · U24) |

- 문구는 전부 `strings.xml`(android-architecture 규칙 6). 장소 이름 · 주소는 후보 고정 값에서 옴(문구 아님).
- 실패 이유 ↔ 내비 오류 코드 대응은 4.2 표대로(결정 기록 U5).

#### 상태별 표시

| 상태 | 고정 장소(E4) · 빈 안내(E6) | 「여기로 안내」(E5) | E2 · E3 | 「장소 고르기」(2.2) |
|---|---|---|---|---|
| 정차(PARKED) | 전부 보임 · 0곳이면 E6(정차 문구) | 내비 연결됨 · 요청 중 아님 → 누를 수 있음 | 아래 「요청 상태별」 그대로 | 보임 |
| 주행(DRIVING) | 전부 보임 · **스크롤 없이 첫 화면 안에** · 0곳이면 E6(주행 문구) | 정차와 같음 — 한 번 터치로 동작(사람 결정) · **의존: #2**(주행 중 사용 선언) | 정차와 같음 | **숨김**(결정 기록 U18) |
| 모름(UNKNOWN) | 주행과 같음 | 주행과 같음 | 주행과 같음 | 주행과 같음 |

- 주행 · 모름에서도 「가 볼 장소」 목록 · 버튼을 숨기거나 줄이지 않음 — 이슈 완료 조건 「주행 중에도 한 번 터치로 동작」. 대신 주행 중에 생기면 안 되는 것(긴 목록 스크롤 · 글 입력 · 애니메이션 · 자동 화면 전환 · 목록 고르기 조작)이 화면에 없음.
- 고정 최대 개수 3곳(결정 기록 U16)은 「가 볼 장소」 구역이 스크롤 없이 첫 화면(창 1개 · portrait 미지정 · 창 크기 변경 포함)에 확실히 다 들어오는 수 — 바뀌기 전 U2 의 근거를 그대로 이어받음.
- 기존 진단 카드 3개(기어 · NaviHelper · Gleo)는 「장소 고르기」 아래(주행 · 모름이면 「가 볼 장소」 아래)에 그대로 둠 · 주행 · 모름 때도 숨기지 않음 · 정리는 화면 구조(2-1) 때(결정 기록 U8).
- 기본 내비 앱이 앞으로 나오는 것은 사용자의 터치로 일어난 전환이라 「자동 화면 전환」이 아님. 정차 → 주행 때 「장소 고르기」가 사라지는 것은 주행 규제에 따른 내용 숨김(애니메이션 없이 바로).

| 요청 상태 | E5 | E3 |
|---|---|---|
| 대기 | 연결됨이면 전부 누를 수 있음 | 없음 |
| 요청 중(장소 X) | X 의 버튼은 「안내 요청 중…」 · **모든 버튼 비활성**(중복 요청 막음) | 직전 결과 문구는 지움 |
| 시작됨(장소 X) | 연결됨이면 전부 누를 수 있음 | 「X까지 안내를 시작했어요」 |
| 실패(이유) | 연결됨이면 전부 누를 수 있음 | 「안내를 시작하지 못했어요 · <이유>」 |

| 내비 연결 | E2 | E5 |
|---|---|---|
| 연결됨 | 없음 | 요청 상태에 따름 |
| 연결 안 됨(초기화 전 · 실패 · 해제) | 「내비 서비스에 연결되지 않았어요」 | 전부 비활성 |

- 결과 문구(E3)는 다음 요청을 시작할 때까지 남음 — 자동으로 사라지지 않음 · 닫기 버튼 없음(결정 기록 U7). 내비 앱에서 홈으로 돌아왔을 때 보이게 하기 위함 — 이슈 사용 장면 4. 고정 장소가 0곳이 되어도 E3 는 그대로 남음.

### 2.2 홈 안 「장소 고르기」 구역 — 고정 · 해제 (수정: 2026-11-05 리뷰 · 미확정 답 반영 · 새 절)

「가 볼 장소」 구역 바로 아래 · 진단 카드 위(결정 기록 U17). 별도 화면이 아님(화면 구조는 범위 밖). **정차 때만 보임**(결정 기록 U18).

#### 요소

| 번호 | 요소 | 내용 |
|---|---|---|
| E7 | 구역 제목 | 「장소 고르기」(초안 · U24) |
| E8 | 한도 안내 | 고정 수가 최대(결정 기록 U16)일 때만 · 구역 제목 아래 한 줄 · 「가 볼 장소는 최대 <n>곳까지 고정할 수 있어요」(초안 · U24 · n = 고정 최대 개수 · AppConfig 한 곳) |
| E9 | 후보 항목(후보 수만큼) | 장소 이름(한 줄) · 주소(한 줄 · 넘치면 말줄임) · 오른쪽에 고정 버튼 · 순서 = 후보 JSON 에 적힌 순서 |
| E10 | 고정 버튼 | 고정 안 된 후보 「고정」 · 고정된 후보 「고정 해제」(초안 · U24) · 한 번 터치로 끝 · 확인 대화상자 없음 · 고정 수가 최대면 「고정」 버튼만 비활성(「고정 해제」는 누를 수 있음) |

- 「고정」 → 그 장소가 「가 볼 장소」 맨 아래(결정 기록 U19)에 바로 나타나고 기기 안에 저장. 「고정 해제」 → 「가 볼 장소」에서 바로 빠지고 남은 장소의 순서는 그대로.
- 길안내 요청 상태와 무관 — 요청 중에도 고정 · 해제할 수 있음. 요청 중인 장소를 해제해도 요청은 그대로 진행 · 결과 문구는 요청할 때의 장소 이름으로.
- 고정 해제는 안내 요청 기록(4.1)을 지우지 않음.
- 기기 저장 쓰기가 실패하면 고정 상태는 저장된 값 그대로(화면도 바뀌지 않음) · 문구 없음 · 앱 꺼지지 않음.
- 정차에서는 홈 전체 스크롤 허용(후보 수만큼 길어질 수 있음). 글 입력 · 검색 · 키보드 · 애니메이션 없음.

| 상태 | 「장소 고르기」 |
|---|---|
| 정차(PARKED) | 위 표대로 |
| 주행(DRIVING) | 그리지 않음 · 고정 · 해제 조작이 와도 무시(저장 쓰기 없음) |
| 모름(UNKNOWN) | 주행과 같음 |

## 3. 상태

### 3.1 홈 UiState 에 더하는 칸 (수정: 2026-11-05 리뷰 · 미확정 답 반영)

기존 칸(`drivingState` · `naviReady` · `navi` · `openedByGleoAction`)은 그대로.

| 칸 | 뜻 | 처음 값 |
|---|---|---|
| `pinnedPlaces` | 「가 볼 장소」에 그릴 고정 장소 목록(고정 순서 · 장소마다 id · 이름 · 주소) · 기기 저장을 아직 못 읽었으면 「읽기 전」 — 좌표는 화면 상태에 넣지 않음(화면이 그리지 않으므로) | 읽기 전 |
| `candidates` | 「장소 고르기」에 그릴 후보 목록(후보 JSON 순서 · 장소마다 id · 이름 · 주소 · 고정 여부) | 후보 전부 · 고정 여부는 고정 목록을 읽은 뒤 채움 |
| `pickerVisible` | 「장소 고르기」 구역을 그리나 — `drivingState` 가 PARKED 일 때만 참 | 거짓 |
| `canPinMore` | 고정 수 < 최대(결정 기록 U16) — 거짓이면 E8 표시 · 「고정」 비활성 | 참 |
| `routeRequest` | 마지막 길안내 요청 상태 — 대기 · 요청 중(장소 id) · 시작됨(장소 이름) · 실패(이유 코드) | 대기 |

- 바뀌기 전의 `places`(고정 값 전부) 칸은 `pinnedPlaces` · `candidates` 로 나뉨.
- 「여기로 안내」 버튼 활성 여부는 따로 칸을 두지 않고 `naviReady` · `routeRequest` 에서 정함 — 활성 = `naviReady` 이고 `routeRequest` 가 요청 중이 아님.
- 실패 이유 코드(앱 안 값 · 4.2): `NOT_CONNECTED` · `TOO_CLOSE` · `TOO_FAR` · `LOW_BATTERY` · `ROUTE_NOT_FOUND` · `TIMEOUT` · `REQUEST_FAILED`.
- 사용자 행동은 ViewModel 메서드 셋 — 「여기로 안내」(장소 id) · 「고정」(장소 id) · 「고정 해제」(장소 id). 결과는 이벤트가 아니라 상태 칸으로(android-architecture 규칙 5).
- 고정 목록의 단일 출처는 기기 저장을 감싼 저장소 — ViewModel 은 그 흐름을 받아 `pinnedPlaces` · `candidates` · `canPinMore` 를 만듦(화면에서 따로 들고 있지 않음).

### 3.2 전이 (수정: 2026-11-05 서버 전송 결정 반영)

길안내 요청

| 지금 | 일 | 다음 | 비고 |
|---|---|---|---|
| 대기 · 시작됨 · 실패 | 「여기로 안내」 터치 · 내비 연결됨 | 요청 중(X) | 내비에 목적지 요청 1건 |
| 대기 · 시작됨 · 실패 | 「여기로 안내」 터치 · 그 순간 내비 연결 안 됨 | 실패(`NOT_CONNECTED`) | 내비에 요청하지 않음(버튼 비활성과 연결 끊김이 엇갈린 경우) |
| 요청 중(X) | 「여기로 안내」 터치(어느 장소든) | 요청 중(X) 그대로 | 버튼이 비활성이라 원래 일어나지 않음 · 일어나도 무시 |
| 요청 중(X) | 내비가 경로 탐색 시작을 알림(`onRouteStarted`) | 시작됨(X 의 이름) | 성공 판정 = 이 알림(결정 기록 U4) · 탐색 완료 · `ROUTE_DRIVE` 를 기다리지 않음 · 안내 요청 기록 1건 추가(결정 기록 U21 — 이 시점만) · 그 기록을 서버로 보냄(4.6 · 화면 상태는 바꾸지 않음) |
| 요청 중(X) | 내비가 오류를 알림 | 실패(오류 코드에 맞는 이유) | 4.2 대응표 · 기록 없음(결정 기록 U21) |
| 요청 중(X) | 응답 대기 한도 안에 시작 · 오류 둘 다 안 옴 | 실패(`TIMEOUT`) | 한도는 앱의 기존 SDK 응답 대기 한도(지금 10초 · AppConfig 한 곳) 재사용 |
| 요청 중(X) | 요청 호출 자체가 예외(권한 없음 등) | 실패(`REQUEST_FAILED`) | — |
| 요청 중(X) | 내비 연결 끊김 | 실패(`NOT_CONNECTED`) | — |
| 요청 중(X) | X 고정 해제 | 요청 중(X) 그대로 | 결과는 요청할 때의 이름으로 |
| 시작됨 · 실패 | 정차 ↔ 주행 ↔ 모름 바뀜 | 그대로 | 주행 상태는 요청 상태를 바꾸지 않음 |
| 아무 상태 | 프로세스 재생성 | 대기 | 요청 상태는 복원하지 않음(기기 저장 대상 아님 · 일회성 결과) |

고정 목록

| 지금 | 일 | 다음 | 비고 |
|---|---|---|---|
| 읽기 전 | 기기 저장에서 읽음 | 고정 목록(0곳 이상) | 0곳이면 E6 |
| 정차 · 고정 수 < 최대 · Y 고정 안 됨 | 「고정」(Y) | Y 가 목록 맨 아래에 붙음(결정 기록 U19) · 저장 | — |
| 정차 · 고정 수 = 최대 | 「고정」(Y) | 그대로 | 버튼 비활성이라 원래 일어나지 않음 · 일어나도 무시 |
| 정차 · Y 고정됨 | 「고정 해제」(Y) | Y 빠짐 · 남은 순서 그대로 · 저장 | 안내 요청 기록은 그대로 |
| 주행 · 모름 | 「고정」 · 「고정 해제」 | 그대로 | 구역이 없어 원래 일어나지 않음 · 일어나도 무시 · 저장 쓰기 없음 |
| 아무 상태 | 저장 쓰기 실패 | 저장된 값 그대로 | 화면도 바뀌지 않음 |
| 아무 상태 | 프로세스 재생성 · 앱 종료 뒤 다시 실행 | 기기 저장에서 다시 읽음 | 고정 목록 · 순서 유지(결정 기록 U13) |
| 정차 | 주행 · 모름으로 바뀜 | `pickerVisible` 거짓 | 고정 목록 그대로 |

- 앱이 뒤에 있는 동안(내비 앱이 앞에 있음) 온 시작 · 오류도 상태에 반영되고, 홈으로 돌아오면 그 결과가 보임. 그동안 온 시작 알림도 기록 1건.
- 기본 내비 앱 약관에 동의하지 않은 상태(경로 상태의 `termsAgreed` 거짓)의 요청은 따로 막거나 문구를 더하지 않음 — 오류 · 응답 없음 경로(실패 이유 4.2 · `TIMEOUT`)로 처리(결정 기록 U11).
- 시작됨 뒤에 같은 요청에서 오는 늦은 오류 · 경로 상태 변화(안내 종료 등)는 이번 범위에서 결과 문구 · 기록을 바꾸지 않음.
- 주행 상태(`drivingState`)는 기존 그대로(기어 → DrivingState). CarUxRestrictions 합성은 이 이슈 범위 밖 — 모름은 주행처럼 다룸.

## 4. 데이터

### 4.1 앱 모델 (수정: 2026-11-05 서버 전송 결정 반영 · 미확정 답 반영)

| 모델 | 칸 | 출처 · 규칙 |
|---|---|---|
| 장소(domain/model) | id(앱 안 고유 문자열 · 영문 소문자 · 숫자 · `-` · 64자 이하 — 서버 계약의 `placeId` 꼴과 같음 · 5절) · 이름 · 주소 · 위도 · 경도 | 앱 안 **후보 장소** 고정 값 · 수 · 구체 값은 미확정 U1 |
| 고정 목록(domain/model) | 장소 id 의 순서 있는 목록 | 기기 안 저장(앱 종료 · 재부팅 뒤 유지 · 결정 기록 U13) · 최대 3곳(결정 기록 U16 · AppConfig 한 곳) · 이름 · 주소 · 좌표는 저장하지 않음(후보에서 id 로 찾음) |
| 안내 요청 기록(domain/model) | 장소 id · 시각(시간대 포함) | 기기 안 저장 · 서버로 보낼 원본(결정 기록 U15) · 계정은 칸으로 두지 않음(서버가 토큰에서 얻음) · 좌표 · 이름 · 주소 · 결과 칸 없음 · 기록 시점 = 시작됨 때만(결정 기록 U21) · 역할 = **서버 전송 대기 큐**(Room · 보내면 지움 · 최대 100건 · 넘치면 오래된 것부터 지움 · 결정 기록 U29 · U22) · 삭제 화면 없음(결정 기록 U23) · 이번엔 보여 주는 화면 없음 |
| 길안내 요청 결과(domain/model) | 시작됨 / 실패(이유 코드) | 내비 콜백을 data/navi 에서 이 모델로 바꿔 내보냄 — SDK 타입은 `data.vehicle` · `data.navi` 밖으로 나가지 않음(ArchitectureTest 규칙 7) |

- 후보 장소 자리: **앱 안 한 곳 · 쉽게 바꿀 수 있는 자리**(android-architecture 규칙 6 의 콘텐츠 자리) — `app/src/main/assets/` 의 JSON 파일 하나(결정 기록 U3). 화면 · ViewModel 은 저장소를 거쳐 받음(규칙 4). 고를 원본이 생기면(#3 방문 기록 · 공유 받은 여행) 후보 저장소의 자료원만 바꿈 — 그때 id 체계가 달라 기존 고정이 보이지 않을 수 있음(시연용이라 받아들임 · 2.1 「후보에 없는 id」 규칙으로 처리).
- 고정 목록 · 안내 요청 기록은 각각 저장소 하나를 거침(규칙 4) · 저장 방식: 고정 목록 = DataStore · 안내 요청 기록 = Room 전송 대기 큐(android-architecture 「오프라인」 · 결정 기록 U20 · U29).
- 서버 호출은 Retrofit 인터페이스(`data/remote/AppApi`) 하나 · 계약(5절)과 경로 · 메서드 1:1 · 요청 DTO 는 `placeId` · `startedAt` 두 칸만(좌표 · userId 칸 없음 · android-architecture 규칙 8) · 주소는 기존 `BuildConfig.API_BASE_URL`(debug `http://10.0.2.2:4000/`).
- 기기 저장은 백업 대상 아님 — 매니페스트 `allowBackup="false"` 유지.
- 내비에 넘기는 값: 위도 · 경도 · 장소 이름 · 주소. POI 식별자(poiId · poiSubId)는 비움 — 우리 장소엔 POI 가 없음 · 비운 채 안내가 시작되는지 에뮬레이터에서 확인하고 안 되면 보고(장소 칸 추가는 사람 결정)(결정 기록 U6). 경로 옵션은 지정하지 않음 — 내비 기본값 · 운전자 설정을 따름(결정 기록 U12).
- 이미 안내 중일 때 새 요청 = 목적지 바꾸기(Pleos 문서: `requestRoute` 는 목적지 설정 · 변경 · 재설정에 씀). 경유지 · 전체 코스 재안내는 만들지 않음.

### 4.2 내비 오류 → 실패 이유 (수정: U5 사람 결정 반영)

내비 오류 코드는 Pleos 문서 「목적지 설정」에 값 목록이 없어, 저장소가 쓰는 SDK(navi-data 3.0.4)의 `NaviErrorCode` 값 이름을 근거로 함. 값의 뜻 설명은 문서에 없어 값 이름에서 읽은 뜻으로 아래 표대로 확정 · 표에 없는 값은 일반 문구로 떨어짐(결정 기록 U5).

| 내비 오류 코드 | 실패 이유 코드 | E3 이유 문구 |
|---|---|---|
| `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE` | `TOO_CLOSE` | 「너무 가까운 곳이에요」 |
| `CAUSE_SEARCH_ROUTE_OVER_DISTANCE` | `TOO_FAR` | 「너무 먼 곳이에요」 |
| `CAUSE_LOW_BATTERY` | `LOW_BATTERY` | 「배터리가 부족해요」 |
| `CAUSE_SEARCH_ROUTE` · `CAUSE_SEARCH_TRIP` · 그 밖의(새로 생긴) 값 | `ROUTE_NOT_FOUND` | 「경로를 찾지 못했어요」 |

- 앱 쪽 실패: 미연결 `NOT_CONNECTED` · 응답 없음 `TIMEOUT` · 호출 예외 `REQUEST_FAILED`.

### 4.3 권한 (수정: 2026-11-05 서버 전송 결정 반영 — 네트워크 행 추가)

| 권한 | 지금 | 이번 | 왜 필요한가(근거) |
|---|---|---|---|
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | 없음 | **추가** | 목적지 설정 `requestRoute()` 의 요구 권한 — Pleos 문서 「권한 선언」 표: `NAVI_ROUTE_SEARCH` = `requestRoute()` · `requestRouteWithWaypoints()` · `cancelRoute()` · `requestReRoute()` · `addWaypoint()` · `removeWaypoint()` · `changeRouteOption()`. 이 기능(한 번 터치로 내비 안내 시작)은 이 호출 없이 할 길이 없음 — 내비 앱에 목적지를 넘기는 공개 수단이 NaviHelper 뿐. 에뮬레이터 실증(11-04)에서 protectionLevel normal(런타임 대화상자 없음). 이번에 쓰는 호출은 `requestRoute()` 하나 — 경유지 · 취소 · 재탐색 · 경로 옵션 호출은 쓰지 않음 |
| `pleos.car.permission.NAVI_ROUTE` | 있음 | 그대로 | 기존 경로 상태 조회 `getRouteStateInfo()`(진단 카드) — 문서 표: `get*Info()` · `requestTrip()`. 이번 기능이 새로 쓰지는 않음 |
| `pleos.car.permission.NAVI_CUSTOM_MAP` | 없음 | 선언 안 함 | `requestNavigationScreen()` 용 · 이번에 안 씀(최소 권한) |
| 위치 권한 | 없음 | 선언 안 함 | 목적지 좌표는 고정 값 · 현재 위치는 내비 앱이 앎 · 위치 수집(2-10)은 범위 밖 |
| `android.permission.INTERNET` | 있음 | 그대로 · **이번에 처음 씀** | 안내 시작 기록을 서버로 보냄(4.6) · `ALLOWED` 에 이미 있음(「서버 API 호출」) |
| `android.permission.ACCESS_NETWORK_STATE` | 없음 | 연결 유무를 보고 보내는 데 필요하면 추가 | 「연결되면 보냄」(4.6) · `ALLOWED` 에 이미 있음(「연결 유무 확인 · 오프라인 처리」) · 쓰지 않으면 선언 안 함 |

- 허용 목록 원본(`check_manifest.py` 의 `ALLOWED`)에는 지금 `NAVI_ROUTE` · `NAVI_ROUTE_SEARCH` 둘 다 없음 — `NAVI_ROUTE` 는 이미 센서 위반 1건(2026-11-04 로컬 실행: 「허용 밖 권한 pleos.car.permission.NAVI_ROUTE」). 이 절이 두 권한의 근거이고, 목록 확장은 하네스 변경으로 **하네스 이슈 #2 에 묶음**(같은 `check_manifest.py` · 같은 센서라 충돌이 적음) · `ALLOWED` 근거 칸에 이 산출물 경로(`docs/spec/navigation/artifacts/set-destination.md`) · #4 를 적음(결정 기록 U10). #2 반영 전에는 구현 PR 의 매니페스트 센서가 녹색이 될 수 없음.
- 주행 중 사용 선언(DO): MainActivity 가 주행 중에도 화면에 남도록 하는 선언이 필요 — 지금 `check_manifest.py` 가 `distraction` meta-data 를 막고 있음 → **의존: #2**. 이 스펙은 「주행 중에도 홈 · 「여기로 안내」가 보이고 동작함」을 요구로 둠.

### 4.4 최소 수집 · 개인정보 점검 (수정: 2026-11-05 서버 전송 결정 반영 — 서버 전송 기준으로 다시 씀 · 미확정 답 반영)

| 항목 | 결과 |
|---|---|
| 무엇이 서버로 가나 | 안내 시작 기록 1건 = 장소 id(앱 후보 목록의 고정 id) · 시작 시각 · 계정. 계정은 요청 본문 칸이 아니라 로그인 토큰에서 서버가 얻음(server-architecture 규칙 4) · 정의 밖 칸은 서버가 거절(422) |
| 무엇이 안 가나 | 좌표 · 장소 이름 · 주소 · 기어 · 속도 · VIN · 내비 결과 · 실패 기록 · 고정 목록. 장소 id 는 앱 안 후보 목록에서만 뜻을 가짐 — 서버는 그 장소의 좌표 · 이름을 모름(하네스 규칙 「좌표는 서버에 없음」 유지) |
| 언제 | 내비가 안내 시작을 알린 뒤 즉시(결정 기록 U21 · U30) · 로그인 · 동의 상태일 때만(결정 기록 U27 · U31 — #1 전에는 보내지 않음) · 실패하면 연결될 때 간격을 늘려 다시 보냄(4.6 · 결정 기록 U30) |
| 왜 | 장소별로 안내가 쌓인 수를 다른 사용자 추천의 근거로 씀(PR #5 리뷰 2) — 이번엔 건수 조회 API 까지 · 추천 화면은 별도 이슈 |
| 기록의 성격 | 계정 + 장소 id + 시각 = 「이 사람이 언제 어디로 가려 했나」를 드러내는 이동 기록 → Pleos 심사 「개인정보 · 보안」 대상 · 동의는 #1 로그인 동의에 항목으로 넣음(결정 기록 U31) |
| 서버 보관 · 삭제 | 보관 365일 · 지난 행은 하루 한 번 지움(결정 기록 U32 · 수치는 `Settings` 한 곳 · #1 동의 안내문에 기간을 적음) · 계정 삭제(동의 철회 웹훅 포함 · #1)면 그 계정 행 전부 삭제(`ON DELETE CASCADE`) |
| 조회 응답 | 장소 id 별 건수만 — 계정 · 시각 · 개별 행은 내보내지 않음 |
| 기기 저장 | 고정 목록(장소 id · 순서) · 안내 요청 기록(장소 id · 시각)만 · 계정 · 이름 · 주소 · 좌표 · 결과 없음 · 백업 안 됨(`allowBackup="false"` 유지) · 안내 요청 기록은 보내면 지움 · 못 보낸 것만 최대 100건(결정 기록 U29 · U22) · 삭제 화면 없음(결정 기록 U23) |
| 좌표 | 기기 안에서 후보 고정 값 → NaviHelper(기본 내비 앱)로만 · 화면 상태 · 로그 · 기기 저장 · 서버에 없음 |
| 기어 · 속도 · VIN | 이번에 새로 읽거나 내보내는 것 없음(기존 기어 → 주행 상태만) · 기록 · 서버에도 없음 |
| 로그(앱) | 실패 시 이유 코드 · 오류 코드 이름 · HTTP 상태만 · 좌표 · 장소 이름 · 주소 · 고정 목록 · 기록 내용(장소 id · 시각) · 토큰을 남기지 않음(#3 뒤 실제 방문 장소가 같은 길을 타므로 지금부터 같은 규칙) |
| 로그(서버) | 경로 · 메서드 · 상태 코드 · 오류 코드만(structlog 마스킹 · server-architecture 규칙 6) · 계정 id · pleos_user_id · placeId · startedAt · 토큰을 남기지 않음 |
| LLM | 없음 |

### 4.5 서버 테이블 (수정: 2026-11-05 서버 전송 결정 반영 · 새 절 · 미확정 답 반영)

`server/` 는 server-architecture 「폴더」 · 「도구 묶음」대로 처음 만듦(uv · FastAPI · Pydantic v2 · SQLAlchemy async · Alembic · PostgreSQL 17 · PyJWT · structlog · ruff · mypy · import-linter · pytest). 도메인 패키지 하나(`app/navigation/` — router · service · schemas · models · repository). LLM · Pleos 호출 없음(`app/llm/` · `app/auth/pleos/` 는 이번에 만들지 않음 — 결정 기록 U28). 첫 Alembic 리비전에 아래 테이블.

| 테이블 | 칸(전부 `comment=`) | 규칙 |
|---|---|---|
| `users` | server-architecture 「데이터」의 칸 그대로(id uuid · pleos_user_id unique · brand · display_name · token_version · created_at) | 표는 #4 가 만듦 · 계정을 넣는 로그인 · 토큰 발급은 #1(결정 기록 U27 · U28) · #4 시험은 시험용 계정 행 + 시험용 비밀로 만든 토큰 |
| `navigation_starts` | id uuid(PK · 서버 생성) · user_id uuid(FK `users.id` `ON DELETE CASCADE`) · place_id varchar(64)(앱 후보 고정 id) · started_at timestamptz(기기가 보낸 시작 시각) · received_at timestamptz(서버가 받은 시각) | 유일: (user_id · place_id · started_at) — 같은 기록을 다시 보내도 한 행(재시도 안전 · 결정 기록 U33) · 색인: place_id · started_at(보관 정리 · 집계) · 좌표 · 이름 · 주소 · VIN 칸 없음 |

- 서버는 후보 장소 목록을 모름 — `place_id` 가 실제 후보인지 확인하지 않음(꼴만 확인).
- 집계 · 조회는 보관 기간(365일) 안 행만(결정 기록 U32 · U33). 시각 허용 폭(미래 5분)도 `Settings` 한 곳.

### 4.6 전송 규칙(앱) (수정: 2026-11-05 서버 전송 결정 반영 · 새 절 · 미확정 답 반영)

| 상황 | 앱의 처리 |
|---|---|
| 안내 시작됨(기록 1건 생김) | 보낼 수 있으면 바로 `POST /navigation-starts` 1회(결정 기록 U30) |
| 로그인 안 됨(토큰 없음 · #1 전 포함) · 동의 없음 | 보내지 않음 · 기록은 큐에 남김(결정 기록 U27 · U31) |
| 201 · 200(이미 받음) | 보냄 끝 — 큐에서 그 기록을 지움(결정 기록 U29) |
| 네트워크 없음 · 시간 초과 · 5xx | 기기에 남기고 다시 보냄 — 연결되면 · 간격을 늘려 가며 · 앱이 꺼져 있어도(30초부터 두 배 · 최대 1시간 · 수치는 AppConfig 한 곳 · 결정 기록 U30) |
| 401 `UNAUTHENTICATED` | 보내지 않고 남김 — 토큰 갱신은 #1(TokenAuthenticator) 몫 · 갱신되면 다시 보냄 |
| 403 `CONSENT_REQUIRED` | 보내지 않고 남김(결정 기록 U31) |
| 422(`VALIDATION_FAILED` · `STARTED_AT_OUT_OF_RANGE`) | 다시 보내도 같은 결과 → 큐에서 지움 · 로그에 오류 코드만 |

- 전송은 화면 상태(UiState) · 문구 · 버튼에 영향 없음 — 사용자에게 보이는 전송 상태 없음(기록 화면이 없음 · 결정 기록 U23). 주행 중에도 화면 변화 없이 뒤에서 일어남.
- 순서 보장 없음 — 서버는 `startedAt` 으로 시각을 앎.
- 앱 종료 · 재부팅 뒤에도 못 보낸 기록은 큐에서 다시 보냄. 큐가 100건을 넘으면 오래된 것부터 지움(결정 기록 U22).
- 이번에 앱이 부르는 서버 경로는 POST 하나 — 건수 조회(GET)는 부르지 않음(추천 화면 이슈 몫).

## 5. API 계약 변경 (수정: 2026-11-05 서버 전송 결정 반영 — 새 계약 · 미확정 답 반영)

`docs/api/openapi.json` 을 처음 만듦(서버가 내보낸 파일 · server-architecture 「OpenAPI」). 초안: `_workspace/4/01_openapi_draft.json`(메인이 판단해 커밋 · 구현 PR 에서는 서버가 내보낸 파일로 갈음 · 경로 · 메서드 · 칸은 이 절과 같아야 함).

공통

- 인증: `Authorization: Bearer <우리 서버 액세스 토큰>`(server-architecture 「세션」 — PyJWT HS256 · 클레임 `sub` = `users.id` · `ver` = `token_version` · `exp` · 서버는 서명 · 만료 · `ver` 대조 · 발급은 #1 로그인 · 결정 기록 U28). 두 경로 모두 필요.
- 오류 응답: `{"code": "<UPPER_SNAKE>"}` 하나(한글 문장 없음 · server-architecture 규칙 8). 앱이 코드를 보고 처리(4.6).
- 요청 본문 · 쿼리에 userId 칸 없음(규칙 4) · 정의 밖 칸이 오면 422 — 좌표 · 이름 등이 들어오지 못하게.

| 경로 | 메서드 | 요청 | 성공 응답 | 오류 |
|---|---|---|---|---|
| `/navigation-starts` | POST | 본문 `NavigationStartIn` — `placeId`(문자열 · `^[a-z0-9][a-z0-9-]{0,63}$`) · `startedAt`(date-time · 시간대 필수) | 201 `NavigationStartOut`(새로 받음) · 200 `NavigationStartOut`(같은 계정 · placeId · startedAt 이 이미 있음 — 기존 행 그대로) | 401 `UNAUTHENTICATED`(토큰 없음 · 만료 · 위조 · 지워진 계정) · 403 `CONSENT_REQUIRED`(결정 기록 U31 — #1 의 `consents` 가 생긴 뒤 켬) · 422 `VALIDATION_FAILED`(꼴 위반 · 정의 밖 칸 · 시간대 없음) · 422 `STARTED_AT_OUT_OF_RANGE`(서버 시각 + 5분보다 늦음 · 보관 기간 365일보다 오래됨) |
| `/navigation-starts/stats` | GET | 쿼리 `limit`(정수 1 ~ 100 · 기본 20) · `placeId`(0 ~ 50개 반복 · 꼴은 위와 같음 · 주면 그 장소만) | 200 `NavigationStatsOut` — `items`: [{`placeId` · `startCount`(안내 시작 건수) · `userCount`(서로 다른 계정 수)}] · 정렬 `userCount` 내림 → `startCount` 내림 → `placeId` 오름 · 건수 0인 장소는 없음 · 보관 기간 안 행만 | 401 `UNAUTHENTICATED` · 422 `VALIDATION_FAILED` |

- `NavigationStartOut`: `id`(uuid) · `placeId` · `startedAt` · `receivedAt`. 계정 칸 없음.
- 집계 단위 · 중복 계산: 보관 기간 안 전체 누적 · 같은 (계정 · 장소 · 시각) 재전송은 한 행 · 같은 계정의 반복은 `startCount` 에만 더해짐(결정 기록 U33).

```yaml
openapi: 3.1.0
paths:
  /navigation-starts:
    post:
      operationId: createNavigationStart
      security: [{ bearerAuth: [] }]
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/NavigationStartIn' }
      responses:
        '201': { description: 새로 받음, content: { application/json: { schema: { $ref: '#/components/schemas/NavigationStartOut' } } } }
        '200': { description: 이미 받은 기록, content: { application/json: { schema: { $ref: '#/components/schemas/NavigationStartOut' } } } }
        '401': { $ref: '#/components/responses/Error' }
        '403': { $ref: '#/components/responses/Error' }
        '422': { $ref: '#/components/responses/Error' }
  /navigation-starts/stats:
    get:
      operationId: getNavigationStats
      security: [{ bearerAuth: [] }]
      parameters:
        - { name: limit, in: query, required: false, schema: { type: integer, minimum: 1, maximum: 100, default: 20 } }
        - { name: placeId, in: query, required: false, style: form, explode: true, schema: { type: array, maxItems: 50, items: { $ref: '#/components/schemas/PlaceId' } } }
      responses:
        '200': { description: 장소별 건수, content: { application/json: { schema: { $ref: '#/components/schemas/NavigationStatsOut' } } } }
        '401': { $ref: '#/components/responses/Error' }
        '422': { $ref: '#/components/responses/Error' }
components:
  securitySchemes:
    bearerAuth: { type: http, scheme: bearer, bearerFormat: JWT }
  schemas:
    PlaceId: { type: string, pattern: '^[a-z0-9][a-z0-9-]{0,63}$' }
    NavigationStartIn:
      type: object
      additionalProperties: false
      required: [placeId, startedAt]
      properties:
        placeId: { $ref: '#/components/schemas/PlaceId' }
        startedAt: { type: string, format: date-time }
    NavigationStartOut:
      type: object
      required: [id, placeId, startedAt, receivedAt]
      properties:
        id: { type: string, format: uuid }
        placeId: { $ref: '#/components/schemas/PlaceId' }
        startedAt: { type: string, format: date-time }
        receivedAt: { type: string, format: date-time }
    PlaceStat:
      type: object
      required: [placeId, startCount, userCount]
      properties:
        placeId: { $ref: '#/components/schemas/PlaceId' }
        startCount: { type: integer, minimum: 1 }
        userCount: { type: integer, minimum: 1 }
    NavigationStatsOut:
      type: object
      required: [items]
      properties:
        items: { type: array, items: { $ref: '#/components/schemas/PlaceStat' } }
    ErrorOut:
      type: object
      required: [code]
      properties:
        code: { type: string, enum: [UNAUTHENTICATED, CONSENT_REQUIRED, VALIDATION_FAILED, STARTED_AT_OUT_OF_RANGE] }
  responses:
    Error: { description: 오류 코드, content: { application/json: { schema: { $ref: '#/components/schemas/ErrorOut' } } } }
```

## 6. 시험 케이스 (수정: 2026-11-05 미확정 답 반영 — U27 ~ U34 확정 · T45 · T46 큐 기준으로)

| 번호 | 조건 | 행동 | 결과 | 비고 |
|---|---|---|---|---|
| T1 | 정차(P) · 내비 연결됨 · 고정 3곳(A · B · C 순) | 홈 열기 | 맨 위 「가 볼 장소」 아래 A · B · C 순서로 3곳 · 항목마다 이름 · 주소 · 「여기로 안내」 · E2 · E3 · E6 없음 · 그 아래 「장소 고르기」 | 에뮬레이터 + ViewModel 단위(fake 저장소) |
| T2 | 정차 · 연결됨 · 대기 | 장소 A 「여기로 안내」 한 번 터치 | 기본 내비 앱이 앞으로 나오고 A 로 경로 안내 시작 | 에뮬레이터 실기 · 완료 조건 |
| T3 | T2 뒤 | 홈으로 돌아옴 | E3 「A까지 안내를 시작했어요」 · 버튼 다시 누를 수 있음 | 에뮬레이터 |
| T4 | 연결됨 · 대기 | A 터치 | 상태 요청 중(A) · A 버튼 「안내 요청 중…」 · 모든 버튼 비활성 · 직전 E3 지워짐 | ViewModel 단위(fake 저장소) |
| T5 | 요청 중(A) | B 터치 시도(메서드 호출) | 상태 요청 중(A) 그대로 · 내비 요청 1건만 | ViewModel 단위 |
| T6 | 요청 중(A) | 내비가 경로 탐색 시작 알림(`onRouteStarted`) | 시작됨(A) · E3 「A까지 안내를 시작했어요」 | ViewModel 단위 |
| T7 | 내비 연결 안 됨 | 홈 열기 | E2 「내비 서비스에 연결되지 않았어요」 · 모든 버튼 비활성 | ViewModel 단위 + Compose |
| T8 | 연결 안 됨 → 연결됨 | 연결 상태 바뀜 | E2 사라짐 · 버튼 누를 수 있음 | ViewModel 단위 |
| T9 | 연결 안 됨(버튼 비활성과 엇갈림) | 「여기로 안내」 메서드 호출 | 실패(`NOT_CONNECTED`) · 내비 요청 0건 · E3 「…연결되지 않았어요」 | ViewModel 단위 |
| T10 | 요청 중(A) | 내비 오류 `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE` | 실패(`TOO_CLOSE`) · E3 「안내를 시작하지 못했어요 · 너무 가까운 곳이에요」 | data/navi 대응 단위 + ViewModel 단위 |
| T11 | 요청 중(A) | 내비 오류 `CAUSE_SEARCH_ROUTE_OVER_DISTANCE` | 실패(`TOO_FAR`) · 「…너무 먼 곳이에요」 | 단위 |
| T12 | 요청 중(A) | 내비 오류 `CAUSE_LOW_BATTERY` | 실패(`LOW_BATTERY`) · 「…배터리가 부족해요」 | 단위 |
| T13 | 요청 중(A) | 내비 오류 `CAUSE_SEARCH_ROUTE` 또는 `CAUSE_SEARCH_TRIP` | 실패(`ROUTE_NOT_FOUND`) · 「…경로를 찾지 못했어요」 | 단위 |
| T14 | 요청 중(A) | 응답 대기 한도 안에 시작 · 오류 둘 다 없음 | 실패(`TIMEOUT`) · 「…내비가 응답하지 않아요」 · 버튼 다시 누를 수 있음 | ViewModel 단위(가상 시간) |
| T15 | 연결됨 | 요청 호출이 예외(권한 없음 등) | 실패(`REQUEST_FAILED`) · 「…요청을 보내지 못했어요」 · 앱 꺼지지 않음 | 단위 |
| T16 | 요청 중(A) | 내비 연결 끊김 | 실패(`NOT_CONNECTED`) | ViewModel 단위 |
| T17 | 실패 상태 | B 터치 → 시작 알림 | E3 가 「B까지 안내를 시작했어요」로 바뀜 · 실패 문구 없음 | ViewModel 단위 |
| T18 | 내비가 A 로 안내 중 | B 「여기로 안내」 | 내비 목적지가 B 로 바뀜 · E3 「B까지 안내를 시작했어요」 | 에뮬레이터 |
| T19 | 주행(Virtual Console 기어 D) · 연결됨 · 고정 3곳 | 홈 보기 | 홈이 막히지 않고 보임 · 「가 볼 장소」 3곳이 스크롤 없이 첫 화면에 다 보임 · 버튼 누를 수 있음 · 「장소 고르기」 없음 | **의존: #2** · 에뮬레이터 |
| T20 | 주행(D) · 연결됨 | 장소 A 한 번 터치 | 기본 내비 앱이 A 로 안내 시작 · 돌아오면 「A까지 안내를 시작했어요」 | **의존: #2** · 에뮬레이터 · 완료 조건 |
| T21 | 모름(기어를 읽지 못함) | 홈 보기 · A 터치 | T19 · T20 과 같음(「장소 고르기」 없음 포함) | ViewModel 단위(fake 주행 상태 UNKNOWN) |
| T22 | 시작됨 또는 실패 | 정차 ↔ 주행 ↔ 모름 바뀜 | 고정 목록 · E3 그대로 · 버튼 활성 여부 그대로 | ViewModel 단위 |
| T23 | 요청 중(A) · 앱이 뒤(내비 앱이 앞) | 시작 · 오류가 뒤에 있는 동안 옴 | 홈으로 돌아오면 그 결과가 E3 에 보임 | 에뮬레이터 |
| T24 | 시작됨 · 고정 A · B | 프로세스 재생성 | 요청 상태 대기 · E3 없음 · 고정 A · B 순서 그대로 | 에뮬레이터(개발자 옵션 「활동 유지 안 함」) |
| T25 | 주소가 한 줄을 넘는 장소 | 홈 보기(「가 볼 장소」 · 「장소 고르기」) | 주소 한 줄 · 말줄임 · 버튼 위치 · 크기 그대로 | Compose 또는 미리보기 |
| T26 | 모든 상태 | 홈 전체 | 「가 볼 장소」 · 「장소 고르기」에 글 입력 칸 · 검색 · 키보드 · 회전 표시 · 애니메이션 없음 | 구조 리뷰 + 에뮬레이터 |
| T27 | 고정 · 해제 · 실패 · 시작 · 전송 경로 | logcat 확인 | 좌표 · 장소 이름 · 주소 · 고정 목록 · 기록 내용(장소 id · 시각) · 토큰이 로그에 없음 · 이유 · 오류 코드 이름 · HTTP 상태만 | 에뮬레이터 + 리뷰 |
| T28 | 매니페스트 | 권한 · 백업 확인 | `NAVI_ROUTE_SEARCH` 선언 · `NAVI_CUSTOM_MAP` · 위치 권한 추가 없음 · 네트워크는 `INTERNET`(이미 있음) · 쓰면 `ACCESS_NETWORK_STATE` 만 더함 · `allowBackup="false"` 그대로 | `check_manifest.py`(#2 의 ALLOWED 확장 뒤) |
| T29 | 저장소 코드 | Konsist | `ai.pleos.*` import 는 `data.vehicle` · `data.navi` 에만 · ui 는 자료원을 직접 부르지 않음 | ArchitectureTest 규칙 4 · 7 |
| T30 | 정차 · 고정 0곳 · 연결됨 | 홈 열기 | 「가 볼 장소」 제목 + E6 「아직 가 볼 장소가 없어요 · 아래에서 장소를 고정해 보세요」 · E4 없음 · 「장소 고르기」 후보 전부 「고정」 | ViewModel 단위 + Compose |
| T31 | 주행 · 고정 0곳 | 홈 보기 | 「가 볼 장소」 제목 + E6 「아직 가 볼 장소가 없어요 · 정차 중에 고정할 수 있어요」 · 「장소 고르기」 없음 | ViewModel 단위 + Compose |
| T32 | 고정 목록 읽기 전 | 홈 열기 직후 | E4 · E6 둘 다 없음 → 읽은 뒤 목록 또는 E6 | ViewModel 단위(fake 저장소가 늦게 내보냄) |
| T33 | 정차 · 고정 0곳 | 후보 A 「고정」 | 「가 볼 장소」에 A · E6 사라짐 · 후보 A 버튼 「고정 해제」 · 기기 저장에 A | ViewModel 단위 + 저장소 단위 |
| T34 | 정차 · 고정 A · B | 후보 C 「고정」 | 순서 A · B · C | 결정 기록 U19 · ViewModel 단위 |
| T35 | 정차 · 고정 수 = 최대(3곳 · 결정 기록 U16) | 홈 보기 | E8 「가 볼 장소는 최대 3곳까지 고정할 수 있어요」(n = 최대값) · 고정 안 된 후보의 「고정」 비활성 · 「고정 해제」는 누를 수 있음 | ViewModel 단위 + Compose |
| T36 | 정차 · 고정 수 = 최대 | 「고정」 메서드 호출(엇갈림) | 고정 목록 그대로 · 저장 쓰기 0 | ViewModel 단위 |
| T37 | 정차 · 고정 A · B · C | B 「고정 해제」 | 「가 볼 장소」 A · C(순서 유지) · E8 사라짐 · 후보 B 「고정」 · 안내 요청 기록 그대로 | ViewModel 단위 |
| T38 | 정차 · 고정 A · B | 앱 강제 종료 → 다시 실행 | 「가 볼 장소」 A · B 순서 그대로 | 에뮬레이터 · 완료 조건(결정 U13) |
| T39 | 주행 또는 모름 | 「고정」 · 「고정 해제」 메서드 호출 | 고정 목록 그대로 · 저장 쓰기 0 | ViewModel 단위 |
| T40 | 정차 · 「장소 고르기」 보는 중 | 기어 D | 「장소 고르기」 바로 사라짐(애니메이션 없음) · 「가 볼 장소」 그대로 · P 로 돌아오면 다시 보임 | ViewModel 단위 + 에뮬레이터 |
| T41 | 기기 저장의 고정 id 하나가 후보 JSON 에 없음 | 홈 열기 | 그 id 는 보이지 않음 · 고정 수에 세지 않음 · 앱 꺼지지 않음 | 저장소 · ViewModel 단위 |
| T42 | 요청 중(A) | 「장소 고르기」에서 A 「고정 해제」 → 시작 알림 | 요청 그대로 진행 · E3 「A까지 안내를 시작했어요」 · 「가 볼 장소」에 A 없음 | ViewModel 단위 |
| T43 | 요청 중(A) | 시작 알림(`onRouteStarted`) | 안내 요청 기록 1건(장소 id A · 시각) · 기록에 이름 · 주소 · 좌표 · 결과 칸 없음 | 결정 기록 U21 · 저장소 단위(fake 시계) |
| T44 | 요청 중(A) | 내비 오류 · 응답 없음 · 연결 끊김 · 호출 예외 | 기록 0건 | 결정 기록 U21 · ViewModel 단위 |
| T45 | 못 보낸 기록이 최대 건수(100건 · 결정 기록 U22) | 시작 알림 1건 더(보낼 수 없는 상태) | 가장 오래된 1건이 지워지고 건수 = 최대 유지 | 결정 기록 U22 · U29 · 저장소 단위 |
| T46 | 못 보낸 기록 3건 | 앱 강제 종료 → 다시 실행(보낼 수 없는 상태 유지) | 큐에 3건 그대로 | 결정 기록 U29 · 저장소 단위 또는 에뮬레이터 |
| T47 | 기기 저장 쓰기 실패(fake) | 「고정」 | 고정 목록 · 화면 그대로 · 앱 꺼지지 않음 · 로그에 이유 코드만 | ViewModel 단위 |
| T48 | 서버 · 유효 토큰(계정 1) | `POST /navigation-starts` {placeId `a-place` · startedAt 지금} | 201 · 응답 id · placeId · startedAt · receivedAt · 계정 칸 없음 · DB 1행(user_id = 토큰 주인) | 서버 통합(testcontainers + `alembic upgrade head`) |
| T49 | T48 뒤 | 같은 본문 다시 POST | 200 · 같은 id · 행 1개 그대로 | 서버 통합 |
| T50 | 토큰 없음 · 만료 · 서명 위조 · `token_version` 다름 · 지워진 계정 | POST | 401 `{"code":"UNAUTHENTICATED"}` · 행 0 | 서버 통합 |
| T51 | 유효 토큰 | placeId 빈 값 · 65자 · 대문자 · 공백 · `_` 포함 | 422 `VALIDATION_FAILED` · 행 0 | 서버 단위(schemas) + 통합 |
| T52 | 유효 토큰 | 본문에 `userId` · `lat` · `lng` · `name` 칸을 더함 | 422 `VALIDATION_FAILED` · 행 0 | 서버 통합 · 최소 수집 |
| T53 | 유효 토큰 | startedAt = 서버 시각 + 6분 / 보관 기간 + 1일 전 / 시간대 없는 값 | 422 `STARTED_AT_OUT_OF_RANGE` / 422 `STARTED_AT_OUT_OF_RANGE` / 422 `VALIDATION_FAILED` | 서버 단위(fake 시계) + 통합 |
| T54 | 계정 1 이 A 2건 · 계정 2 가 A 1건 · B 1건 | `GET /navigation-starts/stats` | items = [A(startCount 3 · userCount 2) · B(1 · 1)] 이 순서 · 계정 · 시각 칸 없음 | 서버 통합 · 결정 기록 U33 |
| T55 | T54 상태 | `?placeId=b` / `?limit=1` / `?limit=0` · `?limit=101` | [B] / [A] / 422 `VALIDATION_FAILED` | 서버 통합 |
| T56 | 토큰 없음 | GET stats | 401 `UNAUTHENTICATED` | 서버 통합 |
| T57 | T54 상태 | `users` 에서 계정 2 삭제 | 계정 2 의 행 모두 사라짐 · stats = [A(2 · 1)] | 서버 통합(`ON DELETE CASCADE`) |
| T58 | 보관 기간 지난 행 1 · 안 지난 행 1 | stats 조회 → 정리 작업 실행 | 지난 행은 집계에 없음 · 정리 뒤 DB 에도 없음 | 서버 통합(fake 시계 · 결정 기록 U32) |
| T59 | T48 ~ T58 실행 중 | 서버 로그(JSON) 확인 | 계정 id · pleos_user_id · placeId · startedAt · 토큰 없음 · 경로 · 메서드 · 상태 · 오류 코드만 | 서버 단위(로그 캡처) + 리뷰 |
| T60 | 서버 · 앱 코드 | OpenAPI 내보내기 · 센서 | 내보낸 `docs/api/openapi.json` 과 커밋 파일 차이 0 · 5절과 같음 · `check_api_contract.py` 위반 0(앱 POST 1개 = 계약) | 센서 |
| T61 | 앱 · 로그인됨 · 동의됨 · 연결됨 | 시작 알림(A) | `POST /navigation-starts` 1회(placeId A · startedAt) → 201 → 큐에서 지움(결정 기록 U29) · 화면은 T6 과 같음 | 앱 단위(fake AppApi · fake 시계) |
| T62 | 앱 · 네트워크 없음 | 시작 알림(A) | 보내지 못함 · 기록 남음 → 연결되면 1회 보냄 | 앱 단위(fake 연결 상태) |
| T63 | 앱 · 서버 5xx 또는 시간 초과 | 시작 알림(A) | 큐에 남음 · 다시 보냄 간격 30초 → 60초 → … 최대 1시간(AppConfig) · 성공하면 큐에서 지움 | 앱 단위(가상 시간) · 결정 기록 U30 |
| T64 | 앱 · 서버 422 | 보냄 | 큐에서 지움 · 다시 보내지 않음 · 로그에 오류 코드만 | 앱 단위 |
| T65 | 앱 · 토큰 없음(#1 전) / 401 / 403 `CONSENT_REQUIRED` | 시작 알림(A) | 보내지 않음(토큰 없음) · 더 보내지 않고 멈춤(401 · 403) · 기록 남음 · 화면 변화 없음 | 앱 단위 · 결정 기록 U27 · U31 |
| T66 | 앱 · 못 보낸 기록 2건 | 앱 강제 종료 → 다시 실행(로그인됨 · 연결됨) | 2건 보냄 · 큐 비움 | 앱 단위 또는 에뮬레이터 |
| T67 | 앱 · 주행(D) · 서버 실패 | 시작 알림(A) | 홈 화면 · 문구 · 버튼은 시작됨 그대로 · 전송 실패 표시 없음 · 앱 꺼지지 않음 | ViewModel 단위 |
| T68 | 에뮬레이터 · 서버 실행(debug `http://10.0.2.2:4000/`) · 로그인됨 | 「여기로 안내」 → 안내 시작 | 서버 DB `navigation_starts` 1행(place_id · started_at · user_id) · 좌표 · 이름 없음 · stats 에 그 장소 | 왕복 · **#1 대기**(결정 기록 U27) · 보고서에 DB 조회 결과 |
| T69 | 앱 코드 | Konsist · 센서 | 요청 DTO 칸은 placeId · startedAt 둘뿐 · 좌표 · userId 없음 · Retrofit 인터페이스는 `data/remote` 에만 | ArchitectureTest 규칙 8 · `check_api_contract.py` |

## 7. 미확정 (수정: 2026-11-05 미확정 답 반영 — U27 ~ U34 결정 기록으로 옮김 · U1 · U24 · U25 · U26 남김)

| 번호 | 질문 | 선택지 | 추천 |
|---|---|---|---|
| U1 | 후보 장소 목록의 구체 값(수 · 장소 이름 · 주소 · 위도 · 경도)은? | (a) 사람이 직접 지정 (b) 회의록 2-B 의 예시 장소 (c) Pleos 공식 문서 예시 주소 포함 | (a) — 사람이 정함(2단계 시작 전 담당자에게 물음 · 2026-11-04 지시). 수는 고정 최대(3곳 · 결정 기록 U16)보다 많게(예: 5곳) — 그래야 「고르기」가 뜻이 있고 이슈 「3곳 이상」도 채움. 에뮬레이터 내비 지도 안 · 거리가 서로 다른 곳(가까운 곳 · 먼 곳)이면 시작 · 오류 케이스 재현에도 쓸모 있음. 스펙에는 「`assets/` JSON 한 곳의 고정 값」까지만 |
| U24 | 새 문구 초안(E6 두 가지 · E7 「장소 고르기」 · E8 「가 볼 장소는 최대 <n>곳까지 고정할 수 있어요」 · E10 「고정」 · 「고정 해제」)을 그대로 쓰나? | (a) 초안 그대로 (b) 사람이 고침 | (a) — 기존 문구 톤(「…어요」)과 맞춤 · 한 줄 · 스펙 PR 리뷰에서 고칠 수 있음 |
| U25 | 이슈 본문이 결정 U13 · U15 와 어긋남 — 「영역: app」만 · 「이번에 하지 않는 것: 서버 연동」 · 「데이터: 저장 없음 · 서버로 보내는 것 없음」 · 완료 조건 「홈에 장소 목록(3곳 이상)」(고정 0곳이면 빈 안내) | (a) 담당자가 이슈 본문을 고침(영역에 server 추가 · 「하지 않는 것」에서 서버 연동을 빼고 「추천 화면」을 넣음 · 데이터 칸에 고정 목록 · 안내 요청 기록 · 서버로 보내는 칸(장소 id · 시각 · 계정) · 완료 조건을 「고정 3곳이면 3곳이 보임」 · 「안내 시작 기록이 서버에 쌓이고 장소별 건수를 조회할 수 있음」으로) (b) 그대로 두고 이 스펙 결정 기록으로 갈음 | (a) — 이슈가 범위의 원본이라 리뷰어 · builder 가 이슈와 스펙을 대조할 때 어긋나지 않게 · 영역이 바뀌어 리뷰 관점(서버 구조 · 개인정보)도 달라짐 |
| U26 | #6 앱 뼈대 spec PR #7(`docs/spec/app-shell/artifacts/app-shell.md`)과 부딪침 — #7 은 주행 · 모름 때 앱 전체를 「주행 전용 화면 1장 · 버튼 3개」로 바꾸고 정차 홈에 기록 상태 · 「이 장소 기록」을 둠 → 이 스펙의 「주행 중에도 홈에서 「여기로 안내」 한 번 터치」가 그릴 자리가 없음(그 밖에 진단 카드 debug 전용 · landscape 고정도 다름) | (a) #7 주행 화면에 「가 볼 장소」(최대 3곳 · 「여기로 안내」)를 더함 (b) #4 는 정차 홈에서만 · 이슈 완료 조건 「주행 중에도」를 고침 (c) #7 주행 화면에 버튼 하나 → 「가 볼 장소」 목록(한 단계 전환) | **상태: 제안함 · #6 담당 답 대기.** 결정(최성호 · 2026-11-05) — (a) 쪽: #6 주행 전용 화면에 고정 장소 안내 버튼을 넣자고 #6 담당(노희주)에게 제안 · 이 스펙의 「주행 중에도 「여기로 안내」 한 번 터치」는 그대로 둠 · 최종 확정은 #6 쪽 답 뒤 결정 기록으로 옮김 |

### 결정 기록 (수정: 2026-11-05 미확정 답 반영 — U27 ~ U34 추가 · U20 · U22 · U23 정리)

| 번호 | 결정 | 결정자 | 날짜 |
|---|---|---|---|
| U2 | 임시 장소는 정확히 3곳(주행 중 스크롤 없이 첫 화면) → **2026-11-05 U13 으로 바뀜** — 고정 값 3곳이 아니라 사용자가 고정 · 개수는 고정 최대 3곳(U16)으로 · 「스크롤 없이 첫 화면」 근거는 U16 이 이어받음 | 최성호 | 2026-11-04 |
| U3 | 고정 값 자리는 `app/src/main/assets/` 의 JSON 파일 하나 · 저장소를 거쳐 받음 → **2026-11-05**: 이 JSON 은 「후보 장소」 목록(홈에 바로 보이는 목록 아님) | 최성호 | 2026-11-04 |
| U4 | 「안내 시작됨」 판정 = 경로 탐색 시작 알림(`onRouteStarted`) | 최성호 | 2026-11-04 |
| U5 | 내비 오류 → 실패 이유 · 문구는 4.2 표대로 · 표에 없는 값은 「경로를 찾지 못했어요」 | 최성호 | 2026-11-04 |
| U6 | POI 식별자(poiId · poiSubId)는 비움 · 안 되면 보고 | 최성호 | 2026-11-04 |
| U7 | 결과 문구(E3)는 다음 요청 시작까지 남김 · 자동 사라짐 · 닫기 버튼 없음 | 최성호 | 2026-11-04 |
| U8 | 기존 진단 카드 3개는 장소 목록 아래 그대로 · 정리는 2-1 때 | 최성호 | 2026-11-04 |
| U9 | 화면 문구는 2.1 문구 표대로 확정(2026-11-05 새 문구는 U24) | 최성호 | 2026-11-04 |
| U10 | `ALLOWED` 확장(`NAVI_ROUTE_SEARCH` · `NAVI_ROUTE`)은 하네스 이슈 #2 에 묶음 · 근거 칸에 이 산출물 경로 · #4 | 최성호 | 2026-11-04 |
| U11 | 내비 약관 미동의 상태는 따로 처리 없이 오류 · 응답 없음 경로로 | 최성호 | 2026-11-04 |
| U12 | 경로 옵션은 지정하지 않음(내비 기본값) | 최성호 | 2026-11-04 |
| U13 | 「가 볼 장소」는 **사용자가 핀 고정**하는 목록 · 고를 원본(#3 방문 기록 · 공유 받은 여행)이 없어 지금은 앱 안 후보 장소(assets JSON)에서 고정 · 해제 · 고정 목록은 기기 안 저장 · 앱 재실행 뒤 유지(4.1 의 「저장 없음」 대체) — PR #5 리뷰 1(giboJK) | 최성호 | 2026-11-05 |
| U14 | 고정한 장소가 0곳이면 구역 제목은 남기고 안내 한 줄 표시(구역을 숨기지 않음) — PR #5 리뷰 1 | 최성호 | 2026-11-05 |
| U15 | 처음(2026-11-05 오전): #4 는 서버 전송 없음 · 기기 안 기록(장소 id · 시각)만 → **2026-11-05 다시 결정(뒤집음)**: PR #5 리뷰 2(giboJK) 적극 수용 — #4 = app + server · `server/`(FastAPI)를 처음 만들고 안내 시작 기록 수신 API · 장소별 안내 건수 조회 API(추천 근거)까지 · 추천 화면은 별도 이슈 · 전송 칸 = 장소 id · 시각 · 계정(계정은 토큰에서) · 좌표 · 장소 이름 · 주소는 보내지 않음(「좌표는 서버에 없음」 유지) · 기기 안 기록은 전송 대기 큐(U29) | 최성호 | 2026-11-05 |
| U16 | 고정 최대 개수 = 3곳(AppConfig 한 곳) — 옛 U2 의 「주행 중 스크롤 없이 첫 화면」 근거를 이어받음 · CarUxRestrictions `UX_RESTRICTIONS_LIMIT_CONTENT` 취지 | 최성호 | 2026-11-05 |
| U17 | 고정 · 해제 자리 = 홈 안 「장소 고르기」 구역(「가 볼 장소」 아래) · 해제도 여기서 · 별도 화면 없음 · 「가 볼 장소」 항목엔 「여기로 안내」 하나만 | 최성호 | 2026-11-05 |
| U18 | 주행 · 모름 중 고정 · 해제는 막음 — 「장소 고르기」 구역 숨김 · 조작이 와도 무시(근거: CarUxRestrictions `UX_RESTRICTIONS_NO_SETUP` · `UX_RESTRICTIONS_LIMIT_CONTENT` · Pleos 주행 안전 가이드 「최소한의 조작 · 메뉴 단순화 · 터치 최소화」 · 이슈 완료 조건 「주행 중 긴 목록 스크롤 없음」) | 최성호 | 2026-11-05 |
| U19 | 「가 볼 장소」 순서 = 먼저 고정한 장소가 위 · 새 고정은 맨 아래 · 순서 바꾸기 없음 | 최성호 | 2026-11-05 |
| U20 | 기기 저장 방식 = 고정 목록은 DataStore · 안내 요청 기록은 **Room 전송 대기 큐**(U29 로 고침 — 처음엔 둘 다 DataStore) | 최성호 | 2026-11-05 |
| U21 | 안내 요청 기록 시점 = 시작됨(`onRouteStarted`)을 받은 때만 · 실패 · 응답 없음 · 미연결은 기록 안 함 | 최성호 | 2026-11-05 |
| U22 | 안내 요청 기록 = 보내면 지움 · 못 보낸 기록은 최대 100건(AppConfig) · 넘치면 오래된 것부터 지움(U29 로 고침 — 처음엔 「최근 100건 보관」) · 서버 보관은 U32 | 최성호 | 2026-11-05 |
| U23 | 기기 기록 삭제 화면은 이번엔 없음 — 보내면 지워짐 · 앱 데이터 지우기 · 앱 삭제로 사라짐 · 백업 안 됨 · 고정 해제는 기록을 지우지 않음 · 서버 쪽 삭제는 U32(계정 삭제 · 365일) | 최성호 | 2026-11-05 |
| U27 | #1 로그인 전에는 전송 보류 — 앱은 큐에 남기고 토큰이 생기면 보냄 · 서버는 시험용 비밀로 만든 토큰으로 통합 시험만 · 에뮬레이터 왕복(T68)은 #1 머지 뒤 · 시연 날까지 #1 이 늦으면 debug 전용 시드 계정을 그때 검토 | 최성호 | 2026-11-05 |
| U28 | #4 = 서버 뼈대(설정 · DB · Alembic · 오류 코드 · 로그 마스킹 · `users` 표 · 토큰 검증 `current_user`) · #1 = Pleos 교환(`app/auth/pleos/` · 스텁) · 토큰 발급 · 갱신 · 동의 철회 웹훅 · 토큰 꼴 HS256 · `sub` = `users.id` · `ver` = `token_version` · `exp` · **#1 담당(기보)과 맞춤 필요** — 경계 · 토큰 꼴 · `server/` 첫 판 머지 순서 | 최성호 | 2026-11-05 |
| U29 | 기기 안 안내 요청 기록 = 서버 전송 대기 큐 · Room(android-architecture 「오프라인」) · 보내면 지움 · 최대 100건 — U20 · U22 의 기록 부분을 고침 · 고정 목록은 DataStore 그대로 | 최성호 | 2026-11-05 |
| U30 | 전송 = 시작됨 즉시 1회 · 실패하면 연결될 때 · 30초부터 두 배 · 최대 1시간(AppConfig) · 앱이 꺼져 있어도 | 최성호 | 2026-11-05 |
| U31 | 동의 = #1 로그인 동의(`consents` · policy_version)에 「안내 시작 기록 수집 · 추천 근거 사용」 항목 · 동의 전엔 보내지 않음 · 서버 403 `CONSENT_REQUIRED` 검사는 `consents` 가 생긴 뒤 켬 · **#1 담당(기보)과 맞춤 필요** — 동의 항목 · 문구 · 검사 켜는 때 | 최성호 | 2026-11-05 |
| U32 | 서버 보관 365일(`Settings`) · 지난 행은 하루 한 번 지움 · 계정 삭제 · 동의 철회면 즉시 전부 삭제(`ON DELETE CASCADE`) | 최성호 | 2026-11-05 |
| U33 | 집계 = 보관 기간 안 전체 누적 · 응답에 `startCount` · `userCount` 둘 다 · 같은 (계정 · 장소 · 시각) 재전송은 한 행 · 같은 계정의 반복은 `startCount` 에만 | 최성호 | 2026-11-05 |
| U34 | 실행 · 시연 = 개발 PC 의 Docker PostgreSQL 17 + `uv run uvicorn app.main:app --port 4000` · 에뮬레이터는 debug 빌드 `http://10.0.2.2:4000/` · 시연은 debug 빌드 · 배포는 별도 이슈 · 두 PC 에 Docker 필요 | 최성호 | 2026-11-05 |

## 8. 완료 조건 (수정: 2026-11-05 미확정 답 반영 — U27 ~ U34 확정 값)

### app

- [ ] 홈 맨 위 「가 볼 장소」에 고정한 장소(고정 순서 · 이름 · 주소 · 「여기로 안내」)가 보임 · 고정 3곳(최대)이면 3곳 — T1 · T34
- [ ] 고정 0곳이면 구역 제목 + 빈 안내 한 줄(정차 · 주행 문구) — T30 · T31 · T32
- [ ] 정차 중 홈 「장소 고르기」에서 후보를 고정 · 해제 · 최대 3곳(결정 기록 U16 · AppConfig 한 곳)에서 「고정」 비활성 + 한도 안내 — T33 ~ T37 · T41 · T42 · T47
- [ ] 고정 목록이 기기 안에 저장되어 앱 강제 종료 · 프로세스 재생성 뒤에도 같은 순서로 남음 — T24 · T38
- [ ] 주행 · 모름에서는 「장소 고르기」 없음 · 고정 · 해제 조작 무시 — T19 · T21 · T39 · T40
- [ ] 후보 장소는 `app/src/main/assets/` 의 JSON 파일 한 곳의 고정 값 · 저장소를 거쳐 화면에 옴 · 다른 파일에 같은 값 없음 — 구체 값은 U1 답대로
- [ ] 「여기로 안내」 한 번 터치로 기본 내비 앱이 그 장소로 안내를 시작함(에뮬레이터 확인 · 화면 기록 또는 콜백 로그를 보고서에) — T2 · T3 · T18
- [ ] 요청 중 · 시작됨 · 실패(이유별 문구)가 홈 상태 · 화면에 반영됨 — T4 ~ T17 · T23
- [ ] 내비 미연결이면 버튼 비활성 + 「내비 서비스에 연결되지 않았어요」 — T7 · T8 · T9
- [ ] 정차 · 주행 · 모름 모두 「여기로 안내」 한 번 터치로 동작 · 화면에 스크롤(주행) · 글 입력 · 애니메이션 없음 — T19 ~ T22 · T26(T19 · T20 의 에뮬레이터 확인은 **의존: #2** 해결 뒤 · 그 전엔 보고서에 「#2 대기」로 적음)
- [ ] 안내 요청 기록이 Room 전송 대기 큐에 장소 id · 시각만으로 쌓임 · 시작됨 때만 기록(결정 기록 U21) · 보내면 지움 · 못 보낸 것 최대 100건(결정 기록 U29 · U22) · 재실행 뒤 유지 · 고정 목록은 DataStore — T43 ~ T46
- [ ] 시작된 안내 기록을 4.6 규칙대로 `POST /navigation-starts` 로 보냄(placeId · startedAt 두 칸) · 로그인 · 동의 전에는 보내지 않음(결정 기록 U27 · U31) · 실패 시 큐에 남기고 30초부터 두 배 · 최대 1시간 간격으로 다시 보냄(결정 기록 U30) · 화면 영향 없음 — T61 ~ T67 · T69(에뮬레이터 왕복 T68 은 **#1 대기** · 그 전엔 보고서에 「#1 대기」)
- [ ] 매니페스트에 `NAVI_ROUTE_SEARCH` 추가 · 다른 NAVI 권한 · 위치 권한 추가 없음 · 네트워크는 `INTERNET`(있음) · 쓰면 `ACCESS_NETWORK_STATE` 만 · `allowBackup="false"` 유지 — T28
- [ ] 로그에 좌표 · 장소 이름 · 주소 · 고정 목록 · 기록 내용 · 토큰 없음 — T27
- [ ] 시험 케이스 번호를 이름에 단 단위 테스트(ViewModel · 오류 대응 · 고정 · 기록 저장소 · 전송) — fake 저장소 · fake AppApi · fake 시계 · mock 라이브러리 없음
- [ ] 센서 녹색: Android Lint · 단위 테스트 · Konsist(ArchitectureTest) · assembleRelease · 매니페스트 검사(`check_manifest.py` — 하네스 이슈 #2 의 ALLOWED 확장 뒤) · API 계약 대조(`check_api_contract.py`) · 비밀 리터럴 · 저장소 위생
- [ ] 보고서에 에뮬레이터 확인 결과: 콜백 순서(성공 판정 `onRouteStarted` 가 실제로 오는지) · POI 식별자를 비운 채 안내 시작 여부 · 약관 미동의 상태에서의 동작(오류 · 응답 없음 중 어느 쪽인지) · 고정 목록이 앱 강제 종료 뒤 남는지(T38)
- [ ] 2단계 시작 전 U1 · U24 · U25 답 확인 · 결정 기록 U28 · U31 은 #1 담당(기보)과 맞춘 결과를 보고서에 · U26 은 #6 담당 답(제안: #6 주행 전용 화면에 고정 장소 안내 버튼) 뒤 이 스펙의 해당 절을 맞춤

### server

- [ ] `server/` 를 server-architecture 「폴더」 · 「도구 묶음」대로 만듦 · `uv sync` · `docker compose up -d db` · `uv run alembic upgrade head` · `uv run uvicorn app.main:app --port 4000` 으로 뜸(맥 · 윈도우 같은 명령 · Docker PostgreSQL 17 · 결정 기록 U34) · `.env.example` 만 커밋(비밀 없음)
- [ ] 첫 Alembic 리비전 = `users` · `navigation_starts`(4.5 — 칸마다 `comment=` · 유일 (user_id · place_id · started_at) · FK `ON DELETE CASCADE` · 좌표 · 이름 · 주소 · VIN 칸 없음)
- [ ] `POST /navigation-starts` · `GET /navigation-starts/stats` 가 5절 계약대로(성공 · 오류 코드 · 정렬 · 정의 밖 칸 거절) — T48 ~ T56
- [ ] 토큰 검증(`current_user`)은 결정 기록 U28 의 꼴(HS256 · `sub` · `ver` · `exp`) · user_id 는 토큰에서만 · 시험은 시험용 비밀로 만든 토큰 — T50
- [ ] 계정 삭제면 기록 전부 삭제 · 보관 365일(결정 기록 U32 · `Settings`) 지난 행 하루 한 번 정리 · 집계에서 빠짐 — T57 · T58
- [ ] 서버 로그에 계정 id · pleos_user_id · placeId · startedAt · 토큰 없음 — T59
- [ ] `docs/api/openapi.json` 을 서버에서 내보내 커밋 · 5절과 같음 · 앱 Retrofit 과 1:1 — T60
- [ ] 센서 녹색: ruff(lint · format) · mypy --strict · import-linter(layers · domains) · pytest(단위 + testcontainers 통합 · 테스트 이름에 T 번호 · 외부 호출 0) · `alembic check` · `check_alembic_heads.py`(1개) · `check_sql_comments.py` · `check_routes.py` · `check_api_contract.py` · 비밀 리터럴 · 저장소 위생
- [ ] #1 담당(기보)과 결정 기록 U28(서버 경계 · 토큰 꼴 · `server/` 첫 판 머지 순서) · U31(동의 항목 · 403 검사 켜는 때)을 맞춤

읽은 문서: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations · https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions · https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline(2026-11-05 다시 읽음 — 주행 중 목록 선택 · 편집에 대한 수치 기준 없음 · 「최소한의 조작 · 메뉴 구조 단순화 · 터치 최소화」 원칙만) · https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictions(`UX_RESTRICTIONS_NO_SETUP` · `UX_RESTRICTIONS_LIMIT_CONTENT` · `getMaxCumulativeContentItems`) · https://developer.android.com/topic/architecture/recommendations(스킬 요약으로 · 직접 열지 않음) · https://developer.android.com/design/ui/cars/guides/foundations/interaction-principles(열리지 않음 · 404) · 저장소 SDK `ai.pleos.playground:navi-data:3.0.4` 의 `NaviErrorCode` 값 이름(Gradle 캐시) · `origin/app-shell/spec` 의 `docs/spec/app-shell/artifacts/app-shell.md`(U26 충돌 확인용) · `app/src/main/AndroidManifest.xml`(`allowBackup` · `INTERNET`) · `app/build.gradle.kts`(`API_BASE_URL`) · `di/NetworkModule.kt` · `.claude/skills/sensors/scripts/check_api_contract.py` · `check_manifest.py`(`ALLOWED`) · `server-architecture` 스킬(서버 폴더 · 인증 · 데이터 · OpenAPI · 테스트 — FastAPI · Alembic 원문 문서는 직접 열지 않음)
