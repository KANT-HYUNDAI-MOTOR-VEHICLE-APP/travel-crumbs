# Travel Crumbs 데이터 표준 정의서 v0.2

작성일: 2026-11-06

## 1. 명명 규칙

| 규칙ID | 구분 | 규칙 | 예시 |
|---|---|---|---|
| R01 | 문서 표기 | ERD·정의서의 테이블명·컬럼명은 영문 대문자 snake_case 사용 | `TRIP_STATUS_CD` |
| R02 | DB 물리명 | 실제 DB의 테이블명·컬럼명은 영문 소문자 snake_case 사용 | `trip_status_cd` |
| R03 | 기본키 | 기본키는 엔티티 단어와 번호를 결합하고 BIGINT 대리키 사용 | `USER_NO`, `TRIP_NO` |
| R04 | 로그인 식별자 | 로그인용 식별자는 `LOGIN_ID` 사용 | `USER_MST.LOGIN_ID` |
| R05 | 외래키 | 외래키는 참조하는 기본키의 컬럼명 유지 | `TRIP_MST.USER_NO` |
| R06 | 역할 접두어 | 같은 테이블을 여러 역할로 참조할 경우 역할명 접두어 사용 | `SENDER_USER_NO`, `RECEIVER_USER_NO` |
| R07 | 외부 식별자 | 외부 공유·동기화 식별자는 연속 번호 대신 UUID 사용 | `TRIP_UUID` |
| R08 | 코드 | 합의된 유한 목록은 코드그룹과 코드내역으로 관리 | `TRIP_STATUS_CD` |
| R09 | 여부 | Y/N 값은 CHAR(1) 컬럼으로 관리 | `DELETED_YN`, `DRAFT_YN` |
| R10 | 일시 | 모든 일시는 UTC로 저장하고 화면에서 현지 시각으로 변환 | `START_AT`, `RECORDED_AT` |
| R11 | 이력 | 업무 객체의 상태·값 변화는 이력 테이블(HIST)에 추가 저장 | `TRIP_STATUS_HIST` |
| R12 | 로그 | 시스템 요청·응답·오류는 로그 테이블(LOG)에 추가 저장 | `NAVI_REQ_LOG` |
| R13 | 상세 | 상위 객체에 속한 상세 속성과 순서는 상세 테이블(DTL)에 저장 | `TRIP_PLACE_DTL` |
| R14 | 관계 | 객체 간 다대다 연결은 관계 테이블(REL)에 저장 | `TRIP_PLACE_TAG_REL` |

## 2. 테이블 유형

| 유형 | 논리명 | 정의 | 수정·삭제 기준 |
|---|---|---|---|
| MST | 기준·주요 객체 | 독립적으로 식별하고 여러 업무에서 참조하는 데이터 | 수정 가능, 논리 삭제 적용 |
| DTL | 상세 내역 | 상위 객체에 속한 속성·순서·내용 데이터 | 수정 가능, 업무 특성에 따라 논리 삭제 적용 |
| REL | 관계 | 두 객체 간 연결 데이터 | 중복 방지 제약조건 적용 |
| HIST | 이력 | 동일 업무 객체의 상태·값 변경 데이터 | 추가만 허용 |
| LOG | 로그 | 시스템 요청·응답·오류·처리 결과 데이터 | 추가만 허용, 보관기간 적용 가능 |

## 3. 공통 컬럼

| 적용 대상 | 컬럼 | 정의 |
|---|---|---|
| MST·일반 DTL | `CREATED_AT` | 데이터 생성일시 |
| MST·일반 DTL | `CREATED_USER_NO` | 데이터 생성사용자번호 |
| MST·일반 DTL | `UPDATED_AT` | 최종 수정일시 |
| MST·일반 DTL | `UPDATED_USER_NO` | 최종 수정사용자번호 |
| MST·일반 DTL | `DELETED_YN` | 논리 삭제여부 |
| REL | `CREATED_AT` | 관계 생성일시 |
| REL | `CREATED_USER_NO` | 관계 생성사용자번호 |
| HIST·LOG | `CREATED_AT` | 이력·로그 생성일시 |
| HIST·LOG | `CREATED_USER_NO` | 이력·로그 생성사용자번호 |

## 4. 표준 단어 사전

