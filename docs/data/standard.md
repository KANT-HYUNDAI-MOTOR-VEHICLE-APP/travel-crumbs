# Travel Crumbs 데이터 표준 정의서 v0.55

작성: 2026-11-11 · 김지수

## 1. 명명 규칙

| 규칙ID | 구분 | 규칙 | 예시 |
|---|---|---|---|
| R01 | 문서 표기 | ERD·정의서의 테이블명·컬럼명은 영문 대문자 SNAKE_CASE | TRIP_STATUS_CD |
| R02 | DB 물리명 | 실제 PostgreSQL의 테이블명·컬럼명은 영문 소문자 snake_case | trip_status_cd |
| R03 | 기본키(PK) | 업무 엔티티는 BIGINT 대리키를 기본으로 사용. 개별 코드 마스터는 코드 의미키, 순수 관계 테이블은 복합키 허용. 자동 증가는 대리키에만 적용 | USER_MST → USER_NO / PLACE_TYPE_MST → PLACE_TYPE_CD |
| R04 | 로그인 아이디 | 로그인용 아이디는 LOGIN_ID 사용. 아이디(ID) 분류어는 LOGIN_ID와 외부 제공 식별자에만 사용 | USER_MST.LOGIN_ID |
| R05 | 외래키(FK) | 외래키 컬럼명은 참조하는 PK 컬럼명을 그대로 사용 | TRIP_MST.USER_NO |
| R06 | 역할 접두어 | 같은 테이블을 두 번 이상 참조하면 역할 단어를 앞에 붙임 | SENDER_USER_NO, RECEIVER_USER_NO, COPY_TRIP_NO |
| R07 | 용어 구성 | 용어는 표준 단어의 조합. 물리명은 단어 물리명을 `_`로 연결. 마지막 단어는 분류어이며 분류어가 도메인을 결정 | 사용자(USER)+번호(NO) = USER_NO |
| R08 | 단어 등록 | 표준 단어 사전에 없는 단어는 먼저 등록 | — |
| R09 | 공통 컬럼 | MST·DTL: 생성·수정·삭제 컬럼 / REL: 생성 컬럼 / HIST·LOG: 생성 컬럼만 사용 | — |
| R10 | 외부 노출 키 | 공유 링크·QR 등 외부 노출 식별자는 `{대상}_UUID` 사용 | TRIP_UUID |
| R11 | 도메인·코드·마스터 구분 | 도메인: 값 형식 규칙 / 코드: 합의된 유한 목록 / 마스터: 속성과 독립 생명주기를 가진 기준정보 | — |
| R12 | 코드 테이블 구조 | 통합 공통 코드는 CD_MST(PK CD_NO · UK CD_GRP_NM + CD). 코드그룹 식별값은 `_CD`로 끝남. 값이 많고 속성이 있는 코드는 개별 코드 마스터 사용. 앱 로직과 연결된 상태 코드는 앱 상수로도 정의 | CD_MST, PLACE_TYPE_MST |
| R13 | 코드값 관리 | 한 번 사용한 코드값의 의미는 변경하지 않음. 사용 중지는 삭제 대신 USE_YN = N | — |
| R14 | 이력 테이블 대상 | 업무 로직에 쓰이는 이력은 전용 이력 테이블, 그 외 값 변경은 DATA_CHANGE_HIST 사용 | TRIP_STATUS_HIST, USER_CONSENT_HIST, DATA_CHANGE_HIST |
| R15 | 위치 데이터 저장 | 거리 기준 저장 + 방향 전환·정차 시점 추가. 일시정지 중 미저장. 원본 경로와 간소화 경로 분리 | TRIP_ROUTE_POINT_DTL, TRIP_ROUTE_DTL |
| R16 | 위치정보 동의 | 첫 여행 시작 시 1회 동의, 설정에서 철회. 공개 동의와 분리 | USER_CONSENT_HIST |

## 2. 테이블 표준

### 2-1. 테이블 규칙

| 규칙ID | 구분 | 규칙 | 예시 |
|---|---|---|---|
| T01 | 테이블 유형 | 마스터·내역·이력·로그·관계 5가지 (아래 표). 코드 테이블도 마스터로 분류 | CD_MST, PLACE_TYPE_MST |
| T02 | 유형 단어 | 모든 테이블명 끝에 유형 단어를 붙임: _mst, _dtl, _hist, _log, _rel | USER_MST |
| T03 | 테이블명 구성 | 내역·이력·관계는 상위 마스터 단어 + 대상 단어 + 유형 단어 | USER_ADDR_DTL, TRIP_PLACE_DTL, TRIP_STATUS_HIST |
| T04 | 마스터 원칙 | 마스터는 개체 하나당 한 행. 개체에 딸린 여러 건은 내역으로, 값이 바뀐 기록은 이력으로 분리 | USER_MST는 자연인 한 사람의 정보만 |
| T05 | 컬럼 순서 | PK → FK → 업무 컬럼 → 공통 컬럼 | — |
| T06 | 로그 테이블 | 추가만 하고 수정·삭제하지 않음. 공통 컬럼은 CREATED_AT, CREATED_USER_NO만 | NAVI_REQ_LOG |

### 2-2. 테이블 유형 정의

| 유형 | 물리명 단어 | 정의 | 판별 질문 | 예시 |
|---|---|---|---|---|
| 마스터 | MST | 업무의 기준이 되는 독립 개체. 개체 하나당 한 행 | 다른 테이블 없이 혼자 의미가 있는가? | USER_MST, TRIP_MST, PLACE_MST |
| 내역 | DTL | 마스터 1건에 딸린, 동시에 유효한 여러 건 | 지금 여러 건이 함께 유효한가? | 홍길동의 집 주소·회사 주소 → USER_ADDR_DTL 2행 |
| 이력 | HIST | 대상 값의 변경 전후 기록 | 과거 값을 시간순으로 남기는가? | 홍길동이 이사해서 바뀐 주소 → USER_ADDR_HIST |
| 로그 | LOG | 발생한 사건의 건별 기록. 추가만 함 | 원본을 바꾸지 않고 사건만 쌓는가? | 길안내 요청·성공·실패 → NAVI_REQ_LOG |
| 관계 | REL | 두 마스터(또는 내역)의 N:M 연결 | 양쪽 PK 연결 외에 내용이 거의 없는가? | 여행장소-태그 → TRIP_PLACE_TAG_REL |

