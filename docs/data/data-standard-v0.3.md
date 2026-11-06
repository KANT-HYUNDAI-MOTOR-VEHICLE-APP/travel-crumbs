# Travel Crumbs 데이터 표준 정의서 v0.3 (초안)

작성: 2026-11-06 · 상태: **결정** = 사용자와 논의해 확정 / **제안** = Claude 제안, 사용자 미확정 / **미결** = 논의 필요 / **가안** = 값 목록 초안

## 0. v0.2 대비 변경 사항

| 구분 | 내용 |
|---|---|
| 삭제 단어 | "경유지" 단어를 사용하지 않음. 계획경로의 장소 목록도 장소(place) 구조로 통일 |
| 용어 변경 | 계획경로장소번호(planned_route_place_no), 장소순서(place_ord) 사용. v0.2의 계획경로경유지번호·경유지순서 삭제 |
| 테이블 변경 | planned_route_place_dtl (계획경로장소내역) |
| 기타 | 이 외의 내용은 v0.2와 같음. 팀 회의 결과는 이후 버전에 반영 |

## 1. 명명 규칙

| 규칙ID | 구분 | 규칙 | 예시 | 상태 |
|---|---|---|---|---|
| R01 | 표기 방식 | 테이블명·컬럼명은 영문 소문자 snake_case | trip_status_cd | 제안 |
| R02 | 기본키(PK) | 모든 엔티티 테이블은 시퀀스 대리키를 PK로 사용. PK 용어 = 테이블명에서 유형 단어를 뺀 단어 + 번호(no). 단 이력·로그는 이력·로그 1건이 대상이라 유형 단어 유지 (T03) | user_mst → user_no / trip_status_hist → trip_status_hist_no | 결정 (이력·로그 예외는 제안) |
| R03 | 로그인 아이디 | 로그인용 아이디는 login_id (xxx_id를 외래키로 읽는 관습과 혼동 방지). 아이디(id) 분류어는 login_id에만 사용 | user_mst.login_id | 제안 |
| R04 | 외래키(FK) | 외래키 컬럼명은 참조하는 PK 컬럼명을 그대로 사용 | trip_mst.user_no | 제안 |
| R05 | 역할 접두어 | 같은 테이블을 두 번 이상 참조하면 역할 단어를 앞에 붙임 | sender_user_no, receiver_user_no, copy_trip_no | 제안 |
| R06 | 용어 구성 | 용어(컬럼)는 표준 단어의 조합. 물리명은 단어 물리명을 _로 연결. 마지막 단어는 분류어이며 분류어가 도메인을 결정 | 사용자(user)+번호(no) = user_no | 결정 |
| R06-1 | 단어 등록 | 표준 단어 사전에 없는 단어는 쓸 수 없음. 새 단어는 먼저 등록 | — | 제안 |
| R07 | 공통 컬럼 | 모든 테이블: created_at, created_user_no, updated_at, updated_user_no, deleted_yn. 로그 테이블: created_at, created_user_no만 (T06) | — | 제안 |
| R08 | 외부 노출 키 | 공유 링크·QR 등 외부에 보이는 식별자는 {대상}_uuid 사용 | trip_uuid | 제안 (4번 안건과 함께 확정) |
| R09 | 도메인·코드·마스터 구분 | 도메인: 값 형식 규칙(_yn, 일시 등) → 정의서로만 관리 / 코드: 합의된 유한 목록(상태·유형) → 통합 코드 테이블 / 마스터: 속성을 가진 관리 대상(태그·장소 등) → 별도 테이블 | — | 결정 |
| R10 | 코드 테이블 구조 | 통합 공통 코드: cd_grp_mst + cd_dtl. 앱 로직과 연결된 상태 코드는 앱 상수로도 정의 | — | 결정 |
| R11 | 코드값 관리 | 한 번 사용한 코드값의 의미는 바꾸지 않음. 사용 중지는 삭제가 아니라 use_yn = N | — | 제안 |
| R12 | 이력 테이블 대상 | 상태 변화·동의처럼 추적이 필요한 대상만 이력 테이블을 둠. 그 외는 공통 컬럼 | trip_status_hist, user_consent_hist | 제안 |
| R13 | 위치 데이터 저장 | 저장 주기: 거리 기준 + 방향 전환·정차 시점 추가. 원본(trip_route_point_dtl)과 간소화 경로(trip_route_dtl) 분리. 원본 삭제 기능은 추후. 삭제 이유는 저장 공간보다 개인정보 보관 기간 | — | 결정 |
| R14 | 위치정보 동의 | 첫 여행 시작 시(A-04) 1회 동의, 설정에서 철회. 공개 동의(B-12)와 분리. 동의·철회는 user_consent_hist에 기록. 법적 요건은 별도 확인 | — | 결정 |

## 2. 테이블 표준

### 2-1. 테이블 규칙

