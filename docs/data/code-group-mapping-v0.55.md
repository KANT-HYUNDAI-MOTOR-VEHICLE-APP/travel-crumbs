# Travel Crumbs v0.55 Room·Fake Data ↔ PostgreSQL 매핑

기준 문서: `데이터표준정의서_v0.55.md`  
적용 브랜치: `data-crud/main`

## 1. 적용 원칙

- 문서·ERD: 영문 대문자 `SNAKE_CASE`
- PostgreSQL·Room 물리명: 영문 소문자 `snake_case`
- PostgreSQL: 개발 중 데이터 원본
- Room: 현재 앱 구현 데이터와 전송 대기 상태 확인 대상
- 기존 기능 삭제 금지
- 기존 Alembic 파일 수정 금지, 신규 마이그레이션 추가
- 다른 브랜치 수정 금지
- `docs/data/archive/` 과거 버전 수정 금지

## 2. 코드그룹 식별값 매핑

`CD_MST.CD_GRP_NM` 컬럼명은 유지하고, 컬럼에 저장하는 식별값만 변경.

| 변경 전 | 변경 후 | 코드값 |
|---|---|---|
| `COMPANION_TYPE` | `COMPANION_TYPE_CD` | `SOLO`, `SPOUSE`, `CHILD`, `PARENT`, `FRIEND`, `PET` |
| `CONSENT_TYPE` | `CONSENT_TYPE_CD` | `LOCATION_COLLECT`, `RECO_PUBLIC` |
| `DATA_SRC` | `DATA_SRC_CD` | `PUBLIC_API`, `USER`, `NAVI` |
| `REVIEW_POLARITY` | `REVIEW_POLARITY_CD` | `POSITIVE`, `NEGATIVE` |
| `TRIP_STATUS` | `TRIP_STATUS_CD` | `RECORDING`, `PAUSED`, `COMPLETED`, `INTERRUPTED`, `CANCELED` |
| `VISIBILITY` | `VISIBILITY_CD` | `PRIVATE`, `ANONYMOUS_PUBLIC`, `DIRECT_SHARE` |
| `SHARE_STATUS` | `SHARE_STATUS_CD` | `NEW`, `ACCEPTED` |
| `NAVI_REQ_STATUS` | `NAVI_REQ_STATUS_CD` | `REQUESTING`, `SUCCESS`, `FAILED` |
| `RECO_TYPE` | `RECO_TYPE_CD` | `CHILD_FRIENDLY`, `FOOD`, `EASY_PARKING`, `FAST` |

### API 경로

| 변경 전 | 변경 후 |
|---|---|
| `GET /codes/COMPANION_TYPE` | `GET /codes/COMPANION_TYPE_CD` |
| `GET /codes/CONSENT_TYPE` | `GET /codes/CONSENT_TYPE_CD` |
| `GET /codes/DATA_SRC` | `GET /codes/DATA_SRC_CD` |
| `GET /codes/REVIEW_POLARITY` | `GET /codes/REVIEW_POLARITY_CD` |
| `GET /codes/TRIP_STATUS` | `GET /codes/TRIP_STATUS_CD` |
| `GET /codes/VISIBILITY` | `GET /codes/VISIBILITY_CD` |
| `GET /codes/SHARE_STATUS` | `GET /codes/SHARE_STATUS_CD` |
| `GET /codes/NAVI_REQ_STATUS` | `GET /codes/NAVI_REQ_STATUS_CD` |
| `GET /codes/RECO_TYPE` | `GET /codes/RECO_TYPE_CD` |

## 3. Room ↔ PostgreSQL 테이블 매핑

| Room·앱 데이터 | PostgreSQL v0.55 | 처리 |
|---|---|---|
| `UserMstEntity` | `USER_MST` | 동일 사용자 기준정보 |
| `UserConsentHistEntity` | `USER_CONSENT_HIST` | 동의·철회 행 추가 |
| `TripMstEntity` | `TRIP_MST` | 여행 마스터 |
| `TripCompanionDtlEntity` | `TRIP_COMPANION_DTL` | 동행자 유형별 행 |
| `TripPlaceDtlEntity` | `TRIP_PLACE_DTL` | 여행 중 기록한 장소·경험 |
| `TagMstEntity` | `TAG_MST` | 경험 태그 마스터 |
| `TripPlaceTagRelEntity` | `TRIP_PLACE_TAG_REL` | 여행장소-태그 관계 |
| `NaviReqLogEntity` | 직접 매핑 금지 | 현재 Room 행은 전송 대기 큐, PostgreSQL `NAVI_REQ_LOG`는 길안내 사건 로그 |
| `TripStatus` 변경 | `TRIP_STATUS_HIST` | 상태 변경 때마다 이력 추가 |
| 위치 샘플 | `TRIP_ROUTE_POINT_DTL` | 원본 경로점 저장 |
| 간소화 경로 | `TRIP_ROUTE_DTL` | 여행 종료 후 인코딩 폴리라인 저장 |
| `RecommendedTrip` | `TRIP_MST` | 완료·익명 공개 여행 |
| `RecommendedPlace` | `PLACE_MST` + `TRIP_PLACE_DTL` | 장소 기준정보와 여행별 방문 기록 분리 |
| `ratingAvg`, `ratingCnt` | `TRIP_RATING_DTL` 조회 | 컬럼 저장 금지, `AVG`·`COUNT` 계산 |
| `DemoRoadmap` | `TRIP_MST` | 완료·익명 공개 여행 |
| `DemoStop` | `PLACE_MST` + `TRIP_PLACE_DTL` | 장소와 여행장소 분리 |

