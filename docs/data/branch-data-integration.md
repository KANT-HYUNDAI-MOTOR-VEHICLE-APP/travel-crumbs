# 브랜치 데이터 통합 기준

## 목표

- 각 브랜치의 Room·DataStore·가짜 데이터를 PostgreSQL 공통 데이터로 통합
- Docker PostgreSQL 기준으로 팀원별 동일 화면 재현
- Room SQLite 전환은 제출 APK 생성 직전 별도 작업

## 반영 범위

| 브랜치 | 기존 데이터 | PostgreSQL 반영 |
|---|---|---|
| data-crud/main | 사용자·여행·경로·장소·태그 | 기존 서버 모델 유지 |
| recommendation-map/main | 공개 여행 25건·방문 장소 91건·평점·태그 | 공개 여행·장소·동행자·평점 시드 |
| share-prototype/impl | 공개 로드맵 5건·장소 9건·별점·화면 상태 | 공개 여행·장소·평점 시드 |
| navi-autorecord/main | 도착 대기·자동 장소명·길안내 상태 | 앱 전송 대기 데이터와 서버 로그 분리 대상 |
| gleo-actions/main | 음성 명령·콜백 상태 | 런타임 입출력, 업무 데이터와 분리 |

## 확정 매핑

| 앱 데이터 | PostgreSQL | 변환 기준 |
|---|---|---|
| RecommendedTrip | `trip_mst` | 완료·익명 공개 여행 |
| companionTypes | `trip_companion_dtl` | 동행자 코드 그대로 저장 |
| RecommendedPlace | `place_mst` | 장소번호·명·주소·좌표 유지 |
| 여행별 장소 순서 | `trip_place_dtl.recorded_at` | 목록 순서대로 20분 간격 생성 |
| 장소 태그 | `trip_place_tag_rel` | 표준 태그번호로 변환 |
| ratingAvg·ratingCnt | `trip_rating_dtl` | 평점 건수만큼 시드 평가 생성 |
| publishedAt | `trip_mst.end_at` | 공개 여행 완료 시각으로 저장 |
| DemoRoadmap | `trip_mst` | 완료·익명 공개 여행 |
| DemoStop | `place_mst`, `trip_place_dtl` | POI ID·명·주소·좌표·화면 분류 유지 |
| stars·count | `trip_rating_dtl` | 표시 평균과 평가 수가 같도록 평가 행 생성 |
| published | `trip_mst.visibility_cd` | 공개 시 `ANONYMOUS_PUBLIC`, 취소 시 `PRIVATE` |
| saved | 앱 화면 상태 | 별도 서버 업무 데이터로 추가하지 않음 |
| places.json | `place_mst` | 후보 5건의 ID·명·주소·좌표 유지 |
| 안내 시작 성공 | `navi_req_log` | 사용자·장소·시작 시각을 추가 전용 로그로 저장 |
| 도착 후 재출발 자동 기록 | `trip_place_dtl` | `draft_yn = Y`, `recorded_at`은 도착 시각 |
| uploadState | 앱 Room 전송 큐 | 서버 로그의 업무 상태로 변환하지 않음 |
| utcOffsetSeconds | 앱 Room 전송 큐 | `startedAt` ISO-8601 변환에만 사용 |
| PendingArrival·AutoPlaceName | 앱 DataStore | 기기 임시 상태로 유지 |

## 태그 번호 변환

| 표준 번호 | 태그명 | 추천 브랜치 기존 번호 |
|---:|---|---:|
| 1 | 주차 편해요 | 2 |
| 2 | 아이와 가기 좋아요 | 1 |
| 3 | 화장실 깨끗해요 | 5 |
| 4 | 다시 가고 싶어요 | 3 |
| 5 | 웨이팅 있어요 | 4 |

## 추가 표준

| 논리명 | 물리명 | 타입 | 사용 테이블 |
|---|---|---|---|
| 주소 | `addr` | `VARCHAR(300)` | `place_mst`, `trip_place_dtl` |
| 여행평점번호 | `trip_rating_no` | `BIGINT` | `trip_rating_dtl`, `trip_rating_tag_rel` |
| 여행평점태그번호 | `trip_rating_tag_no` | `BIGINT` | `trip_rating_tag_rel` |
| 평점점수 | `rating_score` | `DECIMAL(2,1)` | `trip_rating_dtl` |
| 평가일시 | `rated_at` | `TIMESTAMP WITH TIME ZONE` | `trip_rating_dtl` |

## 공통 시드 결과

- 공개 여행: 25건
- 여행장소: 91건
- 고유 장소: 90건
- 평점: 287건
- 태그: 표준 13종
- 공유 프로토타입 공개 여행: 5건
- 공유 프로토타입 여행장소: 11건
- 공유 프로토타입 고유 장소: 9건
- 공유 프로토타입 평점: 90건
- 내비 후보 장소: 5건

## 실행

```bash
cd server
docker compose up -d db
uv sync
uv run alembic upgrade head
uv run python -m app.tools.seed_all
uv run python -m app.tools.check_all_seeds
```

## 후속 작업

- 앱 Repository의 로컬 추천 시드를 서버 조회로 교체
- 앱 `places.json` 조회를 서버 장소 조회로 교체
- 최종 제출 직전 PostgreSQL 모델을 Room SQLite로 변환