| 논리명 | 영문명 | 물리명 | 분류어 | 정의 |
|---|---|---|---|---|
| 사용자 | USER | USER | N | 앱을 사용하는 계정 주체 |
| 로그인 | LOGIN | LOGIN | N | 계정 인증 행위 |
| 비밀번호 | PASSWORD | PASSWORD | N | 인증용 비밀 문자열 |
| 닉네임 | NICKNAME | NICKNAME | N | 화면 표시 사용자명 |
| 차량 | VEHICLE | VEHICLE | N | 앱과 연결되는 차량 |
| 기본 | MASTER | MST | Y | 기준·주요 객체 테이블 유형 |
| 상세 | DETAIL | DTL | Y | 상세 내역 테이블 유형 |
| 관계 | RELATION | REL | Y | 관계 테이블 유형 |
| 이력 | HISTORY | HIST | Y | 상태·값 변경 테이블 유형 |
| 로그 | LOG | LOG | Y | 시스템 처리 기록 테이블 유형 |
| 여행 | TRIP | TRIP | N | 기록 시작부터 종료까지의 이동 단위 |
| 동행자 | COMPANION | COMPANION | N | 여행에 함께한 사람·반려동물 유형 |
| 경로 | ROUTE | ROUTE | N | 이동한 길 또는 계획한 길 |
| 점 | POINT | POINT | N | 경로를 구성하는 위치 기록 |
| 장소 | PLACE | PLACE | N | 사용자가 방문하거나 계획한 실제 위치 |
| 편의시설 | FACILITY | FACILITY | N | 화장실·수유실·주차장 등 장소의 이용시설 |
| 태그 | TAG | TAG | N | 경험과 검색 조건을 분류하는 키워드 |
| 공유 | SHARE | SHARE | N | 여행 기록을 다른 사용자에게 전달하는 행위 |
| 발신 | SENDER | SENDER | N | 공유를 보낸 사용자 역할 |
| 수신 | RECEIVER | RECEIVER | N | 공유를 받은 사용자 역할 |
| 공개범위 | VISIBILITY | VISIBILITY | N | 데이터 공개 범위 |
| 임시저장 | DRAFT | DRAFT | N | 상세 입력 전 위치만 저장한 상태 |
| 재방문 | REVISIT | REVISIT | N | 동일 장소를 다시 방문할 의사 |
| 동의 | CONSENT | CONSENT | N | 위치 수집·추천 공개에 대한 사용자 의사 |
| 길안내 | NAVIGATION | NAVI | N | NaviHelper를 통한 차량 경로 안내 |
| 요청 | REQUEST | REQ | N | 시스템에 처리를 요구한 건 |
| 응답 | RESPONSE | RESP | N | 시스템 요청 처리 결과 |
| 오류 | ERROR | ERR | N | 요청 처리 실패 정보 |
| 추천 | RECOMMENDATION | RECO | N | 여행 기록을 바탕으로 생성한 경로 결과 |
| 후기 | REVIEW | REVIEW | N | 장소 이용 경험에 대한 평가 |
| 성격 | POLARITY | POLARITY | N | 후기의 긍정·주의 구분 |
| 계획 | PLANNED | PLANNED | N | 실제 이동 전에 선택해 저장한 경로 |
| 검색 | SEARCH | SEARCH | N | 조건으로 데이터를 찾는 행위 |
| 조건 | CONDITION | COND | N | 검색과 추천에 사용하는 기준 |
| 매칭 | MATCH | MATCH | N | 검색조건과 유사 여행의 연결 결과 |
| 유사도 | SIMILARITY | SIMILARITY | N | 두 경로가 비슷한 정도 |
| 메모 | MEMO | MEMO | N | 사용자가 입력한 짧은 글 |
| 위도 | LATITUDE | LAT | N | 위치의 남북 좌표 |
| 경도 | LONGITUDE | LNG | N | 위치의 동서 좌표 |
| 출발 | DEPARTURE | DEP | N | 경로의 시작 위치 |
| 목적지 | DESTINATION | DEST | N | 경로의 최종 위치 |
| 기록 | RECORD | RECORDED | N | 위치·장소 저장 행위 |
| 시작 | START | START | N | 시작 시점 |
| 종료 | END | END | N | 종료 시점 |
| 변경 | CHANGE | CHANGED | N | 값이나 상태를 바꾼 행위 |
| 이전 | BEFORE | BEFORE | N | 변경 전 값 |
| 이후 | AFTER | AFTER | N | 변경 후 값 |
| 거리 | DISTANCE | DIST | N | 이동 거리 |
| 소요시간 | DURATION | DURATION | N | 이동·처리에 걸린 시간 |
| 생성 | CREATED | CREATED | N | 데이터를 처음 만든 행위 |
| 수정 | UPDATED | UPDATED | N | 데이터를 마지막으로 바꾼 행위 |
| 삭제 | DELETED | DELETED | N | 논리 삭제 처리 |
| 사용 | USE | USE | N | 사용 가능 상태 |
| 정렬 | SORT | SORT | N | 화면 표시 순서 |
| 그룹 | GROUP | GRP | N | 같은 종류를 묶는 단위 |
| 상태 | STATUS | STATUS | N | 대상의 현재 업무 단계 |
| 유형 | TYPE | TYPE | N | 대상을 구분하는 종류 |
| 방문 | VISIT | VISIT | N | 여행 중 장소에 들른 행위 |
| 원본 | SOURCE | SOURCE | N | 데이터 제공 출처 |
| 외부 | EXTERNAL | EXT | N | 외부 시스템·API에서 제공된 값 |
| 기본값 | DEFAULT | DEFAULT | N | 우선 적용되는 값 |
| 형식 | FORMAT | FORMAT | N | 데이터 저장 표현 방식 |
| 검증 | VERIFY | VERIFIED | N | 정보 확인 행위 |
| 번호 | NUMBER | NO | Y | 내부 대리키·일련번호 |
| 아이디 | ID | ID | Y | 사용자가 입력하는 로그인 식별 문자열 |
| UUID | UUID | UUID | Y | 외부 공유·동기화용 고유 식별자 |
| 코드 | CODE | CD | Y | 코드 테이블에서 관리하는 값 |
| 명 | NAME | NM | Y | 이름 |
| 여부 | YES OR NO | YN | Y | Y 또는 N 값 |
| 일시 | DATETIME | AT | Y | 날짜와 시각 |
| 개수 | COUNT | CNT | Y | 건수 |
| 순서 | ORDER | ORD | Y | 정렬·이동 순서 |
| 해시 | HASH | HASH | Y | 단방향 변환 문자열 |