- **내역 vs 이력:** 내역은 같은 시점에 동시에 유효한 여러 건(집 주소와 회사 주소), 이력은 시간에 따라 바뀐 값의 기록(이사 전후 주소).
- **이력 vs 로그:** 이력은 특정 대상의 값이 바뀐 기록(여행 상태 기록 중 → 완료), 로그는 일어난 사건의 건별 기록(길안내 요청, 성공·실패).
- **참고:** 주민등록번호는 법적으로 수집이 엄격히 제한되므로 모델에 넣지 않음.

### 2-3. 추가 반영 테이블

| 논리명 | 물리명 | 유형 | 용도 |
|---|---|---|---|
| 여행평점내역 | TRIP_RATING_DTL | 내역(DTL) | 사용자별 여행 평점 저장 |
| 여행평점태그관계 | TRIP_RATING_TAG_REL | 관계(REL) | 여행 평점과 태그의 N:M 연결 |
| 길안내요청로그 | NAVI_REQ_LOG | 로그(LOG) | 길안내 요청·응답·성공·실패 기록 |

## 3. 표준 단어 사전

논리명(한글)과 물리명(영문 약어)을 1:1로 정의. 분류어 = Y는 용어의 마지막에 와서 도메인을 결정하는 단어.

| 논리명 | 영문명 | 물리명 | 분류어 여부 | 정의 |
|---|---|---|---|---|
| 사용자 | USER | USER | N | 앱에 가입한 자연인 한 사람 |
| 로그인 | LOGIN | LOGIN | N | 계정 인증 행위 |
| 비밀번호 | PASSWORD | PASSWORD | N | 로그인 인증용 비밀 문자열 |
| 닉네임 | NICKNAME | NICKNAME | N | 화면에 표시하는 사용자 이름 |
| 차량 | VEHICLE | VEHICLE | N | 사용자가 보유한 자동차 (연구소 차종 개체와 구분) |
| 여행 | TRIP | TRIP | N | 기록 시작부터 종료까지 한 번의 이동 단위 |
| 동행자 | COMPANION | COMPANION | N | 여행에 함께한 사람·반려동물의 유형 |
| 경로 | ROUTE | ROUTE | N | 이동한 길 또는 계획·추천한 길 |
| 점 | POINT | POINT | N | 경로를 이루는 하나의 좌표 기록 |
| 장소 | PLACE | PLACE | N | 실제 존재하는 고정된 곳 (이름·유형·좌표) |
| 시설 | FACILITY | FACILITY | N | 장소가 갖춘 설비 (예: 수유실) |
| 태그 | TAG | TAG | N | 경험을 분류하는 짧은 키워드 |
| 후기 | REVIEW | REVIEW | N | 장소에서의 경험에 대한 평가 |
| 평점 | RATING | RATING | N | 공개 여행에 사용자가 부여한 별점 |
| 평가 | RATED | RATED | N | 사용자가 평점을 입력한 행위 |
| 성격 | POLARITY | POLARITY | N | 후기의 좋았어요·별로예요 구분 |
| 공유 | SHARE | SHARE | N | 다른 사용자에게 여행을 전달하는 행위 |
| 발신 | SENDER | SENDER | N | 공유를 보낸 쪽 (역할 접두어) |
| 수신 | RECEIVER | RECEIVER | N | 공유를 받은 쪽 (역할 접두어) |
| 복제 | COPY | COPY | N | 원본을 복사해 만든 것 |
| 공개범위 | VISIBILITY | VISIBILITY | N | 기록을 누구에게 보여줄지의 범위 |
| 임시저장 | DRAFT | DRAFT | N | 상세 입력 전 위치만 저장한 상태 |
| 동의 | CONSENT | CONSENT | N | 개인정보 처리에 대한 사용자 동의 |
| 길안내 | NAVIGATION | NAVI | N | 차량 내비게이션 경로 안내 (NaviHelper) |
| 요청 | REQUEST | REQ | N | 시스템에 처리를 요구한 건 |
| 응답 | RESPONSE | RESP | N | 요청 처리 결과를 돌려준 건 |
| 추천 | RECOMMENDATION | RECO | N | 다른 여행 기록을 바탕으로 제안한 경로 |
| 계획 | PLANNED | PLANNED | N | 이동 전에 정한 경로 |
| 검색 | SEARCH | SEARCH | N | 조건으로 기록을 찾는 행위 |
| 조건 | CONDITION | COND | N | 검색·추천에 쓰는 기준 |
| 출발 | DEPARTURE | DEP | N | 경로의 시작 지점 |
| 목적지 | DESTINATION | DEST | N | 경로의 도착 지점 |
| 유사도 | SIMILARITY | SIMILARITY | N | 두 경로가 비슷한 정도 |
| 메모 | MEMO | MEMO | N | 사용자가 자유롭게 쓴 짧은 글 |
| 오류 | ERROR | ERR | N | 요청 또는 처리 과정에서 발생한 문제 |
| 위도 | LATITUDE | LAT | N | 위치의 남북 좌표 |
| 경도 | LONGITUDE | LNG | N | 위치의 동서 좌표 |
| 기록 | RECORD | RECORDED | N | 사용자·앱이 위치·장소를 저장한 행위 (일시와 결합: RECORDED_AT) |
| 시작 | START | START | N | 시작 시점 |
| 종료 | END | END | N | 종료 시점 |
| 거리 | DISTANCE | DIST | N | 이동 거리 (미터) |
| 소요시간 | DURATION | DURATION | N | 걸린 시간 (초) |
| 데이터 | DATA | DATA | N | 저장되는 값의 묶음 |
| 출처 | SOURCE | SRC | N | 데이터를 가져온 곳 |
| 설치 | INSTALL | INSTALL | N | 시설이 갖춰져 있음 |
| 확인 | CONFIRM | CONFIRM | N | 정보가 맞는지 확인한 행위 |
| 상태 | STATUS | STATUS | N | 대상이 현재 놓인 단계 |
| 유형 | TYPE | TYPE | N | 대상을 나누는 종류 |
| 그룹 | GROUP | GRP | N | 같은 종류를 묶는 단위 |
| 생성 | CREATED | CREATED | N | 데이터를 처음 만든 행위 |
| 수정 | UPDATED | UPDATED | N | 데이터를 마지막으로 바꾼 행위 |
| 삭제 | DELETED | DELETED | N | 논리 삭제 처리 |
| 사용 | USE | USE | N | 사용 가능 여부 |
| 정렬 | SORT | SORT | N | 화면 표시 순서 |
| 마스터 | MASTER | MST | N | 테이블 유형 단어 (테이블명에만 사용) |
| 내역 | DETAIL | DTL | N | 테이블 유형 단어 (테이블명에만 사용) |
| 이력 | HISTORY | HIST | N | 테이블 유형 단어 / 변경 기록 |
| 로그 | LOG | LOG | N | 테이블 유형 단어 / 사건 기록 |
| 관계 | RELATION | REL | N | 테이블 유형 단어 (테이블명에만 사용) |
| 주소 | ADDRESS | ADDR | Y | 장소의 도로명·지번 주소 원문 |
| 점수 | SCORE | SCORE | Y | 평가 결과의 정수 점수 |
| 메시지 | MESSAGE | MSG | Y | 시스템이 기록하는 설명 문자열 |
| 번호 | NUMBER | NO | Y | 내부 식별번호·참조번호 |
| 아이디 | ID | ID | Y | 사용자 입력 또는 외부 제공 식별 문자열 |
| UUID | UUID | UUID | Y | 외부 노출·동기화용 범용 고유 식별자 |
| 코드 | CODE | CD | Y | 표준 코드 값 |
| 명 | NAME | NM | Y | 이름 |
| 여부 | YN | YN | Y | Y/N 값 |
| 일시 | DATETIME | AT | Y | 날짜와 시각 |
| 개수 | COUNT | CNT | Y | 건수 |
| 순서 | ORDER | ORD | Y | 정렬·경로 내 순서 |
| 해시 | HASH | HASH | Y | 단방향 암호화 값 |
| POI | POI | POI | N | 내비게이션 지도 제공자가 관리하는 지점(Point of Interest) |
| 제공자 | PROVIDER | PROVIDER | N | 데이터를 제공하는 회사·서비스 |
| 카테고리 | CATEGORY | CATEGORY | N | 유형을 묶는 대분류 (약어 없이) |
| 변경 | CHANGE | CHANGE | N | 값이 바뀐 사건 (약어 없이) |
| 테이블 | TABLE | TABLE | N | DB 테이블 |
| 컬럼 | COLUMN | COLUMN | N | DB 컬럼 |
| 행 | ROW | ROW | N | 테이블의 한 행 |
| 이전 | BEFORE | BEFORE | N | 바뀌기 전 |
| 이후 | AFTER | AFTER | N | 바뀐 뒤 |
| 값 | VALUE | VALUE | Y | 형식이 정해지지 않은 저장 값(문자열로 보관) |