| 규칙ID | 구분 | 규칙 | 예시 | 상태 |
|---|---|---|---|---|
| T01 | 테이블 유형 | 마스터·내역·이력·로그·관계 5가지 (아래 표). 코드 테이블도 마스터/내역으로 분류 | 결정 | 결정 |
| T02 | 유형 단어 | 모든 테이블명 끝에 유형 단어를 붙임: _mst, _dtl, _hist, _log, _rel. user 예약어 문제 해결 | 결정 | 결정 |
| T03 | 테이블명 구성 | 내역·이력·관계는 상위 마스터 단어 + 대상 단어 + 유형 단어 | user_addr_dtl, trip_place_dtl, trip_status_hist | 제안 |
| T04 | 마스터 원칙 | 마스터는 개체 하나당 한 행. 개체에 딸린 여러 건은 내역으로, 값이 바뀐 기록은 이력으로 분리 | user_mst는 자연인 한 사람의 정보만 | 결정 |
| T05 | 컬럼 순서 | PK → FK → 업무 컬럼 → 공통 컬럼 | — | 제안 |
| T06 | 로그 테이블 | 추가만 하고 수정·삭제하지 않음. 공통 컬럼은 created_at, created_user_no만 | navi_req_log | 제안 |

### 2-2. 테이블 유형 정의

| 유형 | 물리명 단어 | 정의 | 판별 질문 | 예시 |
|---|---|---|---|---|
| 마스터 | mst | 업무의 기준이 되는 독립 개체. 개체 하나당 한 행 | 다른 테이블 없이 혼자 의미가 있는가? | user_mst, trip_mst, place_mst |
| 내역 | dtl | 마스터 1건에 딸린, 동시에 유효한 여러 건 | 지금 여러 건이 함께 유효한가? | 홍길동의 집 주소·회사 주소 → user_addr_dtl 2행 |
| 이력 | hist | 대상 값의 변경 전후 기록 | 과거 값을 시간순으로 남기는가? | 홍길동이 이사해서 바뀐 주소 → user_addr_hist |
| 로그 | log | 발생한 사건의 건별 기록. 추가만 함 | 원본을 바꾸지 않고 사건만 쌓는가? | 길안내 요청·성공·실패 → navi_req_log |
| 관계 | rel | 두 마스터(또는 내역)의 N:M 연결 | 양쪽 PK 연결 외에 내용이 거의 없는가? | 여행장소-태그 → trip_place_tag_rel |

- **내역 vs 이력:** 내역은 같은 시점에 동시에 유효한 여러 건(집 주소와 회사 주소), 이력은 시간에 따라 바뀐 값의 기록(이사 전후 주소).
- **이력 vs 로그:** 이력은 특정 대상의 값이 바뀐 기록(여행 상태 기록 중 → 완료), 로그는 일어난 사건의 건별 기록(길안내 요청, 성공·실패).
- **참고:** 주민등록번호는 법적으로 수집이 엄격히 제한되므로 모델에 넣지 않음.

## 3. 표준 단어 사전

논리명(한글)과 물리명(영문 약어)을 1:1로 정의. 분류어 = Y는 용어의 마지막에 와서 도메인을 결정하는 단어.

