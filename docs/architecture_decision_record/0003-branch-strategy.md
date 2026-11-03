# ADR 0003 — 브랜치 전략: develop · <기능명>/main · spec · impl · bug(2026-10-30 승인)

> 템플릿 주: 하네스 원본 저장소에서 내린 결정을 근거로 옮겨 온 기록. 본문의 이슈 · PR 번호와 날짜는 원본 저장소 것이며, 이 저장소의 새 결정은 다음 번호부터 `0000-template.md` 꼴로 씀.

> 하네스 첫 설계의 「main 하나 + `feat/<area>-<slug>`」를 대체. 원본 규칙은 `team-workflow` 2 · 3절.

## 결정

```
main ◀─ 릴리스 PR(사람) ─ develop ◀─ PR(사람) ─ <기능명>/main ◀─ PR(사람) ─ <기능명>/spec
                                │                                ◀─ PR(사람) ─ <기능명>/impl
                                ◀─ PR(사람) ─ bug/<이슈번호>-<짧은이름>
                                ◀─ PR(사람) ─ harness/<짧은이름>
```

- 기능: develop → `<기능명>/main`(에이전트가 처음 한 번 만듦) → `<기능명>/spec` PR → 사람 머지 → 따로 지시가 없으면 바로 `<기능명>/impl` → PR → 사람 머지 → `<기능명>/main` → develop PR → 여러 사람 피드백 뒤 사람 머지.
- 버그: develop → `bug/<n>-<짧은이름>` → PR → develop.
- 기능명은 영문 짧은 명사(`login` · `navigation`). 스펙은 `docs/spec/<이슈>-<기능명>.md` 한 폴더.
- 라벨은 쓰지 않음 · 단계는 PR 과 `state.md` 로 앎.

## 이유

| 무엇 | 이유 |
|---|---|
| 기능명 브랜치 | 이슈 번호보다 사람이 알아보기 쉬움 · 기능 단위로 spec · impl · 피드백을 한 줄기에 모음 |
| `<기능명>/main` 을 사이에 둠 | 스펙 확인 · 구현 확인을 각각 PR 하나로 · develop 은 기능이 다 끝난 뒤 한 번만 바뀜 |
| impl 브랜치에서 사람이 직접 써 봄 | 구조 리뷰와 앱 시험을 머지 전에 |
| 스펙 머지 → 바로 구현 | 사람이 다시 지시하지 않아도 흐름이 이어짐 · 멈추려면 「멈춰」 |

## 기계로 막는 것

| 장치 | 막는 것 |
|---|---|
| deny(`.claude/settings.json`) | `git push … main` · `git push … develop` 꼴 |
| guard(`.claude/hooks/guard.py`) | main · develop 푸시 · 삭제(모든 꼴) · 원격에 이미 있는 `<기능명>/main` 푸시 · `<기능명>/main` 삭제 · 인자 없는 push 가 그 브랜치로 가는 것 |

- `<기능명>/main` 을 처음 만드는 푸시는 통과(`git ls-remote` 로 원격에 없을 때만).
- 시험: `.claude/hooks/tests/test_guard.py` 의 `test_feature_main`.

## 남은 것

- 머지 기다리기는 세션이 열려 있을 때만(60초마다 PR 상태 확인 · 최대 8시간). 닫혀 있으면 「#n 이어서」.
- CI 관련 문구(team-workflow 8 · 9절 등)는 아직 남아 있음 — 따로 정리.