## 4. 표준 도메인 사전

| 도메인명 | 분류어 물리명 | 데이터 타입 | 길이 | 소수점 | 허용값·형식 | 용도 |
|---|---|---|---|---|---|---|
| 번호 | NO | BIGINT | — | — | 1 이상 정수 | 내부 식별번호·참조번호(PK·FK) |
| UUID | UUID | CHAR | 36 | — | UUID v4 문자열 | 외부 노출·동기화 키 |
| 아이디 | ID | VARCHAR | 50 | — | 영문·숫자 조합 | 로그인 아이디 |
| 해시 | HASH | VARCHAR | 255 | — | 해시값만 저장 (평문 금지) | 비밀번호 해시 |
| 코드 | CD | VARCHAR | 30 | — | CD_MST의 코드값 | 상태·유형 코드 |
| 명 | NM | VARCHAR | 100 | — | — | 이름 |
| 메모 | MEMO | VARCHAR | 500 | — | — | 짧은 자유 입력 |
| 주소 | ADDR | VARCHAR | 300 | — | 주소 원문 | 장소 주소 및 기록 시점 주소 스냅샷 |
| 점수 | SCORE | INTEGER | — | — | 1 ~ 5 | 사용자 별점 |
| 메시지 | MSG | VARCHAR | 500 | — | — | 오류·처리 결과 설명 |
| 여부 | YN | CHAR | 1 | — | Y / N | 여부 값 (코드 테이블 사용 안 함) |
| 일시 | AT | TIMESTAMP WITH TIME ZONE | — | — | UTC 저장, 화면에서 현지 시각 변환 | 모든 일시 |
| 개수 | CNT | INTEGER | — | — | 0 이상 | 건수 (반정규화 집계값 포함) |
| 순서 | ORD | INTEGER | — | — | 1 이상 | 정렬·경로 내 순서 |
| 위도 | LAT | DECIMAL | 10 | 7 | -90 ~ 90 | 위치 좌표 |
| 경도 | LNG | DECIMAL | 10 | 7 | -180 ~ 180 | 위치 좌표 |
| 거리 | DIST | INTEGER | — | — | 미터 단위, 0 이상 | 이동 거리 |
| 소요시간 | DURATION | INTEGER | — | — | 초 단위, 0 이상 | 소요 시간 |
| 유사도 | SIMILARITY | DECIMAL | 5 | 2 | 0 ~ 100 (%) | 경로 유사도 |
| 경로데이터 | DATA | TEXT | — | — | Google 인코딩 폴리라인(정밀도 1e-5) · 더글러스-포이커 10m 간소화 | ROUTE_DATA 전용 |
| 값 | VALUE | VARCHAR | 500 | — | 문자열로 변환한 값 | DATA_CHANGE_HIST.BEFORE_VALUE · AFTER_VALUE |