## 5. 표준 도메인 사전

| 도메인명 | 분류어 | 데이터 타입 | 길이·정밀도 | 허용값·형식 | 용도 |
|---|---|---|---|---|---|
| 번호 | NO | BIGINT | 64비트 | 1 이상 자동 생성 | 내부 PK·FK |
| UUID | UUID | CHAR | 36 | UUID v4 | 외부 노출 식별자 |
| 아이디 | ID | VARCHAR | 50 | 영문·숫자 조합 | 로그인 아이디 |
| 비밀번호해시 | PASSWORD_HASH | VARCHAR | 255 | 해시값만 저장 | 비밀번호 |
| 코드 | CD | VARCHAR | 30 | 코드내역 등록값 | 상태·유형 코드 |
| 명 | NM | VARCHAR | 100 | 문자열 | 이름 |
| 메모 | MEMO | VARCHAR | 500 | 문자열 | 사용자 메모 |
| 메시지 | MSG | VARCHAR | 1000 | 문자열 | 오류·처리 메시지 |
| 여부 | YN | CHAR | 1 | Y 또는 N | 불리언 값 |
| 일시 | AT | TIMESTAMP WITH TIME ZONE | UTC | ISO 8601 | 모든 업무 일시 |
| 개수 | CNT | INTEGER | 32비트 | 0 이상 | 건수 |
| 순서 | ORD | INTEGER | 32비트 | 1 이상 | 표시·경로 순서 |
| 위도 | LAT | DECIMAL | 10,7 | -90 이상 90 이하 | 위치 좌표 |
| 경도 | LNG | DECIMAL | 10,7 | -180 이상 180 이하 | 위치 좌표 |
| 거리 | DIST | INTEGER | 미터 | 0 이상 | 이동 거리 |
| 소요시간 | DURATION | INTEGER | 초 | 0 이상 | 이동·처리 시간 |
| 유사도 | SIMILARITY | DECIMAL | 5,2 | 0 이상 100 이하 | 경로 유사도 백분율 |
| 경로데이터 | ROUTE_DATA | TEXT | 가변 | 인코딩 경로 문자열 | 간소화 경로 |

## 6. 표준 용어 사전