| 논리명 | 영문명 | 물리명 | 분류어 여부 | 정의 |
|---|---|---|---|---|
| 사용자 | USER | user | N | 앱에 가입한 자연인 한 사람 |
| 로그인 | LOGIN | login | N | 계정 인증 행위 |
| 비밀번호 | PASSWORD | password | N | 로그인 인증용 비밀 문자열 |
| 닉네임 | NICKNAME | nickname | N | 화면에 표시하는 사용자 이름 |
| 차량 | VEHICLE | vehicle | N | 사용자가 보유한 자동차 (연구소 차종 개체와 구분) |
| 여행 | TRIP | trip | N | 기록 시작부터 종료까지 한 번의 이동 단위 |
| 동행자 | COMPANION | companion | N | 여행에 함께한 사람·반려동물의 유형 |
| 경로 | ROUTE | route | N | 이동한 길 또는 계획·추천한 길 |
| 점 | POINT | point | N | 경로를 이루는 하나의 좌표 기록 |
| 장소 | PLACE | place | N | 실제 존재하는 고정된 곳 (이름·유형·좌표) |
| 시설 | FACILITY | facility | N | 장소가 갖춘 설비 (예: 수유실) |
| 태그 | TAG | tag | N | 경험을 분류하는 짧은 키워드 |
| 후기 | REVIEW | review | N | 장소에서의 경험에 대한 평가 |
| 성격 | POLARITY | polarity | N | 후기의 긍정·주의 구분 |
| 공유 | SHARE | share | N | 다른 사용자에게 여행을 전달하는 행위 |
| 발신 | SENDER | sender | N | 공유를 보낸 쪽 (역할 접두어) |
| 수신 | RECEIVER | receiver | N | 공유를 받은 쪽 (역할 접두어) |
| 복제 | COPY | copy | N | 원본을 복사해 만든 것 |
| 공개범위 | VISIBILITY | visibility | N | 기록을 누구에게 보여줄지의 범위 |
| 임시저장 | DRAFT | draft | N | 상세 입력 전 위치만 저장한 상태 |
| 동의 | CONSENT | consent | N | 개인정보 처리에 대한 사용자 동의 |
| 길안내 | NAVIGATION | navi | N | 차량 내비게이션 경로 안내 (NaviHelper) |
| 요청 | REQUEST | req | N | 시스템에 처리를 요구한 건 |
| 추천 | RECOMMENDATION | reco | N | 다른 여행 기록을 바탕으로 제안한 경로 |
| 계획 | PLANNED | planned | N | 이동 전에 정한 경로 |
| 검색 | SEARCH | search | N | 조건으로 기록을 찾는 행위 |
| 조건 | CONDITION | cond | N | 검색·추천에 쓰는 기준 |
| 출발 | DEPARTURE | dep | N | 경로의 시작 지점 |
| 목적지 | DESTINATION | dest | N | 경로의 도착 지점 |
| 유사도 | SIMILARITY | similarity | N | 두 경로가 비슷한 정도 |
| 메모 | MEMO | memo | N | 사용자가 자유롭게 쓴 짧은 글 |
| 위도 | LATITUDE | lat | N | 위치의 남북 좌표 |
| 경도 | LONGITUDE | lng | N | 위치의 동서 좌표 |
| 기록 | RECORD | recorded | N | 사용자·앱이 위치·장소를 저장한 행위 (일시와 결합: recorded_at) |
| 시작 | START | start | N | 시작 시점 |
| 종료 | END | end | N | 종료 시점 |
| 거리 | DISTANCE | dist | N | 이동 거리 (미터) |
| 소요시간 | DURATION | duration | N | 걸린 시간 (초) |
| 데이터 | DATA | data | N | 저장되는 값의 묶음 |
| 출처 | SOURCE | src | N | 데이터를 가져온 곳 |
| 설치 | INSTALL | install | N | 시설이 갖춰져 있음 |
| 확인 | CONFIRM | confirm | N | 정보가 맞는지 확인한 행위 |
| 상태 | STATUS | status | N | 대상이 현재 놓인 단계 |
| 유형 | TYPE | type | N | 대상을 나누는 종류 |
| 그룹 | GROUP | grp | N | 같은 종류를 묶는 단위 |
| 생성 | CREATED | created | N | 데이터를 처음 만든 행위 |
| 수정 | UPDATED | updated | N | 데이터를 마지막으로 바꾼 행위 |
| 삭제 | DELETED | deleted | N | 논리 삭제 처리 |
| 사용 | USE | use | N | 사용 가능 여부 |
| 정렬 | SORT | sort | N | 화면 표시 순서 |
| 마스터 | MASTER | mst | N | 테이블 유형 단어 (테이블명에만 사용) |
| 내역 | DETAIL | dtl | N | 테이블 유형 단어 (테이블명에만 사용) |
| 이력 | HISTORY | hist | N | 테이블 유형 단어 / 변경 기록 |
| 로그 | LOG | log | N | 테이블 유형 단어 / 사건 기록 |
| 관계 | RELATION | rel | N | 테이블 유형 단어 (테이블명에만 사용) |
| 번호 | NUMBER | no | Y | 대리키·일련번호 |
| 아이디 | ID | id | Y | 사용자가 입력하는 식별 문자열 (login_id에만 사용) |
| UUID | UUID | uuid | Y | 외부 노출·동기화용 범용 고유 식별자 |
| 코드 | CODE | cd | Y | 표준 코드 값 |
| 명 | NAME | nm | Y | 이름 |
| 여부 | YN | yn | Y | Y/N 값 |
| 일시 | DATETIME | at | Y | 날짜와 시각 |
| 개수 | COUNT | cnt | Y | 건수 |
| 순서 | ORDER | ord | Y | 정렬·경로 내 순서 |
| 해시 | HASH | hash | Y | 단방향 암호화 값 |

## 4. 표준 도메인 사전

| 도메인명 | 분류어 물리명 | 데이터 타입 | 길이 | 소수점 | 허용값·형식 | 용도 |
|---|---|---|---|---|---|---|
| 번호 | no | BIGINT | — | — | 1 이상 자동 증가 (시퀀스) | 대리키·일련번호 |
| UUID | uuid | CHAR | 36 | — | UUID v4 문자열 | 외부 노출·동기화 키 |
| 아이디 | id | VARCHAR | 50 | — | 영문·숫자 조합 | 로그인 아이디 |
| 해시 | hash | VARCHAR | 255 | — | 해시값만 저장 (평문 금지) | 비밀번호 해시 |
| 코드 | cd | VARCHAR | 30 | — | cd_dtl의 코드값 | 상태·유형 코드 |
| 명 | nm | VARCHAR | 100 | — | — | 이름 |
| 메모 | memo | VARCHAR | 500 | — | — | 짧은 자유 입력 |
| 여부 | yn | CHAR | 1 | — | Y / N | 여부 값 (코드 테이블 사용 안 함) |
| 일시 | at | TIMESTAMP WITH TIME ZONE | — | — | UTC 저장, 화면에서 현지 시각 변환 | 모든 일시 |
| 개수 | cnt | INTEGER | — | — | 0 이상 | 건수 (반정규화 집계값 포함) |
| 순서 | ord | INTEGER | — | — | 1 이상 | 정렬·경로 내 순서 |
| 위도 | lat | DECIMAL | 10 | 7 | -90 ~ 90 | 위치 좌표 |
| 경도 | lng | DECIMAL | 10 | 7 | -180 ~ 180 | 위치 좌표 |
| 거리 | dist | INTEGER | — | — | 미터 단위, 0 이상 | 이동 거리 |
| 소요시간 | duration | INTEGER | — | — | 초 단위, 0 이상 | 소요 시간 |
| 유사도 | similarity | DECIMAL | 5 | 2 | 0 ~ 100 (%) | 경로 유사도 |
| 경로데이터 | data | TEXT | — | — | 간소화 경로 인코딩 (형식 미결) | route_data 전용 |