## 5. 표준 용어 사전

용어(컬럼)는 표준 단어의 조합(R06). 사용 테이블은 데이터 모델 기준.

| 논리명 | 물리명 | 구성 단어 | 도메인 | 사용 테이블 |
|---|---|---|---|---|
| 코드 | CD | 코드(CD) | 코드 | CD_MST |
| 코드번호 | CD_NO | 코드(CD) + 번호(NO) | 번호 | CD_MST |
| 코드그룹명 | CD_GRP_NM | 코드(CD) + 그룹(GRP) + 명(NM) | 명 | CD_MST |
| 코드명 | CD_NM | 코드(CD) + 명(NM) | 명 | CD_MST |
| 동행자유형코드 | COMPANION_TYPE_CD | 동행자(COMPANION) + 유형(TYPE) + 코드(CD) | 코드 | ROUTE_SEARCH_COND_MST, TRIP_COMPANION_DTL |
| 확인일시 | CONFIRM_AT | 확인(CONFIRM) + 일시(AT) | 일시 | PLACE_FACILITY_DTL |
| 동의유형코드 | CONSENT_TYPE_CD | 동의(CONSENT) + 유형(TYPE) + 코드(CD) | 코드 | USER_CONSENT_HIST |
| 동의여부 | CONSENT_YN | 동의(CONSENT) + 여부(YN) | 여부 | USER_CONSENT_HIST |
| 복제여행번호 | COPY_TRIP_NO | 복제(COPY) + 여행(TRIP) + 번호(NO) | 번호 | TRIP_SHARE_DTL |
| 생성일시 | CREATED_AT | 생성(CREATED) + 일시(AT) | 일시 | CD_MST, NAVI_REQ_LOG, PLACE_FACILITY_DTL, PLACE_MST, PLANNED_ROUTE_MST, PLANNED_ROUTE_PLACE_DTL, ROUTE_RECO_DTL, ROUTE_RECO_PLACE_DTL, ROUTE_SEARCH_COND_MST, ROUTE_SEARCH_COND_TAG_REL, TAG_MST, TRIP_COMPANION_DTL, TRIP_MST, TRIP_PLACE_DTL, TRIP_PLACE_TAG_REL, TRIP_RATING_DTL, TRIP_RATING_TAG_REL, TRIP_ROUTE_DTL, TRIP_ROUTE_POINT_DTL, TRIP_SHARE_DTL, TRIP_STATUS_HIST, USER_CONSENT_HIST, USER_MST, USER_VEHICLE_DTL |
| 생성사용자번호 | CREATED_USER_NO | 생성(CREATED) + 사용자(USER) + 번호(NO) | 번호 | CD_MST, NAVI_REQ_LOG, PLACE_FACILITY_DTL, PLACE_MST, PLANNED_ROUTE_MST, PLANNED_ROUTE_PLACE_DTL, ROUTE_RECO_DTL, ROUTE_RECO_PLACE_DTL, ROUTE_SEARCH_COND_MST, ROUTE_SEARCH_COND_TAG_REL, TAG_MST, TRIP_COMPANION_DTL, TRIP_MST, TRIP_PLACE_DTL, TRIP_PLACE_TAG_REL, TRIP_RATING_DTL, TRIP_RATING_TAG_REL, TRIP_ROUTE_DTL, TRIP_ROUTE_POINT_DTL, TRIP_SHARE_DTL, TRIP_STATUS_HIST, USER_CONSENT_HIST, USER_MST, USER_VEHICLE_DTL |
| 데이터출처코드 | DATA_SRC_CD | 데이터(DATA) + 출처(SRC) + 코드(CD) | 코드 | PLACE_FACILITY_DTL, PLACE_MST |
| 주소 | ADDR | 주소(ADDR) | 주소 | PLACE_MST, TRIP_PLACE_DTL |
| 삭제여부 | DELETED_YN | 삭제(DELETED) + 여부(YN) | 여부 | CD_MST, PLACE_FACILITY_DTL, PLACE_MST, PLANNED_ROUTE_MST, PLANNED_ROUTE_PLACE_DTL, ROUTE_RECO_DTL, ROUTE_RECO_PLACE_DTL, ROUTE_SEARCH_COND_MST, TAG_MST, TRIP_COMPANION_DTL, TRIP_MST, TRIP_PLACE_DTL, TRIP_RATING_DTL, TRIP_ROUTE_DTL, TRIP_ROUTE_POINT_DTL, TRIP_SHARE_DTL, USER_MST, USER_VEHICLE_DTL |
| 출발위도 | DEP_LAT | 출발(DEP) + 위도(LAT) | 위도 | ROUTE_SEARCH_COND_MST |
| 출발경도 | DEP_LNG | 출발(DEP) + 경도(LNG) | 경도 | ROUTE_SEARCH_COND_MST |
| 목적지위도 | DEST_LAT | 목적지(DEST) + 위도(LAT) | 위도 | NAVI_REQ_LOG, ROUTE_SEARCH_COND_MST |
| 목적지경도 | DEST_LNG | 목적지(DEST) + 경도(LNG) | 경도 | NAVI_REQ_LOG, ROUTE_SEARCH_COND_MST |
| 거리 | DIST | 거리(DIST) | 거리 | TRIP_MST |
| 임시저장여부 | DRAFT_YN | 임시저장(DRAFT) + 여부(YN) | 여부 | TRIP_PLACE_DTL |
| 소요시간 | DURATION | 소요시간(DURATION) | 소요시간 | ROUTE_RECO_DTL, TRIP_MST |
| 종료일시 | END_AT | 종료(END) + 일시(AT) | 일시 | TRIP_MST |
| 시설코드 | FACILITY_CD | 시설(FACILITY) + 코드(CD) | 코드 | PLACE_FACILITY_DTL |
| 설치여부 | INSTALL_YN | 설치(INSTALL) + 여부(YN) | 여부 | PLACE_FACILITY_DTL |
| 위도 | LAT | 위도(LAT) | 위도 | PLACE_MST, TRIP_PLACE_DTL, TRIP_ROUTE_POINT_DTL |
| 경도 | LNG | 경도(LNG) | 경도 | PLACE_MST, TRIP_PLACE_DTL, TRIP_ROUTE_POINT_DTL |
| 로그인아이디 | LOGIN_ID | 로그인(LOGIN) + 아이디(ID) | 아이디 | USER_MST |
| 메모 | MEMO | 메모(MEMO) | 메모 | TRIP_PLACE_DTL |
| 길안내요청로그번호 | NAVI_REQ_LOG_NO | 길안내(NAVI) + 요청(REQ) + 로그(LOG) + 번호(NO) | 번호 | NAVI_REQ_LOG |
| 길안내요청상태코드 | NAVI_REQ_STATUS_CD | 길안내(NAVI) + 요청(REQ) + 상태(STATUS) + 코드(CD) | 코드 | NAVI_REQ_LOG |
| 오류메시지 | ERR_MSG | 오류(ERR) + 메시지(MSG) | 메시지 | NAVI_REQ_LOG |
| 요청일시 | REQ_AT | 요청(REQ) + 일시(AT) | 일시 | NAVI_REQ_LOG |
| 응답일시 | RESP_AT | 응답(RESP) + 일시(AT) | 일시 | NAVI_REQ_LOG |
| 닉네임명 | NICKNAME_NM | 닉네임(NICKNAME) + 명(NM) | 명 | USER_MST |
| 비밀번호해시 | PASSWORD_HASH | 비밀번호(PASSWORD) + 해시(HASH) | 해시 | USER_MST |
| 장소개수 | PLACE_CNT | 장소(PLACE) + 개수(CNT) | 개수 | TRIP_MST |
| 장소시설번호 | PLACE_FACILITY_NO | 장소(PLACE) + 시설(FACILITY) + 번호(NO) | 번호 | PLACE_FACILITY_DTL |
| 장소명 | PLACE_NM | 장소(PLACE) + 명(NM) | 명 | PLACE_MST |
| 장소번호 | PLACE_NO | 장소(PLACE) + 번호(NO) | 번호 | NAVI_REQ_LOG, PLACE_FACILITY_DTL, PLACE_MST, PLANNED_ROUTE_PLACE_DTL, ROUTE_RECO_PLACE_DTL, TRIP_PLACE_DTL |
| 장소순서 | PLACE_ORD | 장소(PLACE) + 순서(ORD) | 순서 | PLANNED_ROUTE_PLACE_DTL, ROUTE_RECO_PLACE_DTL |
| 장소유형코드 | PLACE_TYPE_CD | 장소(PLACE) + 유형(TYPE) + 코드(CD) | 코드 | PLACE_MST, PLACE_TYPE_MST, TRIP_PLACE_DTL |
| 계획경로번호 | PLANNED_ROUTE_NO | 계획(PLANNED) + 경로(ROUTE) + 번호(NO) | 번호 | NAVI_REQ_LOG, PLANNED_ROUTE_MST, PLANNED_ROUTE_PLACE_DTL |
| 계획경로장소번호 | PLANNED_ROUTE_PLACE_NO | 계획(PLANNED) + 경로(ROUTE) + 장소(PLACE) + 번호(NO) | 번호 | PLANNED_ROUTE_PLACE_DTL |
| 수신사용자번호 | RECEIVER_USER_NO | 수신(RECEIVER) + 사용자(USER) + 번호(NO) | 번호 | TRIP_SHARE_DTL |
| 추천유형코드 | RECO_TYPE_CD | 추천(RECO) + 유형(TYPE) + 코드(CD) | 코드 | ROUTE_RECO_DTL |
| 기록일시 | RECORDED_AT | 기록(RECORDED) + 일시(AT) | 일시 | TRIP_PLACE_DTL, TRIP_ROUTE_POINT_DTL |
| 후기성격코드 | REVIEW_POLARITY_CD | 후기(REVIEW) + 성격(POLARITY) + 코드(CD) | 코드 | TRIP_PLACE_DTL |
| 평점점수 | RATING_SCORE | 평점(RATING) + 점수(SCORE) | 점수 | TRIP_RATING_DTL |
| 평가일시 | RATED_AT | 평가(RATED) + 일시(AT) | 일시 | TRIP_RATING_DTL |
| 경로데이터 | ROUTE_DATA | 경로(ROUTE) + 데이터(DATA) | 경로데이터 | TRIP_ROUTE_DTL |
| 경로추천번호 | ROUTE_RECO_NO | 경로(ROUTE) + 추천(RECO) + 번호(NO) | 번호 | PLANNED_ROUTE_MST, ROUTE_RECO_DTL, ROUTE_RECO_PLACE_DTL |
| 경로추천장소번호 | ROUTE_RECO_PLACE_NO | 경로(ROUTE) + 추천(RECO) + 장소(PLACE) + 번호(NO) | 번호 | ROUTE_RECO_PLACE_DTL |
| 경로검색조건번호 | ROUTE_SEARCH_COND_NO | 경로(ROUTE) + 검색(SEARCH) + 조건(COND) + 번호(NO) | 번호 | ROUTE_RECO_DTL, ROUTE_SEARCH_COND_MST, ROUTE_SEARCH_COND_TAG_REL |
| 경로검색조건태그번호 | ROUTE_SEARCH_COND_TAG_NO | 경로(ROUTE) + 검색(SEARCH) + 조건(COND) + 태그(TAG) + 번호(NO) | 번호 | ROUTE_SEARCH_COND_TAG_REL |
| 발신사용자번호 | SENDER_USER_NO | 발신(SENDER) + 사용자(USER) + 번호(NO) | 번호 | TRIP_SHARE_DTL |
| 공유상태코드 | SHARE_STATUS_CD | 공유(SHARE) + 상태(STATUS) + 코드(CD) | 코드 | TRIP_SHARE_DTL |
| 유사도 | SIMILARITY | 유사도(SIMILARITY) | 유사도 | ROUTE_RECO_DTL |
| 정렬순서 | SORT_ORD | 정렬(SORT) + 순서(ORD) | 순서 | CD_MST, PLACE_TYPE_MST |
| 시작일시 | START_AT | 시작(START) + 일시(AT) | 일시 | TRIP_MST |
| 태그명 | TAG_NM | 태그(TAG) + 명(NM) | 명 | TAG_MST |
| 태그번호 | TAG_NO | 태그(TAG) + 번호(NO) | 번호 | ROUTE_SEARCH_COND_TAG_REL, TAG_MST, TRIP_PLACE_TAG_REL, TRIP_RATING_TAG_REL |
| 여행개수 | TRIP_CNT | 여행(TRIP) + 개수(CNT) | 개수 | ROUTE_RECO_DTL |
| 여행동행자번호 | TRIP_COMPANION_NO | 여행(TRIP) + 동행자(COMPANION) + 번호(NO) | 번호 | TRIP_COMPANION_DTL |
| 여행명 | TRIP_NM | 여행(TRIP) + 명(NM) | 명 | TRIP_MST |
| 여행번호 | TRIP_NO | 여행(TRIP) + 번호(NO) | 번호 | PLANNED_ROUTE_MST, TRIP_COMPANION_DTL, TRIP_MST, TRIP_PLACE_DTL, TRIP_RATING_DTL, TRIP_ROUTE_DTL, TRIP_ROUTE_POINT_DTL, TRIP_SHARE_DTL, TRIP_STATUS_HIST |
| 여행평점번호 | TRIP_RATING_NO | 여행(TRIP) + 평점(RATING) + 번호(NO) | 번호 | TRIP_RATING_DTL, TRIP_RATING_TAG_REL |
| 여행평점태그번호 | TRIP_RATING_TAG_NO | 여행(TRIP) + 평점(RATING) + 태그(TAG) + 번호(NO) | 번호 | TRIP_RATING_TAG_REL |
| 여행장소번호 | TRIP_PLACE_NO | 여행(TRIP) + 장소(PLACE) + 번호(NO) | 번호 | TRIP_PLACE_DTL, TRIP_PLACE_TAG_REL |
| 여행장소태그번호 | TRIP_PLACE_TAG_NO | 여행(TRIP) + 장소(PLACE) + 태그(TAG) + 번호(NO) | 번호 | TRIP_PLACE_TAG_REL |
| 여행경로번호 | TRIP_ROUTE_NO | 여행(TRIP) + 경로(ROUTE) + 번호(NO) | 번호 | TRIP_ROUTE_DTL |
| 여행경로점번호 | TRIP_ROUTE_POINT_NO | 여행(TRIP) + 경로(ROUTE) + 점(POINT) + 번호(NO) | 번호 | TRIP_ROUTE_POINT_DTL |
| 여행공유번호 | TRIP_SHARE_NO | 여행(TRIP) + 공유(SHARE) + 번호(NO) | 번호 | TRIP_SHARE_DTL |
| 여행상태코드 | TRIP_STATUS_CD | 여행(TRIP) + 상태(STATUS) + 코드(CD) | 코드 | TRIP_MST, TRIP_STATUS_HIST |
| 여행상태이력번호 | TRIP_STATUS_HIST_NO | 여행(TRIP) + 상태(STATUS) + 이력(HIST) + 번호(NO) | 번호 | TRIP_STATUS_HIST |
| 여행UUID | TRIP_UUID | 여행(TRIP) + UUID(UUID) | UUID | TRIP_MST |
| 수정일시 | UPDATED_AT | 수정(UPDATED) + 일시(AT) | 일시 | CD_MST, PLACE_FACILITY_DTL, PLACE_MST, PLANNED_ROUTE_MST, PLANNED_ROUTE_PLACE_DTL, ROUTE_RECO_DTL, ROUTE_RECO_PLACE_DTL, ROUTE_SEARCH_COND_MST, TAG_MST, TRIP_COMPANION_DTL, TRIP_MST, TRIP_PLACE_DTL, TRIP_RATING_DTL, TRIP_ROUTE_DTL, TRIP_ROUTE_POINT_DTL, TRIP_SHARE_DTL, USER_MST, USER_VEHICLE_DTL |
| 수정사용자번호 | UPDATED_USER_NO | 수정(UPDATED) + 사용자(USER) + 번호(NO) | 번호 | CD_MST, PLACE_FACILITY_DTL, PLACE_MST, PLANNED_ROUTE_MST, PLANNED_ROUTE_PLACE_DTL, ROUTE_RECO_DTL, ROUTE_RECO_PLACE_DTL, ROUTE_SEARCH_COND_MST, TAG_MST, TRIP_COMPANION_DTL, TRIP_MST, TRIP_PLACE_DTL, TRIP_RATING_DTL, TRIP_ROUTE_DTL, TRIP_ROUTE_POINT_DTL, TRIP_SHARE_DTL, USER_MST, USER_VEHICLE_DTL |
| 사용여부 | USE_YN | 사용(USE) + 여부(YN) | 여부 | CD_MST, PLACE_TYPE_MST, TAG_MST |
| 사용자동의이력번호 | USER_CONSENT_HIST_NO | 사용자(USER) + 동의(CONSENT) + 이력(HIST) + 번호(NO) | 번호 | USER_CONSENT_HIST |
| 사용자번호 | USER_NO | 사용자(USER) + 번호(NO) | 번호 | NAVI_REQ_LOG, PLANNED_ROUTE_MST, ROUTE_SEARCH_COND_MST, TRIP_MST, TRIP_RATING_DTL, USER_CONSENT_HIST, USER_MST, USER_VEHICLE_DTL |
| 사용자차량번호 | USER_VEHICLE_NO | 사용자(USER) + 차량(VEHICLE) + 번호(NO) | 번호 | USER_VEHICLE_DTL |
| 차량명 | VEHICLE_NM | 차량(VEHICLE) + 명(NM) | 명 | USER_VEHICLE_DTL |
| 공개범위코드 | VISIBILITY_CD | 공개범위(VISIBILITY) + 코드(CD) | 코드 | TRIP_MST |
| 사용자장소명 | USER_PLACE_NM | 사용자(USER) + 장소(PLACE) + 명(NM) | 명 | TRIP_PLACE_DTL |
| POI번호 | POI_ID | POI(POI) + 아이디(ID) | 아이디 | PLACE_MST (외부 제공자의 식별 문자열 · VARCHAR(50)) |
| POI제공자 | POI_PROVIDER | POI(POI) + 제공자(PROVIDER) | 명 | PLACE_MST (VARCHAR(30)) |
| 장소유형명 | PLACE_TYPE_NM | 장소(PLACE) + 유형(TYPE) + 명(NM) | 명 | PLACE_TYPE_MST |
| 장소카테고리코드 | PLACE_CATEGORY_CD | 장소(PLACE) + 카테고리(CATEGORY) + 코드(CD) | 코드 | PLACE_TYPE_MST |
| 장소카테고리명 | PLACE_CATEGORY_NM | 장소(PLACE) + 카테고리(CATEGORY) + 명(NM) | 명 | PLACE_TYPE_MST |
| 데이터변경이력번호 | DATA_CHANGE_HIST_NO | 데이터(DATA) + 변경(CHANGE) + 이력(HIST) + 번호(NO) | 번호 | DATA_CHANGE_HIST |
| 테이블명 | TABLE_NM | 테이블(TABLE) + 명(NM) | 명 | DATA_CHANGE_HIST |
| 컬럼명 | COLUMN_NM | 컬럼(COLUMN) + 명(NM) | 명 | DATA_CHANGE_HIST |
| 행번호 | ROW_NO | 행(ROW) + 번호(NO) | 번호 | DATA_CHANGE_HIST (바뀐 행의 PK 값) |
| 이전값 | BEFORE_VALUE | 이전(BEFORE) + 값(VALUE) | 값 | DATA_CHANGE_HIST |
| 이후값 | AFTER_VALUE | 이후(AFTER) + 값(VALUE) | 값 | DATA_CHANGE_HIST |