| 논리명 | 물리명 | 도메인 | 정의 |
|---|---|---|---|
| 사용자번호 | USER_NO | 번호 | 사용자 내부 식별번호 |
| 로그인아이디 | LOGIN_ID | 아이디 | 로그인 입력 식별자 |
| 비밀번호해시 | PASSWORD_HASH | 비밀번호해시 | 비밀번호 해시값 |
| 닉네임명 | NICKNAME_NM | 명 | 화면 표시 사용자명 |
| 차량번호 | VEHICLE_NO | 번호 | 차량 내부 식별번호 |
| 사용자차량번호 | USER_VEHICLE_NO | 번호 | 사용자와 차량 연결 식별번호 |
| 여행번호 | TRIP_NO | 번호 | 여행 내부 식별번호 |
| 여행UUID | TRIP_UUID | UUID | 외부 공유·동기화 여행 식별자 |
| 여행명 | TRIP_NM | 명 | 여행 이름 |
| 여행상태코드 | TRIP_STATUS_CD | 코드 | 여행 현재 상태 |
| 공개범위코드 | VISIBILITY_CD | 코드 | 여행 공개 범위 |
| 시작일시 | START_AT | 일시 | 여행 시작일시 |
| 종료일시 | END_AT | 일시 | 여행 종료일시 |
| 여행장소개수 | TRIP_PLACE_CNT | 개수 | 여행에 포함된 장소 수 |
| 여행동행자번호 | TRIP_COMPANION_NO | 번호 | 여행동행자내역 식별번호 |
| 동행자유형코드 | COMPANION_TYPE_CD | 코드 | 동행자 유형 |
| 동행자개수 | COMPANION_CNT | 개수 | 유형별 동행 인원 수 |
| 경로점번호 | ROUTE_POINT_NO | 번호 | 원본 경로점 식별번호 |
| 경로점순서 | POINT_ORD | 순서 | 원본 경로점 저장 순서 |
| 여행경로번호 | TRIP_ROUTE_NO | 번호 | 간소화 경로 식별번호 |
| 경로데이터 | ROUTE_DATA | 경로데이터 | 간소화 경로 문자열 |
| 경로데이터형식코드 | ROUTE_DATA_FORMAT_CD | 코드 | 간소화 경로 인코딩 형식 |
| 장소번호 | PLACE_NO | 번호 | 실제 장소 식별번호 |
| 장소명 | PLACE_NM | 명 | 실제 장소 이름 |
| 장소유형코드 | PLACE_TYPE_CD | 코드 | 장소 유형 |
| 장소원본코드 | PLACE_SOURCE_CD | 코드 | 장소정보 제공 출처 |
| 외부장소아이디 | EXT_PLACE_ID | 아이디 | 외부 API의 장소 식별자 |
| 장소편의시설번호 | PLACE_FACILITY_NO | 번호 | 장소편의시설내역 식별번호 |
| 편의시설유형코드 | FACILITY_TYPE_CD | 코드 | 편의시설 종류 |
| 이용가능여부 | AVAILABLE_YN | 여부 | 편의시설 이용 가능 여부 |
| 여행장소번호 | TRIP_PLACE_NO | 번호 | 여행장소내역 식별번호 |
| 기록위도 | RECORDED_LAT | 위도 | 장소 기록 시점의 위도 |
| 기록경도 | RECORDED_LNG | 경도 | 장소 기록 시점의 경도 |
| 기록일시 | RECORDED_AT | 일시 | 장소·경로점 기록일시 |
| 방문순서 | VISIT_ORD | 순서 | 여행 중 장소 방문 순서 |
| 임시저장여부 | DRAFT_YN | 여부 | 상세 경험 입력 전 임시 기록 여부 |
| 후기성격코드 | REVIEW_POLARITY_CD | 코드 | 긍정·주의 후기 구분 |
| 재방문여부 | REVISIT_YN | 여부 | 동일 장소 재방문 의사 |
| 태그번호 | TAG_NO | 번호 | 태그 식별번호 |
| 태그명 | TAG_NM | 명 | 태그 표시 이름 |
| 여행장소태그번호 | TRIP_PLACE_TAG_NO | 번호 | 여행장소와 태그 연결번호 |
| 여행공유번호 | TRIP_SHARE_NO | 번호 | 여행 공유 건 식별번호 |
| 발신사용자번호 | SENDER_USER_NO | 번호 | 공유 발신 사용자번호 |
| 수신사용자번호 | RECEIVER_USER_NO | 번호 | 공유 수신 사용자번호 |
| 복제여행번호 | COPY_TRIP_NO | 번호 | 수락 시 생성된 여행번호 |
| 공유상태코드 | SHARE_STATUS_CD | 코드 | 공유 처리 상태 |
| 경로검색조건번호 | ROUTE_SEARCH_COND_NO | 번호 | 경로 검색조건 식별번호 |
| 출발위도 | DEP_LAT | 위도 | 출발 위치 위도 |
| 출발경도 | DEP_LNG | 경도 | 출발 위치 경도 |
| 목적지위도 | DEST_LAT | 위도 | 목적지 위치 위도 |
| 목적지경도 | DEST_LNG | 경도 | 목적지 위치 경도 |
| 경로매칭번호 | ROUTE_MATCH_NO | 번호 | 유사 여행 매칭 식별번호 |
| 유사도 | SIMILARITY | 유사도 | 경로 유사도 백분율 |
| 경로추천번호 | ROUTE_RECO_NO | 번호 | 경로 추천 결과 식별번호 |
| 추천유형코드 | RECO_TYPE_CD | 코드 | 추천 경로 유형 |
| 계획경로번호 | PLANNED_ROUTE_NO | 번호 | 저장한 계획 경로 식별번호 |
| 계획경로명 | PLANNED_ROUTE_NM | 명 | 계획 경로 이름 |
| 계획경로장소번호 | PLANNED_ROUTE_PLACE_NO | 번호 | 계획 경로 장소 식별번호 |
| 경로순서 | ROUTE_ORD | 순서 | 추천·계획 경로의 장소 순서 |
| 동의이력번호 | CONSENT_HIST_NO | 번호 | 동의이력 식별번호 |
| 동의유형코드 | CONSENT_TYPE_CD | 코드 | 위치 수집·추천 공개 동의 유형 |
| 동의여부 | CONSENT_YN | 여부 | 동의 또는 철회 여부 |
| 여행상태이력번호 | TRIP_STATUS_HIST_NO | 번호 | 여행상태이력 식별번호 |
| 이전여행상태코드 | BEFORE_TRIP_STATUS_CD | 코드 | 상태 변경 전 여행상태 |
| 이후여행상태코드 | AFTER_TRIP_STATUS_CD | 코드 | 상태 변경 후 여행상태 |
| 길안내요청로그번호 | NAVI_REQ_LOG_NO | 번호 | 길안내 요청 로그 식별번호 |
| 길안내요청상태코드 | NAVI_REQ_STATUS_CD | 코드 | NaviHelper 처리 상태 |
| 오류메시지 | ERR_MSG | 메시지 | 요청 실패 원인 메시지 |
| 요청일시 | REQ_AT | 일시 | 시스템 요청일시 |
| 응답일시 | RESP_AT | 일시 | 시스템 응답일시 |
| 생성일시 | CREATED_AT | 일시 | 데이터 생성일시 |
| 생성사용자번호 | CREATED_USER_NO | 번호 | 데이터 생성사용자번호 |
| 수정일시 | UPDATED_AT | 일시 | 최종 수정일시 |
| 수정사용자번호 | UPDATED_USER_NO | 번호 | 최종 수정사용자번호 |
| 삭제여부 | DELETED_YN | 여부 | 논리 삭제 여부 |