## 5. 표준 용어 사전

ERD v0.3의 모든 컬럼(75개)을 단어로 분해해 대조함. 사용 테이블은 ERD v0.3 기준.

| 논리명 | 물리명 | 구성 단어 | 도메인 | 사용 테이블 |
|---|---|---|---|---|
| 코드 | cd | 코드(cd) | 코드 | cd_dtl |
| 코드그룹코드 | cd_grp_cd | 코드(cd) + 그룹(grp) + 코드(cd) | 코드 | cd_dtl, cd_grp_mst |
| 코드그룹명 | cd_grp_nm | 코드(cd) + 그룹(grp) + 명(nm) | 명 | cd_grp_mst |
| 코드명 | cd_nm | 코드(cd) + 명(nm) | 명 | cd_dtl |
| 동행자유형코드 | companion_type_cd | 동행자(companion) + 유형(type) + 코드(cd) | 코드 | route_search_cond_mst, trip_companion_dtl |
| 확인일시 | confirm_at | 확인(confirm) + 일시(at) | 일시 | place_facility_dtl |
| 동의유형코드 | consent_type_cd | 동의(consent) + 유형(type) + 코드(cd) | 코드 | user_consent_hist |
| 동의여부 | consent_yn | 동의(consent) + 여부(yn) | 여부 | user_consent_hist |
| 복제여행번호 | copy_trip_no | 복제(copy) + 여행(trip) + 번호(no) | 번호 | trip_share_dtl |
| 생성일시 | created_at | 생성(created) + 일시(at) | 일시 | cd_dtl, cd_grp_mst, navi_req_log, place_facility_dtl, place_mst, planned_route_mst, planned_route_place_dtl, route_reco_dtl, route_reco_place_dtl, route_search_cond_mst, route_search_cond_tag_rel, tag_mst, trip_companion_dtl, trip_mst, trip_place_dtl, trip_place_tag_rel, trip_route_dtl, trip_route_point_dtl, trip_share_dtl, trip_status_hist, user_consent_hist, user_mst, user_vehicle_dtl |
| 생성사용자번호 | created_user_no | 생성(created) + 사용자(user) + 번호(no) | 번호 | cd_dtl, cd_grp_mst, navi_req_log, place_facility_dtl, place_mst, planned_route_mst, planned_route_place_dtl, route_reco_dtl, route_reco_place_dtl, route_search_cond_mst, route_search_cond_tag_rel, tag_mst, trip_companion_dtl, trip_mst, trip_place_dtl, trip_place_tag_rel, trip_route_dtl, trip_route_point_dtl, trip_share_dtl, trip_status_hist, user_consent_hist, user_mst, user_vehicle_dtl |
| 데이터출처코드 | data_src_cd | 데이터(data) + 출처(src) + 코드(cd) | 코드 | place_facility_dtl, place_mst |
| 삭제여부 | deleted_yn | 삭제(deleted) + 여부(yn) | 여부 | cd_dtl, cd_grp_mst, place_facility_dtl, place_mst, planned_route_mst, planned_route_place_dtl, route_reco_dtl, route_reco_place_dtl, route_search_cond_mst, route_search_cond_tag_rel, tag_mst, trip_companion_dtl, trip_mst, trip_place_dtl, trip_place_tag_rel, trip_route_dtl, trip_route_point_dtl, trip_share_dtl, trip_status_hist, user_consent_hist, user_mst, user_vehicle_dtl |
| 출발위도 | dep_lat | 출발(dep) + 위도(lat) | 위도 | route_search_cond_mst |
| 출발경도 | dep_lng | 출발(dep) + 경도(lng) | 경도 | route_search_cond_mst |
| 목적지위도 | dest_lat | 목적지(dest) + 위도(lat) | 위도 | navi_req_log, route_search_cond_mst |
| 목적지경도 | dest_lng | 목적지(dest) + 경도(lng) | 경도 | navi_req_log, route_search_cond_mst |
| 거리 | dist | 거리(dist) | 거리 | trip_mst |
| 임시저장여부 | draft_yn | 임시저장(draft) + 여부(yn) | 여부 | trip_place_dtl |
| 소요시간 | duration | 소요시간(duration) | 소요시간 | route_reco_dtl, trip_mst |
| 종료일시 | end_at | 종료(end) + 일시(at) | 일시 | trip_mst |
| 시설코드 | facility_cd | 시설(facility) + 코드(cd) | 코드 | place_facility_dtl |
| 설치여부 | install_yn | 설치(install) + 여부(yn) | 여부 | place_facility_dtl |
| 위도 | lat | 위도(lat) | 위도 | place_mst, trip_place_dtl, trip_route_point_dtl |
| 경도 | lng | 경도(lng) | 경도 | place_mst, trip_place_dtl, trip_route_point_dtl |
| 로그인아이디 | login_id | 로그인(login) + 아이디(id) | 아이디 | user_mst |
| 메모 | memo | 메모(memo) | 메모 | trip_place_dtl |
| 길안내요청로그번호 | navi_req_log_no | 길안내(navi) + 요청(req) + 로그(log) + 번호(no) | 번호 | navi_req_log |
| 길안내요청상태코드 | navi_req_status_cd | 길안내(navi) + 요청(req) + 상태(status) + 코드(cd) | 코드 | navi_req_log |
| 닉네임명 | nickname_nm | 닉네임(nickname) + 명(nm) | 명 | user_mst |
| 비밀번호해시 | password_hash | 비밀번호(password) + 해시(hash) | 해시 | user_mst |
| 장소개수 | place_cnt | 장소(place) + 개수(cnt) | 개수 | trip_mst |
| 장소시설번호 | place_facility_no | 장소(place) + 시설(facility) + 번호(no) | 번호 | place_facility_dtl |
| 장소명 | place_nm | 장소(place) + 명(nm) | 명 | place_mst |
| 장소번호 | place_no | 장소(place) + 번호(no) | 번호 | navi_req_log, place_facility_dtl, place_mst, planned_route_place_dtl, route_reco_place_dtl, trip_place_dtl |
| 장소순서 | place_ord | 장소(place) + 순서(ord) | 순서 | planned_route_place_dtl, route_reco_place_dtl, trip_place_dtl |
| 장소유형코드 | place_type_cd | 장소(place) + 유형(type) + 코드(cd) | 코드 | place_mst |
| 계획경로번호 | planned_route_no | 계획(planned) + 경로(route) + 번호(no) | 번호 | navi_req_log, planned_route_mst, planned_route_place_dtl |
| 계획경로장소번호 | planned_route_place_no | 계획(planned) + 경로(route) + 장소(place) + 번호(no) | 번호 | planned_route_place_dtl |
| 수신사용자번호 | receiver_user_no | 수신(receiver) + 사용자(user) + 번호(no) | 번호 | trip_share_dtl |
| 추천유형코드 | reco_type_cd | 추천(reco) + 유형(type) + 코드(cd) | 코드 | route_reco_dtl |
| 기록일시 | recorded_at | 기록(recorded) + 일시(at) | 일시 | trip_place_dtl, trip_route_point_dtl |
| 후기성격코드 | review_polarity_cd | 후기(review) + 성격(polarity) + 코드(cd) | 코드 | trip_place_dtl |
| 경로데이터 | route_data | 경로(route) + 데이터(data) | 경로데이터 | trip_route_dtl |
| 경로추천번호 | route_reco_no | 경로(route) + 추천(reco) + 번호(no) | 번호 | planned_route_mst, route_reco_dtl, route_reco_place_dtl |
| 경로추천장소번호 | route_reco_place_no | 경로(route) + 추천(reco) + 장소(place) + 번호(no) | 번호 | route_reco_place_dtl |
| 경로검색조건번호 | route_search_cond_no | 경로(route) + 검색(search) + 조건(cond) + 번호(no) | 번호 | route_reco_dtl, route_search_cond_mst, route_search_cond_tag_rel |
| 경로검색조건태그번호 | route_search_cond_tag_no | 경로(route) + 검색(search) + 조건(cond) + 태그(tag) + 번호(no) | 번호 | route_search_cond_tag_rel |
| 발신사용자번호 | sender_user_no | 발신(sender) + 사용자(user) + 번호(no) | 번호 | trip_share_dtl |
| 공유상태코드 | share_status_cd | 공유(share) + 상태(status) + 코드(cd) | 코드 | trip_share_dtl |
| 유사도 | similarity | 유사도(similarity) | 유사도 | route_reco_dtl |
| 정렬순서 | sort_ord | 정렬(sort) + 순서(ord) | 순서 | cd_dtl |
| 시작일시 | start_at | 시작(start) + 일시(at) | 일시 | trip_mst |
| 태그명 | tag_nm | 태그(tag) + 명(nm) | 명 | tag_mst |
| 태그번호 | tag_no | 태그(tag) + 번호(no) | 번호 | route_search_cond_tag_rel, tag_mst, trip_place_tag_rel |
| 여행개수 | trip_cnt | 여행(trip) + 개수(cnt) | 개수 | route_reco_dtl |
| 여행동행자번호 | trip_companion_no | 여행(trip) + 동행자(companion) + 번호(no) | 번호 | trip_companion_dtl |
| 여행명 | trip_nm | 여행(trip) + 명(nm) | 명 | trip_mst |
| 여행번호 | trip_no | 여행(trip) + 번호(no) | 번호 | planned_route_mst, trip_companion_dtl, trip_mst, trip_place_dtl, trip_route_dtl, trip_route_point_dtl, trip_share_dtl, trip_status_hist |
| 여행장소번호 | trip_place_no | 여행(trip) + 장소(place) + 번호(no) | 번호 | trip_place_dtl, trip_place_tag_rel |
| 여행장소태그번호 | trip_place_tag_no | 여행(trip) + 장소(place) + 태그(tag) + 번호(no) | 번호 | trip_place_tag_rel |
| 여행경로번호 | trip_route_no | 여행(trip) + 경로(route) + 번호(no) | 번호 | trip_route_dtl |
| 여행경로점번호 | trip_route_point_no | 여행(trip) + 경로(route) + 점(point) + 번호(no) | 번호 | trip_route_point_dtl |
| 여행공유번호 | trip_share_no | 여행(trip) + 공유(share) + 번호(no) | 번호 | trip_share_dtl |
| 여행상태코드 | trip_status_cd | 여행(trip) + 상태(status) + 코드(cd) | 코드 | trip_mst, trip_status_hist |
| 여행상태이력번호 | trip_status_hist_no | 여행(trip) + 상태(status) + 이력(hist) + 번호(no) | 번호 | trip_status_hist |
| 여행UUID | trip_uuid | 여행(trip) + UUID(uuid) | UUID | trip_mst |
| 수정일시 | updated_at | 수정(updated) + 일시(at) | 일시 | cd_dtl, cd_grp_mst, place_facility_dtl, place_mst, planned_route_mst, planned_route_place_dtl, route_reco_dtl, route_reco_place_dtl, route_search_cond_mst, route_search_cond_tag_rel, tag_mst, trip_companion_dtl, trip_mst, trip_place_dtl, trip_place_tag_rel, trip_route_dtl, trip_route_point_dtl, trip_share_dtl, trip_status_hist, user_consent_hist, user_mst, user_vehicle_dtl |
| 수정사용자번호 | updated_user_no | 수정(updated) + 사용자(user) + 번호(no) | 번호 | cd_dtl, cd_grp_mst, place_facility_dtl, place_mst, planned_route_mst, planned_route_place_dtl, route_reco_dtl, route_reco_place_dtl, route_search_cond_mst, route_search_cond_tag_rel, tag_mst, trip_companion_dtl, trip_mst, trip_place_dtl, trip_place_tag_rel, trip_route_dtl, trip_route_point_dtl, trip_share_dtl, trip_status_hist, user_consent_hist, user_mst, user_vehicle_dtl |
| 사용여부 | use_yn | 사용(use) + 여부(yn) | 여부 | cd_dtl, tag_mst |
| 사용자동의이력번호 | user_consent_hist_no | 사용자(user) + 동의(consent) + 이력(hist) + 번호(no) | 번호 | user_consent_hist |
| 사용자번호 | user_no | 사용자(user) + 번호(no) | 번호 | navi_req_log, planned_route_mst, route_search_cond_mst, trip_mst, user_consent_hist, user_mst, user_vehicle_dtl |
| 사용자차량번호 | user_vehicle_no | 사용자(user) + 차량(vehicle) + 번호(no) | 번호 | user_vehicle_dtl |
| 차량명 | vehicle_nm | 차량(vehicle) + 명(nm) | 명 | user_vehicle_dtl |
| 공개범위코드 | visibility_cd | 공개범위(visibility) + 코드(cd) | 코드 | trip_mst |