## 4. Room 컬럼 수정 매핑

### USER_MST

| Room 현재 | PostgreSQL v0.55 | 처리 |
|---|---|---|
| `user_no` | `USER_NO` | 유지 |
| `login_id` | `LOGIN_ID` | 유지 |
| 없음 | `PASSWORD_HASH` | 서버 인증 전용, Room 평문·해시 복제 금지 |
| `nickname_nm` | `NICKNAME_NM` | 유지 |
| 공통 컬럼 | 공통 컬럼 | MST 규칙 적용 |

### TRIP_MST

| Room 현재 | PostgreSQL v0.55 | 처리 |
|---|---|---|
| `trip_no` | `TRIP_NO` | 유지 |
| 없음 | `TRIP_UUID` | 서버 생성 |
| `user_no` | `USER_NO` | 유지 |
| `trip_nm` | `TRIP_NM` | 유지 |
| `trip_status_cd` | `TRIP_STATUS_CD` | 유지, 값 변경 때 `TRIP_STATUS_HIST` 추가 |
| `visibility_cd` | `VISIBILITY_CD` | 유지 |
| `start_at` | `START_AT` | epoch ms → UTC timestamptz 변환 |
| `end_at` | `END_AT` | epoch ms → UTC timestamptz 변환 |
| `duration` | `DURATION` | 밀리초 입력이면 초 단위로 변환 |
| 없음 | `DIST` | 경로점 기준 미터 집계 |
| 없음 | `PLACE_CNT` | 여행장소 건수 집계 |
| `dest_place_nm` | 직접 대응 컬럼 없음 | 앱 기능 유지, 목적지 장소 관계 확정 전 PostgreSQL 저장 제외 |
| `accumulated_millis` | `DURATION` 산출 입력 | 앱 런타임 값, 종료 시 초 단위로 집계 |
| `segment_start_millis` | 저장 제외 | 앱 런타임 상태 |

### TRIP_PLACE_DTL

| Room 현재 | PostgreSQL v0.55 | 처리 |
|---|---|---|
| `trip_place_no` | `TRIP_PLACE_NO` | 유지 |
| `trip_no` | `TRIP_NO` | 유지 |
| 없음 | `PLACE_NO` | `PLACE_MST` 연결 시 저장, 임시 기록은 NULL 허용 |
| `place_ord` | 직접 대응 컬럼 없음 | 여행 타임라인은 `RECORDED_AT` 정렬, 계획·추천 경로 순서와 혼용 금지 |
| `user_place_nm` | `USER_PLACE_NM` | 유지 |
| 없음 | `PLACE_TYPE_CD` | 장소 유형 선택 시 저장 |
| `memo` | `MEMO` | 유지 |
| 없음 | `REVIEW_POLARITY_CD` | `POSITIVE`·`NEGATIVE` 저장 |
| `draft_yn` | `DRAFT_YN` | 유지 |
| `lat`, `lng` | `LAT`, `LNG` | 유지 |
| `addr` | `ADDR` | 유지 |
| `recorded_at` | `RECORDED_AT` | epoch ms → UTC timestamptz 변환 |

### TAG_MST·TRIP_PLACE_TAG_REL

| Room 현재 | PostgreSQL v0.55 | 처리 |
|---|---|---|
| `tag_no` | `TAG_NO` | 유지 |
| `tag_nm` | `TAG_NM` | 유지 |
| `use_yn` | `USE_YN` | 유지 |
| `sort_ord` | `SORT_ORD` | 유지 |
| `trip_place_tag_no` | `TRIP_PLACE_TAG_NO` | 유지 |
| 관계 테이블의 `updated_*`, `deleted_yn` | 없음 | REL 공통 규칙에 맞춰 생성 컬럼만 사용 |

### NAVI_REQ_LOG

| Room 현재 | PostgreSQL v0.55 | 처리 |
|---|---|---|
| `sequence` / `navi_req_log_no` | `NAVI_REQ_LOG_NO` | 서버 대리키 자동 생성, 여행 내 순서로 사용 금지 |
| `placeId: String` | `PLACE_NO: BIGINT` | POI ID이면 먼저 `PLACE_MST.POI_ID`로 장소 조회 |
| 없음 | `USER_NO` | 요청 사용자 저장 |
| 없음 | `PLANNED_ROUTE_NO` | 계획 경로 요청일 때 저장 |
| 없음 | `NAVI_REQ_STATUS_CD` | `REQUESTING` → `SUCCESS` 또는 `FAILED` |
| 없음 | `ERR_MSG` | 실패 사유 저장 |
| `startedAtMillis` / `created_at` | `REQ_AT` | 요청 시각으로 변환, `CREATED_AT`과 분리 |
| 없음 | `RESP_AT` | 성공·실패 응답 시각 |
| `utcOffsetSeconds` | 저장 제외 | 화면 표시 계산값 |
| `uploadState` | 저장 제외 | Room 전송 대기 상태, 서버 업무 상태와 혼용 금지 |