## 7. 표준 코드 정의서

| 코드그룹 | 그룹명 | 코드값 | 코드명 | 정의 | 정렬순서 | 사용여부 |
|---|---|---|---|---|---|---|
| TRIP_STATUS | 여행 상태 | RECORDING | 기록 중 | 위치 기록 진행 상태 | 1 | Y |
| TRIP_STATUS | 여행 상태 | COMPLETED | 완료 | 정상 종료 상태 | 2 | Y |
| TRIP_STATUS | 여행 상태 | INTERRUPTED | 중단 | 앱·시동·오류로 기록이 끊긴 상태 | 3 | Y |
| TRIP_STATUS | 여행 상태 | CANCELED | 취소 | 사용자가 기록을 취소한 상태 | 4 | Y |
| SHARE_STATUS | 공유 상태 | NEW | 새 공유 | 수신자가 확인·수락하기 전 상태 | 1 | Y |
| SHARE_STATUS | 공유 상태 | ACCEPTED | 수락 | 수신자 여행으로 복제된 상태 | 2 | Y |
| PLACE_TYPE | 장소 유형 | ATTRACTION | 명소 | 관광·볼거리 장소 | 1 | Y |
| PLACE_TYPE | 장소 유형 | RESTAURANT | 음식점 | 식사 장소 | 2 | Y |
| PLACE_TYPE | 장소 유형 | PARKING | 주차장 | 주차 장소 | 3 | Y |
| PLACE_TYPE | 장소 유형 | FACILITY | 편의시설 | 화장실·수유실 등 편의시설 | 4 | Y |
| PLACE_TYPE | 장소 유형 | REST_AREA | 휴게소 | 고속도로·국도 휴게소 | 5 | Y |
| COMPANION_TYPE | 동행자 유형 | SOLO | 혼자 | 동행자 없음 | 1 | Y |
| COMPANION_TYPE | 동행자 유형 | SPOUSE | 배우자 | 배우자 동행 | 2 | Y |
| COMPANION_TYPE | 동행자 유형 | CHILD | 아이 동반 | 아이 동행 | 3 | Y |
| COMPANION_TYPE | 동행자 유형 | PARENT | 부모님 | 부모님 동행 | 4 | Y |
| COMPANION_TYPE | 동행자 유형 | PET | 반려동물 | 반려동물 동행 | 5 | Y |
| VISIBILITY | 공개 범위 | PRIVATE | 나만 보기 | 본인만 조회 | 1 | Y |
| VISIBILITY | 공개 범위 | ANONYMOUS_PUBLIC | 익명 공개 | 추천 데이터로 익명 제공 | 2 | Y |
| VISIBILITY | 공개 범위 | DIRECT_SHARE | 직접 공유만 | 지정 사용자에게만 공유 | 3 | Y |
| NAVI_REQ_STATUS | 길안내 요청 상태 | REQUESTING | 요청 중 | NaviHelper 처리 대기 | 1 | Y |
| NAVI_REQ_STATUS | 길안내 요청 상태 | SUCCESS | 성공 | 차량 길안내 시작 | 2 | Y |
| NAVI_REQ_STATUS | 길안내 요청 상태 | FAILED | 실패 | 호출 실패 또는 폴백 전환 | 3 | Y |
| CONSENT_TYPE | 동의 유형 | LOCATION_COLLECT | 위치 수집 | 여행 기록용 위치정보 수집 | 1 | Y |
| CONSENT_TYPE | 동의 유형 | RECO_PUBLIC | 추천 공개 | 익명 추천 데이터 제공 | 2 | Y |
| RECO_TYPE | 추천 경로 유형 | CHILD_FRIENDLY | 아이 동반 | 아이 편의시설 중심 | 1 | Y |
| RECO_TYPE | 추천 경로 유형 | FOOD | 맛집 | 음식점·휴게소 맛집 중심 | 2 | Y |
| RECO_TYPE | 추천 경로 유형 | EASY_PARKING | 주차 편의 | 주차가 편한 장소 중심 | 3 | Y |
| RECO_TYPE | 추천 경로 유형 | FAST | 빠른 이동 | 경유 최소 경로 | 4 | Y |
| REVIEW_POLARITY | 후기 성격 | POSITIVE | 긍정 | 추천 근거 후기 | 1 | Y |
| REVIEW_POLARITY | 후기 성격 | CAUTION | 주의 | 주의사항 후기 | 2 | Y |
| FACILITY_TYPE | 편의시설 유형 | RESTROOM | 화장실 | 일반 화장실 | 1 | Y |
| FACILITY_TYPE | 편의시설 유형 | NURSING_ROOM | 수유실 | 수유 공간 | 2 | Y |
| FACILITY_TYPE | 편의시설 유형 | DIAPER_TABLE | 기저귀 교환대 | 영유아 기저귀 교환시설 | 3 | Y |
| FACILITY_TYPE | 편의시설 유형 | CHILD_CHAIR | 아동 의자 | 음식점 아동용 의자 | 4 | Y |
| PLACE_SOURCE | 장소정보 출처 | USER | 사용자 입력 | 사용자가 직접 입력한 장소 | 1 | Y |
| PLACE_SOURCE | 장소정보 출처 | EXTERNAL_API | 외부 API | 외부 장소·편의시설 API | 2 | Y |
| ROUTE_DATA_FORMAT | 경로데이터 형식 | POLYLINE | 폴리라인 | 인코딩 폴리라인 경로 | 1 | Y |