## 6. 표준 코드 정의서

코드값은 영문 대문자 가안 (형식은 M01에서 확정). 비고 = 상태 전이 규칙 등.

| 코드그룹 | 그룹명 | 코드값(가안) | 코드명 | 정의 | 비고 | 상태 |
|---|---|---|---|---|---|---|
| TRIP_STATUS | 여행 상태 | RECORDING | 기록 중 | 위치를 기록하고 있는 상태 | → COMPLETED / INTERRUPTED / CANCELED | 결정 |
| TRIP_STATUS | 여행 상태 | COMPLETED | 완료 | 사용자가 여행 종료를 눌러 정상 종료 | 종료 상태 (변경 불가) | 결정 |
| TRIP_STATUS | 여행 상태 | INTERRUPTED | 중단 | 시동 꺼짐·앱 종료·오류로 의도 없이 끊긴 상태 | → RECORDING(재개) / COMPLETED / CANCELED | 결정 |
| TRIP_STATUS | 여행 상태 | CANCELED | 취소 | 사용자가 기록을 남기지 않기로 하고 끝냄 | 종료 상태. 기록 삭제 여부는 3번 안건 | 결정 |
| SHARE_STATUS | 공유 상태 | NEW | 새 공유 | 공유되었으나 수신자가 아직 수락하지 않음 | → ACCEPTED | 결정 |
| SHARE_STATUS | 공유 상태 | ACCEPTED | 수락 | 수신자가 수락해 복제 여행 생성 | 종료 상태 | 결정 |
| PLACE_TYPE | 장소 유형 | RESTAURANT | 음식점 | 식사 장소 |  | 가안 |
| PLACE_TYPE | 장소 유형 | PARKING | 주차장 | 주차 장소 |  | 가안 |
| PLACE_TYPE | 장소 유형 | REST_AREA | 휴게소 | 고속도로·국도 휴게소 |  | 가안 |
| PLACE_TYPE | 장소 유형 | ATTRACTION | 명소 | 관광·볼거리 장소 |  | 가안 |
| PLACE_TYPE | 장소 유형 | MUSEUM | 박물관 | 박물관 |  | 가안 |
| PLACE_TYPE | 장소 유형 | GALLERY | 미술관 | 미술관 | 분류 체계(계층) 미결 M03. 편의시설은 유형에서 제외 → FACILITY | 가안 |
| COMPANION_TYPE | 동행자 유형 | SOLO | 혼자 | 동행자 없음 |  | 결정 |
| COMPANION_TYPE | 동행자 유형 | SPOUSE | 배우자 | 배우자 동행 | 명칭 미결 M02 (배우자/부부) | 미결 |
| COMPANION_TYPE | 동행자 유형 | CHILD | 아이 | 아이 동행 | 명칭 미결 M02 (아이/아이 동반) | 미결 |
| COMPANION_TYPE | 동행자 유형 | PARENT | 부모님 | 부모님 동행 |  | 결정 |
| COMPANION_TYPE | 동행자 유형 | PET | 반려동물 | 반려동물 동행 |  | 결정 |
| VISIBILITY | 공개 범위 | PRIVATE | 나만 보기 | 본인만 조회 |  | 결정 |
| VISIBILITY | 공개 범위 | ANONYMOUS_PUBLIC | 익명 공개 | 작성자를 숨기고 추천 데이터로 제공 | 익명화 범위 5번 안건 | 결정 |
| VISIBILITY | 공개 범위 | DIRECT_SHARE | 직접 공유만 | 지정한 사용자에게만 공유 |  | 결정 |
| NAVI_REQ_STATUS | 길안내 요청 상태 | REQUESTING | 요청 중 | NaviHelper 호출 후 결과 대기 | → SUCCESS / FAILED | 결정 |
| NAVI_REQ_STATUS | 길안내 요청 상태 | SUCCESS | 성공 | 차량 길안내 시작됨 |  | 결정 |
| NAVI_REQ_STATUS | 길안내 요청 상태 | FAILED | 실패 | 호출 실패 (더미 경로 폴백) |  | 결정 |
| CONSENT_TYPE | 동의 유형 | LOCATION_COLLECT | 위치 수집 | 여행 기록용 위치정보 수집 동의 | 첫 여행 시작 시 (R14) | 결정 |
| CONSENT_TYPE | 동의 유형 | RECO_PUBLIC | 추천 공개 | 기록을 익명 추천 데이터로 제공하는 동의 | B-12 | 결정 |
| RECO_TYPE | 추천 경로 유형 | CHILD_FRIENDLY | 아이 동반 | 아이 편의시설 중심 경로 | 커지면 마스터 전환 검토 | 결정 |
| RECO_TYPE | 추천 경로 유형 | FOOD | 맛집 | 맛집 중심 경로 |  | 결정 |
| RECO_TYPE | 추천 경로 유형 | EASY_PARKING | 주차 편의 | 주차가 편한 경로 |  | 결정 |
| RECO_TYPE | 추천 경로 유형 | FAST | 빠른 이동 | 경유 최소 경로 |  | 결정 |
| REVIEW_POLARITY | 후기 성격 | POSITIVE | 긍정 | 추천 근거가 되는 후기 |  | 결정 |
| REVIEW_POLARITY | 후기 성격 | CAUTION | 주의 | 주의점으로 표시하는 후기 |  | 결정 |
| FACILITY | 시설 | NURSING_ROOM | 수유실 | 수유실 | 공공데이터 항목명과 매핑 확인 필요 | 가안 |
| FACILITY | 시설 | SHOWER_ROOM | 샤워실 | 샤워실 |  | 가안 |
| FACILITY | 시설 | ACCESSIBLE_TOILET | 장애인 화장실 | 장애인 화장실 |  | 가안 |
| FACILITY | 시설 | EV_CHARGER | 전기차 충전기 | 전기차 충전기 |  | 가안 |
| FACILITY | 시설 | KIDS_ZONE | 키즈존 | 키즈존 |  | 가안 |
| DATA_SRC | 데이터 출처 | PUBLIC_API | 공공데이터 | 공공데이터 API에서 가져옴 |  | 가안 |
| DATA_SRC | 데이터 출처 | USER | 사용자 입력 | 앱 사용자가 기록함 |  | 가안 |