## 5. 가짜 데이터 ↔ PostgreSQL 매핑

| 앱·시드 필드 | PostgreSQL v0.55 | 변환 |
|---|---|---|
| `RecommendedTrip.tripNo` | `TRIP_MST.TRIP_NO` | 유지 |
| `tripNm` | `TRIP_MST.TRIP_NM` | 유지 |
| `nicknameNm` | `USER_MST.NICKNAME_NM` | 시드 사용자 연결 |
| `companionTypes` | `TRIP_COMPANION_DTL.COMPANION_TYPE_CD` | 유형별 행 생성 |
| `publishedAt` | `TRIP_MST.END_AT` | UTC timestamptz 변환 |
| `ratingAvg`, `ratingCnt` | `TRIP_RATING_DTL.RATING_SCORE` | 평균·건수와 일치하도록 평가 행 생성 |
| `RecommendedPlace.placeNo` | `PLACE_MST.PLACE_NO` | 유지 |
| `placeNm`, `addr`, `lat`, `lng` | `PLACE_MST` | 장소 기준정보 저장 |
| `placeOrd` | `TRIP_PLACE_DTL.RECORDED_AT` | 순서대로 시각 간격 생성 |
| `tags` | `TRIP_PLACE_TAG_REL` | 표준 `TAG_NO`로 연결 |
| `published` | `TRIP_MST.VISIBILITY_CD` | 공개 `ANONYMOUS_PUBLIC`, 비공개 `PRIVATE` |
| `saved` | 저장 제외 | 앱 UI 상태 |
| `places.json` | `PLACE_MST` | 장소명·주소·좌표·POI 유지 |
| `PendingArrival`, `AutoPlaceName` | 저장 제외 | 앱 임시 상태 |

## 6. 개별 코드·태그 반영

### PLACE_TYPE_MST

- `PLACE_TYPE_CD`: 의미 PK
- `PLACE_TYPE_NM`: 장소유형명
- `PLACE_CATEGORY_CD`: 장소카테고리코드
- `PLACE_CATEGORY_NM`: 장소카테고리명
- `SORT_ORD`, `USE_YN`
- `PLACE_TYPE_CD_MST`로 변경 금지

### TAG_MST

- `TAG_CATEGORY_CD` 추가 금지
- v0.55 태그 13종 반영
- 기존 1~5 유지
- 신규 6~13 추가

| TAG_NO | TAG_NM |
|---:|---|
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

## 7. 코드 수정 위치

| 순서 | 경로 | 수정 범위 |
|---:|---|---|
| 1 | `server/alembic/versions/` | 신규 마이그레이션 |
| 2 | `server/app/codes/` | 코드그룹 식별값 |
| 3 | `server/app/tools/` | 시드·검증 데이터 |
| 4 | `server/app/trips/` | 여행·장소·상태 이력 |
| 5 | `server/app/places/` | 장소유형·태그 |
| 6 | `server/app/navigation/` | 길안내 로그 |
| 7 | `app/src/main/.../data/local/` | Room 엔티티·DAO·마이그레이션 |
| 8 | `app/src/main/.../domain/model/` | 앱 모델과 서버 DTO 변환 |
| 9 | `app/src/test/`, `server/tests/` | 변경된 기대값 |

## 8. 검증 기준

- 이전 코드그룹 식별값 단독 사용 0건
- `CD_MST.CD_GRP_NM`의 9개 그룹명 `_CD` 접미사 확인
- `PLACE_TYPE_MST` 24종 확인
- `TAG_MST` 13종 확인
- Room 전송 대기 상태와 `NAVI_REQ_STATUS_CD` 분리 확인
- 앱 시간값 epoch ms ↔ PostgreSQL UTC timestamptz 변환 확인
- 여행 `DURATION` 초 단위 확인
- `alembic upgrade head` 성공
- 전체 시드·검증 스크립트 성공

## 9. 다른 AI 전달 범위

> `data-crud/main`에서만 작업. `데이터표준정의서_v0.55.md`와 이 매핑표를 기준으로 현재 Room·가짜 데이터·PostgreSQL 모델을 정렬. 기존 기능과 데이터는 삭제하지 않고, 직접 대응하지 않는 앱 상태는 저장 제외 또는 변환 규칙을 유지. 기존 Alembic 파일과 보관 문서는 수정하지 않고 신규 마이그레이션 추가. 다른 브랜치는 수정하지 않음. 수정 후 8절 검증 기준 실행.
