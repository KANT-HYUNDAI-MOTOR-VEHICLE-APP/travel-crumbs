# 데이터 의사결정 기록 (ERD v0.4 반영 대기)

ERD v0.3 · 데이터 표준정의서 v0.35 이후 결정한 내용. ERD v0.4 작성 시 이 목록을 반영함.

| 번호 | 날짜 | 결정 | 이유 | 영향 범위 | 결정 |
|---|---|---|---|---|---|
| D01 | 2026-11-07 | "경유지(waypoint)" 단어를 데이터·화면 명세·화면 문구 어디에도 쓰지 않고 모두 "장소(place)"로 통일 | 경로는 순서 있는 장소 목록(place_ord)이고, 목적지는 마지막 순서의 장소라 따로 저장할 필요 없음. 경유지를 따로 두면 순서 변경·삭제 시 목적지 누락·중복 오류 위험. 와이어프레임에 남은 "경유지"가 코드 이름(waypoint)으로 다시 새어 들어옴 | ERD v0.3 navi_req_log.planned_route_no 설명("경유지 경로일 때" → "여러 장소 경로일 때") · 와이어프레임 v5 B-10~B-12 등 13곳 | 지수 |
| D02 | 2026-11-07 | 목적지는 저장하지 않고 계획경로장소내역(planned_route_place_dtl)의 마지막 place_ord 장소로 판단 | D01과 같음 | planned_route_place_dtl | 지수 |
| D03 | 2026-11-07 | 차량 길안내(NaviHelper) 요청 코드에서만 Pleos API 용어(목적지 1개 + 경유지 목록)로 변환 | NaviHelper API가 목적지·경유지를 나눠 받음. 변환은 요청 시점 앱 코드에서 처리 | app/data/navi | 지수 |
| D04 | 2026-11-07 | 서버 DB는 PostgreSQL 17(Docker) · 테이블은 Alembic 리비전으로 관리 | 팀 리드 지시 · docs/setup.md 기준. 팀원이 `docker compose up -d db` · `uv run alembic upgrade head`로 같은 DB를 만들 수 있음 | server/ | 팀 리드 |
| D05 | 2026-11-07 | 테이블은 화면 구현 순서에 맞춰 단계적으로 추가 · 첫 테이블은 user_mst(샘플 사용자) | 화면에 필요한 컬럼만 만들고 화면과 데이터를 함께 검증 | server/alembic | 팀 리드 · 지수 |
| D06 | 2026-11-07 | 기준 문서 역할 구분: 표준정의서 v0.35 = 명명 규칙 · 도메인 · 코드, ERD v0.3 = 테이블 · 컬럼 정의 | v0.35는 하네스가 읽는 문서 양을 줄이려고 규칙만 남김 | 테이블 정의 작업 | 팀 리드 · 지수 |
| D07 | 2026-11-07 | CRUD 검증은 SQL 수준 + 화면에 필요한 데이터 확인까지 | 다음 작업자(Gleo AI 입출력)가 데이터 흐름을 믿고 쓸 수 있는 수준 | 시나리오 1 · 2 | 지수 |
| D08 | 2026-11-07 | 작업 위치는 data-crud/main 브랜치 · DB 정의는 server/ · ERD v0.4는 docs/data/ | ERD도 이 브랜치에 두라는 팀 리드 지시 · v0.3이 docs/data/에 있음 | 저장소 구조 | 팀 리드 |
| D09 | 2026-11-07 | trip_place_dtl.place_ord는 시나리오 1에서 만들지 않음 · 방문 순서는 기록일시(recorded_at)로 판단 · planned_route_place_dtl.place_ord는 유지 | 와이어프레임 v5에서 여행 기록 장소 순서를 바꾸는 화면이 없음(A-09는 수정·삭제만). place_ord Update는 B-10 계획 경로에만 있음. D05 원칙(화면에 필요한 컬럼만) 적용 — 순서를 바꾸는 화면이 생기면 추가 | trip_place_dtl | 지수 |
| D10 | 2026-11-07 | 공유는 시나리오 1에서 제외 · 시나리오 1 범위는 로그인부터 여행 종료까지(A-02 · A-03 · A-03-1 · A-04 · A-05 · A-06 · A-08) | 공유 방식은 다른 팀원이 정하는 중이고, 와이어프레임 v5의 "다른 사용자에게 보내는" 방식과 다름 | 홈 받은 여행함 · 받은함 메뉴 · A-08 공유 버튼 · trip_share_dtl | 지수 |
| D11 | 2026-11-07 | 여행명 기본값은 날짜만(예: 2026.11.05 여행) · 「수정」을 누르면 이름 추천 자리(TripNameSuggester)를 거침 — 지금은 그대로 돌려주고 Gleo AI 담당이 구현만 교체 · 지역 자동 생성은 하지 않음 | 시작 위치는 보통 출발지라 지역을 붙여도 여행지와 다름. 위치 권한 · Geocoder 의존이 없어져 시연 위험이 없음 | trip_mst.trip_nm · app/domain/TripName · di/TripModule | 지수 |
| D12 | 2026-11-07 | (가안) 동행자는 1개 이상 필수 · 여러 개 선택 가능 · 「혼자(SOLO)」는 다른 동행자와 함께 고를 수 없음 | 「혼자」와 동행자가 같이 저장되면 뜻이 어긋남. 아무것도 안 고르면 「혼자」인지 「입력 안 함」인지 구분 안 됨. 서버(422)와 앱 둘 다 검사 | trip_companion_dtl | 지수 |
| D13 | 2026-11-07 | 새 여행의 공개범위(visibility_cd)는 PRIVATE(나만 보기)로 시작 | A-04에 공개범위 선택이 없음. 공개 · 공유는 시나리오 1 제외(D10) | trip_mst.visibility_cd | 지수 |
| D14 | 2026-11-07 | 공통 코드값(cd_grp_mst · cd_dtl)은 시드가 아닌 Alembic 마이그레이션에 넣음 · 그 생성자인 시스템 계정(user_no 1)도 같은 마이그레이션에서 먼저 만듦 · 코드 그룹은 화면에 필요해질 때 추가 | 코드는 개발용 샘플이 아니라 앱이 늘 쓰는 기준 데이터 → `alembic upgrade head` 만으로 모든 팀원 DB에 같은 코드가 생김. 지금은 TRIP_STATUS · VISIBILITY · COMPANION_TYPE · CONSENT_TYPE | server/alembic · cd_dtl | 지수 |
| D15 | 2026-11-07 | 한 사용자의 기록 중(RECORDING) 여행은 1건 · 있으면 새 여행 시작을 막음(서버 409) · 여행 시작 시 trip_mst · trip_companion_dtl · trip_status_hist(RECORDING)를 한 번에 저장 | 기록 중 화면(A-05)이 「진행 중 trip_mst」 하나를 읽음. 종료(A-08) 전 재테스트는 server/README의 취소 명령 사용 | trip_mst · trip_status_hist | 지수 |
| D16 | 2026-11-07 | 코드(cd)는 영어 대문자 · 화면 글자는 cd_dtl.cd_nm · 데이터가 쌓인 뒤 cd 는 바꾸지 않고 뜻이 바뀌면 새 코드 추가 + 기존 코드 use_yn = N · M02 확정: SPOUSE 「배우자」 · CHILD 「아이」 | 그룹이 COMPANION_TYPE(동행자 = 나와 함께 가는 사람)이라 나를 포함하는 「부부」는 맞지 않고, 「동반」은 그룹 이름에 이미 있음. 「부부 여행」 같은 표현은 화면 문구로 처리. 숫자 코드는 뜻이 없어 매번 코드표를 봐야 하지만 영어 코드는 SQL · 로그에서 바로 읽히고 앱 상수로도 씀(R10) · 한글 인코딩 문제 없음 | cd_dtl COMPANION_TYPE · 표준정의서 v0.35 6절 M02 | 지수 |
| D17 | 2026-11-07 | 값 변경 이력은 공통 변경 이력 테이블 하나로 남김(테이블명 · 컬럼명 · 행 번호 · 이전 값 · 이후 값 · 변경 일시 · 변경 사용자) · 업무 로직에 쓰이는 이력(trip_status_hist · user_consent_hist)은 그대로 둠 · 테이블은 기존 값을 고치는 화면이 처음 생기는 묶음에서 생성 | 데이터가 바뀌면 이력을 남김. 테이블별 이력은 ERD 테이블 수가 크게 늘어남. 시나리오 1에는 아직 수정 화면이 없음 | 공통(새 테이블 1개) | 지수 |
| D18 | 2026-11-07 | trip_place_dtl.memo · review_polarity_cd 를 NULL 허용으로 변경 | A-05에서 「이 장소 기록」은 위치 · 시각만 임시 저장(draft_yn = Y)하고 메모 · 후기는 정차 후 A-06에서 입력. 빈 문자열은 「아직 입력 안 함」과 구분 안 됨 | trip_place_dtl | 지수 |
| D19 | 2026-11-07 | trip_place_dtl.user_place_nm(사용자장소명 · varchar(100) · NULL 허용) 추가 — 사용자가 부르는 이름은 개인 기록에 · place_mst 는 내비 POI · 공공데이터로 확인된 공식 장소만(직접 입력한 이름으로는 만들지 않음) · 확인되면 place_no 로 연결 | 같은 장소도 사람마다 부르는 이름이 다름(중앙시장 / 강릉 중앙시장). place_mst 는 여러 사용자의 후기 · 시설을 장소 단위로 모으는 공식 목록이라 1곳 1행 유지 | trip_place_dtl · place_mst | 지수 |
| D20 | 2026-11-07 | place_mst.poi_id(POI번호 · varchar(50)) · poi_provider(POI제공자 · varchar(30)) 추가 · DATA_SRC 코드 NAVI(내비 POI) 추가 · 같은 장소 판단은 poi_provider + poi_id | 내비(NaviHelper 목적지 · 경유지 · 최근 목적지 · 즐겨찾기)가 장소명 · 주소 · 좌표와 함께 POI 고유번호를 줌. 이름이 달라도 같은 장소로 묶어 1곳 1행 유지. 지도 회사마다 번호 체계가 달라 제공자도 저장 | place_mst · cd_dtl DATA_SRC | 지수 |
| D21 | 2026-11-07 | REVIEW_POLARITY 를 POSITIVE(좋았어요) · NEGATIVE(별로예요)로 변경(CAUTION 은 쓰지 않음) · A-06에 둘 중 하나를 고르는 버튼 추가 · 주의할 점 · 팁(웨이팅 있음 등)은 경험 태그로 | 그룹 이름이 극성(좋다 ↔ 나쁘다)인데 「주의」는 반대값이 아님. 평가와 주의점을 나누면 「좋았지만 웨이팅 있음」도 기록 가능. 지난 기록에 나중에 채울 수 없어 지금 입력받음 | trip_place_dtl.review_polarity_cd · cd_dtl REVIEW_POLARITY · tag_mst · 표준정의서 v0.35 6절 | 지수 |
| D22 | 2026-11-07 | 사용자가 고른 장소 유형은 trip_place_dtl.place_type_cd(장소유형코드 · NULL 허용)에 저장 · place_mst.place_type_cd 는 NULL 허용으로 변경(내비 장소는 유형 정보가 없음) | D19와 같은 방식 — 개인이 보는 값은 개인 기록에, 공식 장소는 확인된 값만 | trip_place_dtl · place_mst | 지수 |
| D23 | 2026-11-07 | 장소유형을 카테고리(대분류) → 유형(소분류) 2단계로 · 자연은 바다 · 산 · 계곡 · 호수 · 강 · 섬 · 공원으로 나눔 · 편의시설은 한국도로공사 휴게소 공공데이터 항목 기준 · 전기차 충전소 유형 코드는 EV_STATION(편의시설 EV_CHARGER 와 구분) · 단어 「카테고리(category)」는 약어 없이 | 바다 · 산처럼 사용자가 찾는 단위로 검색 | place_type_mst | 지수 |
| D24 | 2026-11-07 | 코드그룹을 가리키는 값(REVIEW_POLARITY 등)은 코드가 아니라 목록의 이름 → 코드그룹명(cd_grp_nm · 명 도메인)에 저장 · cd_grp_cd 는 쓰지 않음 · 한글 그룹명(후기 성격 등)은 DB 에 두지 않고 표준정의서 코드 표에만 | 코드 도메인 허용값은 cd_dtl 의 코드값뿐. 목록 이름은 개별 코드 테이블이라면 테이블명(DEPT)에 해당. 한글 이름은 화면에 쓰이지 않고 표준 용어로 충분 | cd_grp_mst · cd_dtl · 표준정의서 v0.4(반영은 v0.4 때) | 지수 |
| D25 | 2026-11-07 | 통합 공통 코드는 cd_mst 한 테이블(PK cd_no · 코드그룹명 + 코드 중복 불가) · 값이 많고 속성이 있는 코드는 개별 코드 테이블로: place_type_mst(PK place_type_cd · place_type_nm · place_category_cd · place_category_nm) · 다른 테이블은 place_type_cd 로 FK | 그룹에 딸린 속성이 코드그룹명뿐이라 그룹 테이블이 필요 없음. 장소유형은 값이 많고 카테고리 속성이 있음 | cd_mst · place_type_mst · trip_place_dtl · place_mst | 지수 |
| D26 | 2026-11-07 | 공통 변경 이력 테이블 data_change_hist(데이터변경이력): data_change_hist_no · table_nm · column_nm · row_no · before_value · after_value(변경 일시 · 사용자 = created_at · created_user_no) · 단어는 change(약어 없이) · 분류어 「값(VALUE · value)」 등록(VARCHAR(500)) — 다른 컬럼의 값을 형식과 상관없이 문자로 옮겨 담을 때만, 데이터변경이력의 before_value · after_value 에만 사용(다른 곳은 맞는 도메인 등록) | D17 실행. 「값」이 도메인을 못 찾을 때 붙이는 말이 되지 않게 사용처를 제한(R03 아이디와 같은 방식). 첫 사용: A-08 여행명 수정 | data_change_hist · 표준 단어 · 도메인 | 지수 |