## 7. 지금까지의 결정 요약 (안건별)

| 안건 | 결정 내용 |
|---|---|
| PK | 시퀀스 대리키, PK 용어 = 단어 + 번호(no). 로그인 아이디와 분리 |
| 1-1 위치 저장 주기 | 거리 기준 + 방향 전환·정차 시점 추가 (경로 모양 정확도) |
| 1-2 위치 보관 | 원본·간소화 경로 분리, 원본 삭제 기능은 추후 (삭제 이유: 개인정보 보관 기간) |
| 1-3 위치 동의 | 첫 여행 시작 시 1회, 설정에서 철회, 공개 동의와 분리 |
| 2 코드 정의 | 코드 = 합의된 유한 목록, 값마다 의미 정의, 관리 절차 필요, 사용자가 만들지 않음. 도메인·코드·마스터 구분 |
| 2-1 코드 구조 | 통합 코드 테이블 + 상태 코드 앱 상수 병행. 마스터성 코드(지역·태그·차량)는 별도 |
| 2-2 코드 목록 | 9개 그룹 확정 + 여행 상태에 중단·취소 추가, 전이 규칙 확정 |
| 테이블 유형 | 마스터·내역·이력·로그·관계. 모든 테이블에 유형 단어(B안) |
| 사용자 차량 | user_vehicle_dtl (연구소 차종 개체와 구분) |
| 길안내 | navi_req_log (로그) |