## 6. 표준 코드 정의서

### 6-1. 통합 공통 코드 (CD_MST)

코드값은 영어 대문자, 화면 글자는 코드명(CD_NM). 한 번 쓴 코드값은 바꾸지 않고, 뜻이 바뀌면 새 코드를 추가하고 기존 코드는 USE_YN = N(R13). 그룹 한글명은 DB에 두지 않고 이 표에만 둠.

| 코드그룹명(CD_GRP_NM) | 그룹 한글명 | 코드(CD) | 코드명(CD_NM) | 비고 |
|---|---|---|---|---|
| COMPANION_TYPE_CD | 동행자 유형 | SOLO | 혼자 | 다른 동행자와 함께 고를 수 없음 |
| COMPANION_TYPE_CD | 동행자 유형 | SPOUSE | 배우자 |  |
| COMPANION_TYPE_CD | 동행자 유형 | CHILD | 아이 |  |
| COMPANION_TYPE_CD | 동행자 유형 | PARENT | 부모님 |  |
| COMPANION_TYPE_CD | 동행자 유형 | FRIEND | 친구 |  |
| COMPANION_TYPE_CD | 동행자 유형 | PET | 반려동물 |  |
| CONSENT_TYPE_CD | 동의 유형 | LOCATION_COLLECT | 위치 수집 |  |
| CONSENT_TYPE_CD | 동의 유형 | RECO_PUBLIC | 추천 공개 |  |
| DATA_SRC_CD | 데이터 출처 | PUBLIC_API | 공공데이터 |  |
| DATA_SRC_CD | 데이터 출처 | USER | 사용자 입력 |  |
| DATA_SRC_CD | 데이터 출처 | NAVI | 내비 POI | POI_PROVIDER + POI_ID 로 같은 장소 판단 |
| REVIEW_POLARITY_CD | 후기 성격 | POSITIVE | 좋았어요 |  |
| REVIEW_POLARITY_CD | 후기 성격 | NEGATIVE | 별로예요 |  |
| TRIP_STATUS_CD | 여행 상태 | RECORDING | 기록 중 | → PAUSED / COMPLETED / INTERRUPTED / CANCELED |
| TRIP_STATUS_CD | 여행 상태 | PAUSED | 일시정지 | → RECORDING / COMPLETED / INTERRUPTED / CANCELED · 경로점 저장 안 함 · 장소 기록 가능 |
| TRIP_STATUS_CD | 여행 상태 | COMPLETED | 완료 | 종료 상태 |
| TRIP_STATUS_CD | 여행 상태 | INTERRUPTED | 중단 | → RECORDING(재개) / COMPLETED / CANCELED |
| TRIP_STATUS_CD | 여행 상태 | CANCELED | 취소 | 종료 상태 |
| VISIBILITY_CD | 공개 범위 | PRIVATE | 나만 보기 | 새 여행 기본값 |
| VISIBILITY_CD | 공개 범위 | ANONYMOUS_PUBLIC | 익명 공개 |  |
| VISIBILITY_CD | 공개 범위 | DIRECT_SHARE | 직접 공유만 |  |
| SHARE_STATUS_CD | 공유 상태 | NEW | 새 공유 | 공유되었으나 수신자가 아직 수락하지 않음 · → ACCEPTED |
| SHARE_STATUS_CD | 공유 상태 | ACCEPTED | 수락 | 수신자가 수락해 복제 여행 생성 · 종료 상태 |
| NAVI_REQ_STATUS_CD | 길안내 요청 상태 | REQUESTING | 요청 중 | NaviHelper 호출 후 결과 대기 · → SUCCESS / FAILED |
| NAVI_REQ_STATUS_CD | 길안내 요청 상태 | SUCCESS | 성공 | 차량 길안내 시작됨 |
| NAVI_REQ_STATUS_CD | 길안내 요청 상태 | FAILED | 실패 | 호출 실패 (더미 경로 폴백) |
| RECO_TYPE_CD | 추천 경로 유형 | CHILD_FRIENDLY | 아이 동반 | 아이 편의시설 중심 경로 · 커지면 마스터 전환 검토 |
| RECO_TYPE_CD | 추천 경로 유형 | FOOD | 맛집 | 맛집 중심 경로 |
| RECO_TYPE_CD | 추천 경로 유형 | EASY_PARKING | 주차 편의 | 주차가 편한 경로 |
| RECO_TYPE_CD | 추천 경로 유형 | FAST | 빠른 이동 | 경유 최소 경로 |