## 확인 필요

- 장소 체류 시간(시나리오 1 이후): 회의(10/31)에서 "주차 시간으로 방문 여부 판단"으로 정했으나 trip_place_dtl에 도착 · 출발 시각 컬럼이 없음. 시나리오 1 화면(A-08 타임라인은 도착 시각만 표시)에는 쓰이지 않아 이후에 정함. 방법 후보: 경로점에 정차여부 추가 / trip_place_dtl에 도착 · 출발 시각 추가 / 경로점 시간 간격으로 계산(경로점은 이동할 때만 저장하므로 같은 위치에서 다음 점까지 걸린 시간 = 멈춘 시간). 정차 판단 재료(기어 P · 속도)는 Vehicle SDK에 있음
- 팀 리드 확인: user_mst.created_user_no NOT NULL(ERD v0.3) — 첫 사용자 · 시드 데이터의 생성자를 위해 시스템 계정(user_no 1, login_id system)을 임시로 둠. 시스템 계정 유지 또는 NULL 허용 중 결정 필요
- 팀 리드 · 기보 확인: user_mst.password_hash NOT NULL(ERD v0.3) — Pleos 로그인 계정은 비밀번호가 없어 임시로 '!'(비밀번호 로그인 불가 표시)를 넣음. 로그인 방식에 맞춰 NULL 허용 여부 결정 필요
- 동행자 선택 규칙(D12 가안): 데이터 표준이 아닌 화면 · 업무 규칙이라 결정자 확인 필요. 지금은 가안대로 서버(422) · 앱 둘 다 검사
- 나중에 보완(프로토타입 이후): place_mst.place_type_cd 가 비어 있는 내비 장소는 시나리오 2 유형 검색(예: 휴게소 찾아줘)에 안 잡힘 → 사용자 기록에서 가장 많이 고른 유형으로 채우는 방식 검토