## 8. 다음 논의 순서

1. M16 trip_place_dtl 이름·경험 통합 확정 → M17 trip_route_dtl → M18~M20 유형 미결 테이블
2. M01 코드값 형식 → M02~M04 코드 값 불일치
3. 3번 삭제 정책(M08) → 4번 로컬 DB·서버(M09) → 5번 익명화(M10)
4. 나머지 확인 항목(M05~M07, M12~M15, M21~M23)
5. 데이터 모델 확정 후 와이어프레임 수정(W01~W07)

## 9. 미결 사항

| ID | 안건 | 내용 | 상태 |
|---|---|---|---|
| M01 | 2-3 | 코드값 형식: 영문 대문자(RECORDING) vs 숫자 | 다음 논의 1순위 |
| M02 | 2-2 | 동행자 명칭 통일: 배우자/부부, 아이/아이 동반 | 논의 필요 |
| M03 | 2-2 | 장소 유형 분류 체계: 계층(대·소분류) 여부, 유형 목록 확정 | 논의 필요 (현재 가안) |
| M04 | 2-2 | 재방문: 태그로 둘지 revisit_yn으로 둘지 | 논의 필요 |
| M05 | 명명 | login_id 확정 (R03) | 확인 필요 |
| M06 | 명명 | 공통 컬럼 created_user_no / updated_user_no 확정 (created_by 대신) | 확인 필요 |
| M07 | 명명 | 코드 테이블만 업무키(cd_grp_cd + cd) PK 예외 허용 여부 | 확인 필요 |
| M08 | 3번 안건 | 삭제 정책: 원본 삭제 시 공유 복제본·추천 데이터, 취소 여행 기록 처리 | 안건 대기 |
| M09 | 4번 안건 | 로컬 DB와 서버 병행 여부 → PK 생성 방식(R02·R08)에 영향 | 안건 대기 |
| M10 | 5번 안건 | 추천 데이터 익명화 범위 (작성자, 출발·도착 근처 위치) | 안건 대기 |
| M12 | ERD | 같은 장소 판별 기준 (좌표 반경 / 장소명 / 외부 POI ID). 외부 API 매칭 키와 연결 | 논의 필요 |
| M13 | ERD | 공유 복제 1회로 단순화 승인 여부 | 확인 필요 |
| M14 | ERD | 경로점 저장 사유(거리·방향 전환·정차)를 코드로 남길지 | 논의 필요 |
| M15 | ERD | 후기 "도움됨" 수(B-09) 저장 여부 (저장 시 단어 등록) | 논의 필요 |
| M16 | ERD | trip_place_dtl 이름과 경험 통합(memo, review_polarity_cd) 확정 | 확인 필요 (Claude 제안 상태) |
| M17 | ERD | trip_route_dtl(1:1) 분리 유지 여부. 분리 시 내역 정의를 "1건 이상"으로 넓힐지, 1:1 전용 유형을 둘지 | 논의 필요 |
| M18 | ERD | route_search_cond_mst, planned_route_mst를 마스터로 둘지 사용자 내역으로 둘지 | 논의 필요 |
| M19 | ERD | route_search_cond_tag_rel을 관계로 둘지 | 논의 필요 |
| M20 | ERD | route_reco_place_dtl을 내역으로 둘지 관계로 둘지 | 논의 필요 |
| M21 | 명명 | PK 규칙에서 이력·로그만 유형 단어 유지 (trip_status_hist_no) 예외 확정 | 확인 필요 |
| M22 | ERD | place_facility_dtl 신규 테이블과 FACILITY·DATA_SRC 코드 확정 | 확인 필요 |
| M23 | ERD | A→B→C 순서 목록 3곳(실제·계획·추천) 통합 여부 | 추후 검토 |

## 10. 와이어프레임 반영 목록

와이어프레임 v5에 1차 반영함 (W01·W02·W04·W05·W07, W06은 M02 확정 후 재확인, W03 일부). v5 이후 피드백: 사용자 지칭 일반화, 계정 전환 화면 제거, 대표 시설 키즈존.

| ID | 내용 |
|---|---|
| W01 | 위치정보 동의 화면 추가 (첫 여행 시작 시, A-04 앞) |
| W02 | 설정 화면 추가 (동의 철회) |
| W03 | B-12 공개 동의 문구에 위치 동의와의 분리 표시 |
| W04 | A-06 장소 유형 목록을 PLACE_TYPE 코드로 교체, "편의시설" 유형 제거 → 장소 시설 정보로 |
| W05 | 와이어프레임 v4의 장소·경험 표기를 trip_place_dtl 기준으로 수정, 계획경로는 planned_route_place_dtl 기준 |
| W06 | 동행자 명칭을 COMPANION_TYPE 확정값으로 통일 (A-04, B-03) |
| W07 | 상태바: 주행 중 화면(A-05)이 "정차"로 표시된 오류 수정 |