### 6-2. 장소유형코드 (PLACE_TYPE_MST · 장소유형마스터)

| 장소카테고리코드(PLACE_CATEGORY_CD) | 장소카테고리명(PLACE_CATEGORY_NM) | 장소유형코드(PLACE_TYPE_CD) | 장소유형명(PLACE_TYPE_NM) |
|---|---|---|---|
| MOBILITY | 이동·차량 | REST_AREA | 휴게소 |
| MOBILITY | 이동·차량 | DROWSY_SHELTER | 졸음쉼터 |
| MOBILITY | 이동·차량 | GAS_STATION | 주유소 |
| MOBILITY | 이동·차량 | EV_STATION | 전기차 충전소 |
| MOBILITY | 이동·차량 | PARKING | 주차장 |
| FOOD | 음식 | RESTAURANT | 음식점 |
| FOOD | 음식 | CAFE | 카페 |
| NATURE | 자연 | BEACH | 바다·해변 |
| NATURE | 자연 | MOUNTAIN | 산 |
| NATURE | 자연 | VALLEY | 계곡 |
| NATURE | 자연 | LAKE_RIVER | 호수·강 |
| NATURE | 자연 | ISLAND | 섬 |
| NATURE | 자연 | PARK | 공원·수목원 |
| SIGHTSEEING | 관광 | ATTRACTION | 명소·랜드마크 |
| SIGHTSEEING | 관광 | VIEWPOINT | 전망대 |
| CULTURE | 문화 | MUSEUM | 박물관 |
| CULTURE | 문화 | GALLERY | 미술관·전시 |
| SHOPPING | 쇼핑 | MARKET | 전통시장 |
| SHOPPING | 쇼핑 | MALL | 쇼핑몰·아웃렛 |
| LEISURE | 체험·레저 | EXPERIENCE | 체험시설 |
| LEISURE | 체험·레저 | THEME_PARK | 테마파크 |
| LEISURE | 체험·레저 | CAMPING | 캠핑장 |
| STAY | 숙박 | LODGING | 숙소 |
| ETC | 기타 | ETC_PLACE | 기타 |

### 6-3. 경험 태그 (TAG_MST · 태그마스터)

화면 노출 개수·우선순위·접기 방식은 UX 설계에서 확정.

| TAG_NO | 태그명(TAG_NM) |
|---|---|
| 1 | 주차 편해요 |
| 2 | 아이와 가기 좋아요 |
| 3 | 화장실 깨끗해요 |
| 4 | 다시 가고 싶어요 |
| 5 | 웨이팅 있어요 |
| 6 | 주차 어려워요 |
| 7 | 유모차 이동 편해요 |
| 8 | 수유실 있어요 |
| 9 | 기저귀 교환대 있어요 |
| 10 | 쉬어가기 좋아요 |
| 11 | 붐벼요 |
| 12 | 아이 메뉴 있어요 |
| 13 | 음식이 맛있어요 |

## 7. 저장 제외 값

| 구분 | 값 | 처리 기준 |
|---|---|---|
| 계산값 | 여행 평점 평균 | `TRIP_RATING_DTL.RATING_SCORE`의 `AVG` 조회 |
| 계산값 | 여행 평점 개수 | `TRIP_RATING_DTL`의 `COUNT` 조회 |
| 앱 상태 | `uploadState` | 앱 메모리·UI 상태로 관리 |
| 앱 상태 | `utcOffsetSeconds` | 표시 시점 계산값으로 관리 |
| 앱 상태 | `PendingArrival` | 앱 메모리·UI 상태로 관리 |
| 앱 상태 | `AutoPlaceName` | 장소명 생성 과정의 임시값으로 관리 |
| 앱 상태 | `saved` | 저장 처리 결과 상태로 관리 |
